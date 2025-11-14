# Text Blocks (JDK 13 Preview → JDK 15 Final)

## Overview

Text blocks are multi-line string literals that avoid the need for most escape sequences and automatically format strings in a predictable way. They were introduced as a preview in JDK 13 and became final in JDK 15.

## What Problem Does It Solve?

Before text blocks, multi-line strings required verbose concatenation and escape sequences:

```java
// Pre-JDK 15: Traditional approach
String json = "{\n" +
              "  \"name\": \"John Doe\",\n" +
              "  \"age\": 30,\n" +
              "  \"email\": \"john@example.com\"\n" +
              "}";

String sql = "SELECT u.id, u.username, u.email, o.order_date\n" +
             "FROM users u\n" +
             "JOIN orders o ON u.id = o.user_id\n" +
             "WHERE u.active = true\n" +
             "ORDER BY o.order_date DESC";

String html = "<html>\n" +
              "  <body>\n" +
              "    <h1>Hello World</h1>\n" +
              "  </body>\n" +
              "</html>";
```

## The Text Blocks Solution

```java
// JDK 15+: Using text blocks
String json = """
    {
      "name": "John Doe",
      "age": 30,
      "email": "john@example.com"
    }
    """;

String sql = """
    SELECT u.id, u.username, u.email, o.order_date
    FROM users u
    JOIN orders o ON u.id = o.user_id
    WHERE u.active = true
    ORDER BY o.order_date DESC
    """;

String html = """
    <html>
      <body>
        <h1>Hello World</h1>
      </body>
    </html>
    """;
```

## Syntax Rules

### 1. Opening and Closing Delimiters

Text blocks start with `"""` followed by a line terminator and end with `"""`:

```java
String text = """
    This is a text block
    """;
```

### 2. Indentation Management

The compiler automatically removes incidental whitespace:

```java
public class Example {
    String query = """
        SELECT *
        FROM users
        WHERE active = true
        """;  // Closing delimiter position determines indentation removal
}
```

### 3. Trailing Whitespace

By default, trailing whitespace is removed. Use `\s` to preserve it:

```java
String text = """
    Line with trailing spaces\s\s\s
    """;
```

### 4. Line Terminators

Text blocks normalize line terminators to `\n`:

```java
String text = """
    Line 1
    Line 2
    Line 3
    """;
// Always uses \n regardless of platform
```

## Use Cases in Backend Development

### 1. SQL Queries

```java
public class UserRepository {

    private static final String FIND_ACTIVE_USERS = """
        SELECT
            u.id,
            u.username,
            u.email,
            u.created_at,
            COUNT(o.id) as order_count,
            SUM(o.total_amount) as total_spent
        FROM users u
        LEFT JOIN orders o ON u.id = o.user_id
        WHERE u.active = true
          AND u.created_at > ?
        GROUP BY u.id, u.username, u.email, u.created_at
        HAVING COUNT(o.id) > 0
        ORDER BY total_spent DESC
        LIMIT ?
        """;

    public List<UserStats> findActiveUsersWithOrders(LocalDate since, int limit) {
        return jdbcTemplate.query(
            FIND_ACTIVE_USERS,
            userStatsRowMapper,
            since,
            limit
        );
    }

    private static final String BULK_INSERT_USERS = """
        INSERT INTO users (username, email, password_hash, created_at)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (email) DO UPDATE
        SET username = EXCLUDED.username,
            password_hash = EXCLUDED.password_hash,
            updated_at = CURRENT_TIMESTAMP
        """;

    public void upsertUser(User user) {
        jdbcTemplate.update(
            BULK_INSERT_USERS,
            user.username(),
            user.email(),
            user.passwordHash(),
            user.createdAt()
        );
    }
}
```

### 2. JSON Templates

```java
public class ApiResponseBuilder {

    public String buildErrorResponse(String message, List<String> errors, int statusCode) {
        return """
            {
              "status": "error",
              "statusCode": %d,
              "message": "%s",
              "errors": [
                %s
              ],
              "timestamp": "%s"
            }
            """.formatted(
                statusCode,
                message,
                errors.stream().map(e -> "\"" + e + "\"").collect(Collectors.joining(", ")),
                Instant.now()
            );
    }

    public String buildSuccessResponse(Object data) {
        return """
            {
              "status": "success",
              "data": %s,
              "timestamp": "%s"
            }
            """.formatted(
                objectMapper.writeValueAsString(data),
                Instant.now()
            );
    }
}
```

### 3. Email Templates

