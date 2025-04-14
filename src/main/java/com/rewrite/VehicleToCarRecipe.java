package com.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

import java.util.List;

// This recipe modifies Java classes that extend 'Vehicle' to instead extend 'Car'
public class VehicleToCarRecipe extends Recipe {

    // Recipe display name shown in OpenRewrite tooling
    @Override
    public String getDisplayName() {
        return "Replace Vehicle extends with Car extends";
    }

    // Detailed explanation of what the recipe does
    @Override
    public String getDescription() {
        return "Updates any class extending Vehicle to extend Car instead, including inner classes. " +
               "Avoids unsupported API usage in OpenRewrite 8.23.1 and logs steps for debugging.";
    }

    // The main transformation logic is defined in this visitor
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            // Cache to store parsed 'Car' type tree for reuse
            private TypeTree carTypeTree;

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                // Build the Car type tree only once per CompilationUnit
                if (carTypeTree == null) {
                    String snippet = "class Temp extends Car {}";
                    JavaParser parser = JavaParser.fromJavaVersion().build();
                    List<SourceFile> parsed = parser.parse(snippet).toList();

                    // Extract the extends type from the parsed snippet
                    if (!parsed.isEmpty() && parsed.get(0) instanceof J.CompilationUnit) {
                        J.ClassDeclaration tempClass = ((J.CompilationUnit) parsed.get(0)).getClasses().get(0);
                        TypeTree carExtends = tempClass.getExtends();

                        // Ensure it’s an identifier and assign the proper JavaType
                        if (carExtends instanceof J.Identifier carIdentifier) {
                            // Explicit type declaration to avoid validation errors
                            carTypeTree = carIdentifier.withType(JavaType.ShallowClass.build("com.example.Car"));
                        }
                    }
                }

                return super.visitCompilationUnit(cu, ctx);
            }

            // 4. Class Declaration Modification
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                // Visit the class and print debug info
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                System.out.println("Visiting class: " + cd.getSimpleName());

                // Check if the class extends Vehicle and replace it with Car
                if (cd.getExtends() != null && "Vehicle".equals(cd.getExtends().printTrimmed()) && carTypeTree != null) {
                    System.out.println(" - Replacing Vehicle with Car");
                    cd = cd.withExtends(carTypeTree);  // Replace parent class
                    maybeRemoveImport("Vehicle");      // Cleanup imports
                    maybeAddImport("Car");             // Add new import
                    }

                return cd;
            }
        };
    }
}
