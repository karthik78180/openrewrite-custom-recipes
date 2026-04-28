package com.rewrite.jdk25Upgrade;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * Tests for UpgradeToJava25Recipe.
 *
 * Covers Gradle Groovy and Kotlin DSL toolchain updates, source/target compatibility-only
 * variants, GitHub workflow YAML javaVersion updates, no-op behavior, and recipe metadata.
 */
class UpgradeToJava25RecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeToJava25Recipe());
    }

    @Test
    void upgradesToolchainInGroovyBuildGradle() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'java'
                        }

                        java {
                            toolchain {
                                languageVersion = JavaLanguageVersion.of(21)
                            }
                        }
                        """,
                        """
                        plugins {
                            id 'java'
                        }

                        java {
                            toolchain {
                                languageVersion = JavaLanguageVersion.of(25)
                            }
                        }
                        """
                )
        );
    }

    @Test
    void upgradesSourceAndTargetCompatibility() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'java'
                        }

                        java {
                            sourceCompatibility = JavaVersion.VERSION_21
                            targetCompatibility = JavaVersion.VERSION_21
                        }
                        """,
                        """
                        plugins {
                            id 'java'
                        }

                        java {
                            sourceCompatibility = JavaVersion.VERSION_25
                            targetCompatibility = JavaVersion.VERSION_25
                        }
                        """
                )
        );
    }

    @Test
    void upgradesSourceCompatibilityOnly() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'java'
                        }

                        java {
                            sourceCompatibility = JavaVersion.VERSION_21
                        }
                        """,
                        """
                        plugins {
                            id 'java'
                        }

                        java {
                            sourceCompatibility = JavaVersion.VERSION_25
                        }
                        """
                )
        );
    }

    @Test
    void upgradesKotlinDslToolchain() {
        rewriteRun(
                buildGradleKts(
                        """
                        plugins {
                            java
                        }

                        java {
                            toolchain {
                                languageVersion.set(JavaLanguageVersion.of(21))
                            }
                        }
                        """,
                        """
                        plugins {
                            java
                        }

                        java {
                            toolchain {
                                languageVersion.set(JavaLanguageVersion.of(25))
                            }
                        }
                        """
                )
        );
    }

    @Test
    void updatesJavaVersionInWorkflowYaml() {
        rewriteRun(
                yaml(
                        """
                        name: CI
                        on: [push]
                        jobs:
                          build:
                            runs-on: ubuntu-latest
                            javaVersion: '21'
                            steps:
                              - uses: actions/checkout@v3
                        """,
                        """
                        name: CI
                        on: [push]
                        jobs:
                          build:
                            runs-on: ubuntu-latest
                            javaVersion: '25'
                            steps:
                              - uses: actions/checkout@v3
                        """,
                        spec -> spec.path(".github/workflows/ci.yml")
                )
        );
    }

    @Test
    void doesNotUpdateJavaVersionOutsideWorkflows() {
        rewriteRun(
                yaml(
                        """
                        config:
                          javaVersion: '21'
                        """,
                        spec -> spec.path("config/build-config.yml")
                )
        );
    }

    @Test
    void noOpWhenAlreadyJava25() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'java'
                        }

                        java {
                            toolchain {
                                languageVersion = JavaLanguageVersion.of(25)
                            }
                        }
                        """
                )
        );
    }

    @Test
    void recipeHasCorrectMetadata() {
        UpgradeToJava25Recipe recipe = new UpgradeToJava25Recipe();
        assertThat(recipe.getDisplayName())
                .isEqualTo("Upgrade build to Java 25 and bump workflow javaVersion");
        assertThat(recipe.getDescription())
                .contains("Java 25")
                .contains("javaVersion")
                .contains(".github/workflows");
    }

    @Test
    void recipeIsCompositeWithTwoDelegates() {
        UpgradeToJava25Recipe recipe = new UpgradeToJava25Recipe();
        List<Recipe> recipeList = recipe.getRecipeList();
        assertThat(recipeList).hasSize(2);
        assertThat(recipeList)
                .anyMatch(r -> r.getClass().getSimpleName().equals("UpgradeJavaVersion"));
        assertThat(recipeList)
                .anyMatch(r -> r.getClass().getSimpleName().equals("ChangePropertyValue"));
    }
}
