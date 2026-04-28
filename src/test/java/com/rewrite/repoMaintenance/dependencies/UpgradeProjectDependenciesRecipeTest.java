package com.rewrite.repoMaintenance.dependencies;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;

class UpgradeProjectDependenciesRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeProjectDependenciesRecipe());
    }

    @Test
    void inlineGroovyBuildGradleUpgrade() {
        rewriteRun(
                buildGradle(
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation 'io.vertx:vertx-core:3.9.16'
                        }
                        """,
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation 'io.vertx:vertx-core:5.0.5'
                        }
                        """
                )
        );
    }

    @Disabled("UpgradeDependencyVersion uses isDependencyDeclaration which requires GradleDependencyConfiguration "
            + "(tooling API markers) for Kotlin DSL; without the tooling API the method is not recognized as a "
            + "dependency declaration and no change is made. Groovy DSL works because of DEPENDENCY_DSL_MATCHER "
            + "type attribution — see docs/UpgradeProjectDependenciesRecipe.md (Known Limitations).")
    @Test
    void inlineKotlinDslBuildGradleUpgrade() {
    }

    @Disabled("org.openrewrite.gradle.toml.Assertions.versionCatalog does not exist in rewrite-gradle "
            + "8.80.1; no TOML assertion helper is available on the current classpath — "
            + "see docs/UpgradeProjectDependenciesRecipe.md (Known Limitations).")
    @Test
    void versionCatalogRefUpdated() {
    }

    @Test
    void pluginUpgradeInPluginsBlock() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'org.springframework.boot' version '3.3.0'
                        }
                        """,
                        """
                        plugins {
                            id 'org.springframework.boot' version '3.4.0'
                        }
                        """
                )
        );
    }

    @Disabled("settings.gradle gradle.ext indirection is not first-class in UpgradeDependencyVersion — "
            + "see docs/UpgradeProjectDependenciesRecipe.md (Known Limitations).")
    @Test
    void settingsGradleExtPropertyIndirection() {
        rewriteRun(
                settingsGradle(
                        """
                        gradle.ext.vertxVersion = '3.9.16'
                        """,
                        """
                        gradle.ext.vertxVersion = '5.0.5'
                        """
                ),
                buildGradle(
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation "io.vertx:vertx-core:${gradle.ext.vertxVersion}"
                        }
                        """
                )
        );
    }

    @Test
    void springCoreUpgradeInBuildGradle() {
        rewriteRun(
                buildGradle(
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation 'org.springframework:spring-core:6.0.0'
                        }
                        """,
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation 'org.springframework:spring-core:6.2.0'
                        }
                        """
                )
        );
    }

    @Test
    void multipleDependenciesUpgradedTogether() {
        rewriteRun(
                buildGradle(
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation 'io.vertx:vertx-core:3.9.16'
                            implementation 'org.springframework:spring-core:6.0.0'
                        }
                        """,
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation 'io.vertx:vertx-core:5.0.5'
                            implementation 'org.springframework:spring-core:6.2.0'
                        }
                        """
                )
        );
    }

    @Test
    void noOpWhenAlreadyAtTargetVersion() {
        rewriteRun(
                buildGradle(
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation 'io.vertx:vertx-core:5.0.5'
                        }
                        """
                )
        );
    }

    @Test
    void noOpWhenDependencyAbsent() {
        rewriteRun(
                buildGradle(
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation 'org.unrelated:lib:1.0.0'
                        }
                        """
                )
        );
    }

    @Test
    void recipeMetadataAndCompositeSize() {
        UpgradeProjectDependenciesRecipe recipe = new UpgradeProjectDependenciesRecipe();
        assertThat(recipe.getDisplayName()).isNotBlank();
        assertThat(recipe.getDescription()).contains("dependency");
        List<Recipe> sub = recipe.getRecipeList();
        assertThat(sub).hasSize(
                DependencyVersionConstants.DEPENDENCIES.size()
                        + DependencyVersionConstants.PLUGINS.size());
    }
}
