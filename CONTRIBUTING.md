# Contributing to OpenRewrite Custom Recipes

Thank you for your interest in contributing! This document provides guidelines for participating in this project.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Branch Naming](#branch-naming)
- [Commit Messages](#commit-messages)
- [Submitting Changes](#submitting-changes)
- [Code Style](#code-style)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)
- [Code Review Process](#code-review-process)

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/openrewrite-custom-recipes.git
   cd openrewrite-custom-recipes
   ```
3. **Add upstream remote** for syncing:
   ```bash
   git remote add upstream https://github.com/original-owner/openrewrite-custom-recipes.git
   ```
4. **Create a branch** following [branch naming conventions](#branch-naming)

## Development Setup

### Prerequisites

- Java 21 or higher
- Git

### Initial Setup

```bash
# Verify Java version
java -version

# Verify gradle wrapper
./gradlew --version

# Run tests to ensure environment is working
./gradlew test
```

### Common Commands

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests 'com.rewrite.MyRecipeTest'

# Clean build
./gradlew clean build

# Compile only (fast feedback)
./gradlew compileJava compileTestJava

# Build without running tests
./gradlew build -x test

# Publish to local Maven for testing
./gradlew publishToMavenLocal
```

## Branch Naming

Use descriptive branch names with the following prefixes:

| Prefix | Purpose | Example |
|--------|---------|---------|
| `feature/` | New recipe or feature | `feature/add-logging-recipe` |
| `fix/` | Bug fixes | `fix/imports-not-updating` |
| `docs/` | Documentation only | `docs/update-readme` |
| `test/` | Test improvements | `test/add-edge-cases` |
| `refactor/` | Code refactoring | `refactor/simplify-visitor` |
| `chore/` | Build/deps/tooling | `chore/update-gradle` |

### Branch Naming Rules

- Use lowercase letters only
- Use hyphens to separate words (not underscores or spaces)
- Keep names concise but descriptive (max 40 characters)
- Reference issue number if applicable: `feature/recipe-description-#123`

### Example Branch Names

```
feature/add-java-22-recipe
feature/support-sealed-classes-#42
fix/import-handling-generics
docs/add-recipe-examples
test/improve-coverage-constant-reference
refactor/extract-import-helper
chore/upgrade-gradle-wrapper
```

## Commit Messages

Follow the **Conventional Commits** specification for clear, semantic messages.

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type

Must be one of the following:

- **feat** — New feature or recipe
- **fix** — Bug fix
- **docs** — Documentation changes only
- **test** — Test additions or changes (no code change)
- **refactor** — Code refactoring (no functional change)
- **perf** — Performance improvements
- **chore** — Build, dependencies, tooling changes

### Scope (Optional but Recommended)

The component or recipe affected:

- `VehicleToCarRecipe` — specific recipe name
- `ChangeConstantsReference` — specific recipe name
- `gradle` — build system
- `tests` — test utilities
- `docs` — documentation

### Subject

- Use imperative mood: "add" not "added" or "adds"
- Don't capitalize the first letter
- Don't end with a period
- Maximum 50 characters

### Body (Optional for Small Changes)

- Explain **what** and **why**, not how (code shows the how)
- Wrap at 72 characters
- Separate from subject with a blank line

### Footer (Optional)

- Close issues: `Fixes #123` or `Closes #456`
- Note breaking changes: `BREAKING CHANGE: description`

### Commit Message Examples

#### Simple Fix

```
fix(VehicleToCarRecipe): handle wildcard imports correctly
```

#### Feature with Breaking Change

```
feat(ChangeConstantsReference): add support for nested class constants

Previously, nested class constants were ignored. This change adds
full support for migrating constants from nested classes.

BREAKING CHANGE: constant mappings now require fully qualified
names including the parent class name (e.g., OuterClass.InnerClass.CONSTANT).

Fixes #88
```

#### Documentation Update

```
docs: add quick start guide to README
```

#### Test Improvement

```
test(Java21MigrationRecipes): add edge case for diamond operator

Added test cases to verify correct transformation of diamond
operator usage when combined with Java 21 features.
```

#### Refactoring

```
refactor(VehicleToCarRecipe): extract type building logic to helper method

Improves code readability and reduces duplication.
```

## Submitting Changes

### Before You Start

1. **Check existing issues** — Avoid duplicate work
2. **Discuss major changes** — Open an issue first for significant features
3. **Keep changes focused** — One recipe or feature per PR

### Development Process

1. **Keep your fork in sync**:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Write tests first** (TDD approach):
   - Create `src/test/java/com/rewrite/YourRecipeTest.java`
   - Define input and expected output
   - Run: `./gradlew test --tests 'com.rewrite.YourRecipeTest'`

4. **Implement the recipe**:
   - Create `src/main/java/com/rewrite/YourRecipe.java`
   - Extend `Recipe` and implement required methods
   - Use existing recipes as templates

5. **Register the recipe**:
   - Add to `src/main/resources/META-INF/rewrite/rewrite.yml`

6. **Make commits**:
   - Follow Conventional Commits format
   - Keep commits logical and atomic
   - One concern per commit when possible

7. **Validate locally**:
   ```bash
   ./gradlew test                    # Run all tests
   ./gradlew clean build             # Full build
   ./gradlew compileJava             # Check for errors
   ```

8. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

9. **Create a Pull Request**:
   - Use the PR template
   - Fill in all sections
   - Reference related issues
   - Describe testing approach

### Pull Request Guidelines

- **Title**: Use clear, descriptive title (same format as branch name)
- **Description**: Explain what, why, and how
- **Testing**: Describe testing approach and results
- **Checklist**: Complete all items before submitting
- **Updates**: Keep PR updated if feedback requires changes

### PR Authorship

**Important**: When creating a pull request, you are the **sole author**. Do not include co-authors in the PR.

**Requirements**:
- ✅ No co-author trailers in commit messages (no `Co-Authored-By:` footers)
- ✅ Single author in GitHub PR settings
- ✅ All commits authored by you only
- ✅ Clear accountability and authorship history

**Why This Matters**:
- Maintains clean git history for `git blame` and commit tracking
- Ensures accurate contributor recognition
- Clarifies responsibility for code changes
- Simplifies issue resolution and code maintenance

If you collaborated with others on the work, acknowledge them in the PR description instead of using co-author trailers.

## Code Style

### Java Conventions

- **Naming**:
  - Classes: `PascalCase`, end with `Recipe` for recipes
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Package: `com.rewrite.*`

- **Formatting**:
  - 4-space indentation (not tabs)
  - Line length: keep under 120 characters
  - One statement per line
  - Space after keywords, before braces

- **Documentation**:
  - Add JavaDoc to public methods
  - Explain complex visitor logic
  - Include usage examples for new recipes

### Recipe Structure

Every recipe must implement:

```java
public class MyRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "User-friendly name";
    }

    @Override
    public String getDescription() {
        return "Detailed description of transformation";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            // Visitor implementation
        };
    }
}
```

## Testing Guidelines

### Test Structure

Tests extend `RewriteTest` and use the `rewriteRun()` pattern:

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
                "class Foo { }",    // Input
                "class Bar { }"     // Expected output
            )
        );
    }
}
```

### Testing Best Practices

- **Write tests first** (TDD) before implementing recipes
- **Include complete context**: full class definitions, not fragments
- **Test edge cases**: generics, imports, nested classes, wildcards
- **Use meaningful test names**: describe what's being tested
- **Test one thing per test method**: focused, isolated tests
- **Include imports in tests**: test assumes imports are correct

### Test Coverage Requirements

- [ ] All recipes must have corresponding test classes
- [ ] Tests cover happy path (normal transformations)
- [ ] Tests cover edge cases (generics, complex types, etc.)
- [ ] Tests verify import handling (additions, removals)
- [ ] Composite recipes tested as a whole
- [ ] All tests pass: `./gradlew test`

## Documentation

### README Requirements

- Quick start / getting started
- List of recipes with descriptions
- Build and test commands
- Publishing instructions

### Recipe Documentation

Create `docs/YourRecipe.md`:

```markdown
# YourRecipe

## Description
What this recipe does and why.

## Example
Before → After transformation example

## Configuration
Any configuration options (if applicable)
```

### Code Comments

- Comment **why**, not what (code shows the what)
- Explain complex visitor logic
- Document assumptions and constraints

## Code Review Process

### What to Expect

1. **Automated checks** run first:
   - Tests must pass
   - Build must succeed
   - No compilation errors

2. **Maintainer review**:
   - Code style compliance
   - Test coverage
   - Design and approach
   - Documentation

3. **Feedback loop**:
   - Address comments thoughtfully
   - Push additional commits (don't force-push)
   - Re-request review when ready

### Making Changes After Review

- **Don't force-push**: Add new commits for changes
- **Comment on feedback**: Acknowledge and explain changes
- **Request re-review**: Use GitHub's review request feature

### Merging

- PRs must be approved by at least one maintainer
- All checks must pass
- Commits will be squashed for clean history (usually)

## Questions or Need Help?

- **Open an issue** for questions about features
- **Check existing issues** before opening new ones
- **Ask in PR comments** for implementation guidance
- **Reference test files** for code examples

## Code of Conduct

Be respectful and constructive. Treat everyone with kindness and respect. We're here to build something great together.

## License

By contributing, you agree that your contributions will be licensed under the same license as the project. Check the LICENSE file for details.

---

**Thank you for contributing!** Your effort helps make this project better. 🎉

