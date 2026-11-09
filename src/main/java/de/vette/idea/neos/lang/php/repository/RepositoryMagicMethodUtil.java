package de.vette.idea.neos.lang.php.repository;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.ClassConstantReference;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import de.vette.idea.neos.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Helpers for the magic finder methods Flow repositories provide through __call:
 * findBy&lt;Property&gt;, findOneBy&lt;Property&gt; and countBy&lt;Property&gt;
 */
public final class RepositoryMagicMethodUtil {

    public static final String REPOSITORY_INTERFACE_FQN = "\\Neos\\Flow\\Persistence\\RepositoryInterface";
    public static final String QUERY_RESULT_INTERFACE_FQN = "\\Neos\\Flow\\Persistence\\QueryResultInterface";

    public enum MagicKind {
        FIND_ONE('o'),
        COUNT('c'),
        FIND('f');

        public final char code;

        MagicKind(char code) {
            this.code = code;
        }

        @Nullable
        public static MagicKind fromCode(char code) {
            for (MagicKind kind : values()) {
                if (kind.code == code) {
                    return kind;
                }
            }
            return null;
        }
    }

    private RepositoryMagicMethodUtil() {
    }

    /**
     * Mirrors the checks in Flow's Repository::__call, which requires at least one character after the prefix
     */
    @Nullable
    public static MagicKind kindOf(@Nullable String methodName) {
        if (methodName == null) {
            return null;
        }
        if (methodName.length() > 9 && methodName.startsWith("findOneBy")) {
            return MagicKind.FIND_ONE;
        }
        if (methodName.length() > 7 && methodName.startsWith("countBy")) {
            return MagicKind.COUNT;
        }
        if (methodName.length() > 6 && methodName.startsWith("findBy")) {
            return MagicKind.FIND;
        }
        return null;
    }

    public static boolean isRepository(@NotNull PhpClass phpClass) {
        return PhpElementsUtil.isInstanceOf(phpClass, REPOSITORY_INTERFACE_FQN);
    }

    /**
     * Repository classes for the given type, which do not declare the method themselves
     */
    @NotNull
    public static List<PhpClass> repositoriesHandlingMagicMethod(@NotNull Project project, @NotNull PhpType type, @NotNull String methodName) {
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        PhpType globalType = type.isComplete() ? type : phpIndex.completeType(project, type, new HashSet<>());

        List<PhpClass> result = new ArrayList<>();
        for (String fqn : globalType.getTypes()) {
            if (!fqn.startsWith("\\")) {
                continue;
            }
            for (PhpClass phpClass : phpIndex.getAnyByFQN(fqn)) {
                if (phpClass.findMethodByName(methodName) == null && isRepository(phpClass)) {
                    result.add(phpClass);
                }
            }
        }
        return result;
    }

    /**
     * Resolves the entity class name like Flow does: the ENTITY_CLASSNAME constant if defined,
     * otherwise by convention (Vendor\Package\Domain\Repository\FooRepository => Vendor\Package\Domain\Model\Foo)
     */
    @Nullable
    public static String entityClassFqn(@NotNull PhpClass repository) {
        Field constant = repository.findFieldByName("ENTITY_CLASSNAME", true);
        if (constant != null && constant.isConstant()) {
            PsiElement value = constant.getDefaultValue();
            if (value instanceof ClassConstantReference classConstant
                    && classConstant.getClassReference() instanceof PhpReference classReference) {
                return classReference.getFQN();
            }
            if (value instanceof StringLiteralExpression literal) {
                String name = literal.getContents().replace("\\\\", "\\");
                return name.startsWith("\\") ? name : "\\" + name;
            }
        }

        String fqn = repository.getFQN();
        if (!fqn.endsWith("Repository")) {
            return null;
        }
        // The trailing "Repository" has no backslash after it, so the namespace replacement leaves it intact
        String entity = fqn.replace("\\Repository\\", "\\Model\\");
        return entity.substring(0, entity.length() - "Repository".length());
    }

    @NotNull
    public static Collection<String> entityClassFqns(@NotNull Collection<PhpClass> repositories) {
        List<String> result = new ArrayList<>();
        for (PhpClass repository : repositories) {
            String entity = entityClassFqn(repository);
            if (entity != null) {
                result.add(entity);
            }
        }
        return result;
    }
}
