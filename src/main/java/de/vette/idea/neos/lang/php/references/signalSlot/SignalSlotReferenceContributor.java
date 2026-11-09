package de.vette.idea.neos.lang.php.references.signalSlot;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.vette.idea.neos.NeosProjectService;
import org.jetbrains.annotations.NotNull;

/**
 * Adds references from the signal and slot names of {@code \Neos\Flow\SignalSlot\Dispatcher::connect()}
 * calls to the corresponding PHP methods.
 */
public class SignalSlotReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(StringLiteralExpression.class),
                new SignalSlotReferenceProvider()
        );
    }

    private static class SignalSlotReferenceProvider extends PsiReferenceProvider {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            if (!NeosProjectService.isEnabled(element)) {
                return PsiReference.EMPTY_ARRAY;
            }

            if (!(element instanceof StringLiteralExpression stringLiteral) || stringLiteral.getContents().isEmpty()) {
                return PsiReference.EMPTY_ARRAY;
            }

            ParameterList parameterList = SignalSlotUtil.getConnectParameterList(stringLiteral);
            if (parameterList == null) {
                return PsiReference.EMPTY_ARRAY;
            }

            if (SignalSlotUtil.isSlotMethodArgument(stringLiteral, parameterList)) {
                return new PsiReference[]{new SlotMethodReference(stringLiteral)};
            }

            if (SignalSlotUtil.isSignalNameArgument(stringLiteral, parameterList)) {
                return new PsiReference[]{new SignalMethodReference(stringLiteral)};
            }

            return PsiReference.EMPTY_ARRAY;
        }
    }
}
