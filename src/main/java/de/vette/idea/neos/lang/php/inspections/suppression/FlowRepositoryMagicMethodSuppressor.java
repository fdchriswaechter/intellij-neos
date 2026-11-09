package de.vette.idea.neos.lang.php.inspections.suppression;

import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import de.vette.idea.neos.NeosProjectService;
import de.vette.idea.neos.lang.php.repository.RepositoryMagicMethodUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Suppresses "Method not found" for the magic finder methods of Flow repositories (findBy*, findOneBy*, countBy*)
 */
public class FlowRepositoryMagicMethodSuppressor implements InspectionSuppressor {

    static final String UNDEFINED_METHOD_INSPECTION = "PhpUndefinedMethodInspection";

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
        if (!toolId.equals(UNDEFINED_METHOD_INSPECTION) || !NeosProjectService.isEnabled(element.getProject())) {
            return false;
        }

        MethodReference reference = PsiTreeUtil.getParentOfType(element, MethodReference.class, false);
        if (reference == null || RepositoryMagicMethodUtil.kindOf(reference.getName()) == null) {
            return false;
        }

        if (!(reference.getClassReference() instanceof PhpExpression qualifier)) {
            return false;
        }

        return !RepositoryMagicMethodUtil.repositoriesHandlingMagicMethod(
                element.getProject(), qualifier.getType(), reference.getName()).isEmpty();
    }

    @Override
    public SuppressQuickFix @NotNull [] getSuppressActions(@Nullable PsiElement element, @NotNull String toolId) {
        return SuppressQuickFix.EMPTY_ARRAY;
    }
}
