# Pattern Matching for switch (JDK 17 Preview → JDK 21 Final)

## Overview

Pattern matching for `switch` extends the `switch` statement and expression to work with patterns, making it dramatically more powerful. This feature evolved through several preview versions (JDK 17, 18, 19, 20) and became final in JDK 21.

## What Problem Does It Solve?

Traditional switch statements were limited:
- Only worked with primitives, strings, and enums
- Required verbose if-else chains for type checking
- No destructuring or pattern matching capabilities

```java
// Pre-JDK 21: Verbose if-else chain
public String describe(Object obj) {
    String result;
    if (obj instanceof Integer i) {
        result = String.format("Integer: %d", i);
    } else if (obj instanceof String s) {
        result = String.format("String: %s", s);
    } else if (obj instanceof Long l) {
        result = String.format("Long: %d", l);
    } else {
        result = "Unknown";
    }
    return result;
}
```

## The Pattern Matching Solution

```java
// JDK 21+: Pattern matching switch
public String describe(Object obj) {
    return switch (obj) {
        case Integer i -> String.format("Integer: %d", i);
        case String s  -> String.format("String: %s", s);
        case Long l    -> String.format("Long: %d", l);
        default        -> "Unknown";
    };
}
```

## Key Features

### 1. Type Patterns

```java
public double calculateArea(Shape shape) {
    return switch (shape) {
        case Circle c    -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.width() * r.height();
        case Triangle t  -> 0.5 * t.base() * t.height();
    };
}
```

### 2. Guarded Patterns (when clause)

```java
public String categorizeNumber(Object obj) {
    return switch (obj) {
        case Integer i when i < 0    -> "Negative integer";
        case Integer i when i == 0   -> "Zero";
        case Integer i when i > 0    -> "Positive integer";
        case Double d when d < 0.0   -> "Negative double";
        case Double d when d >= 0.0  -> "Non-negative double";
        case String s                -> "String: " + s;
        default                      -> "Unknown";
    };
}
```

### 3. Record Patterns (Deconstruction)

```java
public record Point(int x, int y) {}

public String describePoint(Object obj) {
    return switch (obj) {
        case Point(int x, int y) when x == 0 && y == 0 -> "Origin";
        case Point(int x, int y) when x == y           -> "On diagonal";
        case Point(int x, int y) when y == 0           -> "On x-axis";
        case Point(int x, int y) when x == 0           -> "On y-axis";
        case Point(int x, int y)                       -> "Point at (%d, %d)".formatted(x, y);
        default                                        -> "Not a point";
    };
}
```

### 4. Null Handling

```java
public String handleValue(String value) {
    return switch (value) {
        case null           -> "No value provided";
        case String s when s.isBlank() -> "Blank string";
        case String s       -> "Value: " + s;
    };
}
```

## Use Cases in Backend Development

### 1. API Response Processing

```java
public sealed interface ApiResponse<T> permits Success, ClientError, ServerError {}
public record Success<T>(T data, int statusCode) implements ApiResponse<T> {}
public record ClientError<T>(String message, int statusCode, List<String> errors) implements ApiResponse<T> {}
public record ServerError<T>(String message, int statusCode, Throwable cause) implements ApiResponse<T> {}

@RestController
public class OrderController {

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        ApiResponse<Order> response = orderService.findById(id);

        return switch (response) {
            case Success<Order>(Order order, int status) ->
                ResponseEntity.ok(order);

            case ClientError<Order>(String msg, int status, List<String> errors) ->
                ResponseEntity.status(status).body(Map.of("message", msg, "errors", errors));

            case ServerError<Order>(String msg, int status, Throwable cause) -> {
                logger.error("Server error: {}", msg, cause);
                yield ResponseEntity.status(status).body(Map.of("message", msg));
            }
        };
    }
}
```

### 2. Domain Event Processing

