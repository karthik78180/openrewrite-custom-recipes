# Foreign Function & Memory API (JDK 19 Preview → JDK 22 Final)

## Overview

The Foreign Function & Memory (FFM) API provides a safe and efficient way to interoperate with native code and manage off-heap memory. It replaces JNI (Java Native Interface) with a modern, safer alternative. This feature was previewed in JDK 19-21 and became final in JDK 22.

## What Problem Does It Solve?

Traditional JNI had several issues:
- **Complex**: Required writing C/C++ code and managing build toolchains
- **Unsafe**: Easy to crash the JVM with incorrect native code
- **Error-prone**: Manual memory management prone to leaks
- **Performance**: Overhead from crossing JNI boundary

```java
// Pre-JDK 22: JNI approach
public class NativeLib {
    static {
        System.loadLibrary("mylib");
    }

    // Requires corresponding C code
    public native int calculateHash(byte[] data);
    public native void processImage(long imagePtr);
}
```

## The FFM Solution

The FFM API provides:
- **Pure Java**: No C/C++ code required
- **Type-safe**: Compile-time checking
- **Memory-safe**: Automatic bounds checking
- **Efficient**: Zero-copy operations

```java
// JDK 22+: FFM API
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class ModernNativeAccess {

    public static void main(String[] args) throws Throwable {
        // Load library
        SymbolLookup stdlib = Linker.nativeLinker().defaultLookup();

        // Find function
        MemorySegment strlen = stdlib.find("strlen").orElseThrow();

        // Create method handle
        FunctionDescriptor descriptor = FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,    // return type
            ValueLayout.ADDRESS       // parameter type
        );

        MethodHandle strlenHandle = Linker.nativeLinker()
            .downcallHandle(strlen, descriptor);

        // Call native function
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment str = arena.allocateUtf8String("Hello, World!");
            long length = (long) strlenHandle.invoke(str);
            System.out.println("String length: " + length);
        }
    }
}
```

## Key Components

### 1. Memory Segments

Represents a contiguous region of memory:

```java
// Allocate off-heap memory
try (Arena arena = Arena.ofConfined()) {
    // Allocate 100 bytes
    MemorySegment segment = arena.allocate(100);

    // Write data
    segment.set(ValueLayout.JAVA_INT, 0, 42);
    segment.set(ValueLayout.JAVA_LONG, 8, 12345L);

    // Read data
    int value = segment.get(ValueLayout.JAVA_INT, 0);
    long longValue = segment.get(ValueLayout.JAVA_LONG, 8);

    System.out.println("Int: " + value + ", Long: " + longValue);
}  // Memory automatically freed
```

### 2. Arena (Memory Management)

Manages lifetime of memory segments:

```java
// Confined arena - single thread access
try (Arena arena = Arena.ofConfined()) {
    MemorySegment seg = arena.allocate(1024);
    // Use segment
}  // Automatically freed

// Shared arena - multi-thread access
try (Arena arena = Arena.ofShared()) {
    MemorySegment seg = arena.allocate(1024);
    // Can be accessed from multiple threads
}

// Global arena - never freed (use sparingly)
Arena global = Arena.global();
MemorySegment persistent = global.allocate(1024);
// Lives until JVM shutdown
```

### 3. Foreign Functions

Call native library functions:

```java
public class MathLibrary {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = LINKER.defaultLookup();

    // Load sqrt function from libm
    private static final MethodHandle SQRT = LINKER.downcallHandle(
        LOOKUP.find("sqrt").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE)
    );

    public static double sqrt(double value) throws Throwable {
        return (double) SQRT.invoke(value);
    }

    public static void main(String[] args) throws Throwable {
        double result = sqrt(144.0);
        System.out.println("sqrt(144) = " + result);
    }
}
```

## Use Cases in Backend Development

### 1. High-Performance Data Processing

```java
public class ImageProcessor {

    private static final MethodHandle PROCESS_IMAGE;

    static {
        System.loadLibrary("imageproc");  // Load native library
        SymbolLookup lookup = SymbolLookup.loaderLookup();

        PROCESS_IMAGE = Linker.nativeLinker().downcallHandle(
            lookup.find("process_rgba_image").orElseThrow(),
            FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS,    // image data
                ValueLayout.JAVA_INT,   // width
                ValueLayout.JAVA_INT,   // height
                ValueLayout.JAVA_INT    // operation
            )
        );
    }

    public void processImage(byte[] imageData, int width, int height, Operation op) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            // Copy image data to off-heap memory
            MemorySegment segment = arena.allocate(imageData.length);
            MemorySegment.copy(imageData, 0, segment, ValueLayout.JAVA_BYTE, 0, imageData.length);

            // Call native function
            PROCESS_IMAGE.invoke(segment, width, height, op.ordinal());

            // Copy result back
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0, imageData, 0, imageData.length);
        }
    }
}
```

