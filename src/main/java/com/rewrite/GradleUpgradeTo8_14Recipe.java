package com.rewrite;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.UpdateGradleWrapper;
import org.openrewrite.gradle.plugins.UpgradePluginVersion;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;

import java.util.List;

/**
 * Recipe to upgrade Gradle from version > 7.5 to 8.14.
 *
 * This composite recipe handles:
 * - Gradle wrapper update to 8.14
 * - Plugin compatibility updates
 * - Deprecated API replacements
 * - Configuration updates for Gradle 8.x compatibility
 */
public class GradleUpgradeTo8_14Recipe extends Recipe {

    @Override
    public @NotNull String getDisplayName() {
        return "Upgrade Gradle from 7.5+ to 8.14";
    }

    @Override
    public @NotNull String getDescription() {
        return "Upgrades Gradle projects from version 7.5 or higher to version 8.14. " +
               "This includes updating the Gradle wrapper, fixing deprecated configurations, " +
               "and ensuring compatibility with Gradle 8.14 APIs.";
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        return List.of(
                // Update Gradle wrapper to 8.14
                new UpdateGradleWrapper("8.14", null, null, null, null),

                // Update common plugins to compatible versions
                new UpgradePluginVersion("com.github.johnrengelman.shadow", "8.1.1", null),
                new UpgradePluginVersion("io.vertx", "5.0.5", null),

                // Custom visitor to handle deprecated APIs
                new FixDeprecatedGradleApis()
        );
    }

    /**
     * Custom recipe to fix deprecated Gradle 8.x APIs
     */
    private static class FixDeprecatedGradleApis extends Recipe {

        @Override
        public @NotNull String getDisplayName() {
            return "Fix deprecated Gradle 8.x APIs";
        }

        @Override
        public @NotNull String getDescription() {
            return "Replaces deprecated Gradle APIs with their Gradle 8.x equivalents.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new GroovyIsoVisitor<ExecutionContext>() {

                @Override
                public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                    // Only process Gradle build files
                    if (!cu.getSourcePath().toString().endsWith(".gradle") &&
                        !cu.getSourcePath().toString().endsWith(".gradle.kts")) {
                        return cu;
                    }
                    return super.visitCompilationUnit(cu, ctx);
                }

                @Override
                public G.MethodInvocation visitMethodInvocation(G.MethodInvocation method, ExecutionContext ctx) {
                    G.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                    // Replace deprecated configuration methods
                    if (m.getSimpleName().equals("compile")) {
                        m = m.withName(m.getName().withSimpleName("implementation"));
                    } else if (m.getSimpleName().equals("testCompile")) {
                        m = m.withName(m.getName().withSimpleName("testImplementation"));
                    } else if (m.getSimpleName().equals("runtime")) {
                        m = m.withName(m.getName().withSimpleName("runtimeOnly"));
                    } else if (m.getSimpleName().equals("testRuntime")) {
                        m = m.withName(m.getName().withSimpleName("testRuntimeOnly"));
                    }

                    return m;
                }
            };
        }
    }
}
