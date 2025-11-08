# OpenRewrite Custom Recipes for Java Migration

This repository contains custom OpenRewrite recipes designed to automate Java code migrations. OpenRewrite is a powerful tool for refactoring and migrating Java codebases. It provides a framework for defining recipes that can analyze and transform Java source code.

---

## Overview

The custom recipes in this repository are designed to:

1. **Replace Base Classes**: Update classes extending `Vehicle` to extend `Car` instead.
2. **Update Constant References**: Replace references to constants from `MicroConstant` to `ServerConstant` and update their imports.
3. **Gradle Upgrades**: Automate Gradle version upgrades from 7.5+ to 8.14, including wrapper updates and deprecated API replacements.
4. **Composite Recipes**: Combine multiple recipes into a single migration process.

These recipes can be used to automate repetitive and error-prone code changes across large Java projects.

---

## How OpenRewrite Works

OpenRewrite operates by parsing Java source code into an abstract syntax tree (AST). Recipes define transformations that are applied to the AST, and the modified AST is written back to the source files.

### Key Components of OpenRewrite

1. **Recipe**: A unit of transformation logic. Recipes define what changes to make to the code.
2. **Visitor**: A visitor traverses the AST and applies transformations to specific nodes.
3. **ExecutionContext**: Provides context for the recipe execution, such as logging and configuration.

---

## Recipes in This Repository

### 1. **VehicleToCarRecipe**
This recipe replaces any class extending `Vehicle` with `Car`. It ensures that imports are updated accordingly.

- **File**: `VehicleToCarRecipe`
- **Test**: `VehicleToCarRecipeTest`

### 2. **ChangeConstantReference**
This recipe replaces references to `MicroConstant.APP_ID` with `ServerConstant.APP_ID` and updates the import statements.

- **File**: `ChangeConstantReference`
- **Test**: `ChangeConstantReferenceTest`

### 3. **ChangeConstantsReference**
This recipe generalizes constant replacement by using a mapping of old constants to new constants. It updates both references and imports.

- **File**: `ChangeConstantsReference`
- **Test**: `ChangeConstantsReferenceTest`

### 4. **GradleUpgradeTo8_14Recipe**
This recipe automates the upgrade of Gradle projects from version 7.5+ to version 8.14. It updates the Gradle wrapper, replaces deprecated dependency configurations (`compile` → `implementation`, `runtime` → `runtimeOnly`, etc.), and ensures plugin compatibility.

- **File**: `GradleUpgradeTo8_14Recipe`
- **Test**: `GradleUpgradeTo8_14RecipeTest`
- **Documentation**: [`docs/GradleUpgradeTo8_14Recipe.md`](docs/GradleUpgradeTo8_14Recipe.md)

**Key Features:**
- Updates Gradle wrapper to version 8.14
- Replaces deprecated configurations: `compile`, `testCompile`, `runtime`, `testRuntime`
- Updates common plugins to Gradle 8.14-compatible versions
- Handles both Groovy (`.gradle`) and Kotlin (`.gradle.kts`) build scripts

### 5. **Java21MigrationRecipes**
This is a composite recipe that combines all the above recipes into a single migration process.

- **File**: `Java21MigrationRecipes`
- **Test**: `Java21MigrationRecipesTest`

---

## How to Use These Recipes in Your Project

### Prerequisites

1. **Java 21**: This project is configured to use Java 21.
2. **Gradle**: Ensure Gradle is installed or use the provided Gradle wrapper (`gradlew`).
3. **OpenRewrite Dependencies**: The project uses OpenRewrite 8.23.1.

---

### Using the OpenRewrite Gradle Plugin

The easiest way to apply these recipes is by using the OpenRewrite Gradle plugin.

#### 1. Add the Plugin to Your Project

Add the following to your `build.gradle` file:

```gradle
plugins {
    d 'org.openrewrite.rewrite' version '7.1.2'}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    rewrite 'com.rewrite:openrewrite-custom-recipes:1.0-SNAPSHOT'
}