### 2. Direct Memory Buffers for Network I/O

```java
public class ZeroCopyNetworkHandler {

    public void sendLargeFile(SocketChannel channel, Path filePath) throws Exception {
        try (Arena arena = Arena.ofConfined();
             FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.READ)) {

            long fileSize = fileChannel.size();

            // Allocate off-heap buffer
            MemorySegment buffer = arena.allocate(fileSize);

            // Read file directly into off-heap memory
            fileChannel.read(buffer.asByteBuffer());

            // Send buffer (zero-copy)
            ByteBuffer bb = buffer.asByteBuffer();
            while (bb.hasRemaining()) {
                channel.write(bb);
            }
        }
    }

    public byte[] receiveData(SocketChannel channel, int size) throws Exception {
        try (Arena arena = Arena.ofConfined()) {
            // Allocate off-heap buffer
            MemorySegment buffer = arena.allocate(size);

            // Read directly into off-heap memory
            ByteBuffer bb = buffer.asByteBuffer();
            channel.read(bb);

            // Copy to heap if needed
            byte[] result = new byte[size];
            MemorySegment.copy(buffer, ValueLayout.JAVA_BYTE, 0, result, 0, size);

            return result;
        }
    }
}
```

### 3. Interfacing with System Libraries

```java
public class SystemInfo {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = LINKER.defaultLookup();

    // Call getpid() from libc
    private static final MethodHandle GET_PID = LINKER.downcallHandle(
        LOOKUP.find("getpid").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_INT)
    );

    // Call getuid() from libc
    private static final MethodHandle GET_UID = LINKER.downcallHandle(
        LOOKUP.find("getuid").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_INT)
    );

    public static int getProcessId() throws Throwable {
        return (int) GET_PID.invoke();
    }

    public static int getUserId() throws Throwable {
        return (int) GET_UID.invoke();
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("Process ID: " + getProcessId());
        System.out.println("User ID: " + getUserId());
    }
}
```

### 4. Database Native Extensions

```java
public class PostgresExtension {

    // Call PostgreSQL's C API for custom aggregates
    private static final MethodHandle PG_CUSTOM_AGGREGATE;

    static {
        System.loadLibrary("pg_extension");
        SymbolLookup lookup = SymbolLookup.loaderLookup();

        PG_CUSTOM_AGGREGATE = Linker.nativeLinker().downcallHandle(
            lookup.find("calculate_custom_aggregate").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_DOUBLE,  // result
                ValueLayout.ADDRESS,       // data array
                ValueLayout.JAVA_INT       // count
            )
        );
    }

    public double calculateAggregate(double[] values) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            // Allocate memory for array
            MemorySegment segment = arena.allocate(ValueLayout.JAVA_DOUBLE, values.length);

            // Copy data
            for (int i = 0; i < values.length; i++) {
                segment.setAtIndex(ValueLayout.JAVA_DOUBLE, i, values[i]);
            }

            // Call native aggregate function
            return (double) PG_CUSTOM_AGGREGATE.invoke(segment, values.length);
        }
    }
}
```

### 5. Encryption/Cryptography Libraries

