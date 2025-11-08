# Claude Instructions for OpenRewrite Custom Recipes

## Project Overview

This repository contains **custom OpenRewrite recipes** — automated Java code transformation rules built on the OpenRewrite framework.

**What You'll Be Doing**:
- Writing/modifying OpenRewrite recipes (Java AST transformations)
- Creating comprehensive tests using TDD principles
- Maintaining recipe registration and configuration
- Ensuring clean builds and test coverage

## Core Technology Stack

- **Language**: Java 21 (target version)
- **Build Tool**: Gradle 9.2.0 (via wrapper)
- **OpenRewrite**: Version 8.66.1 (via `rewrite-bom`)
- **Testing**: JUnit 5 + OpenRewrite test framework
- **Publishing**: Maven local repository

## Codebase Structure

```
src/
  main/
    java/com/rewrite/           # Recipe implementations
      ├── VehicleToCarRecipe.java           # Example: extends transformation
      ├── ChangeConstantReference.java      # Example: single constant migration
      ├── ChangeConstantsReference.java     # Example: batch constant migration
      └── Java21MigrationRecipes.java       # Example: composite recipe
    resources/
      ├── META-INF/rewrite/rewrite.yml      # Recipe registration
      └── constants/change-constants.yaml   # Configuration data
  test/
    java/com/rewrite/           # Test implementations
      ├── *Test.java                        # Recipe tests (use RewriteTest)
      └── com/example/                      # Test fixture classes
```

## What OpenRewrite Does

OpenRewrite is an AST transformation framework that:
1. **Parses** Java code into an Abstract Syntax Tree
2. **Applies** visitor patterns to transform the tree nodes
3. **Outputs** refactored code with proper formatting
4. **Manages** imports, types, and generics automatically

Each recipe is a Java class extending `Recipe` that implements a specific transformation rule.

## Current Recipes in This Repo

| Recipe | Purpose |
|--------|---------|
| `VehicleToCarRecipe` | Replace `extends Vehicle` with `extends Car` (generics-aware) |
| `ChangeConstantReference` | Migrate single constant from one class to another |
| `ChangeConstantsReference` | Migrate multiple constants in batch |
| `Java21MigrationRecipes` | Composite recipe combining the above |

## Commands for Common Tasks

```bash
# Testing
./gradlew test                                          # Run all tests
./gradlew test --tests 'com.rewrite.MyRecipeTest'      # Run single test
./gradlew test --tests 'com.rewrite.*Test'             # Run tests matching pattern

# Building
./gradlew clean build                                   # Full clean build
./gradlew build -x test                                # Build without tests
./gradlew compileJava compileTestJava                  # Compile only

# Publishing
./gradlew publishToMavenLocal                          # Publish to ~/.m2

# Debugging
./gradlew build --stacktrace                           # Show full stack trace
./gradlew build --info                                 # Verbose logging
```

**Key Point**: Always use `./gradlew`, never bare `gradle` command.

## How to Implement a Recipe

### Step 1: Understand the Transformation
Define clearly what code change you want to make:
- Input: Old code pattern
- Output: New code pattern
- Edge cases: Generics, imports, nested classes, etc.

### Step 2: Write Tests First (TDD)
Create `src/test/java/com/rewrite/MyRecipeTest.java`:

```java
public class MyRecipeTest extends RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MyRecipe());
    }

    @Test
    void transformsSimpleCase() {
        rewriteRun(
            java(
                "class Foo { }",  // Input
                "class Bar { }"   // Expected output
            )
        );
    }

    @Test
    void handlesImports() {
        rewriteRun(
            java(
                "import com.old.Thing;\nclass Foo extends Thing {}",
                "import com.new.Thing;\nclass Foo extends Thing {}"
            )
        );
    }
}
```

**Testing Guidelines**:
- Include complete class definitions (not just fragments)
- Test imports, generics, and edge cases
- Use multi-line strings (`"""..."""`) for readability
- See `VehicleToCarRecipeTest.java` and `ChangeConstantsReferenceTest.java` for patterns

### Step 3: Implement the Recipe
Create `src/main/java/com/rewrite/MyRecipe.java`:

```java
public class MyRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Transform X to Y";
    }

    @Override
    public String getDescription() {
        return "Replace extends X with extends Y, updating imports...";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(
                J.ClassDeclaration cd,
                ExecutionContext ctx) {

                // Your transformation logic here
                // Access AST nodes, apply changes, return modified tree

                return cd;
            }
        };
    }
}
```

**Key Methods**:
- `getDisplayName()`: Short user-facing name
- `getDescription()`: Detailed explanation
- `getVisitor()`: Returns visitor that transforms the AST
  - For complex recipes with multiple visitors, override `getRecipeList()` instead (composite pattern)

