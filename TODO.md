# TODO — bluetape4k-projects 개선 목록

개선이 필요한 항목을 우선순위 순으로 관리합니다.
완료된 항목은 ~~취소선~~ 처리 후 CHANGELOG.md로 이동시킵니다.

---

## [HIGH] `virtualFutureOf` — nullable 반환 타입 미지원 (사용성 제약)

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

`bluetape4k-graph`의 `VirtualThread*Adapter` 구현 시 다음과 같은 nullable 반환 메서드에서 컴파일 오류 발생:

```kotlin
// findVertexById → GraphVertex? (nullable)
// shortestPath   → GraphPath?   (nullable)
// updateVertex   → GraphVertex? (nullable)
```

`virtualFutureOf` 를 쓸 수 없어 매번 저수준 API를 직접 호출해야 했다:

```kotlin
// 현재 workaround — 불필요하게 장황함
CompletableFuture.supplyAsync({ delegate.findVertexById(label, id) }, VirtualThreadExecutor)

// 이상적인 코드
virtualFutureOf { delegate.findVertexById(label, id) }  // 컴파일 오류: V: Any
```

### 개선 방안

**Option A — nullable 전용 오버로드 추가 (권장)**

```kotlin
// CompletableFutureSupport.kt 에 추가
inline fun <V> virtualFutureOfNullable(
    crossinline block: () -> V?,
): CompletableFuture<V?> =
    CompletableFuture.supplyAsync({ block() }, VirtualThreadExecutor)
```

사용 예:
```kotlin
override fun findVertexByIdAsync(label: String, id: GraphElementId): CompletableFuture<GraphVertex?> =
    virtualFutureOfNullable { delegate.findVertexById(label, id) }
```

**Option B — `V: Any` 제약 제거 (더 간단하지만 주의 필요)**

```kotlin
inline fun <V> virtualFutureOf(
    crossinline block: () -> V,
): CompletableFuture<V> =
    CompletableFuture.supplyAsync({ block() }, VirtualThreadExecutor)
```

> **주의**: Java의 `CompletableFuture.supplyAsync` 는 `null` 결과를 허용하지만,
> 하위 호환성 관점에서 기존 `V: Any` 호출부에 영향이 없는지 검증 필요.
> 타입 추론 혼동을 방지하기 위해 Option A가 더 명시적임.

**Option C — `virtualFuture` 와 통합**

기존 `virtualFuture<T>` 는 `T` 제약이 없어 nullable을 지원하지만 `VirtualFuture<T>` 를 반환한다.
`CompletableFuture` 를 반환하는 nullable 지원 버전이 별도로 필요하다 (Option A 권장).

### 영향 범위

| 어댑터 파일 | 영향 메서드 |
|------------|------------|
| `VirtualThreadVertexAdapter` | `findVertexByIdAsync`, `updateVertexAsync` |
| `VirtualThreadTraversalAdapter` | `shortestPathAsync` |

---