```java
public class EmailService {

    public void sendWelcomeEmail(User user) {
        String subject = "Welcome to Our Platform!";

        String htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; line-height: 1.6; }
                    .button { background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; display: inline-block; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome, %s!</h1>
                    </div>
                    <div class="content">
                        <p>Thank you for joining our platform. We're excited to have you on board!</p>
                        <p>Your account has been successfully created with the following details:</p>
                        <ul>
                            <li><strong>Username:</strong> %s</li>
                            <li><strong>Email:</strong> %s</li>
                            <li><strong>Member since:</strong> %s</li>
                        </ul>
                        <p>To get started, click the button below:</p>
                        <a href="https://example.com/getting-started" class="button">Get Started</a>
                        <p>If you have any questions, feel free to reply to this email.</p>
                        <p>Best regards,<br>The Team</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                user.username(),
                user.username(),
                user.email(),
                user.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
            );

        emailClient.send(user.email(), subject, htmlBody);
    }

    public void sendPasswordResetEmail(User user, String resetToken) {
        String textBody = """
            Hello %s,

            We received a request to reset your password. If you didn't make this request, please ignore this email.

            To reset your password, click the link below:
            https://example.com/reset-password?token=%s

            This link will expire in 24 hours.

            If you're having trouble clicking the link, copy and paste the URL into your browser.

            Best regards,
            The Team
            """.formatted(user.username(), resetToken);

        emailClient.send(user.email(), "Password Reset Request", textBody);
    }
}
```

### 4. XML/SOAP Messages

```java
public class SoapClient {

    public String createSoapRequest(String operation, Map<String, String> parameters) {
        String params = parameters.entrySet().stream()
            .map(e -> "        <%s>%s</%s>".formatted(e.getKey(), e.getValue(), e.getKey()))
            .collect(Collectors.joining("\n"));

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <soap:Envelope
                xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <soap:Header>
                <Authentication>
                  <Username>%s</Username>
                  <Password>%s</Password>
                </Authentication>
              </soap:Header>
              <soap:Body>
                <%s>
            %s
                </%s>
              </soap:Body>
            </soap:Envelope>
            """.formatted(
                config.getUsername(),
                config.getPassword(),
                operation,
                params,
                operation
            );
    }
}
```

### 5. GraphQL Queries

```java
public class GraphQLClient {

    public String getUserWithOrdersQuery(Long userId) {
        return """
            query GetUserWithOrders($userId: ID!) {
              user(id: $userId) {
                id
                username
                email
                profile {
                  firstName
                  lastName
                  phoneNumber
                }
                orders(first: 10, orderBy: CREATED_AT_DESC) {
                  edges {
                    node {
                      id
                      orderNumber
                      totalAmount
                      status
                      createdAt
                      items {
                        id
                        productName
                        quantity
                        price
                      }
                    }
                  }
                }
              }
            }
            """;
    }

    public String createOrderMutation() {
        return """
            mutation CreateOrder($input: CreateOrderInput!) {
              createOrder(input: $input) {
                order {
                  id
                  orderNumber
                  status
                  totalAmount
                }
                errors {
                  field
                  message
                }
              }
            }
            """;
    }
}
```

### 6. Regular Expressions

```java
public class ValidationPatterns {

    // Complex email validation regex
    private static final String EMAIL_PATTERN = """
        ^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$\
        """;

    // Multi-line regex for parsing log files
    private static final Pattern LOG_PATTERN = Pattern.compile("""
        ^\\[(?<timestamp>\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2})\\]\\s
        \\[(?<level>DEBUG|INFO|WARN|ERROR)\\]\\s
        \\[(?<thread>[^\\]]+)\\]\\s
        (?<logger>[^\\s]+)\\s-\\s
        (?<message>.+)$\
        """.trim(), Pattern.MULTILINE);

    public LogEntry parseLogLine(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.matches()) {
            return new LogEntry(
                LocalDateTime.parse(matcher.group("timestamp"), LOG_FORMATTER),
                LogLevel.valueOf(matcher.group("level")),
                matcher.group("thread"),
                matcher.group("logger"),
                matcher.group("message")
            );
        }
        throw new IllegalArgumentException("Invalid log line: " + line);
    }
}
```

### 7. Documentation and Help Text

```java
public class CommandLineApp {

    private static final String HELP_TEXT = """

        Usage: app [OPTIONS] COMMAND [ARGS]

        Options:
          -h, --help              Show this help message and exit
          -v, --verbose           Enable verbose output
          -c, --config FILE       Specify configuration file
          --version               Show version and exit

        Commands:
          start                   Start the application server
          stop                    Stop the application server
          restart                 Restart the application server
          status                  Show application status
          migrate                 Run database migrations
          rollback [STEPS]        Rollback database migrations

        Examples:
          app --config prod.yml start
          app migrate
          app rollback 2

        For more information, visit: https://docs.example.com
        """;

    public void showHelp() {
        System.out.println(HELP_TEXT);
    }
}
```

### 8. Test Data and Fixtures

