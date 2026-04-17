# TODO — bluetape4k-projects 개선 목록

개선이 필요한 항목을 우선순위 순으로 관리합니다.
완료된 항목은 ~~취소선~~ 처리 후 CHANGELOG.md로 이동시킵니다.

---

## [HIGH] `virtualFutureOf` — nullable 반환 타입 미지원 (사용성 제약)

> **상태**: 🔄 `bluetape4k-graph`에 임시 workaround 적용 중 — `bluetape4k-projects` 공식 추가 대기

**파일**: `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/CompletableFutureSupport.kt`

### 현재 구현

```kotlin
inline fun <V: Any> virtualFutureOf(
    crossinline block: () -> V,
): CompletableFuture<V> =
    CompletableFuture.supplyAsync({ block() }, VirtualThreadExecutor)
```

`V: Any` 제약으로 인해 **nullable 반환 타입(`T?`)에 사용 불가**.

### 발견 경위

`bluetape4k-graph`의 `VirtualThread*Adapter` 구현 시 nullable 반환 메서드에서 컴파일 오류 발생:

```kotlin
// findVertexById → GraphVertex?  /  shortestPath → GraphPath?  /  updateVertex → GraphVertex?
```

### 확정된 해결 방안 — `virtualFutureOfNullable` 추가

```kotlin
// CompletableFutureSupport.kt 에 추가
inline fun <V> virtualFutureOfNullable(
    crossinline block: () -> V?,
): CompletableFuture<V?> =
    CompletableFuture.supplyAsync({ block() }, VirtualThreadExecutor)
```

> **Note**: `Unit`은 `Any`를 만족하므로 `CompletableFuture<Unit>` 반환에는 기존 `virtualFutureOf`를 그대로 사용한다.
> `virtualFutureOfNullable`은 `T?` nullable 결과가 실제로 필요한 경우에만 쓴다.

### 현재 임시 workaround 위치

`bluetape4k-graph/graph/graph-core/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/CompletableFutureNullableSupport.kt`

공식 추가 후 해당 파일을 삭제하고 import만 교체하면 된다 (어댑터 코드 변경 불필요).

### 영향 범위

| 어댑터 파일 | 영향 메서드 |
|------------|------------|
| `VirtualThreadVertexAdapter` | `findVertexByIdAsync`, `updateVertexAsync` |
| `VirtualThreadTraversalAdapter` | `shortestPathAsync` |

---

## [LOW] Kotlin API에서 `CompletableFuture<Void>` 대신 `CompletableFuture<Unit>` 사용 권고

> **상태**: 📝 코딩 컨벤션 추가 권고 — 강제 변경 불필요

Java interop 목적이 없는 순수 Kotlin API에서 void 반환을 `CompletableFuture<Void>` 로 선언하는 것은 부적절하다.

- `Void`는 Java의 `null`-only 타입이므로 `.join()` 결과가 항상 `null`
- `Unit`은 Kotlin의 unit 타입으로 `.join()` 결과가 `Unit` 인스턴스 → 타입 안전

**권장 패턴**:

```kotlin
// ❌ Java-ism
fun createGraphAsync(name: String): CompletableFuture<Void>

// ✅ Kotlin-idiomatic
fun createGraphAsync(name: String): CompletableFuture<Unit>
```

`Unit: Any` 이므로 `virtualFutureOf { unitReturningBlock() }` 으로 구현 가능 — `runAsync` 불필요.

**발견 경위**: `bluetape4k-graph`의 `GraphVirtualThreadSession` 인터페이스가 `Void`로 선언되어  
`CompletableFuture.runAsync`를 억지로 사용했다가 `Unit`으로 교체 후 `virtualFutureOf`로 통일.

---