```java
public sealed interface OrderEvent {}
public record OrderCreated(Long orderId, Long customerId, BigDecimal amount, Instant timestamp) implements OrderEvent {}
public record OrderPaid(Long orderId, String paymentId, Instant timestamp) implements OrderEvent {}
public record OrderShipped(Long orderId, String trackingNumber, Instant timestamp) implements OrderEvent {}
public record OrderDelivered(Long orderId, Instant timestamp) implements OrderEvent {}
public record OrderCancelled(Long orderId, String reason, Instant timestamp) implements OrderEvent {}

public class OrderEventHandler {

    public void handle(OrderEvent event) {
        switch (event) {
            case OrderCreated(var orderId, var customerId, var amount, var timestamp) -> {
                logger.info("Order {} created for customer {} with amount {}", orderId, customerId, amount);
                inventoryService.reserve(orderId);
                emailService.sendOrderConfirmation(customerId, orderId);
            }

            case OrderPaid(var orderId, var paymentId, var timestamp) -> {
                logger.info("Order {} paid with payment {}", orderId, paymentId);
                shippingService.schedule(orderId);
            }

            case OrderShipped(var orderId, var trackingNumber, var timestamp) -> {
                logger.info("Order {} shipped with tracking {}", orderId, trackingNumber);
                emailService.sendShippingNotification(orderId, trackingNumber);
            }

            case OrderDelivered(var orderId, var timestamp) -> {
                logger.info("Order {} delivered", orderId);
                orderRepository.markAsDelivered(orderId, timestamp);
            }

            case OrderCancelled(var orderId, var reason, var timestamp) -> {
                logger.info("Order {} cancelled: {}", orderId, reason);
                inventoryService.release(orderId);
                paymentService.refund(orderId);
            }
        }
    }
}
```

### 3. Validation with Different Error Types

```java
public sealed interface ValidationResult {}
public record Valid() implements ValidationResult {}
public record Invalid(List<String> errors) implements ValidationResult {}
public record Warning(List<String> warnings, Object data) implements ValidationResult {}

public ResponseEntity<?> processValidation(ValidationResult result) {
    return switch (result) {
        case Valid() ->
            ResponseEntity.ok(Map.of("status", "valid"));

        case Invalid(List<String> errors) ->
            ResponseEntity.badRequest().body(Map.of("errors", errors));

        case Warning(List<String> warnings, Object data) ->
            ResponseEntity.ok(Map.of("warnings", warnings, "data", data));
    };
}
```

### 4. Command Pattern with Different Command Types

```java
public sealed interface Command {}
public record CreateUserCommand(String username, String email, String password) implements Command {}
public record UpdateUserCommand(Long id, String email, String firstName, String lastName) implements Command {}
public record DeleteUserCommand(Long id, String reason) implements Command {}
public record BulkImportCommand(List<UserData> users, boolean skipDuplicates) implements Command {}

public class CommandProcessor {

    public CommandResult process(Command command) {
        return switch (command) {
            case CreateUserCommand(var username, var email, var password) -> {
                User user = userService.create(username, email, password);
                yield new CommandResult.Success("User created: " + user.id());
            }

            case UpdateUserCommand(var id, var email, var firstName, var lastName) -> {
                userService.update(id, email, firstName, lastName);
                yield new CommandResult.Success("User updated: " + id);
            }

            case DeleteUserCommand(var id, var reason) when reason.equals("GDPR") -> {
                userService.hardDelete(id);  // Complete deletion for GDPR
                yield new CommandResult.Success("User permanently deleted: " + id);
            }

            case DeleteUserCommand(var id, var reason) -> {
                userService.softDelete(id);  // Soft delete for other reasons
                yield new CommandResult.Success("User deleted: " + id);
            }

            case BulkImportCommand(var users, var skipDuplicates) when users.size() > 1000 -> {
                asyncImportService.scheduleImport(users, skipDuplicates);
                yield new CommandResult.Async("Import scheduled for " + users.size() + " users");
            }

            case BulkImportCommand(var users, var skipDuplicates) -> {
                int imported = bulkImportService.importUsers(users, skipDuplicates);
                yield new CommandResult.Success("Imported " + imported + " users");
            }
        };
    }
}
```

### 5. Complex Query Result Handling

