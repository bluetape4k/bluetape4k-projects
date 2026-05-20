# Module `bluetape4k-annotations`

English | [한국어](./README.ko.md)

`bluetape4k-annotations` provides lightweight API maturity markers for Bluetape libraries.

It focuses on:

- explicit Kotlin opt-in contracts for experimental, beta, internal, delicate, and obsolete APIs
- implementation-only SPI contracts through `@SubclassOptInRequired`
- a dependency-light artifact that low-level modules can expose in public signatures

## Architecture

`bluetape4k-annotations` is intentionally standalone. Production code has no dependency on other Bluetape modules, so any module can depend on it without introducing a dependency cycle.

All marker annotations use Kotlin `@RequiresOptIn`, `AnnotationRetention.BINARY`, and `@MustBeDocumented`. They are declaration-only markers and do not perform runtime scanning.

## Key Features

- **BluetapeExperimentalApi**: Error-level marker for APIs that may change or be removed without compatibility guarantees.
- **BluetapeBetaApi**: Warning-level marker for APIs intended to stabilize, while minor source, binary, or behavior changes are still possible.
- **BluetapeInternalApi**: Error-level marker for declarations that are public only for technical reasons.
- **BluetapeDelicateApi**: Warning-level marker for APIs with lifecycle, concurrency, resource-management, or security contracts.
- **BluetapeObsoleteApi**: Error-level marker for APIs retained only for migration or compatibility.
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
import io.bluetape4k.annotations.BluetapeExperimentalApi

@BluetapeExperimentalApi
fun experimentalFeature(): String = "value"

@OptIn(BluetapeExperimentalApi::class)
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
| `BluetapeExperimentalApi` | Error | The API may change or disappear before stabilization. |
| `BluetapeBetaApi` | Warning | The API is close to stable but may still receive minor changes. |
| `BluetapeInternalApi` | Error | The declaration is public for technical reasons, not as supported user API. |
| `BluetapeDelicateApi` | Warning | Correct usage requires extra lifecycle, concurrency, resource, or security knowledge. |
| `BluetapeObsoleteApi` | Error | The API is retained only for migration or compatibility and should not be used in new code. |
| `BluetapeImplementationApi` | Warning | Users may call the SPI but should explicitly opt in before implementing or subclassing it. |

## Compatibility Notes

When a marked API graduates to stable, remove the marker from that API after checking source and binary compatibility. Keep the marker annotation class available in the artifact unless a major version intentionally removes it, because downstream code may still reference it in `@OptIn` declarations.
