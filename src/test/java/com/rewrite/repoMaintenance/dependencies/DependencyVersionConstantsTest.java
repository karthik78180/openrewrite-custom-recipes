package com.rewrite.repoMaintenance.dependencies;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyVersionConstantsTest {

    @Test
    void dependenciesIncludeVertxAndSpring() {
        assertThat(DependencyVersionConstants.DEPENDENCIES)
                .extracting(DependencyVersionConstants.Gav::groupId,
                        DependencyVersionConstants.Gav::artifactId,
                        DependencyVersionConstants.Gav::newVersion)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("io.vertx", "vertx-core", "5.0.5"),
                        org.assertj.core.groups.Tuple.tuple("org.springframework", "spring-core", "6.2.0"));
    }

    @Test
    void pluginsIncludeSpringBoot() {
        assertThat(DependencyVersionConstants.PLUGINS)
                .extracting(DependencyVersionConstants.PluginUpgrade::pluginId,
                        DependencyVersionConstants.PluginUpgrade::newVersion)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("org.springframework.boot", "3.4.0"));
    }

    @Test
    void gavRecordImplementsValueEquality() {
        DependencyVersionConstants.Gav a = new DependencyVersionConstants.Gav("g", "a", "1.0");
        DependencyVersionConstants.Gav b = new DependencyVersionConstants.Gav("g", "a", "1.0");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void pluginUpgradeRecordImplementsValueEquality() {
        DependencyVersionConstants.PluginUpgrade a =
                new DependencyVersionConstants.PluginUpgrade("p", "1.0");
        DependencyVersionConstants.PluginUpgrade b =
                new DependencyVersionConstants.PluginUpgrade("p", "1.0");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
