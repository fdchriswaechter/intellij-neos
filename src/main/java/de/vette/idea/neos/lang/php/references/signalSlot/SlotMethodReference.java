package de.vette.idea.neos.lang.php.references.signalSlot;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference from the slot method name of a {@code Dispatcher::connect()} call to the method of the
 * slot class. Besides navigation this makes the slot show up as used, so it is not reported as dead
 * code by the "Unused declaration" inspection.
 */
public class SlotMethodReference extends PsiPolyVariantReferenceBase<StringLiteralExpression> {

    public SlotMethodReference(@NotNull StringLiteralExpression element) {
        super(element, ElementManipulators.getValueTextRange(element), true);
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        String methodName = getValue();
        if (methodName.isEmpty()) {
            return ResolveResult.EMPTY_ARRAY;
        }

        List<ResolveResult> results = new ArrayList<>();
        for (PhpClass slotClass : SignalSlotUtil.getSlotClasses(getElement())) {
            Method method = slotClass.findMethodByName(methodName);
            if (method != null) {
                results.add(new PsiElementResolveResult(method));
            }
        }

        return results.toArray(ResolveResult.EMPTY_ARRAY);
    }

    @Override
    public Object @NotNull [] getVariants() {
        List<LookupElement> variants = new ArrayList<>();
        for (PhpClass slotClass : SignalSlotUtil.getSlotClasses(getElement())) {
            for (Method method : slotClass.getMethods()) {
                if (!method.getAccess().isPublic() || method.isAbstract() || method.getName().startsWith("__")) {
                    continue;
                }

                variants.add(LookupElementBuilder.create(method.getName())
                        .withIcon(method.getIcon(0))
                        .withTypeText(slotClass.getName()));
            }
        }

        return variants.toArray();
    }
}
