package com.rewrite.repoMaintenance.dependencies;

import java.util.List;

/** Hardcoded GAV → version and pluginId → version maps for the dependency upgrader. */
public final class DependencyVersionConstants {

    private DependencyVersionConstants() {
    }

    public record Gav(String groupId, String artifactId, String newVersion) {
    }

    public record PluginUpgrade(String pluginId, String newVersion) {
    }

    public static final List<Gav> DEPENDENCIES = List.of(
            new Gav("io.vertx", "vertx-core", "5.0.5"),
            new Gav("org.springframework", "spring-core", "6.2.0")
    );

    public static final List<PluginUpgrade> PLUGINS = List.of(
            new PluginUpgrade("org.springframework.boot", "3.4.0")
    );
}
