package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

/**
 * Tests for the YAML recipe com.recipies.yaml.SetupJavaVersionTo25.
 *
 * Verifies that java-version in GitHub workflow files is rewritten to 25, and that
 * java-version keys in unrelated YAML files are not touched.
 */
class SetupJavaVersionTo25Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
                Environment.builder()
                        .scanRuntimeClasspath()
                        .build()
                        .activateRecipes("com.recipies.yaml.SetupJavaVersionTo25")
        );
    }

    @Test
    void rewritesJavaVersionInWorkflowYaml() {
        rewriteRun(
                yaml(
                        """
                        name: CI
                        on: [push]
                        jobs:
                          build:
                            runs-on: ubuntu-latest
                            steps:
                              - uses: actions/checkout@v4
                              - uses: actions/setup-java@v4
                                with:
                                  distribution: 'temurin'
                                  java-version: '21'
                        """,
                        """
                        name: CI
                        on: [push]
                        jobs:
                          build:
                            runs-on: ubuntu-latest
                            steps:
                              - uses: actions/checkout@v4
                              - uses: actions/setup-java@v4
                                with:
                                  distribution: 'temurin'
                                  java-version: '25'
                        """,
                        spec -> spec.path(".github/workflows/ci.yml")
                )
        );
    }

    @Test
    void rewritesAnyValueToJava25() {
        rewriteRun(
                yaml(
                        """
                        jobs:
                          build:
                            steps:
                              - uses: actions/setup-java@v4
                                with:
                                  java-version: '11'
                        """,
                        """
                        jobs:
                          build:
                            steps:
                              - uses: actions/setup-java@v4
                                with:
                                  java-version: '25'
                        """,
                        spec -> spec.path(".github/workflows/build.yml")
                )
        );
    }

    @Test
    void doesNotTouchYamlOutsideWorkflowsDir() {
        rewriteRun(
                yaml(
                        """
                        config:
                          java-version: '21'
                        """,
                        spec -> spec.path("config/app.yml")
                )
        );
    }
}
