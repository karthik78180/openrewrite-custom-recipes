# Sealed Classes (JDK 15 Preview → JDK 17 Final)

## Overview

Sealed classes and interfaces restrict which other classes or interfaces may extend or implement them. This feature was introduced as a preview in JDK 15 and became final in JDK 17.

## What Problem Does It Solve?

In traditional Java, a class is either:
- **final**: Cannot be extended at all
- **non-final**: Can be extended by anyone, anywhere

Sealed classes provide a middle ground - you can explicitly control which classes can extend your class or implement your interface.

## Syntax

```java
public sealed class Shape
    permits Circle, Rectangle, Triangle {
}

public final class Circle extends Shape {
    private final double radius;
    // ...
}

public final class Rectangle extends Shape {
    private final double width, height;
    // ...
}

public non-sealed class Triangle extends Shape {
    private final double base, height;
    // ...
}
```

## Permitted Subclass Requirements

Every permitted subclass must be one of:
1. **final** - Cannot be extended further
2. **sealed** - Continues the sealing chain
3. **non-sealed** - Opens the hierarchy again

```java
public sealed interface Payment
    permits CreditCardPayment, BankTransferPayment, CryptoPayment {
}

// Final - end of the chain
public final class CreditCardPayment implements Payment {
    private final String cardNumber;
    // ...
}

// Sealed - continues restriction
public sealed class BankTransferPayment implements Payment
    permits DomesticTransfer, InternationalTransfer {
}

// Non-sealed - anyone can extend
public non-sealed class CryptoPayment implements Payment {
    // Subclasses can extend this freely
}
```

## Compact Form (Same File)

When permitted subclasses are in the same file, you can omit the `permits` clause:

```java
public sealed class Result<T, E> {
    public final static class Success<T, E> extends Result<T, E> {
        private final T value;
        public Success(T value) { this.value = value; }
        public T value() { return value; }
    }

    public final static class Failure<T, E> extends Result<T, E> {
        private final E error;
        public Failure(E error) { this.error = error; }
        public E error() { return error; }
    }
}
```

## Use Cases in Backend Development

### 1. API Response Types

```java
public sealed interface ApiResponse<T>
    permits Success, ClientError, ServerError {
}

public record Success<T>(T data, int statusCode) implements ApiResponse<T> {}
public record ClientError<T>(String message, int statusCode, List<String> errors) implements ApiResponse<T> {}
public record ServerError<T>(String message, int statusCode, Throwable cause) implements ApiResponse<T> {}

// Usage in controller
public ApiResponse<User> getUser(Long id) {
    try {
        User user = userService.findById(id);
        return user != null
            ? new Success<>(user, 200)
            : new ClientError<>("User not found", 404, List.of());
    } catch (Exception e) {
        return new ServerError<>("Internal error", 500, e);
    }
}
```

### 2. Domain Events

```java
public sealed interface OrderEvent
    permits OrderCreated, OrderPaid, OrderShipped, OrderCancelled {
}

public record OrderCreated(Long orderId, Long customerId, Instant timestamp) implements OrderEvent {}
public record OrderPaid(Long orderId, BigDecimal amount, Instant timestamp) implements OrderEvent {}
public record OrderShipped(Long orderId, String trackingNumber, Instant timestamp) implements OrderEvent {}
public record OrderCancelled(Long orderId, String reason, Instant timestamp) implements OrderEvent {}

// Event handler
public void handleOrderEvent(OrderEvent event) {
    switch (event) {
        case OrderCreated e -> notifyOrderCreated(e);
        case OrderPaid e -> processPayment(e);
        case OrderShipped e -> sendShipmentNotification(e);
        case OrderCancelled e -> processRefund(e);
        // No default needed - compiler knows all cases are covered!
    }
}
```

### 3. State Machines