**Common Visitor Methods** (override as needed):
- `visitClassDeclaration()`: Process class definitions
- `visitFieldAccess()`: Process field/constant references
- `visitImport()`: Process import statements
- `visitIdentifier()`: Process variable names

### Step 4: Register the Recipe
Add to `src/main/resources/META-INF/rewrite/rewrite.yml`:

```yaml
recipeList:
  - com.rewrite.VehicleToCarRecipe
  - com.rewrite.ChangeConstantsReference
  - com.rewrite.MyNewRecipe      # Add here
  - com.rewrite.Java21MigrationRecipes
```

### Step 5: Validate
```bash
./gradlew test --tests 'com.rewrite.MyRecipeTest'  # Run your test
./gradlew build                                     # Full build with all tests
```

## Important Patterns & Conventions

### Naming
- **Package**: `com.rewrite.*`
- **Class names**: End with `Recipe` (e.g., `AddLoggingRecipe`)
- **Test classes**: `[Recipe]Test` (e.g., `AddLoggingRecipeTest`)

### Import Handling
```java
// Add import
maybeAddImport("com.new.ClassName");

// Remove import
maybeRemoveImport("com.old.ClassName");
```

### Type Information
```java
// Build custom type
JavaType.ShallowClass myType = JavaType.ShallowClass.build(
    "com.example.MyClass"
);

// Use TypeTree.build for complex types
TypeTree tree = TypeTree.build("com.example.List");
```

### Preserving Whitespace
When modifying `J.Import` or other nodes, preserve the prefix (leading whitespace):
```java
import.withPrefix(import.getPrefix())  // Maintain formatting
```

## Composite Recipes (Aggregating Multiple Recipes)

For recipes that apply multiple transformations, use `getRecipeList()`:

```java
public class Java21MigrationRecipes extends Recipe {
    @Override
    public String getDisplayName() {
        return "Java 21 migration";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
            new VehicleToCarRecipe(),
            new ChangeConstantsReference()
        );
    }
}
```

This runs recipes in sequence on the same code.

## Code Examples to Reference

When implementing recipes, use these as templates:

| File | Pattern |
|------|---------|
| `VehicleToCarRecipe.java` | Class extends transformation + imports |
| `ChangeConstantReference.java` | Single field/constant migration |
| `ChangeConstantsReference.java` | Batch constant migration |
| `Java21MigrationRecipes.java` | Composite recipe aggregation |
| `VehicleToCarRecipeTest.java` | Simple test structure |
| `ChangeConstantsReferenceTest.java` | Complex test with edge cases |

## Configuration & Resource Files

- **Recipe metadata**: `src/main/resources/META-INF/rewrite/rewrite.yml`
- **Data files**: `src/main/resources/constants/*.yaml` (optional, can be embedded in Java)
- **Test fixtures**: `src/test/java/com/example/*.java` (helper classes for tests)

## Error Prevention Checklist

Before committing:

- [ ] Tests pass: `./gradlew test`
- [ ] Full build succeeds: `./gradlew clean build`
- [ ] Recipe registered in `META-INF/rewrite/rewrite.yml`
- [ ] `getVisitor()` or `getRecipeList()` implemented (not null)
- [ ] Import statements properly added/removed
- [ ] Test includes full class definitions (not fragments)
- [ ] Visitor methods return modified nodes correctly

## Common Issues & Solutions

| Problem | Solution |
|---------|----------|
| `BUILD FAILED: Could not get unknown property` | Library not in `gradle/libs.versions.toml` or wrong accessor name (hyphens → dots) |
| Test runs but transformation not applied | Recipe not registered in `META-INF/rewrite/rewrite.yml` or `getVisitor()` returns null |
| Imports not updating | Use `maybeRemoveImport()`/`maybeAddImport()` or check import node preservation |
| Compilation errors | Run `./gradlew compileJava --stacktrace` to see exact line |
| Test fails: expected X but got Y | Add complete class definition to test, check for missing imports in expected output |

## Development Workflow Recommendations

1. **Before modifying**: Run `./gradlew test` to ensure baseline passes
2. **Write test first**: Define expected behavior in test code
3. **Implement recipe**: Add transformation logic
4. **Validate**: `./gradlew test --tests 'com.rewrite.MyRecipeTest'`
5. **Full build**: `./gradlew clean build` to ensure no regressions
6. **Publish** (if ready): `./gradlew publishToMavenLocal`

## Key Files to Know

