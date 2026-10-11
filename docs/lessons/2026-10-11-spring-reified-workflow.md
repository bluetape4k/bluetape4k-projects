# Spring reified 확장 작업의 재발 방지 교훈

## 맥락과 결과

이슈 [#1823](https://github.com/bluetape4k/bluetape4k-projects/issues/1823)에서 Spring Boot·Spring Framework의 Java `Class<T>` 기반 API를 Kotlin reified 확장 함수로 감싸는 기능을 `bluetape4k-spring-boot-core`에 추가했다. 최종 코드 HEAD `78bbbe9`에는 28개 API, 집중 테스트와 영문·한국어 README가 포함됐다. 모듈 테스트 337개, 빌드, Detekt, `git diff --check`가 통과했고 여섯 관점의 최종 코드 검토에서 P0/P1 미해결 사항은 없었다. PR 생성과 통합은 승인 범위 밖이다.

## 다음 작업에서 적용할 규칙

### 1. 사용 사례에서 출발해 API 표면을 넓게 조사한다

처음에는 사용자가 직접 제시한 `ApplicationContext.getBean(Class<T>)`와 AWS 저장소의 세 API를 중심으로 후보를 찾았다. 사용자가 Spring API를 더 넓게 조사하라고 보정한 뒤에야 `Environment`, Spring Core·Context, Spring Boot의 호출 패턴까지 범위를 확장했다. 초기 후보 목록을 완성된 조사로 간주하지 않는다.

다음 Kotlin 편의 API 조사는 Spring Core, Context, Boot처럼 사용자가 정한 도메인을 패키지별로 훑고, 다음 항목을 추적한다.

- `Class<T>`나 애너테이션 타입을 받는 API
- 결과 타입이 호출 문맥에서 반복 선언되는 API
- 소비자 코드에서 `T::class.java` 또는 `Class<T>`를 반복 전달하는 API
- 기존 reified 확장이나 유사 helper가 이미 있는지
- 오버로드, nullable 인자, vararg, 생명주기, 실패 처리 차이

사용자가 추가 조사 범위를 명시하면 계획과 수용 기준에 바로 반영하고, 최초 후보 목록만 확인한 채 실행 단계로 넘어가지 않는다.

### 2. 구현 전에 이슈와 승인 계획을 연결한다

이 작업은 구현이 진행된 뒤 사용자가 “이슈 생성은 언제 하나?”라고 물어 이슈를 만들어 정리하라는 지시를 추가로 받았다. 이슈 생성이 늦었다는 피드백을 다음 기능 작업의 선행 점검으로 바꾼다. 조사 결과와 중복 이슈 여부를 확인한 뒤, 승인 계획의 구현 작업을 시작하기 전에 추적 이슈를 생성하거나 기존 이슈를 갱신하고, 범위·완료 기준·제외 항목을 계획서와 서로 연결한다. 이슈 생성 또는 갱신 사실을 live GitHub에서 다시 읽어 확인한다.

### 3. 첫 단계에서 워크플로 기준 정보를 확인하고 순서대로 기록한다

사용자는 `$bluetape-workflow` 지침이 제대로 지켜지지 않는다고 지적했다. 다음 bluetape4k 작업에서는 코드 탐색이나 변경보다 먼저 해당 워크플로와 AGENTS.md 계층을 확인한다. 분류, 순서가 정해진 계획, 사용자 승인, leaf 및 공통 gate, 단계별 증거 기록을 순서대로 진행한다. 계획에 적힌 의존 순서를 건너뛰지 않고, 완료하지 않은 항목을 PASS나 N/A로 축약하지 않는다.

작업 중 사용자 보정은 설명으로만 남기지 말고 해당 수용 기준, 체크리스트, 이슈, 교훈 문서에 반영한다. 이 작업에서 조사 범위 확대와 이슈 선행 생성은 다음 Spring 편의 API 작업에 적용할 구체적인 재발 방지 규칙이다.

### 4. reified 확장 함수는 Spring 계약을 직접 보존한다

확장 함수는 `T::class.java`를 대응하는 Spring 오버로드에 전달하는 얇은 어댑터로 유지한다. 공개 API를 추가할 때는 Java 호출부의 타입 토큰 반복만 제거하고 다음 계약을 그대로 검증한다.

- nullable 인자와 결과
- 오버로드 선택과 인자 순서
- vararg/customizer 전달
- 구체적인 fluent runner 반환형
- 예외와 기본값 처리
- `Supplier` 실행 시점과 Spring 소유 생명주기

각 API의 집중 테스트는 문서 예제와 같은 호출 형태를 컴파일하고, 동등한 Spring 직접 호출과 결과·실패 동작을 비교한다. 특히 기본 오버로드 외에 실패 처리기, 명시적 `null`, 이름 지정 오버로드처럼 의미가 달라질 수 있는 경계도 테스트한다.

### 5. factory 부분 성공을 기본 성공처럼 감추지 않는다

`SpringFactoriesLoader`에서 사용자 `FailureHandler`가 실패 후 정상 반환하면 Spring은 해당 factory를 건너뛰고 부분 목록을 반환할 수 있다. 모든 factory 로딩이 필요하다면 기본 실패 처리기가 예외를 던지게 둔다. 호출자가 부분 결과를 의도적으로 수용하는 실패 처리기를 전달할 때는 그 선택을 문서와 테스트에서 명시한다. `ArgumentResolver`가 받는 인자, `FailureHandler`가 받는 factory 식별 정보, 수신 객체에 설정된 리소스 위치와 `ClassLoader`를 각각 구분해 설명한다.

### 6. lint 억제로 API 밀집을 숨기지 않고 언어별 문서를 같이 검토한다

Detekt 위반을 넓은 suppression으로 가리지 않는다. 파일 책임이 커졌다면 함수군별 파일로 나누고, 공개 API를 설명하는 주석은 저장소 언어 정책을 따른다. `README.md`와 `README.ko.md` 예제는 API 이름, 호출 결과, 의존성 표를 맞춰 함께 검토한다. 기본 테스트·빌드·정적 분석의 성공 여부와 기존 무관 진단은 구분해 기록한다.

### 7. native 검토자의 진행 상태를 결과 품질과 별도로 관리한다

이번 API 관점 검토의 코드는 PASS였지만, App/plain 세션에서 `wait_agent` 차단 대기를 사용했고 해당 lane 상태를 제때 확인·회수하지 못했다. lane 시작 시각 `2026-10-10T14:20:37Z`, `startup_ack` 기록 시각 `2026-10-10T14:26:16Z`는 5분을 넘는다. 검토 결론이 맞더라도 상태 확인 절차 위반을 사후 PASS 처리하지 않는다.

문서 작성자 검토에서도 절차 누락이 반복됐다. `spring_a08_writer_docs_r1`은 sequence 185에서 생성한 뒤 agent 실행 전에 시작 기한이 지나 sequence 186에서 취소했다. `spring_a08_writer_docs_r2`는 187에서 생성, 188에서 시작한 뒤 agent 실행 전에 ACK 기한이 지나 189에서 취소했다. `spring_a08_writer_docs_r3`는 191에서 시작하고 34초 뒤 192에서 `startup_ack`를 기록해 30초 목표를 넘겼다. 또한 `list_agents`로 상태를 확인하기 전에 `probe-sent`를 기록하지 않았다. r3의 독립 작성자 검토는 `REQUEST CHANGES`를 반환해 아래 네 가지 문서 보완을 요청했다.

- 여섯 A-08 관점 각각에 정확한 lane 식별자, 완료 receipt sequence, P0/P1/P2/P3 수와 `changed_paths=[]`를 연결하고, 소스·테스트·README 근거와 승인 계획 및 워크플로 실행/receipt를 표시한다.
- 교훈 문서에 `liveness-contract.md`와 `topology-contract.md`의 `bluetape-flow.py` 상태 변경 계약을 연결한다. 매 상태 변경 직전에 정확한 대상 경로와 `receipt.jsonl`, `run.json`을 포함해 `mutation-check`를 새로 실행하고, 그 checksum을 `--expected-head`에 전달한다. `lane-complete` 전에는 NUL-safe Git 상태와 branch diff를 확인해 변경 경로를 검증한다.
- SpringFactoriesLoader 수신 객체의 리소스 경계 표현을 “수신 객체에 설정된 리소스 위치와 `ClassLoader`”로 구체화한다.
- 계획의 `2026-10-10` 문서 경로 대신 현재 날짜인 2026-10-11 파일명을 쓴 사실과 이유를 문서에 남긴다.

다음에는 `liveness-contract.md`의 main 세션 실행 순서를 그대로 따른다. `lane-create` 뒤 시작 ACK 제한 시간을 확인하고 `lane-start` 직후 native spawn을 호출한다. `startup_ack`는 native 도구 결과에서 실제 실행을 관찰한 뒤 기록하며, 30초 목표를 놓쳤다면 정상 ACK로 처리하지 않고 helper가 허용한 복구 경로를 따른다. App/plain 세션에서는 `wait_agent` 차단 대기를 쓰지 않는다. probe 대상에 `send_message`나 `list_agents`를 호출하기 전에 `probe-sent` receipt를 기록하고 응답을 읽는다. heartbeat는 진행이나 완료 증거로 쓰지 않는다.

네 번째 `spring_a08_writer_docs_r4`는 194에서 생성, 195에서 시작한 뒤 196에서 마감 시각이 지난 `startup_ack`를 남겼다. 197에 stall을 기록하고, 198에 probe를 전송한 뒤 199에서 probe ACK를 기록했다. r4의 독립 검토 결과는 sequence 200에 `REQUEST CHANGES`와 P1 한 건으로 기록됐다. 지적 내용은 이 문서가 아직 실행되지 않은 최종 `completion-check` 결과를 이미 확인한 것처럼 서술했다는 점이었다. 해당 표현을 수정하고 최신 확인 결과로 교체했다.

최신 read-only `completion-check`는 sequence 200에서 `complete=false`, `missing_lanes`에 취소된 `spring_a08_writer_docs_r1`과 `spring_a08_writer_docs_r2`, `missing_main_verification=true`, `missing_components=[]`, `failed_checks=[]`를 보고했다. `bluetape-flow.py`의 `lane-resolve`는 `failed` 상태 lane만 복구 대상으로 받으므로 `cancelled`를 `completed`로 바꾸거나 lane 이력을 다시 쓰지 않는다. 취소 전에 검토 agent를 실제로 시작했는지 확인하고, 취소된 필수 lane을 남겼다면 완료 판정을 PENDING으로 유지한다. 확인된 lane을 성급히 취소하지 말고 재검토 필요 여부를 먼저 판단한다.

운영 정책과 도구의 임계값도 정합성을 확인한다. `references/liveness-contract.md`는 App/plain 세션에서 90초 무증거를 stall로 다루지만 `references/workflow-manifest.json`의 `bluetape-flow.py` 기본값은 `suspected_stall_seconds=120`이다. 이 차이는 해당 운영 기준을 관리하는 별도 변경에서 조정하기 전까지 더 짧은 문서 기준에 맞춰 관찰하되, 도구가 보고한 실제 결과는 별도로 기록한다.

`bluetape-flow.py` 상태 변경은 `references/topology-contract.md`의 Guarded Command Contract에 맞춘다. 매 상태 변경 전 `mutation-check`를 실행해 현재 run checksum과 정확한 대상 경로를 결합하고, 해당 checksum이 바뀌기 전 한 번만 `--expected-head`에 사용한다. JSON 입력은 허용된 필드만 포함한 일반 파일로 두고, 변경 경로는 실제 NUL-safe Git 상태와 branch diff로 교차 확인한다. 도구가 보고한 권고나 관찰하지 않은 agent 응답을 실행 증거로 올리지 않는다.

## 추적 근거

- 계획과 수용 기준: `docs/superpowers/plans/2026-10-09-spring-reified-extensions-plan.md`. 계획서에는 검토 기록과 교훈 문서 이름이 `2026-10-10`으로 적혀 있지만 현재 작성일 2026-10-11에 맞춰 이 두 문서의 파일명을 정했다.
- 최종 코드 검토: `docs/superpowers/reviews/2026-10-11-spring-reified-extensions-final-review.md`
- 기능 이슈: [#1823](https://github.com/bluetape4k/bluetape4k-projects/issues/1823)
- 후보 조사: `gno://bluetape4k-wiki/research/2026-10-09-spring-reified-extension-candidates.md`. 이번 재조회에서 `bluetape4k-github`에는 이 이슈 관련 hit가 없었고, GitHub 이슈는 직접 확인해 `OPEN`, milestone `2.1.0`, assignee `debop`임을 검증했다.
- 실행 증거: workflow run `20261010T060453Z-1b1ba453`, `.bluetape/runs/20261010T060453Z-1b1ba453/receipt.jsonl`; A-08 검토 sequence 170, 171, 172, 180, 182, 184.
- native 검토자 수명주기: `$bluetape-workflow/references/liveness-contract.md`; lane 결과와 복구 상태: `$bluetape-workflow/references/topology-contract.md`.
- 구현 및 테스트: `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/`, `spring-boot/core/src/test/kotlin/io/bluetape4k/spring/`
- 사용자 문서: `spring-boot/core/README.md`, `spring-boot/core/README.ko.md`