```java
public sealed interface QueryResult<T> {}
public record Found<T>(T value) implements QueryResult<T> {}
public record NotFound<T>() implements QueryResult<T> {}
public record MultipleFound<T>(List<T> values) implements QueryResult<T> {}
public record QueryError<T>(String message, Exception cause) implements QueryResult<T> {}

public ResponseEntity<?> handleQueryResult(QueryResult<User> result) {
    return switch (result) {
        case Found<User>(User user) ->
            ResponseEntity.ok(toDto(user));

        case NotFound<User>() ->
            ResponseEntity.notFound().build();

        case MultipleFound<User>(List<User> users) when users.size() <= 10 ->
            ResponseEntity.ok(users.stream().map(this::toDto).toList());

        case MultipleFound<User>(List<User> users) ->
            ResponseEntity.status(300).body(Map.of(
                "message", "Too many results",
                "count", users.size()
            ));

        case QueryError<User>(String msg, Exception cause) -> {
            logger.error("Query error: {}", msg, cause);
            yield ResponseEntity.status(500).body(Map.of("error", msg));
        }
    };
}
```

### 6. State Machine with Complex Transitions

```java
public sealed interface ConnectionState {}
public record Disconnected() implements ConnectionState {}
public record Connecting(String host, int port, Instant startTime) implements ConnectionState {}
public record Connected(Socket socket, Instant connectedAt) implements ConnectionState {}
public record Failed(Exception error, Instant failedAt, int retryCount) implements ConnectionState {}

public ConnectionState transition(ConnectionState state, ConnectionEvent event) {
    return switch (state) {
        case Disconnected() when event instanceof ConnectEvent(var host, var port) ->
            new Connecting(host, port, Instant.now());

        case Connecting(var host, var port, var startTime)
            when event instanceof SuccessEvent(var socket) ->
            new Connected(socket, Instant.now());

        case Connecting(var host, var port, var startTime)
            when event instanceof FailureEvent(var error) ->
            new Failed(error, Instant.now(), 0);

        case Connected(var socket, var connectedAt)
            when event instanceof DisconnectEvent() -> {
            try {
                socket.close();
            } catch (IOException e) {
                logger.warn("Error closing socket", e);
            }
            yield new Disconnected();
        }

        case Failed(var error, var failedAt, var retryCount)
            when event instanceof RetryEvent() && retryCount < 3 ->
            new Connecting("localhost", 8080, Instant.now());

        case Failed(var error, var failedAt, var retryCount)
            when event instanceof RetryEvent() && retryCount >= 3 ->
            state;  // Max retries reached, stay in failed state

        default -> state;  // No transition for this event in this state
    };
}
```

### 7. Payment Processing

```java
public sealed interface PaymentMethod {}
public record CreditCard(String number, String cvv, YearMonth expiry) implements PaymentMethod {}
public record DebitCard(String number, String pin) implements PaymentMethod {}
public record BankTransfer(String accountNumber, String routingNumber) implements PaymentMethod {}
public record PayPal(String email) implements PaymentMethod {}
public record Crypto(String walletAddress, String currency) implements PaymentMethod {}

public PaymentResult processPayment(PaymentMethod method, BigDecimal amount) {
    return switch (method) {
        case CreditCard(var number, var cvv, var expiry)
            when amount.compareTo(new BigDecimal("10000")) > 0 -> {
            // Large transaction - additional verification
            fraudDetectionService.verify(number, amount);
            yield creditCardProcessor.processLarge(number, cvv, expiry, amount);
        }

        case CreditCard(var number, var cvv, var expiry) ->
            creditCardProcessor.process(number, cvv, expiry, amount);

        case DebitCard(var number, var pin) ->
            debitCardProcessor.process(number, pin, amount);

        case BankTransfer(var accountNumber, var routingNumber)
            when amount.compareTo(new BigDecimal("1000")) > 0 ->
            bankTransferProcessor.processWithVerification(accountNumber, routingNumber, amount);

        case BankTransfer(var accountNumber, var routingNumber) ->
            bankTransferProcessor.process(accountNumber, routingNumber, amount);

        case PayPal(var email) ->
            payPalProcessor.process(email, amount);

        case Crypto(var walletAddress, var currency) when currency.equals("BTC") ->
            cryptoProcessor.processBitcoin(walletAddress, amount);

        case Crypto(var walletAddress, var currency) when currency.equals("ETH") ->
            cryptoProcessor.processEthereum(walletAddress, amount);

        case Crypto(var walletAddress, var currency) ->
            throw new UnsupportedCryptoException("Unsupported cryptocurrency: " + currency);
    };
}
```

