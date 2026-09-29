# Coroutine timeout API 설계 명세 통합 리뷰

## 검토 범위와 결론

- **대상 명세:** `docs/superpowers/specs/2026-09-28-coroutines-timeout-api-design.md`
- **저장소/branch:** `bluetape4k/bluetape4k-projects`, `feature/coroutines-timeout`
- **분류/단계:** Type A Full Feature, Step 2-R
- **검토 범위:** `CompletableFuture`, `ExecutorService`, `SuspendLazy`, `Deferred`, `Job` timeout API의 결과·예외·취소·호환성 계약과 테스트·문서 요구사항
- **최종 판정:** `PASS` — 최신 여섯 관점의 P0/P1/P2/P3가 모두 0건이며, 통합 검토에서도 미해결 모순이나 blocker가 없다.
- **다음 gate:** 명세 commit 후 사용자가 문서를 검토·승인해야 한다. 승인 전에는 구현 계획과 코드 변경을 시작하지 않는다.

이번 검토는 명세와 현재 작업 트리의 API 변경을 대조했다. 아래 판정은 테스트, Detekt, ABI, 실제 executor/coroutine 실행 또는 hosted CI가 통과했다는 뜻이 아니다. 이 검토 시점에는 구현과 검증이 아직 시작되지 않았다.

## 독립 관점 결과

| 관점 | 최종 판정 | P0 | P1 | P2 | P3 | 검토 초점 |
|---|---|---:|---:|---:|---:|---|
| API 계약 | PASS | 0 | 0 | 0 | 0 | 반환 타입, 예외 변환, timeout·취소 구분, JVM 호환성 |
| 아키텍처 | PASS | 0 | 0 | 0 | 0 | 인터페이스 기본 메서드와 구현체별 소유권 경계 |
| 성능 | PASS | 0 | 0 | 0 | 0 | timeout 변환 비용, 캐시 빠른 경로, 과도한 타이머·스레드 약속 여부 |
| 보안·실패 격리 | PASS | 0 | 0 | 0 | 0 | 취소 전파, interrupt, 업무 예외 보존, 부분 제출 정리 |
| 안정성 | PASS | 0 | 0 | 0 | 0 | 실패·취소 lifecycle, 결정적 테스트, executor 및 scope 정리 |
| 호출자·문서 | PASS | 0 | 0 | 0 | 0 | 한국어 API 설명, 상대 timeout, nullable 결과 모호성, README 대응 |
| **통합 결과** | **PASS** | **0** | **0** | **0** | **0** | 승인된 설계 선택과 명세·테스트·문서 요구사항의 정합성 |

각 reviewer는 수정 후 명세를 다시 읽고 최종 판정을 제출했다. 초기 검토에서 발견된 사항은 아래 계약 또는 검증 항목에 반영되었으며, 최신 검토에 남은 P0–P3는 없다.

## 통합한 핵심 계약

1. `ExecutorService.invokeAll`은 `List<Future<T>>`, `invokeAny`는 성공 결과 `T`를 반환하고 각 함수형 입력을 실제 실행한다. JDK timed overload의 예외·미시작 작업·미완료 취소 의미를 유지한다.
2. `SuspendLazy` timeout 멤버는 인터페이스 기본 구현으로 제공한다. 이전 인터페이스를 기준으로 사전 컴파일한 구현체 fixture로 기본 메서드 dispatch를 검증하고, `SuspendBlockingLazyImpl`의 캐시 반환 경로는 활성 호출자의 취소를 확인하면서 타이머를 만들지 않는다.
3. `CompletableFuture.join(Duration, defaultValue)`는 실제 timed wait의 `TimeoutException`만 기본값으로 바꾼다. 업무 예외의 원인이 같은 타입이어도 업무 실패로 전파한다.
4. `Job.joinUntil(Duration)`은 대상 job이 성공·실패·취소로 끝나면 활성 대기자에게 정상 반환한다. timeout이나 호출자 취소는 대상 job을 직접 취소하지 않는다.
5. `Duration`은 상대 timeout이고 `Instant`는 절대 deadline이다. coroutine/JDK의 0·음수·무한 timeout 및 실제 시간 정밀도 차이를 명시하며, 취소 요청이 작업 종료를 보장하지 않는다고 구분한다.
6. 테스트는 latch와 virtual time을 우선해 경계를 결정적으로 검증하고, 소유 scope/executor를 정리한다. ABI 검사와 사전 컴파일 fixture runtime 검증을 별도 증거로 유지한다.
7. 한국어 KDoc과 core/coroutines의 한·영 README에 반환값, 예외, timeout 기본값, 소유권·취소 한계, `Job.joinUntil(Duration)`와 `StructuredTaskScope.joinUntil(Instant)`의 차이를 기록한다.