```java
public class NativeCrypto {

    private static final MethodHandle ENCRYPT;
    private static final MethodHandle DECRYPT;

    static {
        System.loadLibrary("crypto");
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        Linker linker = Linker.nativeLinker();

        ENCRYPT = linker.downcallHandle(
            lookup.find("aes_encrypt").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,      // result code
                ValueLayout.ADDRESS,       // input
                ValueLayout.JAVA_INT,      // input length
                ValueLayout.ADDRESS,       // output
                ValueLayout.ADDRESS,       // key
                ValueLayout.ADDRESS        // iv
            )
        );

        DECRYPT = linker.downcallHandle(
            lookup.find("aes_decrypt").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS
            )
        );
    }

    public byte[] encrypt(byte[] plaintext, byte[] key, byte[] iv) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            // Allocate segments
            MemorySegment inputSeg = arena.allocate(plaintext.length);
            MemorySegment outputSeg = arena.allocate(plaintext.length + 16);  // Add padding
            MemorySegment keySeg = arena.allocate(key.length);
            MemorySegment ivSeg = arena.allocate(iv.length);

            // Copy data
            MemorySegment.copy(plaintext, 0, inputSeg, ValueLayout.JAVA_BYTE, 0, plaintext.length);
            MemorySegment.copy(key, 0, keySeg, ValueLayout.JAVA_BYTE, 0, key.length);
            MemorySegment.copy(iv, 0, ivSeg, ValueLayout.JAVA_BYTE, 0, iv.length);

            // Call native encrypt
            int resultCode = (int) ENCRYPT.invoke(
                inputSeg, plaintext.length, outputSeg, keySeg, ivSeg
            );

            if (resultCode != 0) {
                throw new CryptoException("Encryption failed: " + resultCode);
            }

            // Copy result
            byte[] ciphertext = new byte[(int) outputSeg.byteSize()];
            MemorySegment.copy(outputSeg, ValueLayout.JAVA_BYTE, 0, ciphertext, 0, ciphertext.length);

            return ciphertext;
        }
    }
}
```

### 6. Shared Memory for Inter-Process Communication

```java
public class SharedMemoryIPC {

    private final MemorySegment sharedMemory;
    private final Arena arena;

    public SharedMemoryIPC(String name, long size) throws Exception {
        this.arena = Arena.ofShared();

        // Create/open shared memory segment (platform-specific)
        if (System.getProperty("os.name").contains("Linux")) {
            this.sharedMemory = createSharedMemoryLinux(name, size);
        } else {
            throw new UnsupportedOperationException("Platform not supported");
        }
    }

    private MemorySegment createSharedMemoryLinux(String name, long size) throws Throwable {
        SymbolLookup lookup = Linker.nativeLinker().defaultLookup();

        // shm_open
        MethodHandle shmOpen = Linker.nativeLinker().downcallHandle(
            lookup.find("shm_open").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT
            )
        );

        // ftruncate
        MethodHandle ftruncate = Linker.nativeLinker().downcallHandle(
            lookup.find("ftruncate").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_LONG
            )
        );

        // mmap
        MethodHandle mmap = Linker.nativeLinker().downcallHandle(
            lookup.find("mmap").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_LONG
            )
        );

        MemorySegment nameSegment = arena.allocateUtf8String(name);

        int fd = (int) shmOpen.invoke(nameSegment, 0x42 | 0x200, 0666);  // O_CREAT | O_RDWR
        if (fd < 0) {
            throw new IOException("Failed to create shared memory");
        }

        ftruncate.invoke(fd, size);

        MemorySegment addr = (MemorySegment) mmap.invoke(
            MemorySegment.NULL,
            size,
            0x3,  // PROT_READ | PROT_WRITE
            0x1,  // MAP_SHARED
            fd,
            0L
        );

        return addr.reinterpret(size);
    }

    public void write(long offset, byte[] data) {
        MemorySegment.copy(data, 0, sharedMemory, ValueLayout.JAVA_BYTE, offset, data.length);
    }

    public byte[] read(long offset, int length) {
        byte[] result = new byte[length];
        MemorySegment.copy(sharedMemory, ValueLayout.JAVA_BYTE, offset, result, 0, length);
        return result;
    }

    public void close() {
        arena.close();
    }
}
```

### 7. High-Performance Serialization

