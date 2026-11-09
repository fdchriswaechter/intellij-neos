package repositoryMagicMethods;

import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.php.lang.inspections.PhpUndefinedMethodInspection;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;

import helpers.NeosProjectDescriptor;

import java.util.Set;

/**
 * Testcase for the magic finder methods of Flow repositories (findBy*, findOneBy*, countBy*)
 */
public class RepositoryMagicMethodsTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "testData/repositoryMagicMethods";
    }

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new NeosProjectDescriptor();
    }

    private void loadFixture(String usageFile) {
        myFixture.configureByFiles(usageFile, "Classes/Domain.php", "Flow/Persistence.php");
    }

    public void testMagicMethodsAreNotReportedAsUndefined() {
        loadFixture("Classes/Usage.php");
        myFixture.enableInspections(new PhpUndefinedMethodInspection());
        myFixture.checkHighlighting(false, false, true);
    }

    public void testFindOneByReturnsEntityByConvention() {
        Set<String> types = typesOf("findOneByName");
        assertContainsElements(types, "\\Vendor\\Package\\Domain\\Model\\Company", PhpType._NULL);
    }

    public void testFindOneByReturnsEntityFromInheritedEntityClassnameConstant() {
        assertContainsElements(typesOf("findOneByTitle"), "\\Vendor\\Package\\Domain\\Model\\Job");
        assertDoesntContain(typesOf("findOneByTitle"), "\\Vendor\\Package\\Domain\\Model\\JobOffer");
    }

    public void testFindByReturnsQueryResult() {
        assertContainsElements(typesOf("findByMemberId"), "\\Neos\\Flow\\Persistence\\QueryResultInterface");
    }

    public void testCountByReturnsInt() {
        assertContainsElements(typesOf("countByActive"), PhpType._INT);
    }

    public void testDeclaredMethodKeepsItsOwnType() {
        Set<String> types = typesOf("findByCustomCriteria");
        assertContainsElements(types, PhpType._ARRAY);
        assertDoesntContain(types, "\\Neos\\Flow\\Persistence\\QueryResultInterface");
    }

    public void testNonRepositoryGetsNoType() {
        assertDoesntContain(typesOf("findByFoo"), "\\Neos\\Flow\\Persistence\\QueryResultInterface");
    }

    private Set<String> typesOf(String methodName) {
        // Highlighting markup would break the PSI, so the type tests use a markup-free copy of Usage.php
        if (myFixture.getFile() == null) {
            loadFixture("Classes/Types.php");
        }
        for (MethodReference reference : PsiTreeUtil.findChildrenOfType(myFixture.getFile(), MethodReference.class)) {
            if (methodName.equals(reference.getName())) {
                return reference.getType().global(getProject()).getTypes();
            }
        }
        fail("Method reference " + methodName + " not found");
        return Set.of();
    }
}
