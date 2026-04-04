# Module bluetape4k-junit5

English | [한국어](./README.ko.md)

An extension library that reduces repetitive boilerplate in JUnit 5 tests.

## Key Features

- `StopwatchExtension`
- `TempFolderExtension`
- output-capture helpers
- random-data and fake-data injection
- system-property helpers
- Awaitility + coroutine helpers
- stress-testing utilities
- parameter-source extensions
- Mermaid-based reporting

## Adding the Dependency

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-junit5:${version}")
}
```

## Detailed Features

The main extension groups are:

- stopwatch-based execution-time measurement
- temporary folders and files for filesystem tests
- stdout / stderr capture
- Faker-based data generation and injection
- system-property setup and restoration
- coroutine-friendly waiting helpers
- stress-testing helpers for threads, virtual threads, and coroutines

The Korean README keeps the full walkthrough and examples for each extension, including code samples for `TempFolder`, `OutputCapture`, `FakeValue`, and `Fakers`.

## Best Practices

- Use the temp-folder extension instead of ad hoc file paths.
- Capture stdout/stderr when assertions depend on console output.
- Prefer fake-data providers for sample values instead of hardcoding large datasets.
- Reuse the provided stress-testing helpers for concurrency-heavy tests.

## Class Diagram

The Korean README includes the full class diagram for the extension set, helper types, and lifecycle integration points in JUnit 5.

## Extension Composition Diagram

It also includes an extension-composition diagram showing how test methods, injected arguments, temp resources, captured output, and utility helpers fit together.

## References

- JUnit 5
- Awaitility
- Data Faker
