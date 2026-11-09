package de.vette.idea.neos.lang.php.references.signalSlot;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.ClassConstantReference;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.PhpTypedElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import de.vette.idea.neos.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Helpers for signal/slot connections as they are registered in the {@code boot()} method of a
 * Flow package via {@code \Neos\Flow\SignalSlot\Dispatcher::connect()}:
 *
 * <pre>
 * $dispatcher->connect(ExampleController::class, 'somethingHappened', ExampleService::class, 'handleSomethingHappened');
 * </pre>
 */
public final class SignalSlotUtil {

    public static final String DISPATCHER_FQN = "\\Neos\\Flow\\SignalSlot\\Dispatcher";
    public static final String CONNECT_METHOD = "connect";
    public static final String SIGNAL_METHOD_PREFIX = "emit";

    private static final int SIGNAL_CLASS_INDEX = 0;
    private static final String SIGNAL_CLASS_PARAMETER = "signalClassName";
    private static final int SIGNAL_NAME_INDEX = 1;
    private static final String SIGNAL_NAME_PARAMETER = "signalName";
    private static final int SLOT_CLASS_INDEX = 2;
    private static final String SLOT_CLASS_PARAMETER = "slotClassNameOrObject";
    private static final int SLOT_METHOD_INDEX = 3;
    private static final String SLOT_METHOD_PARAMETER = "slotMethodName";

    /**
     * Types that tell us nothing about the class behind an expression. If a receiver only has such
     * types we cannot prove that it is the dispatcher - but we cannot rule it out either.
     */
    private static final Set<String> UNKNOWN_TYPES = Set.of("mixed", "object", "static", "null", "void", "?", "");

    private SignalSlotUtil() {
    }

    /**
     * @return the parameter list of the surrounding {@code Dispatcher::connect()} call or null if
     *         the given element is not an argument of such a call
     */
    @Nullable
    public static ParameterList getConnectParameterList(@NotNull PsiElement argument) {
        if (!(argument.getParent() instanceof ParameterList parameterList)) {
            return null;
        }

        if (!(parameterList.getParent() instanceof MethodReference methodReference)) {
            return null;
        }

        if (!CONNECT_METHOD.equals(methodReference.getName()) || !isDispatcherReference(methodReference)) {
            return null;
        }

        return parameterList;
    }

    /**
     * Resolves an argument either by its PHP 8 named argument or by its position.
     * Replacement for the internal {@code ParameterList#getParameter(String, int)}.
     */
    @Nullable
    private static PsiElement getArgument(@NotNull ParameterList parameterList, @NotNull String name, int index) {
        PsiElement[] arguments = parameterList.getParameters();
        for (PsiElement argument : arguments) {
            if (name.equals(getArgumentName(argument))) {
                return argument;
            }
        }

        // positional arguments must precede named ones in PHP
        if (index < arguments.length && getArgumentName(arguments[index]) == null) {
            return arguments[index];
        }

        return null;
    }

    /**
     * @return the name of a named argument ({@code name: value}) or null for positional arguments
     */
    @Nullable
    private static String getArgumentName(@NotNull PsiElement argument) {
        PsiElement colon = PsiTreeUtil.skipWhitespacesAndCommentsBackward(argument);
        if (colon == null || colon.getNode().getElementType() != PhpTokenTypes.opCOLON) {
            return null;
        }

        PsiElement name = PsiTreeUtil.skipWhitespacesAndCommentsBackward(colon);
        if (name == null || name.getNode().getElementType() != PhpTokenTypes.IDENTIFIER) {
            return null;
        }

        return name.getText();
    }

    public static boolean isSignalNameArgument(@NotNull PsiElement argument, @NotNull ParameterList parameterList) {
        return argument == getArgument(parameterList, SIGNAL_NAME_PARAMETER, SIGNAL_NAME_INDEX);
    }

    public static boolean isSlotMethodArgument(@NotNull PsiElement argument, @NotNull ParameterList parameterList) {
        return argument == getArgument(parameterList, SLOT_METHOD_PARAMETER, SLOT_METHOD_INDEX);
    }

    /**
     * @return the classes emitting the signal of the {@code connect()} call the given element belongs to
     */
    public static Collection<PhpClass> getSignalClasses(@NotNull PsiElement argument) {
        ParameterList parameterList = getConnectParameterList(argument);
        if (parameterList == null) {
            return Collections.emptyList();
        }

        return resolveClasses(getArgument(parameterList, SIGNAL_CLASS_PARAMETER, SIGNAL_CLASS_INDEX));
    }