## Exhaustiveness Checking

When using sealed types, the compiler can verify all cases are covered:

```java
public sealed interface Result<T, E> permits Success, Failure {}
public record Success<T, E>(T value) implements Result<T, E> {}
public record Failure<T, E>(E error) implements Result<T, E> {}

// No default needed - compiler knows all cases are covered
public <T, E> String describe(Result<T, E> result) {
    return switch (result) {
        case Success<T, E>(T value) -> "Success: " + value;
        case Failure<T, E>(E error) -> "Failure: " + error;
        // No default clause needed!
    };
}
```

## Switch Expressions vs Statements

### Expression (Returns a Value)

```java
String result = switch (obj) {
    case Integer i -> "Integer: " + i;
    case String s  -> "String: " + s;
    default        -> "Unknown";
};
```

### Statement (Performs Actions)

```java
switch (event) {
    case OrderCreated e -> {
        logger.info("Order created: {}", e.orderId());
        processOrder(e);
    }
    case OrderCancelled e -> {
        logger.info("Order cancelled: {}", e.orderId());
        refundOrder(e);
    }
}
```

### Yield for Complex Logic in Expressions

```java
String result = switch (response) {
    case Success<Order>(Order order, int status) -> {
        logger.info("Order retrieved: {}", order.id());
        yield "Order: " + order.id();
    }
    case ClientError<Order>(String msg, int status, var errors) -> {
        logger.warn("Client error: {}", msg);
        yield "Error: " + msg;
    }
    default -> "Unknown response";
};
```

## Migration Strategy

1. **Identify if-else chains**: Look for type-checking patterns
2. **Convert to switch**: Replace with pattern matching switch
3. **Use sealed types**: Where appropriate, make hierarchies sealed for exhaustiveness
4. **Add guards**: Use `when` clauses for additional conditions
5. **Leverage records**: Combine with record patterns for deconstruction

## Benefits

| Aspect | if-else Chain | Pattern Matching Switch |
|--------|--------------|------------------------|
| Readability | Verbose | Concise |
| Exhaustiveness | Manual | Compiler-verified (with sealed types) |
| Refactoring | Error-prone | Safe - compiler catches issues |
| Null handling | Separate check | Built-in |
| Deconstruction | Manual | Automatic (with records) |

## Common Patterns

### Option/Maybe Pattern

```java
public sealed interface Option<T> permits Some, None {}
public record Some<T>(T value) implements Option<T> {}
public record None<T>() implements Option<T> {}

public <T> T getOrDefault(Option<T> option, T defaultValue) {
    return switch (option) {
        case Some<T>(T value) -> value;
        case None<T>() -> defaultValue;
    };
}
```

### Either/Result Pattern

```java
public <L, R, T> T fold(
    Either<L, R> either,
    Function<L, T> leftMapper,
    Function<R, T> rightMapper
) {
    return switch (either) {
        case Left<L, R>(L value) -> leftMapper.apply(value);
        case Right<L, R>(R value) -> rightMapper.apply(value);
    };
}
```

## Performance

- **No additional overhead**: Compiles to efficient bytecode
- **JIT optimization**: HotSpot can optimize pattern matching switches
- **Better than if-else**: More opportunities for JVM optimization

## Summary

Pattern matching for `switch` is one of the most powerful additions to modern Java. Combined with records and sealed types, it enables clean, type-safe, exhaustive handling of complex domain models - a game-changer for backend development.

**Migration Priority**: HIGH - Dramatically improves code quality, especially when combined with sealed types and records.
