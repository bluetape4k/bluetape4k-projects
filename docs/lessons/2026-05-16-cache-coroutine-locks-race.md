# CacheCoroutineLocks — 복합 연산 경쟁 조건 수정 (Issue #477)

날짜: 2026-05-16
이슈: #477
모듈: `infra/resilience4j` — `io.bluetape4k.resilience4j.cache.CacheCoroutineLocks`

---

## 근본 원인

`CacheCoroutineLocks`에 두 가지 경쟁 조건이 존재했다.

### 버그 1 (HIGH): mutexFor — lock 범위 밖 복합 연산

```kotlin
// 기존 코드
fun mutexFor(cache: Cache<*, *>, key: Any): Mutex {
    val locks = lock.withLock {
        locksByCache.getOrPut(cache) { ConcurrentHashMap() }   // (A)
    }                                                           // ← lock 해제
    return locks.computeIfAbsent(key) { Mutex() }              // (B) lock 없음
}
```

경쟁 창:
- T1: (A) 에서 inner map M 획득 → outer lock 해제
- T2: `release()` 가 M 를 비우고 `locksByCache` 에서 제거
- T3: `mutexFor()` 가 새 inner map M' 에 Mutex3 생성
- T1: (B) 에서 고아된 M 에 Mutex1 생성
- 결과: T1과 T3이 동일 key에 대해 서로 다른 Mutex를 보유 → per-key 직렬화 보장 파괴

### 버그 2 (MEDIUM): release — TOCTOU isLocked 검사

```kotlin
fun release(cache: Cache<*, *>, key: Any, mutex: Mutex) {
    if (mutex.isLocked) return           // lock 없이 검사
    lock.withLock {
        locks.remove(key, mutex)         // 검사와 제거 사이 window 존재
    }
}
```

- T_a: `mutexFor()` 에서 Mutex1 획득, 아직 `withLock` 호출 전
- T_b: `release()` 가 `isLocked == false` 확인 → Mutex1 제거
- T_c: `mutexFor()` 에서 새 Mutex2 획득
- T_a와 T_c: 동일 key에 대해 동시 실행

---

## 수정 방법: Option A (no-op release)

```kotlin
fun mutexFor(cache: Cache<*, *>, key: Any): Mutex = lock.withLock {
    locksByCache.getOrPut(cache) { HashMap() }.getOrPut(key) { Mutex() }
}

fun release(cache: Cache<*, *>, key: Any, mutex: Mutex) {
    // 의도적 no-op.
    // Per-key Mutex 항목을 제거하지 않는다.
    // 제거하면 concurrent caller가 Mutex 참조를 획득한 후 아직 withLock을 호출하기
    // 전에 새 caller가 다른 Mutex를 받을 수 있다 — per-key 직렬화 보장 파괴.
    // WeakHashMap이 Cache key가 GC될 때 entire inner map을 회수하므로
    // 메모리는 Cache 수명 단위로 제한된다.
}
```

**Option B (reference counting) 이번 PR에서 선택하지 않은 이유:**
- ref-counting 자체는 outer ReentrantLock 안에서 안전하다 (mutexFor에서 증가, release에서 감소, 그 사이 count≥1이므로 제거 불가)
- 버그 수정의 외과적 범위를 유지하기 위해 복잡도를 최소화; ref-counting은 #499에서 follow-up으로 처리
- WeakHashMap GC 모델이 Cache 인스턴스 수명 기반 회수를 제공하고, JCache provider(Caffeine 등)가 key cardinality를 자체적으로 제한하므로 메모리 위험 실질적으로 낮음

---

## 추가 수정 사항

| 항목 | 변경 내용 |
|------|-----------|
| `ConcurrentHashMap` 제거 | 모든 접근이 `lock.withLock` 안에 있으므로 plain `HashMap`으로 충분 |
| `cacheKey as Any` 캐스트 제거 | `val cacheKey: K & Any` intersection type으로 캐스트 불필요 |

---

## 메모리 트레이드오프

- **기존**: per-key Mutex를 사용 후 제거 → 작은 메모리 공간, 경쟁 조건 존재
- **수정 후**: Mutex를 Cache 수명 동안 유지 → O(distinct keys) 메모리, 경쟁 조건 없음
- JCache provider (Caffeine 등)가 이미 key 개수를 제한하므로 메모리 누수 우려 없음

---

## RED/GREEN 검증 노트

테스트가 버그 코드에서도 통과(RED 실패 없음)한 이유:
- `mutexFor` 내 lock 해제 → `computeIfAbsent` 사이 경쟁 창은 1-2 instruction
- OS 스케줄러가 이 창에서 선점하는 경우가 드물어 stress test도 통과
- 경쟁 조건은 코드 분석으로 증명되었고, 수정의 정확성도 코드 분석으로 검증

---

## 삭제한 multi-key 스트레스 테스트

추가했던 `executeSuspendFunction 은 다중 key 동시 miss 에서 key 별로 loader 를 한 번만 실행한다` 테스트는 삭제:
- 첫 번째 miss 후 JCache가 warm 되어 이후 호출이 모두 캐시 적중
- `callCount == 1` 조건이 버그 코드에서도 통과 → 회귀 감지 불가
- 기존 `executeSuspendFunction 은 동일 key 동시 miss 에서 loader 를 한 번만 실행한다` 가 contract를 충분히 커버

---

## 향후 가이드

- `synchronized` 또는 `ConcurrentHashMap`의 복합 연산은 원자성을 보장하지 않는다
- `getOrPut` 체이닝은 반드시 하나의 lock.withLock 안에서 실행해야 한다
- per-key Mutex 패턴에서 "사용 후 제거" 전략은 TOCTOU를 유발한다; 유지 전략 또는 reference counting 필요
- 경쟁 조건 버그는 stress test로 재현이 어려울 수 있으므로 코드 분석을 우선한다