- `build.gradle` — Dependency and build configuration
- `gradle/libs.versions.toml` — Library versions
- `src/main/resources/META-INF/rewrite/rewrite.yml` — Recipe registration
- `src/test/java/com/rewrite/*Test.java` — Test patterns (copy these!)
- `gradle/wrapper/gradle-wrapper.properties` — Gradle version (do not modify)

## Branch Naming Conventions

Use prefixes to categorize your work:

```
feature/   — New recipe or feature
  feature/add-logging-recipe
  feature/support-java-22

fix/       — Bug fixes
  fix/imports-not-updating
  fix/test-failure-on-generics

docs/      — Documentation only
  docs/update-readme
  docs/add-recipe-examples

refactor/  — Code refactoring (no functional change)
  refactor/simplify-visitor
  refactor/extract-helper-methods

chore/     — Build, deps, tooling (no code change)
  chore/update-gradle
  chore/add-pre-commit-hooks

test/      — Tests only
  test/add-edge-case-tests
  test/improve-coverage
```

**Guidelines**:
- Keep branch names lowercase
- Use hyphens to separate words (not underscores or spaces)
- Be descriptive but concise (40 characters or less)
- Reference issue number if applicable: `feature/add-new-recipe-#42`

## Commit Message Format

Follow **Conventional Commits** format for clear, structured messages:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type
- `feat` — New feature/recipe
- `fix` — Bug fix
- `docs` — Documentation changes
- `test` — Test changes (no code change)
- `refactor` — Code refactoring (no functional change)
- `perf` — Performance improvements
- `chore` — Build, dependencies, tooling

### Scope (Optional but Recommended)
The recipe or component affected:
- `VehicleToCarRecipe`
- `ChangeConstantsReference`
- `gradle`
- `tests`

### Subject
- Use imperative mood: "add" not "added" or "adds"
- Don't capitalize first letter
- No period at the end
- Max 50 characters

### Body (Optional for Small Changes)
- Explain **what** and **why**, not how (code shows how)
- Wrap at 72 characters
- Separate from subject with blank line

### Footer (Optional)
- Reference issues: `Fixes #123` or `Closes #123`
- Note breaking changes: `BREAKING CHANGE: description`

### Examples

```
feat(VehicleToCarRecipe): add support for generic types

Extend VehicleToCarRecipe to properly handle generic classes
like Vehicle<T> transforming to Car<T>. Previously, generics
were lost during the transformation.

Fixes #42
```

```
fix(ChangeConstantsReference): handle nested class constants

The recipe now correctly identifies and migrates constants from
nested classes, not just top-level classes.

BREAKING CHANGE: constant mappings now require fully qualified
names for nested classes.

Closes #88
```

```
docs(README): add quick start section
```

```
test(Java21MigrationRecipes): add edge case for wildcard imports
```

```
refactor(VehicleToCarRecipe): extract import handling to helper method
```

### Quick Reference
- **Feat**: New recipe or feature → `feat(scope): description`
- **Fix**: Bug fix → `fix(scope): description`
- **Docs**: Documentation → `docs: description` (no scope needed)
- **Test**: Test changes → `test(scope): description`
- **Refactor**: Code cleanup → `refactor(scope): description`

## Submitting a Pull Request

1. **Create a branch** following naming conventions
2. **Make commits** using conventional format
3. **Push to remote** and create PR
4. **Fill PR template** completely (see PR template sections below)
5. **Ensure checks pass**:
   - GitHub Actions CI (if configured)
   - All tests pass locally: `./gradlew test`
   - Build succeeds: `./gradlew clean build`

### PR Template Sections

Your PR should include:

```markdown
## Description
Brief explanation of changes and motivation

## Type of Change
- [ ] New recipe
- [ ] Bug fix
- [ ] Documentation
- [ ] Refactoring
- [ ] Test improvements

## Related Issues
Fixes #123

## Testing
Describe testing approach and results

## Checklist
- [ ] Tests pass locally
- [ ] All tests added/updated
- [ ] Documentation updated
- [ ] No breaking changes (or documented if yes)
```

See `.github/pull_request_template.md` for full template.

### PR Authorship

**Important**: When creating a pull request, you are the **sole author**. Do not include co-authors in the PR.

- **No co-author trailers** in commit messages
- **Single author** in GitHub PR settings
- All commits should be authored by you only
- This ensures clear accountability and authorship history

**Why**: Maintaining clear authorship records helps with:
- Git blame and commit history tracking
- Contributor recognition
- Issue resolution and responsibility
- Code maintenance and support

## When Asking for Help

Provide:
1. The failing test or expected behavior
2. The actual vs. expected output
3. Stack trace if applicable
4. The recipe code (if asking about implementation)

Tests are the single source of truth about expected behavior.

---

**Last Updated**: 2025-11-07
