package com.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

/**
 * This OpenRewrite recipe updates all Java classes that extend Vehicle (with or without generics)
 * to extend Car instead. It handles both raw and parameterized types and adjusts imports accordingly.
 */
public class VehicleToCarRecipe extends Recipe {

    // Recipe display name shown in OpenRewrite tooling
    @Override
    public String getDisplayName() {
        return "Replace Vehicle<T> with Car<T>";
    }

    // Detailed explanation of what the recipe does
    @Override
    public String getDescription() {
        return "Replaces any class extending Vehicle (with or without generics) with Car, preserving type parameters.";
    }

    // The main transformation logic is defined in this visitor
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

                TypeTree extendType = cd.getExtends();
                switch (extendType) {
                    // Skip classes without an extends clause
                    case null -> {
                        return cd;
                    }

                    // Case 1: Handles class declarations like: class MyVehicle extends Vehicle<T>
                    case J.ParameterizedType pt when pt.getClazz() instanceof J.Identifier id && "Vehicle".equals(id.getSimpleName()) -> {

                        J.Identifier newCar = id.withSimpleName("Car")
                                .withType(JavaType.ShallowClass.build("com.example.Car"));

                        J.ParameterizedType newType = pt.withClazz(newCar);
                        cd = cd.withExtends(newType);
                        maybeRemoveImport("Vehicle");
                        maybeAddImport("Car");
                    }

                    // Case 2: Handles class declarations like: class MyVehicle extends Vehicle
                    case J.Identifier id when "Vehicle".equals(id.getSimpleName()) -> {

                        J.Identifier newCar = id.withSimpleName("Car")
                                .withType(JavaType.ShallowClass.build("com.example.Car"));

                        cd = cd.withExtends(newCar);

                        maybeRemoveImport("Vehicle");
                        maybeAddImport("Car");
                    }

                    // No change needed for all other types
                    default -> {}
                }

                return cd;
            }
        };
    }
}