```java
@SpringBootTest
public class UserServiceTest {

    private static final String SAMPLE_JSON_USERS = """
        [
          {
            "username": "alice",
            "email": "alice@example.com",
            "role": "ADMIN"
          },
          {
            "username": "bob",
            "email": "bob@example.com",
            "role": "USER"
          },
          {
            "username": "charlie",
            "email": "charlie@example.com",
            "role": "USER"
          }
        ]
        """;

    @Test
    void testBulkUserImport() throws JsonProcessingException {
        List<UserDto> users = objectMapper.readValue(
            SAMPLE_JSON_USERS,
            new TypeReference<List<UserDto>>() {}
        );

        userService.bulkImport(users);

        assertEquals(3, userRepository.count());
    }

    @Test
    void testSqlQueryExecution() {
        String setupSql = """
            INSERT INTO users (username, email, active) VALUES
            ('testuser1', 'test1@example.com', true),
            ('testuser2', 'test2@example.com', true),
            ('testuser3', 'test3@example.com', false);
            """;

        jdbcTemplate.execute(setupSql);

        List<User> activeUsers = userRepository.findAllActive();
        assertEquals(2, activeUsers.size());
    }
}
```

### 9. Configuration Files (YAML/TOML Generation)

```java
public class ConfigGenerator {

    public String generateApplicationYaml(DatabaseConfig dbConfig, ServerConfig serverConfig) {
        return """
            spring:
              application:
                name: %s
              datasource:
                url: jdbc:postgresql://%s:%d/%s
                username: %s
                password: ${DB_PASSWORD}
                hikari:
                  maximum-pool-size: %d
                  minimum-idle: %d
                  connection-timeout: 30000
              jpa:
                hibernate:
                  ddl-auto: validate
                show-sql: %s
                properties:
                  hibernate:
                    format_sql: true
                    dialect: org.hibernate.dialect.PostgreSQLDialect

            server:
              port: %d
              servlet:
                context-path: /api
              compression:
                enabled: true

            logging:
              level:
                root: INFO
                com.example: DEBUG
              pattern:
                console: "%%d{yyyy-MM-dd HH:mm:ss} - %%msg%%n"
            """.formatted(
                serverConfig.appName(),
                dbConfig.host(),
                dbConfig.port(),
                dbConfig.database(),
                dbConfig.username(),
                dbConfig.maxPoolSize(),
                dbConfig.minIdle(),
                dbConfig.showSql(),
                serverConfig.port()
            );
    }
}
```

## String Interpolation Alternative

While Java doesn't have native string interpolation, text blocks work well with `formatted()` or `String.format()`:

```java
String name = "Alice";
int age = 30;

String message = """
    Hello, %s!
    You are %d years old.
    """.formatted(name, age);

// Or with positional arguments
String template = """
    Name: %1$s
    Age: %2$d
    Status: %1$s is %2$d years old
    """.formatted(name, age);
```

## Escape Sequences

### Special Escapes in Text Blocks

- `\s` - Preserve trailing space
- `\<line-terminator>` - Continue on next line without adding newline

```java
// Line continuation
String long = """
    This is a very long line that \
    continues on the next line without \
    a line break.
    """;
// Result: "This is a very long line that continues on the next line without a line break.\n"

// Preserve trailing whitespace
String trailing = """
    Line with trailing spaces\s\s\s
    """;
```

## Migration Strategy

1. **Identify Multi-Line Strings**: Search for string concatenations with `\n`
2. **Convert SQL Queries**: Start with SQL - highest readability gain
3. **Convert JSON/XML Templates**: These benefit greatly from text blocks
4. **Update Tests**: Convert test fixtures and sample data
5. **Review Indentation**: Ensure closing delimiter position is correct

## Performance

- **No runtime overhead**: Text blocks are processed at compile-time
- **Same as concatenation**: Results in identical bytecode
- **Memory efficient**: Single string object, not multiple concatenations

## Common Pitfalls

1. **Forgetting line terminator after opening `"""`**
   ```java
   // ✗ Won't compile
   String text = """This is wrong""";

   // ✓ Correct
   String text = """
       This is correct
       """;
   ```

2. **Unintended indentation removal**
   ```java
   String text = """
       Line 1
         Line 2 indented
       """;  // Position of closing delimiter matters!
   ```

3. **Trailing whitespace handling**
   ```java
   // Trailing spaces removed by default
   String text = """
       Line with spaces
       """;  // Spaces are removed

   // Use \s to preserve
   String text = """
       Line with spaces\s\s\s
       """;
   ```

## IDE Support

Modern IDEs provide excellent support:
- Syntax highlighting
- Auto-formatting
- Convert between traditional strings and text blocks
- Preview rendered output

## Summary

Text blocks dramatically improve readability and maintainability of multi-line strings, especially for SQL, JSON, XML, HTML, and other structured text common in backend development. They eliminate escape sequence hell and make code self-documenting.

**Migration Priority**: HIGH - Safe, improves readability significantly, no runtime impact.
