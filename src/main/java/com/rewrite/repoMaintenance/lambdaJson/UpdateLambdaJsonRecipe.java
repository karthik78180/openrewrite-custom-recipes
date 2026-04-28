package com.rewrite.repoMaintenance.lambdaJson;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.JsonPathMatcher;
import org.openrewrite.json.ChangeValue;
import org.openrewrite.json.DeleteKey;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.SearchResult;

import java.util.List;

import static com.rewrite.repoMaintenance.lambdaJson.LambdaJsonConstants.*;

/**
 * Updates fixed keys in lambda.json files (runtime, handler, deploymentCordinates.region)
 * and deletes obsolete top-level keys (functionVersion, version).
 *
 * Scoped to {@link LambdaJsonConstants#FILE_PATTERN}.
 */
public class UpdateLambdaJsonRecipe extends Recipe {

    @Override
    public @NotNull String getDisplayName() {
        return "Update lambda.json keys and delete obsolete keys";
    }

    @Override
    public @NotNull String getDescription() {
        return "In lambda.json files matching " + FILE_PATTERN + ", sets runtime, handler, "
                + "and deploymentCordinates.region to fixed values; deletes top-level "
                + "functionVersion and version keys.";
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        return List.of(
                scopedChangeValue(RUNTIME_PATH, RUNTIME_VALUE),
                scopedChangeValue(HANDLER_PATH, HANDLER_VALUE),
                scopedChangeValue(REGION_PATH, REGION_VALUE),
                scoped(new DeleteKey(DELETE_FUNCTION_VERSION_PATH)),
                scoped(new DeleteKey(DELETE_VERSION_PATH))
        );
    }

    /**
     * Wraps {@link ChangeValue} with file-pattern and value-equality preconditions so the
     * recipe is a true no-op when the target key already holds the desired value.
     */
    private static Recipe scopedChangeValue(String keyPath, String targetValue) {
        JsonPathMatcher matcher = new JsonPathMatcher(keyPath);
        TreeVisitor<?, ExecutionContext> notAlreadyCorrect = new JsonIsoVisitor<ExecutionContext>() {
            @Override
            public Json.Member visitMember(Json.Member member, ExecutionContext ctx) {
                Json.Member m = super.visitMember(member, ctx);
                if (matcher.matches(getCursor()) && m.getValue() instanceof Json.Literal) {
                    String raw = ((Json.Literal) m.getValue()).getSource();
                    String unquoted = raw.startsWith("\"") && raw.endsWith("\"")
                            ? raw.substring(1, raw.length() - 1) : raw;
                    if (!targetValue.equals(unquoted)) {
                        return SearchResult.found(m);
                    }
                }
                return m;
            }
        };
        return new Recipe() {
            @Override
            public @NotNull String getDisplayName() {
                return "Change JSON value at " + keyPath;
            }

            @Override
            public @NotNull String getDescription() {
                return "Sets " + keyPath + " to " + targetValue + " in lambda.json files.";
            }

            @Override
            public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
                return Preconditions.check(
                        Preconditions.and(new FindSourceFiles(FILE_PATTERN).getVisitor(), notAlreadyCorrect),
                        new ChangeValue(keyPath, targetValue).getVisitor()
                );
            }
        };
    }

    /** Wraps an inner recipe so it only runs on files matching {@link LambdaJsonConstants#FILE_PATTERN}. */
    private static Recipe scoped(Recipe inner) {
        return new Recipe() {
            @Override
            public @NotNull String getDisplayName() {
                return inner.getDisplayName();
            }

            @Override
            public @NotNull String getDescription() {
                return inner.getDescription();
            }

            @Override
            public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
                return Preconditions.check(new FindSourceFiles(FILE_PATTERN), inner.getVisitor());
            }
        };
    }
}