```java
public sealed interface ConnectionState
    permits Disconnected, Connecting, Connected, Failed {
}

public final class Disconnected implements ConnectionState {
    public Connecting connect(String host, int port) {
        return new Connecting(host, port, Instant.now());
    }
}

public final class Connecting implements ConnectionState {
    private final String host;
    private final int port;
    private final Instant startTime;

    public Connecting(String host, int port, Instant startTime) {
        this.host = host;
        this.port = port;
        this.startTime = startTime;
    }

    public Connected succeed(Socket socket) {
        return new Connected(socket, Instant.now());
    }

    public Failed fail(Exception error) {
        return new Failed(error, Instant.now());
    }
}

public final class Connected implements ConnectionState {
    private final Socket socket;
    private final Instant connectedAt;

    public Connected(Socket socket, Instant connectedAt) {
        this.socket = socket;
        this.connectedAt = connectedAt;
    }

    public Disconnected disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            // log error
        }
        return new Disconnected();
    }
}

public final class Failed implements ConnectionState {
    private final Exception error;
    private final Instant failedAt;

    public Failed(Exception error, Instant failedAt) {
        this.error = error;
        this.failedAt = failedAt;
    }

    public Disconnected reset() {
        return new Disconnected();
    }
}
```

### 4. Command Pattern

```java
public sealed interface Command
    permits CreateUserCommand, UpdateUserCommand, DeleteUserCommand {
}

public record CreateUserCommand(String username, String email, String password) implements Command {}
public record UpdateUserCommand(Long id, String email) implements Command {}
public record DeleteUserCommand(Long id) implements Command {}

public class CommandHandler {
    public void handle(Command command) {
        switch (command) {
            case CreateUserCommand c -> userService.create(c.username(), c.email(), c.password());
            case UpdateUserCommand c -> userService.update(c.id(), c.email());
            case DeleteUserCommand c -> userService.delete(c.id());
        }
    }
}
```

### 5. Repository Query Results

```java
public sealed interface QueryResult<T>
    permits Found, NotFound, MultipleFound, QueryError {
}

public record Found<T>(T value) implements QueryResult<T> {}
public record NotFound<T>() implements QueryResult<T> {}
public record MultipleFound<T>(List<T> values) implements QueryResult<T> {}
public record QueryError<T>(String message, Exception cause) implements QueryResult<T> {}

// Repository method
public QueryResult<User> findUserByEmail(String email) {
    try {
        List<User> users = jdbcTemplate.query(
            "SELECT * FROM users WHERE email = ?",
            userRowMapper,
            email
        );

        return switch (users.size()) {
            case 0 -> new NotFound<>();
            case 1 -> new Found<>(users.get(0));
            default -> new MultipleFound<>(users);
        };
    } catch (Exception e) {
        return new QueryError<>("Database error", e);
    }
}
```

### 6. Validation Results

```java
public sealed interface ValidationResult
    permits Valid, Invalid {
}

public record Valid() implements ValidationResult {
    public static final Valid INSTANCE = new Valid();
}

public record Invalid(List<ValidationError> errors) implements ValidationResult {
    public record ValidationError(String field, String message) {}
}

public ValidationResult validateUser(CreateUserRequest request) {
    List<ValidationError> errors = new ArrayList<>();

    if (request.username() == null || request.username().isBlank()) {
        errors.add(new ValidationError("username", "Username is required"));
    }

    if (request.email() == null || !request.email().contains("@")) {
        errors.add(new ValidationError("email", "Valid email is required"));
    }

    if (request.password() == null || request.password().length() < 8) {
        errors.add(new ValidationError("password", "Password must be at least 8 characters"));
    }

    return errors.isEmpty() ? Valid.INSTANCE : new Invalid(errors);
}
```

## Benefits

### 1. Exhaustiveness Checking

The compiler knows all possible subclasses, enabling exhaustive switch statements without `default`:

```java
public String describe(Shape shape) {
    return switch (shape) {
        case Circle c -> "Circle with radius " + c.radius();
        case Rectangle r -> "Rectangle " + r.width() + "x" + r.height();
        case Triangle t -> "Triangle with base " + t.base();
        // No default needed!
    };
}
```

### 2. Better Documentation

The sealed hierarchy is self-documenting - you can see all implementations at a glance:

