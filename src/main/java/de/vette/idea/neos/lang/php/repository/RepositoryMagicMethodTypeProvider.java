package de.vette.idea.neos.lang.php.repository;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import de.vette.idea.neos.NeosProjectService;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Provides return types for the magic finder methods of Flow repositories:
 * findOneBy* returns the entity (or null), findBy* a QueryResultInterface and countBy* an int.
 * <p>
 * The signature is "#" + KEY + kind code + method name + SEPARATOR + qualifier type,
 * resolved in {@link #complete(String, Project)} where index access is allowed.
 */
public class RepositoryMagicMethodTypeProvider implements PhpTypeProvider4 {

    static final char KEY = 'ɑ';
    static final char SEPARATOR = 'ɒ';

    @Override
    public char getKey() {
        return KEY;
    }

    @Override
    public @Nullable PhpType getType(PsiElement element) {
        if (!(element instanceof MethodReference reference)) {
            return null;
        }

        String name = reference.getName();
        RepositoryMagicMethodUtil.MagicKind kind = RepositoryMagicMethodUtil.kindOf(name);
        if (kind == null || !(reference.getClassReference() instanceof PhpExpression qualifier)
                || !NeosProjectService.isEnabled(element.getProject())) {
            return null;
        }

        PhpType result = new PhpType();
        for (String qualifierType : qualifier.getType().getTypes()) {
            result.add("#" + KEY + kind.code + name + SEPARATOR + qualifierType);
        }
        return result.isEmpty() ? null : result;
    }

    @Override
    public @Nullable PhpType complete(String expression, Project project) {
        int separator = expression.indexOf(SEPARATOR);
        if (expression.length() < 4 || separator < 0) {
            return null;
        }

        RepositoryMagicMethodUtil.MagicKind kind = RepositoryMagicMethodUtil.MagicKind.fromCode(expression.charAt(2));
        if (kind == null) {
            return null;
        }

        String methodName = expression.substring(3, separator);
        PhpType qualifierType = new PhpType().add(expression.substring(separator + 1));
        List<PhpClass> repositories = RepositoryMagicMethodUtil.repositoriesHandlingMagicMethod(project, qualifierType, methodName);
        if (repositories.isEmpty()) {
            return null;
        }

        return switch (kind) {
            case COUNT -> PhpType.INT;
            case FIND -> new PhpType().add(RepositoryMagicMethodUtil.QUERY_RESULT_INTERFACE_FQN);
            case FIND_ONE -> {
                Collection<String> entities = RepositoryMagicMethodUtil.entityClassFqns(repositories);
                if (entities.isEmpty()) {
                    yield null;
                }
                PhpType type = new PhpType();
                entities.forEach(type::add);
                yield type.add(PhpType.NULL);
            }
        };
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
        return Collections.emptyList();
    }
}
