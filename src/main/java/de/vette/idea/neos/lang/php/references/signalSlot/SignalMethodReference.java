package de.vette.idea.neos.lang.php.references.signalSlot;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reference from the signal name of a {@code Dispatcher::connect()} call to the emitting method of
 * the signal class. Signals are connected by their un-prefixed name ("nodeUpdated"), while the
 * emitting method carries the "emit" prefix ("emitNodeUpdated"); both spellings are resolved.
 */
public class SignalMethodReference extends PsiPolyVariantReferenceBase<StringLiteralExpression> {

    public SignalMethodReference(@NotNull StringLiteralExpression element) {
        super(element, ElementManipulators.getValueTextRange(element), true);
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        List<String> methodNames = SignalSlotUtil.getSignalMethodNames(getValue());
        if (methodNames.isEmpty()) {
            return ResolveResult.EMPTY_ARRAY;
        }

        Set<Method> methods = new LinkedHashSet<>();
        for (PhpClass signalClass : SignalSlotUtil.getSignalClasses(getElement())) {
            for (String methodName : methodNames) {
                Method method = signalClass.findMethodByName(methodName);
                if (method != null) {
                    methods.add(method);
                    break;
                }
            }
        }

        return methods.stream().map(PsiElementResolveResult::new).toArray(ResolveResult[]::new);
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        // keep the un-prefixed spelling when the signal was connected without the "emit" prefix
        if (!getValue().startsWith(SignalSlotUtil.SIGNAL_METHOD_PREFIX)) {
            return super.handleElementRename(SignalSlotUtil.getSignalName(newElementName));
        }

        return super.handleElementRename(newElementName);
    }

    @Override
    public Object @NotNull [] getVariants() {
        List<LookupElement> variants = new ArrayList<>();
        for (PhpClass signalClass : SignalSlotUtil.getSignalClasses(getElement())) {
            for (Method method : signalClass.getMethods()) {
                String methodName = method.getName();
                if (!methodName.startsWith(SignalSlotUtil.SIGNAL_METHOD_PREFIX)
                        || methodName.equals(SignalSlotUtil.SIGNAL_METHOD_PREFIX)) {
                    continue;
                }

                variants.add(LookupElementBuilder.create(SignalSlotUtil.getSignalName(methodName))
                        .withIcon(method.getIcon(0))
                        .withTypeText(signalClass.getName()));
            }
        }

        return variants.toArray();
    }
}
