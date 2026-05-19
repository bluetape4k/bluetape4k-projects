# Module bluetape4k-junit5

English | [한국어](./README.ko.md)

An extension library that reduces repetitive boilerplate in JUnit 5 tests.

## Architecture

### Extension Component Overview

![Extension Component Overview 1](../../docs/images/readme-diagrams/testing-junit5-diagram-01.png)

### Class Diagram

![Class Diagram 2](../../docs/images/readme-diagrams/testing-junit5-diagram-02.png)

## Key Features

- `StopwatchExtension` — measure and log test execution time
- `TempFolderExtension` — provide temp directories/files, auto-deleted after the test
- Output capture helpers — capture `System.out`/`System.err` for assertion
- Random/Faker data injection — inject fake or randomized objects into test fields/parameters
- System property helpers — set properties before a test and restore them after
- Awaitility + coroutine helpers — `suspendUntil` / `awaitSuspending`
- Stress-testing utilities — `MultithreadingTester`, `SuspendedJobTester`, `StructuredTaskScopeTester`
- Parameter-source extensions — `FieldSource` for parameterized tests
- Mermaid-based reporting — Gantt timeline of test execution

## Usage Examples

### StopwatchExtension

```kotlin
@ExtendWith(StopwatchExtension::class)
class MyTest {
    @Test
    fun `measure execution time`() {
        // Logs: Starting test: [measure execution time]
        // ...
        // Logs: Completed test: [measure execution time] took 123 msecs.
    }
}

// Or use the annotation shortcut
@StopwatchTest
fun `annotated stopwatch test`() { }
```

### TempFolderExtension

`TempFolder.createFile(name)` and `createDirectory(name)` accept relative names under the temporary root only. Blank names,
absolute paths, `..` traversal, and symlink parents that resolve outside the temporary root are rejected.

```kotlin
@ExtendWith(TempFolderExtension::class)
class FileProcessingTest {
    lateinit var tempFolder: TempFolder

    @BeforeEach
    fun setup(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @Test
    fun `file processing test`() {
        val inputFile = tempFolder.createFile("input.txt")
        inputFile.writeText("test data")
        val outputDir = tempFolder.createDirectory("output")
        processFile(inputFile, outputDir)
    }
}
```

### OutputCapture

```kotlin
@OutputCapture
class OutputCaptureTest {
    @Test
    fun `capture stdout`(capturer: OutputCapturer) {
        println("Hello, Console!")
        capturer.capture() shouldContain "Hello, Console!"
    }
}
```

### FakeValue / Fakers

```kotlin
@ExtendWith(FakeValueExtension::class)
class FakeValueTest {
    @FakeValue(provider = "name.fullName")
    private lateinit var fullName: String

    @Test
    fun `injected fake value`() {
        println(fullName)  // e.g. "John Doe"
    }
}

// Fakers utility
val randomText = Fakers.randomString(10, 20)
val phone = Fakers.numberString("010-####-####")
```

### Stress Testing

```kotlin
// Platform threads
MultithreadingTester()
    .workers(Runtime.getRuntime().availableProcessors())
    .rounds(100)
    .add { counter.incrementAndGet() }
    .run()

// Coroutines
SuspendedJobTester()
    .workers(16)
    .rounds(100)
    .add { delay(10); results.add(1) }
    .run()

// Virtual Threads (Java 21+)
StructuredTaskScopeTester()
    .rounds(1000)
    .add { processRequest() }
    .run()

// With timeout — hangs are caught as TimeoutException
StructuredTaskScopeTester()
    .rounds(100)
    .withTimeout(5.seconds)   // run() throws TimeoutException if not done in 5s
    .add { processRequest() }
    .run()
```

### Coroutine Test Helpers

```kotlin
@Test
fun `basic suspend test`() = runSuspendTest {
    val result = someSuspendFunction()
    result shouldBe "expected"
}

@Test
fun `io dispatcher test`() = runSuspendIO {
    val data = readFromFile()
    processData(data)
}
```

### SystemProperty

```kotlin
@SystemProperty(name = "app.environment", value = "test")
class SystemPropertyTest {
    @Test
    fun `system property set`() {
        System.getProperty("app.environment") shouldBe "test"
    }
}
```

### FieldSource (Parameterized Test)

```kotlin
class FieldSourceTest {
    val isBlankArguments = listOf(
        argumentOf(null, true),
        argumentOf("", true),
        argumentOf("not blank", false)
    )

    @ParameterizedTest
    @FieldSource("isBlankArguments")
    fun `isBlank test`(input: String?, expected: Boolean) {
        input.isNullOrBlank() shouldBe expected
    }
}
```

### Mermaid Report

```bash
# Extract Mermaid Gantt timeline from test output
./gradlew :testing:junit5:test | awk 'f||/^gantt$/{f=1; print}' > gantt.mermaid
```

## Best Practices

- Use `TempFolderExtension` instead of ad hoc file paths in tests.
- Keep `TempFolder` names relative to the temporary root; path traversal and symlink escapes are rejected.
- Capture stdout/stderr when assertions depend on console output.
- Prefer `FakeValue` / `Fakers` providers for sample values instead of hardcoded data.
- Use the provided stress-testing helpers for concurrency-heavy tests — they maintain a stable worker pool regardless of round count.

## Adding the Dependency

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-junit5:${version}")
}
```

## References

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Awaitility](https://github.com/awaitility/awaitility)
- [Data Faker](https://www.datafaker.net/)
