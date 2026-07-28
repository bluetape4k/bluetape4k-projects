# synchronized → ReentrantLock/Mutex 마이그레이션 (#473)

## 배경

코루틴/가상스레드 환경에서 `synchronized` 블록은 carrier thread를 pin하여 스케줄러 효율을 저하시킨다.
JVM virtual thread(JDK 21+)는 `synchronized` monitor lock을 만나면 blocking되며, OS thread를 점유한 채 대기한다.
CLAUDE.md 규칙: "코루틴/가상스레드 코드에서 `synchronized` 금지, `reentrantLock()` 사용"

## 변경 대상

| 파일 | 기존 | 변경 |
|------|------|------|
| `infra/resilience4j/src/main/kotlin/.../CacheCoroutines.kt` | `Collections.synchronizedMap(WeakHashMap)` + `synchronized(locksByCache)` × 2 | `ReentrantLock` + `lock.withLock {}` |
| `data/r2dbc/src/main/kotlin/.../TransactionSupport.kt` | `synchronized(transactionManagerCache)` | `private val transactionManagerLock = ReentrantLock()` + `transactionManagerLock.withLock {}` |

## 핵심 결정 사항

### WeakHashMap 유지

`Collections.synchronizedMap(WeakHashMap())` 패턴을 `WeakHashMap + ReentrantLock`으로 분리했다.
`WeakHashMap`을 `ConcurrentHashMap`으로 교체하지 않은 이유:
- GC-based cleanup이 목적. `Cache<*, *>` 또는 `ConnectionFactory` 참조가 사라지면 자동 제거.
- `ConcurrentHashMap`은 강한 참조를 유지하므로 메모리 누수 가능성.

### 범위 관리 원칙

코드 리뷰 중 `CacheCoroutineLocks.mutexFor`에 pre-existing race를 발견했다:
- `computeIfAbsent(key) { Mutex() }` 가 `lock` 밖에서 실행되어,
- `release()`가 inner map을 제거하는 순간 orphaned map에 Mutex가 생성될 수 있음.

**이 버그는 이 PR에서 수정하지 않았다.** 이유:
- Type-C Bug Fix 규칙: "요청 범위 밖의 변경 금지"
- #473은 `synchronized` → `ReentrantLock` 마이그레이션이 목적
- Race fix는 별도 이슈 #477로 추적

## 검증

- `infra/resilience4j`: 279 tests passing
- `data/r2dbc`: 165 tests passing

## 교훈

1. **WeakHashMap 보호 패턴**: `Collections.synchronizedMap(WeakHashMap())` 대신 `WeakHashMap + ReentrantLock`이 가상스레드 안전하고 의도가 명확함.
2. **scope discipline**: 코드 리뷰에서 pre-existing 버그를 발견해도 현재 PR 범위 밖이면 별도 이슈로 분리. 하나의 PR에 여러 fix를 묶으면 리뷰와 bisect가 어려워짐.
3. **computeIfAbsent와 외부 lock의 결합**: inner ConcurrentHashMap의 `computeIfAbsent`는 락 없이도 원자적이지만, 외부 lock이 해제된 후 inner map 자체가 교체될 수 있는 구조에서는 여전히 race가 발생함. 관련 fix: #477.

## 관련 이슈

- #473: synchronized → ReentrantLock (이 PR)
- #477: CacheCoroutineLocks mutexFor/release race — `computeIfAbsent` lock 밖 실행 (후속)
