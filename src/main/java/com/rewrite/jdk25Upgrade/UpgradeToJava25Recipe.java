package com.rewrite.jdk25Upgrade;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.openrewrite.java.migrate.UpgradeJavaVersion;
import org.openrewrite.yaml.ChangePropertyValue;

import java.util.List;

/**
 * Upgrades a target project to Java 25.
 *
 * Composes:
 * - org.openrewrite.java.migrate.UpgradeJavaVersion(25): updates JVM toolchain,
 *   sourceCompatibility, targetCompatibility in build.gradle / build.gradle.kts / pom.xml.
 *   This is the underlying transformation used by both UpgradeBuildToJava25 and
 *   UpgradeBuildToJava25ForKotlin in rewrite-migrate-java.
 * - org.openrewrite.yaml.ChangePropertyValue: rewrites javaVersion: '21' to '25'
 *   in .github/workflows/*.yml only.
 */
public class UpgradeToJava25Recipe extends Recipe {

    private static final Integer TARGET_JAVA_VERSION = 25;
    private static final String WORKFLOW_FILE_PATTERN = "**/.github/workflows/*.yml";
    private static final String WORKFLOW_PROPERTY_KEY = "**.javaVersion";

    @Override
    public @NotNull String getDisplayName() {
        return "Upgrade build to Java 25 and bump workflow javaVersion";
    }

    @Override
    public @NotNull String getDescription() {
        return "Updates JVM toolchain and source/target compatibility to Java 25 in Gradle " +
                "(Groovy and Kotlin DSL) and Maven build files. Also updates the `javaVersion` " +
                "key from 21 to 25 in GitHub Actions workflow YAML files under .github/workflows/.";
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        return List.of(
                new UpgradeJavaVersion(TARGET_JAVA_VERSION),
                new ChangePropertyValue(
                        WORKFLOW_PROPERTY_KEY,
                        "25",
                        "21",
                        null,
                        null,
                        WORKFLOW_FILE_PATTERN
                )
        );
    }
}
