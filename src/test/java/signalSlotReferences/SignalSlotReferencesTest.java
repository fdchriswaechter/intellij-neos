package signalSlotReferences;

import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.inspections.deadcode.PhpUnusedDeclarationInspection;
import com.jetbrains.php.lang.inspections.deadcode.local.PhpUnusedSymbolsInspection;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;

import de.vette.idea.neos.lang.php.references.signalSlot.SignalMethodReference;
import de.vette.idea.neos.lang.php.references.signalSlot.SlotMethodReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import helpers.NeosProjectDescriptor;

/**
 * Testcase for references from signal/slot connections to the connected PHP methods
 */
public class SignalSlotReferencesTest extends BasePlatformTestCase {

    private static final String SLOT_CLASS_FQN = "\\Vendor\\Package\\Service\\ExampleService";
    private static final String SIGNAL_CLASS_FQN = "\\Vendor\\Package\\Controller\\ExampleController";

    @Override
    protected String getTestDataPath() {
        return "testData/signalSlotReferences";
    }

    @Override
    protected boolean isWriteActionRequired() {
        return false;
    }

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new NeosProjectDescriptor();
    }

    public void testSignalNameAndSlotMethodAreResolved() {
        loadFixture("Classes/Package.php");

        assertResolvesToMethod(caretOffset(0), SignalMethodReference.class, SIGNAL_CLASS_FQN, "emitSomethingHappened");
        assertResolvesToMethod(caretOffset(1), SlotMethodReference.class, SLOT_CLASS_FQN, "handleSomethingHappened");
    }

    public void testSignalMethodNameAndSlotObjectAreResolved() {
        loadFixture("Classes/PackageWithSignalMethodName.php");

        assertResolvesToMethod(caretOffset(0), SignalMethodReference.class, SIGNAL_CLASS_FQN, "emitSomethingHappened");
        assertResolvesToMethod(caretOffset(1), SlotMethodReference.class, SLOT_CLASS_FQN, "handleSomethingHappened");
    }

    public void testSlotIsFoundWhenSearchingForUsages() {
        loadFixture("Classes/Package.php");

        Method slot = findMethod(SLOT_CLASS_FQN, "handleSomethingHappened");
        Collection<PsiReference> references = ReferencesSearch
                .search(slot, GlobalSearchScope.allScope(getProject()))
                .findAll();

        assertTrue(
                "Slot method is not referenced by the connect() call",
                references.stream().anyMatch(reference -> reference instanceof SlotMethodReference)
        );
    }

    public void testConnectedSlotIsNotHighlightedAsUnused() {
        myFixture.enableInspections(new PhpUnusedDeclarationInspection(), new PhpUnusedSymbolsInspection());
        myFixture.configureByFiles(
                "Classes/Service/ExampleService.php",
                "Classes/PackageForHighlighting.php",
                "Classes/Controller/ExampleController.php",
                "Flow/Core/Bootstrap.php",
                "Flow/SignalSlot/Dispatcher.php"
        );
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(getProject());

        List<String> unusedSymbols = myFixture.doHighlighting().stream()
                .filter(info -> info.type.getAttributesKey() == CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES)
                .map(info -> myFixture.getFile().getText().substring(info.getStartOffset(), info.getEndOffset()))
                .toList();

        // makes sure the unused declaration pass runs at all
        assertContainsElements(unusedSymbols, "neverConnectedMethod");
        assertDoesntContain(unusedSymbols, "handleSomethingHappened");
    }

    public void testConnectOnOtherClassesIsIgnored() {
        loadFixture("Classes/UnrelatedConnect.php");

        StringLiteralExpression literal = literalAt(caretOffset(0));
        assertEmpty(
                "connect() of an unrelated class should not get references",
                Arrays.stream(literal.getReferences())
                        .filter(reference -> reference instanceof SlotMethodReference)
                        .toList()
        );
    }

    private void loadFixture(String fileNameToInclude) {
        myFixture.configureByFiles(
                fileNameToInclude,
                "Classes/Controller/ExampleController.php",
                "Classes/Service/ExampleService.php",
                "Flow/Core/Bootstrap.php",
                "Flow/SignalSlot/Dispatcher.php"
        );

        // actually trigger indexing
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(getProject());
    }

    private int caretOffset(int index) {
        return myFixture.getEditor().getCaretModel().getAllCarets().get(index).getOffset();
    }

    private StringLiteralExpression literalAt(int offset) {
        PsiElement element = myFixture.getFile().findElementAt(offset);
        StringLiteralExpression literal = PsiTreeUtil.getParentOfType(element, StringLiteralExpression.class, false);

        assertNotNull("No string literal at caret", literal);
        return literal;
    }

    private Method findMethod(String classFqn, String methodName) {
        Collection<PhpClass> classes = PhpIndex.getInstance(getProject()).getClassesByFQN(classFqn);
        assertFalse("Class " + classFqn + " was not indexed", classes.isEmpty());

        Method method = classes.iterator().next().findMethodByName(methodName);
        assertNotNull("Method " + methodName + " not found in " + classFqn, method);

        return method;
    }

    private void assertResolvesToMethod(int offset, Class<? extends PsiReference> referenceType, String classFqn, String methodName) {
        PsiReference[] references = Arrays.stream(literalAt(offset).getReferences())
                .filter(referenceType::isInstance)
                .toArray(PsiReference[]::new);

        assertEquals("Expected exactly one " + referenceType.getSimpleName(), 1, references.length);

        ResolveResult[] resolveResults = ((PsiPolyVariantReference) references[0]).multiResolve(false);
        assertEquals("Not the right number of resolve results", 1, resolveResults.length);

        Method method = assertInstanceOf(resolveResults[0].getElement(), Method.class);
        assertEquals("Method name does not match", methodName, method.getName());
        assertNotNull("Method has no containing class", method.getContainingClass());
        assertEquals("Containing class does not match", classFqn, method.getContainingClass().getFQN());
    }
}
