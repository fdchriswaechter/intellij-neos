package de.vette.idea.neos.lang.php.inspections.suppression;

import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import de.vette.idea.neos.NeosProjectService;
import de.vette.idea.neos.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Suppresses the "unused" inspection for public methods which Flow calls by convention:
 * controller actions, CLI commands and object lifecycle methods.
 */
public class FlowUnusedMethodSuppressor implements InspectionSuppressor {

    static final String ACTION_CONTROLLER_FQN = "\\Neos\\Flow\\Mvc\\Controller\\ActionController";
    static final String COMMAND_CONTROLLER_FQN = "\\Neos\\Flow\\Cli\\CommandController";

    static final Set<String> MAGIC_METHODS = Set.of("initializeObject", "shutdownObject", "injectSettings");

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
        if (!toolId.equals("PhpUnused")
                || !(element instanceof Method method)
                || !method.getAccess().isPublic()
                || !NeosProjectService.isEnabled(element.getProject())) {
            return false;
        }

        PhpClass phpClass = method.getContainingClass();
        if (phpClass == null) {
            return false;
        }

        String name = method.getName();
        if (MAGIC_METHODS.contains(name)) {
            return true;
        }

        // Check the whole class hierarchy, as controllers often extend a project specific base controller
        return name.endsWith("Action") && PhpElementsUtil.isInstanceOf(phpClass, ACTION_CONTROLLER_FQN)
                || name.endsWith("Command") && PhpElementsUtil.isInstanceOf(phpClass, COMMAND_CONTROLLER_FQN);
    }

    @Override
    public SuppressQuickFix @NotNull [] getSuppressActions(@Nullable PsiElement element, @NotNull String toolId) {
        return SuppressQuickFix.EMPTY_ARRAY;
    }
}
