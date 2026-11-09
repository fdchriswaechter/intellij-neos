package flowUnusedMethodSuppressor;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;

import de.vette.idea.neos.lang.php.inspections.suppression.FlowUnusedMethodSuppressor;

import helpers.NeosProjectDescriptor;

/**
 * Testcase for suppressing the unused inspection on methods Flow calls by convention
 */
public class FlowUnusedMethodSuppressorTest extends BasePlatformTestCase {

    private final FlowUnusedMethodSuppressor suppressor = new FlowUnusedMethodSuppressor();

    @Override
    protected String getTestDataPath() {
        return "testData/flowUnusedMethodSuppressor";
    }

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new NeosProjectDescriptor();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.configureByFiles("Classes/Controllers.php", "Flow/Controllers.php");
    }

    public void testActionsInDirectSubclassAreSuppressed() {
        assertSuppressed("\\Vendor\\Package\\Controller\\DirectController", "directAction");
    }

    public void testActionsInIndirectSubclassAreSuppressed() {
        assertSuppressed("\\Vendor\\Package\\Controller\\InheritedController", "fooAction");
        assertSuppressed("\\Vendor\\Package\\Controller\\InheritedController", "initializeFooAction");
        assertSuppressed("\\Vendor\\Package\\Controller\\AbstractBaseController", "baseAction");
    }

    public void testCommandsInIndirectSubclassAreSuppressed() {
        assertSuppressed("\\Vendor\\Package\\Command\\InheritedCommandController", "runCommand");
    }

    public void testMagicMethodsAreSuppressedWithoutParentClass() {
        assertSuppressed("\\Vendor\\Package\\Controller\\NotAController", "initializeObject");
    }

    public void testOtherMethodsAreNotSuppressed() {
        assertNotSuppressed("\\Vendor\\Package\\Controller\\InheritedController", "helper");
        assertNotSuppressed("\\Vendor\\Package\\Controller\\InheritedController", "protectedAction");
        assertNotSuppressed("\\Vendor\\Package\\Controller\\NotAController", "fooAction");
    }

    public void testOtherInspectionsAreNotSuppressed() {
        Method method = findMethod("\\Vendor\\Package\\Controller\\InheritedController", "fooAction");
        assertFalse(suppressor.isSuppressedFor(method, "PhpMissingReturnTypeInspection"));
    }

    private void assertSuppressed(String classFqn, String methodName) {
        assertTrue(classFqn + "::" + methodName + " should be suppressed",
                suppressor.isSuppressedFor(findMethod(classFqn, methodName), "PhpUnused"));
    }

    private void assertNotSuppressed(String classFqn, String methodName) {
        assertFalse(classFqn + "::" + methodName + " should not be suppressed",
                suppressor.isSuppressedFor(findMethod(classFqn, methodName), "PhpUnused"));
    }

    private Method findMethod(String classFqn, String methodName) {
        PhpClass phpClass = PhpIndex.getInstance(getProject()).getClassesByFQN(classFqn).iterator().next();
        Method method = phpClass.findOwnMethodByName(methodName);
        assertNotNull(classFqn + "::" + methodName + " not found", method);
        return method;
    }
}
