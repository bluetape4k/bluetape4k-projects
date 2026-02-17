# AGENTS.md - Bluetape4k Project Guidelines

## Project Overview

Bluetape4k is a Kotlin library collection for JVM backend development. Multi-module Gradle project with Kotlin DSL.

## Technology Stack

- **Language:** Kotlin 2.3, Java 21 (JVM Toolchain)
- **Build:** Gradle with Kotlin DSL
- **Framework:** Spring Boot 3.5.10, Spring Cloud 2025.0.1
- **Data:** Kotlin Exposed 1.0.0, Hibernate 6.6.41
- **Testing:** JUnit 5, MockK 1.14.9, Kluent 1.73

## Build Commands

```bash
# Clean build all modules
./gradlew clean build

# Build specific module
./gradlew :bluetape4k-core:build
./gradlew :io:bluetape4k-jackson:build

# Build without tests
./gradlew build -x test

# Run Detekt static analysis
./gradlew detekt
```

## Test Commands

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :bluetape4k-coroutines:test

# Run specific test class
./gradlew test --tests "io.bluetape4k.io.CompressorTest"
./gradlew test --tests "io.bluetape4k.support.*"

# Run tests with detailed logging
./gradlew test --info
```

## Code Style Guidelines

### Language & Documentation

- **KDoc and comments must be written in Korean**
- Kotlin 2.3 (languageVersion & apiVersion)
- Use `@file:OptIn()` annotations at file level for experimental APIs

### Naming Conventions

- **Packages:** `io.bluetape4k.{module}.{feature}`
- **Extension functions:** Use verb/noun describing the transformation
- **Test classes:** Descriptive names with backticks for test methods
  ```kotlin
  @Test
  fun `should return compressed data when input is valid`() { }
  ```

### Imports & Formatting

- Group imports: Kotlin stdlib → third-party → project internal
- Use wildcard imports for large groups from same package
- Prefer extension functions for fluent APIs

### Code Patterns

**Logging:**

```kotlin
companion object: KLogging()
```

**Extension Functions:**

```kotlin
inline fun <T: Any> T?.requireNotNull(parameterName: String): T {
    contract { returns() implies (this@requireNotNull != null) }
    if (this == null) throw IllegalArgumentException("$parameterName must not be null")
    return this
}
```

**Contracts for Smart Casts:**

```kotlin
contract {
    returns() implies (value != null)
}
```

### Error Handling

- Use `require()`, `check()`, `error()` for preconditions
- Return `Result<T>` types for operations that may fail
- Avoid swallowing exceptions; propagate or log appropriately

### Testing Conventions

- Extend `AbstractCoreTest` for common test utilities
- Use MockK for mocking, Kluent for assertions
- Use descriptive test names with backticks
- Use `faker` for test data generation

## Module Structure

- Core: `bluetape4k-{name}` (e.g., `bluetape4k-core`)
- Subdirectory: `bluetape4k-{dir}-{name}` (e.g., `bluetape4k-aws-s3`)
- Examples: Keep original names with `examples-` prefix

## Commit Message Format

- **Language:** Korean
- **Prefix:** Use conventional commits (feat, fix, docs, style, refactor, perf, test, chore)
- **Example:** `feat: Result 패턴 기반 파일 I/O 유틸리티 추가`

## Important Notes

- 최대한 내부 소스를 사용한다. (예: bluetape4k-core 의 RequireSupport.kt 등)
- Workshop and examples modules are not published
- `exposed-tests` module has Detekt disabled
- Testcontainers used for integration tests
