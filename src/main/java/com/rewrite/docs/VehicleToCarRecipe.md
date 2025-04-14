# VehicleToCarRecipe

This OpenRewrite recipe updates all Java classes that extend `Vehicle` to instead extend `Car`.

## Features

- Replaces `extends Vehicle` with `extends Car`
- Updates imports accordingly
- Handles inner classes as well
- Avoids deprecated or removed OpenRewrite APIs (compatible with OpenRewrite 8.23.1)

## Use Case

Suppose you are migrating your codebase where the superclass `Vehicle` is deprecated and should be replaced by `Car`. This recipe helps automate that change across all Java source files.

## How It Works

1. **Parses a temporary Java snippet** (`class Temp extends Car {}`) to create a reusable `Car` type reference.
2. **Traverses all class declarations** using `JavaIsoVisitor`.
3. **Checks if the class extends `Vehicle`**.
4. **Replaces the superclass** with `Car` and adjusts imports.


## Key Features
- 🚀 AST-based transformation
- 📦 Import statement management
- 🐛 Debug logging

## Requirements

- Java 17+
- OpenRewrite version: 8.23.1 or compatible

## Usage

To use this recipe in a rewrite project:

1. Add dependency to your OpenRewrite project
2. Apply recipe in rewrite configuration:
```yaml
recipe:
  - com.rewrite.VehicleToCarRecipe
```
