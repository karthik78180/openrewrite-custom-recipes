package com.rewrite.repoMaintenance.dependencies;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.openrewrite.gradle.UpgradeDependencyVersion;
import org.openrewrite.gradle.plugins.UpgradePluginVersion;

import java.util.ArrayList;
import java.util.List;

import static com.rewrite.repoMaintenance.dependencies.DependencyVersionConstants.DEPENDENCIES;
import static com.rewrite.repoMaintenance.dependencies.DependencyVersionConstants.PLUGINS;

/**
 * Bumps dependency and plugin versions declared in build.gradle / build.gradle.kts /
 * libs.versions.toml. Mappings are held in {@link DependencyVersionConstants}.
 */
public class UpgradeProjectDependenciesRecipe extends Recipe {

    @Override
    public @NotNull String getDisplayName() {
        return "Upgrade declared dependency and plugin versions";
    }

    @Override
    public @NotNull String getDescription() {
        return "Upgrades each dependency listed in DependencyVersionConstants.DEPENDENCIES via "
                + "UpgradeDependencyVersion, and each plugin in PLUGINS via UpgradePluginVersion. "
                + "Resolves version literals in inline build.gradle/build.gradle.kts and "
                + "libs.versions.toml version-refs automatically.";
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        List<Recipe> list = new ArrayList<>(DEPENDENCIES.size() + PLUGINS.size());
        for (DependencyVersionConstants.Gav gav : DEPENDENCIES) {
            list.add(new UpgradeDependencyVersion(
                    gav.groupId(),
                    gav.artifactId(),
                    gav.newVersion(),
                    null
            ));
        }
        for (DependencyVersionConstants.PluginUpgrade plugin : PLUGINS) {
            list.add(new UpgradePluginVersion(
                    plugin.pluginId(),
                    plugin.newVersion(),
                    null
            ));
        }
        return list;
    }
}
