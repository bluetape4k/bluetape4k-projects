# Duration 기반 timeout API와 작업 소유권

## 맥락

`CompletableFuture`, `ExecutorService`, `SuspendLazy`, `Deferred`, `Job`에 상대 `kotlin.time.Duration` timeout API를 추가했다. public API 확장과 blocking·coroutine 작업의 취소 경계를 함께 검증해야 했다.

## 결정과 발견

- `invokeAll`은 `List<Future<T>>`, `invokeAny`는 성공 결과 `T`를 반환해야 JDK 계약을 유지한다. 람다 wrapper는 입력 함수를 실제 호출해야 한다.
- waiter timeout과 원본 작업 취소는 서로 다른 행위다. `Deferred.awaitUntil`과 `Job.joinUntil`은 waiter만 제한하고, timeout getter의 shared initializer는 waiter 하나의 timeout으로 취소하지 않는다. 취소 권한을 가진 owner API만 소유한 작업을 취소한다.
- `SuspendLazy` 새 멤버는 기본 구현으로 제공하고, 이전 인터페이스 형태로 컴파일된 Java 구현체를 fixture로 실행해 구 ABI 호출 가능성을 확인한다.
- blocking initializer의 waiter와 작업 실행기는 서로 독립돼야 한다. 같은 단일 dispatcher를 공유하면 timeout 작업이 waiter까지 막을 수 있다. `runInterruptible`은 interrupt에 협력하는 blocking 작업을 정리하지만 interrupt를 무시하는 작업을 강제로 종료하지는 못한다.
- 공개 `LazyThreadSafetyMode` 동작을 보존하려면 추가 동기화는 `SYNCHRONIZED`에서만 적용해야 한다. `PUBLICATION`은 initializer 중복 실행을 허용하고 `NONE`은 동기화 보장을 제공하지 않는다.
- 생성 context 안의 `Job`이 호출자 `Job`을 대체할 수 있으므로 직접 호출의 취소 계약을 KDoc과 양쪽 README에 적는다.

## 결과

JDK 반환형과 예외를 유지하고 timeout 대기자와 원본 작업의 생명주기를 분리했다. 최초 리뷰는 JUnit에서 실행되지 않는 테스트, 단일 dispatcher 경합, detached initializer의 종료 경로를 찾아냈다. 후속 리뷰는 모든 `LazyThreadSafetyMode`에 공통 잠금을 적용할 때 `PUBLICATION` 의미가 바뀌는 점과 직접 호출의 `Job` 우선순위 문서화를 지적했다. 각각 JUnit XML 개수, latch 기반 회귀 테스트, KDoc과 양쪽 README로 보완했다. 이어서 interrupt를 무시하는 initializer가 timeout 후에도 계속 실행될 수 있음을 별도 테스트로 고정했다.

## 검증

- core 전체: 1,711개 통과, 실패·오류·skip 0.
- coroutines 전체: 684개 통과, 실패·오류·skip 0.
- `ExecutorServiceExtensionsTest`: 11개 모두 발견·실행.
- 변경 Kotlin 경로 Detekt 신규 진단 0. 두 모듈의 기존 전체 XML 진단은 `ignoreFailures`가 켜진 상태에서 core 269건, coroutines 110건.
- `javap` 대상 서명과 구 `SuspendLazy` Java ABI fixture를 확인했다. 저장소 전체 ABI baseline task는 별도로 없다.

## 향후 지침

- timeout API를 검토할 때 timeout이 대기자, future, deferred, job, blocking worker 중 무엇을 취소하는지 각각 명시한다.
- JDK wrapper에서는 결과형, 입력 순서, 예외 변환, 취소 요청을 Java 원본 API와 나란히 테스트한다.
- 동시성 테스트는 임의 sleep 대신 latch/barrier와 bounded wait를 사용하고, executor·Job·latch의 종료 책임을 `finally`에서 확인한다.
- 새 테스트를 추가한 뒤 Gradle 성공 여부만 보지 말고 JUnit XML의 test 발견 개수도 읽는다.
- `Lazy` 같은 공개 인터페이스/모드의 동작을 보조하는 잠금은 각 mode의 의미를 보존하는지 회귀 테스트로 고정한다.
- public Kotlin interface에 기본 멤버를 추가할 때 `javap` 서명만으로 끝내지 말고 이전 ABI의 구현체를 실제 런타임에서 호출한다.
