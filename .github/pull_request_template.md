⚠️ **IMPORTANT**: This PR should be authored by you only. Do not include co-authors. If you collaborated with others, acknowledge them in the description instead of using co-author trailers.

---

## Description

<!-- Brief explanation of the changes and the motivation behind them -->

### What does this PR do?

<!-- Describe what changes are being made and why -->

### Related Issues

<!-- Link to related issues: Fixes #123, Closes #456 -->
<!-- Use keywords like "Fixes" or "Closes" to auto-link issues -->

Fixes #

## Type of Change

<!-- Mark the relevant option with an "x" -->

- [ ] New recipe
- [ ] Bug fix
- [ ] Documentation only
- [ ] Refactoring (no functional change)
- [ ] Test improvements
- [ ] Dependency update
- [ ] Other (please describe):

## Changes Made

<!-- List the concrete changes in this PR -->

-
-
-

## Testing

### How was this tested?

<!-- Describe the testing approach and results -->

### Test Coverage

- [ ] Added new tests for the changes
- [ ] Updated existing tests
- [ ] All tests pass: `./gradlew test`
- [ ] Full build passes: `./gradlew clean build`

### Manual Testing (if applicable)

<!-- Describe any manual testing performed -->

## Breaking Changes

<!-- Check if this PR introduces breaking changes -->

- [ ] No breaking changes
- [ ] Breaking changes (please describe below)

<!-- If breaking changes, describe them here and any migration steps -->

## Checklist

- [ ] Code follows the project style guidelines
- [ ] Branch follows naming convention (`feature/`, `fix/`, `docs/`, etc.)
- [ ] Commit messages follow Conventional Commits format
- [ ] Self-review of own code completed
- [ ] Comments added for complex logic
- [ ] Documentation updated (README, inline docs, etc.)
- [ ] Tests added/updated:
  - [ ] New recipes have corresponding tests
  - [ ] Tests follow TDD pattern (tests use `RewriteTest` + `rewriteRun()`)
  - [ ] Edge cases covered (generics, imports, nested classes)
- [ ] Recipe registered in `src/main/resources/META-INF/rewrite/rewrite.yml`
- [ ] Build succeeds: `./gradlew clean build`
- [ ] All tests pass: `./gradlew test`
- [ ] No warnings or errors in logs

## Screenshots / Examples (if applicable)

<!-- Add screenshots, code diffs, or before/after examples -->

## Additional Notes

<!-- Any other context or notes that reviewers should know -->

---

**Tip**: For recipes, include test examples showing input → output transformation.

