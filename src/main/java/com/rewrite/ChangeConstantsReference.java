package com.rewrite;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.J.FieldAccess;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.Import;

import java.util.Map;

// Recipe to update constant references from one class to another
public class ChangeConstantsReference extends Recipe {
    // 1. Configuration Constants
    private final String oldClassName = "MicroConstant";
    private final String newClassName = "ServerConstant";
    private final String oldImport = "com.old.Constants.MicroConstant";
    private final String newImport = "org.example.updated.ServerConstant";

    // Mapping of constant names from old to new
    private final Map<String, String> constantMapping = Map.of(
        "APP_ID", "APP_ID",
        "CLIENT_SECRET", "CLIENT_SECRET",
        "USER_NAME", "USERNAME"
    );

    // Display name for the recipe
    @Override
    public @NotNull String getDisplayName() {
        return "Change constants from " + oldClassName + " to " + newClassName;
    }

    // Description for documentation
    @Override
    public @NotNull String getDescription() {
        return "Replaces constant references from " + oldClassName + " to " + newClassName + " using hardcoded values.";
    }

    // Main transformation logic using JavaIsoVisitor
    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {

            // Visit and update field accesses (e.g., MicroConstant.APP_ID -> ServerConstant.APP_ID)
            @Override
            public FieldAccess visitFieldAccess(FieldAccess fa, ExecutionContext ctx) {
                FieldAccess updated = super.visitFieldAccess(fa, ctx);

                // Check if the target is the old constant class and matches a known constant
                if (updated.getTarget() instanceof Identifier target &&
                    oldClassName.equals(target.getSimpleName()) &&
                    constantMapping.containsKey(updated.getName().getSimpleName())) {

                    // Replace class name and update constant if renamed
                    return updated.withTarget(target.withSimpleName(newClassName))
                                  .withName(updated.getName().withSimpleName(constantMapping.get(updated.getName().getSimpleName())));
                }

                return updated;
            }

            // Update import statement from old to new constant class
            @Override
            public Import visitImport(Import _import, ExecutionContext ctx) {
                if (_import.getQualid().toString().equals(oldImport)) {
                    // Create a new qualified identifier using TypeTree
                    J.FieldAccess newQualid = (J.FieldAccess) TypeTree.build(newImport);
                    return _import.withQualid(newQualid.withPrefix(_import.getQualid().getPrefix()));
                }
                return super.visitImport(_import, ctx);
            }
        };
    }
}
