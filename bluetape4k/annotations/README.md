# Module `bluetape4k-annotations`

English | [한국어](./README.ko.md)

`bluetape4k-annotations` provides lightweight API maturity markers for Bluetape libraries.

It focuses on:

- explicit Kotlin opt-in contracts for experimental, beta, internal, and delicate APIs
- implementation-only SPI contracts through `@SubclassOptInRequired`
- a dependency-light artifact that low-level modules can expose in public signatures

## Architecture

`bluetape4k-annotations` is intentionally standalone. Production code has no dependency on other Bluetape modules, so any module can depend on it without introducing a dependency cycle.

All marker annotations use Kotlin `@RequiresOptIn`, `AnnotationRetention.BINARY`, and `@MustBeDocumented`. They are declaration-only markers and do not perform runtime scanning.

## Key Features

- **ExperimentalBluetapeApi**: Error-level marker for APIs that may change or be removed without compatibility guarantees.
- **BetaBluetapeApi**: Warning-level marker for APIs intended to stabilize, while minor source, binary, or behavior changes are still possible.
- **InternalBluetapeApi**: Error-level marker for declarations that are public only for technical reasons.
- **DelicateBluetapeApi**: Warning-level marker for APIs with lifecycle, concurrency, resource-management, or security contracts.
- **BluetapeImplementationApi**: Warning-level marker for `@SubclassOptInRequired` on SPI classes and interfaces that are stable to use but not stable to implement.

## Usage Examples

### Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-annotations")
}
```

### Marking unstable use-site APIs

```kotlin
import io.bluetape4k.annotations.ExperimentalBluetapeApi

@ExperimentalBluetapeApi
fun experimentalFeature(): String = "value"

@OptIn(ExperimentalBluetapeApi::class)
fun caller(): String = experimentalFeature()
```

### Marking implementation-sensitive SPI

```kotlin
import io.bluetape4k.annotations.BluetapeImplementationApi
import kotlin.SubclassOptInRequired

@SubclassOptInRequired(BluetapeImplementationApi::class)
interface StorageProvider

@OptIn(BluetapeImplementationApi::class)
class CustomStorageProvider : StorageProvider
```

Use `BluetapeImplementationApi` only with `@SubclassOptInRequired`. It is not a generic function or property opt-in marker.

## Marker Selection

| Marker | Level | Use when |
|---|---:|---|
| `ExperimentalBluetapeApi` | Error | The API may change or disappear before stabilization. |
| `BetaBluetapeApi` | Warning | The API is close to stable but may still receive minor changes. |
| `InternalBluetapeApi` | Error | The declaration is public for technical reasons, not as supported user API. |
| `DelicateBluetapeApi` | Warning | Correct usage requires extra lifecycle, concurrency, resource, or security knowledge. |
| `BluetapeImplementationApi` | Warning | Users may call the SPI but should explicitly opt in before implementing or subclassing it. |

## Compatibility Notes

When a marked API graduates to stable, remove the marker from that API after checking source and binary compatibility. Keep the marker annotation class available in the artifact unless a major version intentionally removes it, because downstream code may still reference it in `@OptIn` declarations.