```java
public sealed interface PaymentMethod
    permits CreditCard, DebitCard, PayPal, BankTransfer {
    // All payment methods are explicitly listed
}
```

### 3. Domain Modeling

Sealed classes are perfect for modeling closed domain concepts:

```java
public sealed interface OrderStatus
    permits Pending, Processing, Shipped, Delivered, Cancelled {
}
```

### 4. Type Safety

Prevents external code from creating invalid subtypes:

```java
// Without sealed: Anyone could create invalid subtypes
public class FakePaymentMethod implements PaymentMethod {} // Not allowed!
```

## Combining Sealed Classes with Records

This is a powerful combination:

```java
public sealed interface Result<T, E> {
    record Success<T, E>(T value) implements Result<T, E> {}
    record Failure<T, E>(E error) implements Result<T, E> {}
}

// Usage
public Result<User, String> findUser(Long id) {
    User user = repository.findById(id);
    return user != null
        ? new Result.Success<>(user)
        : new Result.Failure<>("User not found");
}

// Pattern matching
public void handleResult(Result<User, String> result) {
    switch (result) {
        case Result.Success<User, String>(User user) -> System.out.println("Found: " + user.name());
        case Result.Failure<User, String>(String error) -> System.err.println("Error: " + error);
    }
}
```

## Migration Strategy

1. **Identify Closed Hierarchies**: Look for interfaces/abstract classes where you control all implementations
2. **Add sealed Modifier**: Start with the root type
3. **Specify Permitted Subclasses**: List all known implementations
4. **Mark Subclasses**: Each must be final, sealed, or non-sealed
5. **Remove Defensive Code**: You can now remove `default` cases in switches

## Common Patterns

### Either Type (Result Pattern)

```java
public sealed interface Either<L, R> {
    record Left<L, R>(L value) implements Either<L, R> {}
    record Right<L, R>(R value) implements Either<L, R> {}

    default <T> T fold(Function<L, T> leftMapper, Function<R, T> rightMapper) {
        return switch (this) {
            case Left<L, R>(L left) -> leftMapper.apply(left);
            case Right<L, R>(R right) -> rightMapper.apply(right);
        };
    }
}
```

### Option Type

```java
public sealed interface Option<T> {
    record Some<T>(T value) implements Option<T> {}
    final class None<T> implements Option<T> {
        private static final None<?> INSTANCE = new None<>();
        @SuppressWarnings("unchecked")
        public static <T> None<T> instance() {
            return (None<T>) INSTANCE;
        }
        private None() {}
    }

    default T orElse(T defaultValue) {
        return switch (this) {
            case Some<T>(T value) -> value;
            case None<T> ignored -> defaultValue;
        };
    }
}
```

## Common Pitfalls

1. **Over-sealing**: Don't seal classes that genuinely need extension points
2. **Wrong Subclass Modifier**: Forgetting to mark subclasses as final/sealed/non-sealed
3. **Permits Clause**: Permitted classes must be in the same module/package (if no module)

## Framework Compatibility

### Jackson (JSON)

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Success.class, name = "success"),
    @JsonSubTypes.Type(value = ClientError.class, name = "error")
})
public sealed interface ApiResponse<T> permits Success, ClientError {}
```

### Spring Boot

Sealed classes work seamlessly with Spring:

```java
@RestController
public class OrderController {
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        OrderResult result = orderService.createOrder(request);
        return switch (result) {
            case OrderResult.Success s -> ResponseEntity.ok(s.order());
            case OrderResult.ValidationError e -> ResponseEntity.badRequest().body(e.errors());
            case OrderResult.InsufficientInventory i -> ResponseEntity.status(409).body(i.message());
        };
    }
}
```

## Performance

- **No runtime overhead**: Sealed is a compile-time feature
- **Better JIT optimization**: JVM can optimize knowing all possible types
- **Smaller bytecode**: Less defensive programming needed

## Summary

Sealed classes provide controlled extensibility, making your domain models more explicit, type-safe, and maintainable. Combined with records and pattern matching, they enable powerful algebraic data types in Java.

**Migration Priority**: MEDIUM - High value for domain modeling, but requires thoughtful design.
