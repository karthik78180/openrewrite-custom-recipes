package com.rewrite;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.TypeTree;

public class ChangeConstantReference extends Recipe {

    @Override
    public @NotNull String getDisplayName() {
        return "Change constant reference from MicroConstant.APP_ID to ServerConstant.APP_ID";
    }

    @Override
    public @NotNull String getDescription() {
        return "Replaces all usages of MicroConstant.APP_ID (com.old.Constants) with ServerConstant.APP_ID (org.example.updated).";
    }

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                J.FieldAccess fa = super.visitFieldAccess(fieldAccess, ctx);

                if (fa.getTarget() instanceof Identifier target) {
                    if ("MicroConstant".equals(target.getSimpleName()) &&
                        "APP_ID".equals(fa.getName().getSimpleName())) {

                        // Replace MicroConstant.APP_ID → ServerConstant.APP_ID
                        return fa.withTarget(target.withSimpleName("ServerConstant"));
                    }
                }

                return fa;
            }


            @Override
            public J.Import visitImport(J.Import _import, ExecutionContext ctx) {
                if (_import.getQualid().toString().trim().equals("com.old.Constants.MicroConstant")) {
                    J.FieldAccess newQualid = (J.FieldAccess) TypeTree.build("org.example.updated.ServerConstant");
                    // ✅ Preserve the original prefix (spacing after `import`)
                    return _import.withQualid(newQualid.withPrefix(_import.getQualid().getPrefix()));
                }
                return super.visitImport(_import, ctx);
            }
        };
    }
}