```java
public class BinarySerializer {

    public byte[] serialize(UserRecord user) {
        try (Arena arena = Arena.ofConfined()) {
            // Calculate required size
            int nameBytes = user.name().getBytes(StandardCharsets.UTF_8).length;
            int emailBytes = user.email().getBytes(StandardCharsets.UTF_8).length;
            long totalSize = 8 + 4 + nameBytes + 4 + emailBytes + 4;  // id + nameLen + name + emailLen + email + age

            // Allocate segment
            MemorySegment segment = arena.allocate(totalSize);

            long offset = 0;

            // Write ID
            segment.set(ValueLayout.JAVA_LONG, offset, user.id());
            offset += 8;

            // Write name
            byte[] nameData = user.name().getBytes(StandardCharsets.UTF_8);
            segment.set(ValueLayout.JAVA_INT, offset, nameData.length);
            offset += 4;
            MemorySegment.copy(nameData, 0, segment, ValueLayout.JAVA_BYTE, offset, nameData.length);
            offset += nameData.length;

            // Write email
            byte[] emailData = user.email().getBytes(StandardCharsets.UTF_8);
            segment.set(ValueLayout.JAVA_INT, offset, emailData.length);
            offset += 4;
            MemorySegment.copy(emailData, 0, segment, ValueLayout.JAVA_BYTE, offset, emailData.length);
            offset += emailData.length;

            // Write age
            segment.set(ValueLayout.JAVA_INT, offset, user.age());

            // Copy to byte array
            byte[] result = new byte[(int) totalSize];
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0, result, 0, result.length);

            return result;
        }
    }

    public UserRecord deserialize(byte[] data) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(data.length);
            MemorySegment.copy(data, 0, segment, ValueLayout.JAVA_BYTE, 0, data.length);

            long offset = 0;

            // Read ID
            long id = segment.get(ValueLayout.JAVA_LONG, offset);
            offset += 8;

            // Read name
            int nameLength = segment.get(ValueLayout.JAVA_INT, offset);
            offset += 4;
            byte[] nameData = new byte[nameLength];
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset, nameData, 0, nameLength);
            String name = new String(nameData, StandardCharsets.UTF_8);
            offset += nameLength;

            // Read email
            int emailLength = segment.get(ValueLayout.JAVA_INT, offset);
            offset += 4;
            byte[] emailData = new byte[emailLength];
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset, emailData, 0, emailLength);
            String email = new String(emailData, StandardCharsets.UTF_8);
            offset += emailLength;

            // Read age
            int age = segment.get(ValueLayout.JAVA_INT, offset);

            return new UserRecord(id, name, email, age);
        }
    }
}
```

## Benefits Over JNI

| Aspect | JNI | FFM API |
|--------|-----|---------|
| Code required | Java + C/C++ | Pure Java |
| Type safety | Runtime | Compile-time |
| Memory safety | Manual | Automatic |
| Performance | Good | Better (less overhead) |
| Ease of use | Complex | Simple |
| Debugging | Difficult | Easier |

## Best Practices

### 1. Always Use Try-With-Resources

```java
// ✓ GOOD
try (Arena arena = Arena.ofConfined()) {
    MemorySegment segment = arena.allocate(1024);
    // Use segment
}  // Automatically freed

// ✗ BAD - Memory leak
Arena arena = Arena.ofConfined();
MemorySegment segment = arena.allocate(1024);
// Forgot to close arena!
```

### 2. Choose Appropriate Arena Type

```java
// Single-threaded access
Arena confined = Arena.ofConfined();

// Multi-threaded access
Arena shared = Arena.ofShared();

// Long-lived data (use sparingly)
Arena global = Arena.global();
```

### 3. Bounds Checking

FFM API performs automatic bounds checking:

```java
try (Arena arena = Arena.ofConfined()) {
    MemorySegment segment = arena.allocate(10);

    // ✓ Safe - within bounds
    segment.set(ValueLayout.JAVA_INT, 0, 42);

    // ✗ Throws IndexOutOfBoundsException
    segment.set(ValueLayout.JAVA_INT, 100, 42);
}
```

## Migration from JNI

1. **Identify JNI usage**: Find all `native` methods
2. **Create function descriptors**: Map native signatures to FFM descriptors
3. **Load symbols**: Use `SymbolLookup` instead of `System.loadLibrary`
4. **Replace native methods**: Call via `MethodHandle`
5. **Test thoroughly**: Ensure behavior matches JNI implementation

## Performance

- **Lower overhead**: FFM is faster than JNI for most operations
- **Zero-copy**: Direct memory access without copying
- **Optimized by JIT**: Better optimization than JNI

## Common Pitfalls

1. **Forgetting to close Arena**: Memory leaks
2. **Using wrong layout**: Data corruption
3. **Platform-specific code**: Not portable
4. **Accessing freed memory**: Use try-with-resources

## Summary

The Foreign Function & Memory API modernizes native interop in Java, providing a safe, efficient, and pure-Java way to work with native code and off-heap memory. It's essential for high-performance applications requiring native library integration.

**Migration Priority**: MEDIUM - Only needed if you currently use JNI or need native interop. High value when applicable.
