## Quick Reference

**Project**: OpenRewrite custom recipes for automated Java code transformations and migrations.

**Key Facts**:
- **Language**: Java (target: Java 21)
- **Build System**: Gradle 9.2.0 with Gradle wrapper
- **OpenRewrite Version**: 8.66.1 (via `rewrite-bom`)
- **Testing Framework**: JUnit 5 + OpenRewrite test utilities
- **Recipes Location**: `src/main/java/com/rewrite/` (extends `org.openrewrite.Recipe`)
- **Recipe Registry**: `src/main/resources/META-INF/rewrite/rewrite.yml`
- **Publish Target**: `mavenLocal` for downstream consumption

## What OpenRewrite Does

OpenRewrite is a framework that applies AST (Abstract Syntax Tree) transformation rules to Java code. Each recipe:
- Parses Java source into an AST
- Applies visitor patterns to transform the tree
- Outputs refactored code
- Handles imports, generics, and type information automatically

## Current Recipes

1. **VehicleToCarRecipe**: Replaces `extends Vehicle` with `extends Car`, preserving generics and updating imports.
2. **ChangeConstantReference**: Moves single constant references from one class to another.
3. **ChangeConstantsReference**: Moves multiple constant references from one class to another.
4. **Java21MigrationRecipes**: Composite recipe aggregating the above recipes.

These recipes are example patterns for code modernization and refactoring.

## Essential Commands

| Task | Command |
|------|---------|
| Run all tests | `./gradlew test` |
| Run single test class | `./gradlew test --tests 'com.rewrite.VehicleToCarRecipeTest'` |
| Clean build | `./gradlew clean build` |
| Build without tests | `./gradlew build -x test` |
| Publish to local Maven | `./gradlew publishToMavenLocal` |
| Check compilation only | `./gradlew compileJava compileTestJava` |

**Important**: Always use `./gradlew` (not `gradle`) to use the project's Gradle wrapper with the correct version.

## Recipe Structure & Code Patterns

### Naming & Organization
- **Package**: All recipes under `com.rewrite`
- **Class naming**: End with `Recipe` (e.g., `VehicleToCarRecipe`, `ChangeConstantReference`)
- **Base class**: Extend `org.openrewrite.Recipe`

### Required Methods
Every recipe must implement:
- `getDisplayName()`: User-friendly recipe name
- `getDescription()`: What the recipe does
- Either `getVisitor()` (single transformation) or `getRecipeList()` (composite recipes)

**Example: Simple Recipe**
```java
public class MyRecipe extends Recipe {
    @Override
    public String getDisplayName() { return "Transform X to Y"; }
    @Override
    public String getDescription() { return "..."; }
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() { /* transformation logic */ };
    }
}
```

**Example: Composite Recipe** (see `Java21MigrationRecipes.java`)
```java
@Override
public List<Recipe> getRecipeList() {
    return Arrays.asList(
        new VehicleToCarRecipe(),
        new ChangeConstantsReference()
    );
}
```

### AST Transformation Patterns
- **Single class transformations**: Use `JavaIsoVisitor<ExecutionContext>`
- **Multiple visitor patterns**: Use `TreeVisitor<?, ExecutionContext>`
- **Import handling**: Use `maybeRemoveImport("com.old.Class")` and `maybeAddImport("com.new.Class")`
- **Type preservation**: Use `JavaType.ShallowClass.build(...)` for custom types
- **Keep whitespace**: Preserve `J.Import` node prefixes when modifying imports

See `ChangeConstantReference.java` for import handling examples.

## Testing Recipes (TDD Approach)

**Always write tests first** — they define expected behavior.

### Test Structure
Tests extend `RewriteTest` and use the `rewriteRun()` pattern:

```java
@Override
public void defaults(RecipeSpec spec) {
    spec.recipe(new MyRecipe());
}

@Test
void myTransformWorks() {
    rewriteRun(
        java(
            """
            // OLD CODE
            class OldClass extends Vehicle {}
            """,
            """
            // NEW CODE
            class OldClass extends Car {}
            """
        )
    );
}
```

### Testing Best Practices
- Use `JavaParser.fromJavaVersion()` for Java 21 compatibility
- Include complete context: if testing class extends, include the full class definition in both input and output
- Test edge cases: generics, imports, multiple references
- See `VehicleToCarRecipeTest.java` and `ChangeConstantsReferenceTest.java` for working examples
- Include supporting classes when needed (see `src/test/java/com/example/` for fixture classes)

