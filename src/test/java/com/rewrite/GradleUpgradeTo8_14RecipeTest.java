package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.properties.Assertions.properties;

/**
 * Test suite for GradleUpgradeTo8_14Recipe
 */
class GradleUpgradeTo8_14RecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new GradleUpgradeTo8_14Recipe());
    }

    @Test
    void upgradesGradleWrapperVersion() {
        rewriteRun(
            spec -> spec.beforeRecipe(withToolingApi()),
            properties(
                """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\\://services.gradle.org/distributions/gradle-7.6-bin.zip
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
                """,
                """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip
                networkTimeout=10000
                validateDistributionUrl=true
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
                """,
                spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
            )
        );
    }

    @Test
    void replacesDeprecatedCompileConfiguration() {
        rewriteRun(
            spec -> spec.beforeRecipe(withToolingApi()),
            buildGradle(
                """
                plugins {
                    id 'java'
                }

                dependencies {
                    compile 'org.springframework:spring-core:5.3.20'
                    testCompile 'junit:junit:4.13.2'
                }
                """,
                """
                plugins {
                    id 'java'
                }

                dependencies {
                    implementation 'org.springframework:spring-core:5.3.20'
                    testImplementation 'junit:junit:4.13.2'
                }
                """
            )
        );
    }

    @Test
    void replacesDeprecatedRuntimeConfiguration() {
        rewriteRun(
            spec -> spec.beforeRecipe(withToolingApi()),
            buildGradle(
                """
                plugins {
                    id 'java'
                }

                dependencies {
                    runtime 'com.h2database:h2:2.1.214'
                    testRuntime 'org.hsqldb:hsqldb:2.7.1'
                }
                """,
                """
                plugins {
                    id 'java'
                }

                dependencies {
                    runtimeOnly 'com.h2database:h2:2.1.214'
                    testRuntimeOnly 'org.hsqldb:hsqldb:2.7.1'
                }
                """
            )
        );
    }

    @Test
    void handlesMultipleDeprecatedConfigurations() {
        rewriteRun(
            spec -> spec.beforeRecipe(withToolingApi()),
            buildGradle(
                """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    compile 'org.springframework:spring-core:5.3.20'
                    compile 'org.springframework:spring-context:5.3.20'
                    testCompile 'junit:junit:4.13.2'
                    runtime 'com.h2database:h2:2.1.214'
                    testRuntime 'org.hsqldb:hsqldb:2.7.1'
                }

                test {
                    useJUnitPlatform()
                }
                """,
                """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation 'org.springframework:spring-core:5.3.20'
                    implementation 'org.springframework:spring-context:5.3.20'
                    testImplementation 'junit:junit:4.13.2'
                    runtimeOnly 'com.h2database:h2:2.1.214'
                    testRuntimeOnly 'org.hsqldb:hsqldb:2.7.1'
                }

                test {
                    useJUnitPlatform()
                }
                """
            )
        );
    }

    @Test
    void doesNotChangeModernConfigurations() {
        rewriteRun(
            spec -> spec.beforeRecipe(withToolingApi()),
            buildGradle(
                """
                plugins {
                    id 'java'
                }

                dependencies {
                    implementation 'org.springframework:spring-core:5.3.20'
                    testImplementation 'junit:junit:4.13.2'
                    runtimeOnly 'com.h2database:h2:2.1.214'
                    api 'org.apache.commons:commons-lang3:3.12.0'
                }
                """,
                spec -> spec.markers(createGradleMarkers())
            )
        );
    }

    @Test
    void handlesBuildGradleKts() {
        rewriteRun(
            spec -> spec.beforeRecipe(withToolingApi()),
            buildGradle(
                """
                plugins {
                    id("java")
                }

                dependencies {
                    implementation("org.springframework:spring-core:5.3.20")
                    testImplementation("junit:junit:4.13.2")
                }
                """,
                spec -> spec.path("build.gradle.kts")
            )
        );
    }

    @Test
    void handlesEmptyBuildFile() {
        rewriteRun(
            spec -> spec.beforeRecipe(withToolingApi()),
            buildGradle(
                """
                plugins {
                    id 'java'
                }
                """
            )
        );
    }

    /**
     * Helper method to create Gradle markers for testing
     */
    private Markers createGradleMarkers() {
        List<GradleProject> projects = new ArrayList<>();
        GradleSettings gradleSettings = new GradleSettings(
            "root",
            null,
            projects,
            List.of(),
            null
        );

        return Markers.build(List.of(gradleSettings));
    }
}