    /**
     * @return the classes providing the slot of the {@code connect()} call the given element belongs to
     */
    public static Collection<PhpClass> getSlotClasses(@NotNull PsiElement argument) {
        ParameterList parameterList = getConnectParameterList(argument);
        if (parameterList == null) {
            return Collections.emptyList();
        }

        return resolveClasses(getArgument(parameterList, SLOT_CLASS_PARAMETER, SLOT_CLASS_INDEX));
    }

    /**
     * Signals are emitted by methods prefixed with "emit", while {@code connect()} is usually called
     * with the un-prefixed name.
     *
     * @return the possible method names of the signal, most specific first
     */
    public static List<String> getSignalMethodNames(@NotNull String signalName) {
        if (signalName.isEmpty()) {
            return Collections.emptyList();
        }

        String prefixed = SIGNAL_METHOD_PREFIX + Character.toUpperCase(signalName.charAt(0)) + signalName.substring(1);
        if (signalName.startsWith(SIGNAL_METHOD_PREFIX)) {
            return List.of(signalName, prefixed);
        }

        return List.of(prefixed, signalName);
    }

    /**
     * @return the signal name as it is passed to {@code connect()} for the given signal method name
     */
    public static String getSignalName(@NotNull String methodName) {
        if (!methodName.startsWith(SIGNAL_METHOD_PREFIX) || methodName.length() <= SIGNAL_METHOD_PREFIX.length()) {
            return methodName;
        }

        String name = methodName.substring(SIGNAL_METHOD_PREFIX.length());
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Resolves the classes an argument of {@code connect()} may refer to. Handles {@code Foo::class},
     * plain class name strings and objects (like {@code $this} or an injected service).
     */
    public static Collection<PhpClass> resolveClasses(@Nullable PsiElement argument) {
        if (argument == null) {
            return Collections.emptyList();
        }

        Project project = argument.getProject();
        PhpIndex phpIndex = PhpIndex.getInstance(project);

        if (argument instanceof ClassConstantReference classConstantReference) {
            if (!"class".equals(classConstantReference.getName())) {
                return Collections.emptyList();
            }

            if (classConstantReference.getClassReference() instanceof ClassReference classReference) {
                String fqn = classReference.getFQN();
                if (fqn != null) {
                    return phpIndex.getAnyByFQN(fqn);
                }
            }

            return Collections.emptyList();
        }

        if (argument instanceof StringLiteralExpression stringLiteral) {
            String contents = stringLiteral.getContents();
            return contents.isEmpty() ? Collections.emptyList() : phpIndex.getAnyByFQN(normalizeFqn(contents));
        }

        if (argument instanceof PhpTypedElement typedElement) {
            List<PhpClass> classes = new ArrayList<>();
            for (String type : typedElement.getType().global(project).getTypes()) {
                if (isUnknownType(type)) {
                    continue;
                }

                classes.addAll(phpIndex.getAnyByFQN(normalizeFqn(type)));
            }

            return classes;
        }

        return Collections.emptyList();
    }

    /**
     * Checks whether a method call is made on the signal/slot dispatcher. Receivers we cannot type
     * (because Flow is not indexed for example) are accepted, the resolved references stay empty in
     * that case anyway.
     */
    private static boolean isDispatcherReference(@NotNull MethodReference methodReference) {
        PhpExpression classReference = methodReference.getClassReference();
        if (classReference == null) {
            return false;
        }

        Project project = methodReference.getProject();
        PhpType type = classReference.getType().global(project);
        boolean onlyUnknownTypes = true;

        for (String candidate : type.getTypes()) {
            if (isUnknownType(candidate)) {
                continue;
            }

            onlyUnknownTypes = false;
            if (PhpElementsUtil.isInstanceOf(project, candidate, DISPATCHER_FQN)) {
                return true;
            }
        }

        return onlyUnknownTypes;
    }

    private static boolean isUnknownType(@NotNull String type) {
        // unresolvable types are kept as signatures like "#M#C\Foo.bar" by PhpType
        if (type.startsWith("#")) {
            return true;
        }

        String name = normalizeFqn(type).substring(1).toLowerCase(Locale.ROOT);
        return UNKNOWN_TYPES.contains(name);
    }

    private static String normalizeFqn(@NotNull String fqn) {
        return fqn.startsWith("\\") ? fqn : "\\" + fqn;
    }
}
