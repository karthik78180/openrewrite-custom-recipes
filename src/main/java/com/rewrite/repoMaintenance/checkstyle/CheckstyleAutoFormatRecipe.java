package com.rewrite.repoMaintenance.checkstyle;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.openrewrite.java.OrderImports;
import org.openrewrite.java.format.AutoFormat;
import org.openrewrite.java.format.BlankLines;
import org.openrewrite.java.format.EmptyNewlineAtEndOfFile;
import org.openrewrite.java.format.RemoveTrailingWhitespace;
import org.openrewrite.staticanalysis.EmptyBlock;
import org.openrewrite.staticanalysis.EqualsAvoidsNull;
import org.openrewrite.staticanalysis.ModifierOrder;
import org.openrewrite.staticanalysis.SimplifyBooleanExpression;
import org.openrewrite.staticanalysis.SimplifyBooleanReturn;
import org.openrewrite.staticanalysis.UseJavaStyleArrayDeclarations;

import java.util.List;

/**
 * Apply Checkstyle-derived autoformat by composing built-in OpenRewrite recipes.
 * Style settings (indentation, import layout, blank lines) come from the JAR's
 * META-INF/rewrite/style.yml and apply via AutoFormat.
 */
public class CheckstyleAutoFormatRecipe extends Recipe {

    @Override
    public @NotNull String getDisplayName() {
        return "Apply Checkstyle-derived autoformat";
    }

    @Override
    public @NotNull String getDescription() {
        return "Composes OrderImports, RemoveTrailingWhitespace, BlankLines, "
                + "EmptyNewlineAtEndOfFile, ModifierOrder, "
                + "SimplifyBooleanExpression, SimplifyBooleanReturn, EqualsAvoidsNull, "
                + "EmptyBlock, UseJavaStyleArrayDeclarations, and AutoFormat to enforce a "
                + "Checkstyle-compatible style without writing any Checkstyle config to the "
                + "target repo.";
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        return List.of(
                new OrderImports(true, null),
                new RemoveTrailingWhitespace(),
                new BlankLines(),
                new EmptyNewlineAtEndOfFile(),
                new ModifierOrder(),
                new SimplifyBooleanExpression(),
                new SimplifyBooleanReturn(),
                new EqualsAvoidsNull(),
                new EmptyBlock(),
                new UseJavaStyleArrayDeclarations(),
                new AutoFormat(null)
        );
    }
}