## Configuration & Registration

### Adding a New Recipe
1. **Create recipe class** in `src/main/java/com/rewrite/MyNewRecipe.java`
2. **Register in recipe registry**: Add to `src/main/resources/META-INF/rewrite/rewrite.yml`
   ```yaml
   recipeList:
     - com.rewrite.MyNewRecipe
   ```
3. **External configuration** (optional):
   - Place config files in `src/main/resources/constants/` (e.g., `my-config.yaml`)
   - Example: `change-constants.yaml` — currently embedded in Java, can be externalized if needed

### OpenRewrite Dependency Management
- **Version Source**: `build.gradle` references `rewrite-bom:8.66.1`
- **Core Modules**: `rewrite-java`, `rewrite-gradle`, `rewrite-java-21`
- **Test Module**: `rewrite-test` (provides `RewriteTest` base class)
- **Update process**: Change `openrewrite-bom` version in `gradle/libs.versions.toml`

## Agent Workflow (Step-by-Step)

When asked to modify this repository, follow this order:

### For Adding/Modifying a Recipe
1. **Write the test first** (`src/test/java/com/rewrite/MyRecipeTest.java`)
   - Define input and expected output
   - This clarifies the transformation before coding
2. **Implement the recipe** (`src/main/java/com/rewrite/MyRecipe.java`)
   - Extend `Recipe`, implement required methods
   - Use existing recipes as templates
3. **Register in recipe registry** (`src/main/resources/META-INF/rewrite/rewrite.yml`)
4. **Test and validate**:
   ```bash
   ./gradlew test --tests 'com.rewrite.MyRecipeTest'
   ./gradlew build  # Full build with all tests
   ```

### For Bug Fixes
1. Write a failing test that reproduces the bug
2. Fix the bug in the recipe
3. Verify: `./gradlew test` (all tests pass)
4. Check build: `./gradlew clean build`

### For Publishing
1. Ensure all tests pass: `./gradlew test`
2. Build artifact: `./gradlew clean build`
3. Publish locally: `./gradlew publishToMavenLocal`

## Reference Files (Code Templates)

Use these files as templates when creating new recipes:

| File | Purpose |
|------|---------|
| `VehicleToCarRecipe.java` | Simple visitor-based transformation + import handling |
| `ChangeConstantReference.java` | Single constant migration with complex import logic |
| `ChangeConstantsReference.java` | Multiple constant migration (batch operations) |
| `Java21MigrationRecipes.java` | Composite recipe pattern (aggregates multiple recipes) |
| `VehicleToCarRecipeTest.java` | Test structure for simple recipes |
| `ChangeConstantsReferenceTest.java` | Advanced test patterns with edge cases |
| `src/main/resources/META-INF/rewrite/rewrite.yml` | Recipe registration format |

## Common Errors & Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| `Could not get unknown property 'libsXyz'` | Library not in `gradle/libs.versions.toml` | Check spelling, use correct accessor format (hyphens → dots) |
| `getVisitor() returns null` | Recipe doesn't implement transformation | Implement `getVisitor()` returning a `JavaIsoVisitor` or override `getRecipeList()` |
| `Test fails: expected X but got Y` | Transformation not applied correctly | Add debug output to visitor, check visitor method names (prefix, visit, etc.) |
| `BUILD FAILED: compilation error` | Java syntax error in recipe | Run `./gradlew compileJava` to get detailed error location |
| `Import statements not updating` | Using wrong import helper | Use `maybeRemoveImport()` + `maybeAddImport()` or `addImport()` directly |

## Troubleshooting

**Build fails after code changes?**
- Run: `./gradlew clean build --stacktrace` to see full error
- Check: `./gradlew compileJava` for compilation errors specifically

**Test fails but logic seems right?**
- Add the full class definition to both input and output in test
- Check for import differences (test may expect imports to be added/removed)
- Verify the recipe is registered in `META-INF/rewrite/rewrite.yml`

**Recipe not applied during test?**
- Ensure recipe is registered: `spec.recipe(new MyRecipe())`
- Verify `getVisitor()` or `getRecipeList()` is implemented correctly
- Check visitor method is being called (add logging if needed)

**If something's unclear**

Refer to the failing test or existing recipe that does something similar. Tests are the single best source of truth about expected behavior.

---