## 초기 지적의 처분

| 초기 지적 | 명세에 반영한 처리 | 최신 상태 |
|---|---|---|
| 새 `SuspendLazy` 멤버가 사용자 구현체의 호환성을 깨뜨릴 수 있음 | `-jvm-default=enable`에 맞춘 기본 메서드와 구 API 기반 precompiled fixture 런타임 검증을 명시 | 해소 |
| `invokeAll`/`invokeAny` 래퍼가 입력 함수를 호출하지 않고 결과도 버릴 수 있음 | JDK 반환형을 보존하고 `Callable`이 대응 함수를 호출하며 부분 제출 거부 시 이전 작업 정리를 테스트 | 해소 |
| 캐시된 `suspendBlockingLazy`가 취소된 호출자에게도 값을 반환할 수 있음 | 빠른 경로에서 `ensureActive()`로 호출자 취소를 확인하고 타이머는 만들지 않도록 규정 | 해소 |
| `join(defaultValue)`가 업무 `TimeoutException`을 timeout으로 오인할 수 있음 | timed `get`의 대기 timeout만 변환한 뒤 `ExecutionException` 원인을 unwrap하도록 규정 | 해소 |
| `Job.joinUntil`이 대상 실패·취소와 대기자 취소를 잘못 구분할 수 있음 | 대상 job 완료는 정상 반환하고 활성 대기자 취소만 전파하도록 계약화 | 해소 |
| 한국어 문구 일부가 상태와 원인 관계를 모호하게 표현함 | 호출자·문서 reviewer가 완료/예외 및 근거 표현을 자연스러운 한국어로 교정 | 해소 |

## 근거와 경계

- 저장소 근거는 명세 §근거에 열거한 `CompletableFutureSupport.kt`, `ExecutorServiceExtensions.kt`, `SuspendLazy.kt`, `DeferredSupport.kt`, `JobSupport.kt`, `FutureSupport.kt`와 `-jvm-default=enable` 설정이다.
- 외부 API 계약은 [JDK 25 ExecutorService](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ExecutorService.html), [JDK 25 Future](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/Future.html), [Kotlin Duration.inWholeNanoseconds](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-duration/in-whole-nanoseconds.html), kotlinx.coroutines의 [withTimeout](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-timeout.html), [withTimeoutOrNull](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-timeout-or-null.html), [Job.join](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/join.html), [ensureActive](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html) 문서다.
- 사용자 정의 dispatcher의 `Delay` 정밀도, 협력하지 않는 blocking initializer의 강제 중단, 취소 후 실제 작업 종료 시점은 보장하지 않는 경계로 남겼다.
- 검토 당시 branch 기준점은 `7c6cbd1e261efb23b8146ea5c6f949d6591212ee`였다. 작업 트리의 다섯 production 파일 변경은 사용자 소유이며 이 문서 commit에 포함하지 않는다.

## 작성 및 통합 검토 DoD

- [x] **SPW-01 — 독자·목적·범위·근거:** 대상 API, 저장소, branch, 분류 및 검토 범위를 첫 절에 고정했다.
- [x] **SPW-02 — 검토 문서 계약:** 여섯 관점, 초기 지적의 처분, 통합 계약, 근거 경계와 다음 gate를 기록했다.
- [x] **SPW-03 — 한국어 기술 문체:** 한국어 자연스러움 checklist와 용어 감사를 수행하고 코드 토큰·API명·URL을 보존했다.
- [x] **SPW-04 — 의미·추적성:** 명세, 현재 저장소 변경, 공식 JDK/Kotlin/kotlinx.coroutines 문서를 대조했다. 실행 검증이 아닌 설계 검토라는 한계를 분리했다.
- [x] **SPW-05 — 최종 읽기:** Markdown 구조와 링크를 다시 읽고 whitespace 및 diff 검사를 수행했다.
- [x] 여섯 최신 관점 모두 P0=0, P1=0으로 수렴했다.
- [ ] 사용자의 작성된 명세 검토·승인 — 명세 commit 후 진행한다.
- [ ] API 테스트, Detekt, ABI, PR 생성 — 명세 승인 및 후속 구현 계획 이후 진행한다.
