# Spring reified 확장 작업의 재발 방지 교훈

## 맥락과 결과

이슈 [#1823](https://github.com/bluetape4k/bluetape4k-projects/issues/1823)에서 Spring Boot·Spring Framework의 Java `Class<T>` 기반 API를 Kotlin reified 확장 함수로 감싸는 기능을 `bluetape4k-spring-boot-core`에 추가했다. 구현 코드 HEAD `78bbbe9`에는 28개 API, 집중 테스트와 영문·한국어 README가 포함됐다. 2026-10-10 실행한 모듈 테스트 337개, build, Detekt가 통과했고 여섯 관점의 코드 검토에서 P0/P1 미해결 사항은 없었다. 당시 승인에는 PR이 포함되지 않았지만, 사용자는 2026-10-11 후속 지시로 이 기능을 PR과 merge까지 진행하도록 범위를 넓혔다. merge는 정확한 PR/head에 대한 새 승인을 받아야 한다.

2026-10-12 최신 `develop` rebase 뒤 PR 전달을 재개하면서 테스트 fixture의 Detekt 위반 6건을 강제 실행으로 발견했다. 빈 annotation fixture 메서드를 `Unit` 표현식으로 바꾸고 긴 assertion을 줄바꿈한 뒤 commit `079e6666805d9cf21d287b66cad16623a9456080`에서 모듈 build 337 tests, forced Detekt(변경 Kotlin 파일 findings 0), Kotlin LSP 20/20 진단 0을 다시 확인했다. 이전 fresh code-review lane은 rebase 직후 HEAD `2346515603f72135743db92a4c55869780d1e264`에서 0 findings를 보고했지만 Detekt 검증은 미완료였으므로, 최종 전달 리뷰는 현재 delivery receipt에서 분리해 추적한다.

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

네 번째 `spring_a08_writer_docs_r4`는 194에서 생성, 195에서 시작한 뒤 196에서 마감 시각이 지난 `startup_ack`를 남겼다. 197에 stall을 기록하고, 198에 probe를 전송한 뒤 199에서 probe ACK를 기록했다. r4의 독립 검토 결과는 sequence 200에 `REQUEST CHANGES`와 P1 한 건으로 기록됐다. 지적 내용은 이 문서가 아직 실행되지 않은 최종 `completion-check` 결과를 이미 확인한 것처럼 서술했다는 점이었다. 후속 receipt sequence 201은 취소된 문서 리뷰 lane 때문에 run을 `blocked`로 끝냈다. 2026-10-12에 그 receipt head를 기준으로 실행한 read-only `completion-check`는 완료 불가 상태를 다시 확인했다.

run `20261010T060453Z-1b1ba453`의 마지막 receipt 이벤트 sequence 201은 `run_blocked`다. 이 receipt head를 대상으로 한 read-only `completion-check`는 `complete=false`, `missing_lanes`에 취소된 `spring_a08_writer_docs_r1`과 `spring_a08_writer_docs_r2`, `missing_main_verification=true`, `missing_components=[]`, `failed_checks=[]`를 보고했다. `completion-check` 자체가 receipt 이벤트를 추가한 것은 아니다. `bluetape-flow.py`의 `lane-resolve`는 `failed` 상태 lane만 복구 대상으로 받으므로 `cancelled`를 `completed`로 바꾸거나 lane 이력을 다시 쓰지 않는다. 취소 전에 검토 agent를 실제로 시작했는지 확인하고, 취소된 필수 lane을 남겼다면 완료 판정을 PENDING으로 유지한다. 확인된 lane을 성급히 취소하지 말고 재검토 필요 여부를 먼저 판단한다.

운영 정책과 도구의 임계값도 정합성을 확인한다. `references/liveness-contract.md`는 App/plain 세션에서 90초 무증거를 stall로 다루지만 `references/workflow-manifest.json`의 `bluetape-flow.py` 기본값은 `suspected_stall_seconds=120`이다. 이 차이는 해당 운영 기준을 관리하는 별도 변경에서 조정하기 전까지 더 짧은 문서 기준에 맞춰 관찰하되, 도구가 보고한 실제 결과는 별도로 기록한다.

`bluetape-flow.py` 상태 변경은 `references/topology-contract.md`의 Guarded Command Contract에 맞춘다. 매 상태 변경 전 `mutation-check`를 실행해 현재 run checksum과 정확한 대상 경로를 결합하고, 해당 checksum이 바뀌기 전 한 번만 `--expected-head`에 사용한다. JSON 입력은 허용된 필드만 포함한 일반 파일로 두고, 변경 경로는 실제 NUL-safe Git 상태와 branch diff로 교차 확인한다. 도구가 보고한 권고나 관찰하지 않은 agent 응답을 실행 증거로 올리지 않는다.

### 8. LSP 도구 이름과 실행 가능한 서버를 구분한다

후속 코드 리뷰는 `lsp_diagnostics` 실행 도구를 찾지 못해 검증을 보류했다. 하지만 `~/.local/bin/kotlin-lsp`는 설치되지 않은 `262.4739.0` 경로를 가리켰고, PATH의 `kotlin-language-server` `1.3.13`은 JDK 25 버전 문자열을 파싱하지 못했다. 별도로 설치된 Kotlin LSP `263.6379.0`의 `bin/intellij-server`는 사용할 수 있었다.

첫 LSP client는 `--stdio` 없이 실행됐다. `intellij-server`의 기본값은 TCP server mode였으므로 JSON-RPC client는 초기화 응답을 받을 수 없었다. `intellij-server --help`에서 실행 옵션을 확인해 `--stdio`로 다시 연결하고 `workspace/configuration` 요청에 응답한 뒤 `textDocument/diagnostic`을 요청했다. commit `079e6666805d9cf21d287b66cad16623a9456080`에서 변경 Kotlin 파일 20/20, errors/warnings 0건을 확인했다. 다음에는 `lsp_diagnostics`라는 이름만 검색하지 말고 활성 tool 목록, wrapper 대상, 실제 server binary와 버전을 차례로 확인한다. 직접 프로토콜을 쓸 때는 server 요청 응답과 push/pull diagnostics를 처리하고 대상 파일 수가 결과에 모두 포함됐는지 검증한다. 신뢰할 수 있는 전체 결과를 받지 못하면 LSP를 PASS나 N/A로 바꾸지 않고 PENDING으로 두며 compiler·Detekt·테스트를 보조 증거로 남긴다.

### 9. Type A leaf 지침을 첫 변경 전에 읽는다

이번 PR 전달을 이어가던 중 Type A `bluetape-full-feature` skill을 문서 수정 뒤에 읽었다. 이는 WF-04 순서를 어긴 것이다. 발견 즉시 downstream 진행을 멈추고 전체 leaf, 공통 gate, Kotlin 패턴, PR template과 해당 코드·문서 리뷰 reference를 읽어 범위와 빠진 조건을 다시 대조한다. 현재 보충 계획에 이 누락과 복구를 기록하고, 영향받은 문서는 용어 감사와 독립 검토를 다시 받아야 한다. 다음에는 `$bluetape-workflow` 분류 뒤 선택한 leaf와 triggered reference를 모두 읽고, 첫 mutation 전에 체크리스트를 고정한다.

### 10. Receipt 출력은 허용 필드만 표시한다

일부 `bluetape-flow.py` 응답과 manifest에는 owner fingerprint가 들어 있으므로 raw receipt·manifest나 전체 JSON 객체를 출력하지 않는다. 필요한 sequence·상태·결과 필드만 허용 목록으로 골라 읽는다. fingerprint가 출력된 사실을 발견하면 해당 handle을 노출된 것으로 취급하고, 승인된 실행 session에서 `resume`으로 새 owner epoch를 발급해 이전 handle을 폐기한다. 이번 PR 전달 run에서는 raw receipt 및 `resume-check` 응답으로 당시 owner fingerprint가 출력된 뒤 helper를 통해 sequence 15, owner epoch 2로 전환했다. 이후 manifest에서도 이전 epoch fingerprint가 들어 있음을 확인했다. 현재 handle의 fencing 값은 출력하거나 기록하지 않는다. 변경 전 `mutation-check`를 통과했고 새 handle을 사용 중이라는 결과만 남긴다.

### 11. 필수 lane은 시작 기록 직후 실제 검토자를 실행하고, 종료 상태의 복구 가능성을 확인한다

PR 전달을 재개하면서 `spring_reified_final_docs_review_r2`를 receipt sequence 17에서 시작했지만 native reviewer를 즉시 spawn하지 않았다. startup ACK 기한이 지난 뒤 stall/probe를 기록하고 native agent registry에서 해당 agent가 없음을 확인했다. 그 뒤 lane을 `cancelled`로 끝내 sequence 21을 남겼다. `completion-check`는 receipt head 22에서 이 lane을 계속 `missing_lanes`로 보고했고, `lane-resolve`는 `failed` lane에 대해서만 작동하므로 기존 run은 sequence 22에서 `run_blocked`로 닫았다.

다음에는 `lane-start` 직후 같은 실행 흐름에서 native spawn을 호출하고, 실제 반환 결과를 관찰하기 전에는 ACK를 기록하지 않는다. ACK 기한을 놓친 필수 lane은 임의로 `cancelled` 처리하지 않는다. 먼저 해당 lane의 정확한 상태와 허용된 전이를 확인한다. 작업을 이어가야 하고 실제 실패가 확인되면 근거를 붙여 `failed`로 기록한 뒤, 실패 lane을 `parent_lane_id`로 선언한 완료된 재검토 lane의 증거를 연결해 `lane-resolve`를 사용한다. `cancelled`는 완료나 실패 해결로 바꿀 수 없으므로, 완료 가능성을 확인하기 전에 terminal 전이를 선택하지 않는다.

### 12. 리뷰 승인과 정적 분석 통과를 별도 게이트로 다룬다

최신 base `d9e175f240b16b4f6d6964ff35867ec9d09cd739`를 반영한 HEAD `2346515603f72135743db92a4c55869780d1e264`에서 첫 독립 code-review lane은 APPROVE와 0 findings를 반환했지만 Detekt 강제 재실행이 아직 끝나지 않았다고 표시했다. 이후 강제 실행이 새로 추가된 테스트 파일에서 5개의 `EmptyFunctionBlock`과 1개의 `MaxLineLength`를 찾았다. review verdict만 보고 static-analysis gate를 통과 처리하지 않고, 두 fixture 파일을 수정한 뒤 별도 commit `079e6666805d9cf21d287b66cad16623a9456080`에서 build, 테스트, Detekt, LSP를 다시 실행했다.

이 fresh delivery lane은 `lane-start` 직후 native spawn을 수행했지만 `startup-ack` 기록은 시작 시각에서 30초 제한을 넘겼다. ACK는 실제 agent 결과를 확인한 뒤 기록했으며 뒤늦은 기록을 기한 내 처리로 바꾸지 않는다. 다음 lane은 native spawn 결과 직후 ACK를 기록하고, 90초 무증거면 receipt 정책에 따라 bounded liveness probe를 사용한다. 검토 결과는 내용 정확성과 별도로 lane 기한·관찰 순서를 기록한다. run `20261011T181753Z-8e390735`의 최종 exact-head 판정은 workflow receipt와 PR 상태를 기준으로 추적한다.

### 13. 문서 검토 lane의 무결성과 chronology를 함께 기록한다

새 PR 전달 run `20261011T181753Z-8e390735`의 필수 문서 검토에서 `spring_reified_pr_docs_review_r2`는 sequence 12–17에 생성·시작·ACK·stall·probe·probe ACK를 기록했지만, 검토 결과를 받지 못해 sequence 18에서 `failed`로 종료했다. 이 결과는 PASS가 아니다. 이를 대체한 `spring_reified_pr_docs_review_r3`는 r2를 parent로 선언해 sequence 19에서 생성, 20에서 시작, 21에서 ACK, 22에서 완료했으며 REQUEST CHANGES와 P2 한 건을 반환했다. 지적은 addendum과 최종 리뷰에서 r3의 sequence 19–21 chronology가 빠졌다는 것이었다. sequence 23에서 `final-docs-review` check는 실패로 기록됐다.

문서 수정은 상태 전이를 소급하지 않는다. 성공한 child reviewer가 수정본을 다시 확인한 뒤, 실패한 lane과 새 lane의 evidence digest를 연결해 `lane-resolve`하고 required check를 다시 PASS로 기록한다. 이번 run에서 r4는 sequence 24에서 생성, 25에서 시작, 26에서 ACK, 27에서 APPROVE로 완료됐다(P0–P3=0, `changed_paths=[]`). sequence 28에서 `final-docs-review`를 PASS로 기록했고 sequence 29에서 r2 failed lane을 r4의 terminal evidence digest로 해소했다. `completion-check`는 취소된 필수 r1(sequence 11), component evidence, main verification이 남아 있어 `complete=false`를 반환했다. 취소 lane은 resolve 대상이 아니므로 sequence 30에서 run을 BLOCKED로 종결하고 새 Phase 2 run `20261011T193452Z-45b1f5b2`를 sequence 1–3에서 초기화·승인·시작했다. 최종 artifact에는 reviewer 결과, receipt sequence, 수정 사항과 현재 check 상태를 연결해 기록한다.

### 14. Phase 2 bootstrap과 일반 receipt mutation을 구분한다

새 Phase 2 run `20261011T193452Z-45b1f5b2`는 sequence 1에서 생성됐다. `mutation-check`는 현재 session에 묶인 `running` 또는 `recovering` run만 허용하므로, 아직 `planned`인 run에서는 검증 결과를 만들 수 없다. 이 상태에서 helper의 safe-next 순서 `run-approve`와 `run-start`를 건너뛰지 않는다. 이번에는 초기 receipt를 `verify`로 다시 읽어 sequence 1 checksum을 대조한 뒤 계획 승인과 시작을 기록했고, `running` 상태가 된 뒤 일반 receipt mutation의 `mutation-check`를 재개한다. 새 receipt를 만들 때는 bootstrap 예외와 이후 mutation gate를 혼동하지 않는다.

같은 run의 문서 writer r5 lane은 sequence 4에서 생성, 5에서 시작, 6에서 startup ACK, 7에서 APPROVE 완료됐다. ACK는 deadline `19:41:49Z`에서 55초 지난 `19:42:44Z`에 도착했다. ACK가 기록됐더라도 기한을 넘겼다는 사실을 지우지 않고, 지연과 최종 결과를 각각 receipt와 문서에 남긴다. 당시 수정 문서의 SPW-05는 최종 writer 재검토가 끝날 때까지 PENDING으로 뒀다.

r5의 command deadline은 `19:46:19Z`였지만 완료 기록은 `19:49:21Z`에 남아 3분 2초 늦었다. 결과의 정확성은 기한 준수를 대신하지 않는다. 다음 lane은 startup ACK와 command deadline을 각각 추적하고, 기한을 넘기기 전에 liveness 절차에 따라 native 상태를 확인한다.

r6는 sequence 8에서 생성, 9에서 시작, 10에서 ACK를 기록했다. lane metadata의 canonical ACK deadline `19:54:13Z`와 비교하면 `19:54:22Z` ACK는 9초 늦다. sequence 10 evidence 요약에 적힌 `19:53:41Z`는 잘못된 값이지만 immutable receipt는 수정하지 않았다. canonical deadline과 정정은 sequence 14의 downstream completion evidence에 남겼다. r6는 sequence 11에서 `suspect_stall`, sequence 12에서 probe, sequence 13에서 probe ACK, sequence 14에서 완료됐다. native reviewer 결과는 stall 기록 뒤 도착했고, 이후 probe와 `list_agents` read-back으로 `completed` 상태 및 APPROVE(P0–P3=0, `changed_paths=[]`)를 확인했다. liveness 기록은 결과가 도착했다는 이유로 생략하지 않는다.

문서 writer r7은 sequence 15에서 생성, 16에서 시작, 17에서 startup ACK, 18에서 당시 문서본을 APPROVE했다. 다섯 문서 모두 P0–P3=0, `changed_paths=[]`였고 링크 15/15, 다섯 파일 용어 감사 findings 0건을 확인했다. 그 뒤 lesson, final review, addendum, testlog를 고쳤으므로 r7 승인은 현재 문서본의 승인 근거가 아니다. r8은 sequence 19–24에서 stall·probe 뒤 interrupt됐고 verdict가 없다. 대체 reviewer r9는 sequence 25–31에서 현재 문서본을 REQUEST CHANGES(P0=0, P1=1, P2=1, P3=0)로 판정했다. P1은 이전 r7 승인을 current SPW-04/05 근거로 재사용한 점이고, P2는 final review의 `78bbbe9` 이후 코드 변경 범위가 부정확한 점이다. r9는 링크 15/15, 다섯 파일 용어 감사 findings 0건, `changed_paths=[]`를 확인했으나 파일을 수정하거나 테스트를 실행하지 않았다. sequence 32에서 r8/r9 replacement lineage를 닫았다. 현재 문서 수정 후 독립 재검토 전까지 SPW-04/05를 PENDING으로 유지한다. 코드 리뷰와 main verification도 별도 필수 단계다.

2026-10-12 writer r10은 sequence 33에서 생성, 34에서 시작, 35에서 ACK됐다. ACK는 30초 기한보다 5초 늦었고, r10은 용어 정합성 P2를 보고했다. 지적을 수정한 뒤 다섯 문서의 task-local 감사에서 `findings=[]`를 확인했다. 이후 r11은 sequence 78–81에서 늦은 ACK와 늦은 결과 확인을 기록한 뒤 REQUEST CHANGES(P0=0, P1=0, P2=1, P3=0)를 반환했다. P2는 final review의 Gradle 명령에서 `--rerun-tasks`와 `--console=plain`이 빠져 testlog의 실제 실행 명령과 달랐다는 provenance 불일치다. r11은 파일을 수정하거나 테스트하지 않았고 `changed_paths=[]`였다. sequence 82의 `final-docs-review` check는 실패로 기록했다. 이 revision에서 review 명령을 testlog의 실제 실행과 일치시켰다. 다음 reviewer 결과를 기록하기 전에는 required check를 PASS로 바꾸지 않는다.

여섯 코드 리뷰 lane은 sequence 36–42에서 등록했다. 성능·안정성·보안 reviewer의 첫 시도는 사용량 제한으로 sequence 51–53에서 실패했다. 이 실패는 immutable로 남기고, main-session inline fallback을 각각 71, 73, 75에 완료한 뒤 original/fallback evidence digest를 72, 74, 76에서 연결했다. inline 검토를 independent attestation으로 부르지 않는다. 세 native reviewer의 지연 재시도 결과는 같은 exact HEAD의 supplemental PASS/APPROVE로만 보존한다.

Ops native reviewer는 사전 등록된 기한이 지난 후 sequence 63/64에 시작·ACK됐다. liveness-check가 `suspect_stall`을 반환해 stall record를 시도했지만 `--decision`에 JSON file이 필요한 데 scalar를 넘겨 helper가 거부했다. receipt는 변경되지 않았고, 뒤이어 도착한 exact-head review 결과를 sequence 67에 기록했다. 다음에는 `stall-record --help`의 자료형을 확인하고, 권고를 receipt 변화처럼 보고하지 않는다. Developer/API lane은 65/66/68, caller lane은 69/70/77에 시작·ACK·완료됐다. 이들은 계획 당시 기한이 지난 뒤 실제 시각을 기록했으며 이를 기한 내 성공으로 포장하지 않는다.

현재 여섯 관점 코드 검토에서 P0/P1/P2는 0건이었다. main-session inline stability review는 `AnnotationExtensions.kt:1`의 stale file header를 P3로 기록했다. 기능 영향이 없고 exact-head review를 보존하기 위해 이번 delivery에서 수정하지 않는다고 문서화했다. 결과 요약, P3 처분과 남은 writer/DoD/PR gate는 [final review](../superpowers/reviews/2026-10-11-spring-reified-extensions-final-review.md)와 [delivery addendum](../superpowers/plans/2026-10-11-spring-reified-pr-delivery-addendum.md)에 연결한다.
## 추적 근거

- 계획과 수용 기준: `docs/superpowers/plans/2026-10-09-spring-reified-extensions-plan.md`. 계획서에는 검토 기록과 교훈 문서 이름이 `2026-10-10`으로 적혀 있지만 두 문서는 최초 작성일 2026-10-11을 반영해 파일명을 정했다. 이 기록은 2026-10-12까지 갱신했다.
- 최종 코드 검토: `docs/superpowers/reviews/2026-10-11-spring-reified-extensions-final-review.md`
- PR 전달 계획 보충: `docs/superpowers/plans/2026-10-11-spring-reified-pr-delivery-addendum.md`
- 기능 이슈: [#1823](https://github.com/bluetape4k/bluetape4k-projects/issues/1823)
- 후보 조사: `gno://bluetape4k-wiki/research/2026-10-09-spring-reified-extension-candidates.md`. 이번 재조회에서 `bluetape4k-github`에는 이 이슈 관련 hit가 없었고, GitHub 이슈는 직접 확인해 `OPEN`, milestone `2.1.0`, assignee `debop`임을 검증했다.
- 실행 증거: workflow run `20261010T060453Z-1b1ba453`, `.bluetape/runs/20261010T060453Z-1b1ba453/receipt.jsonl`; A-08 검토 sequence 170, 171, 172, 180, 182, 184.
- native 검토자 수명주기: `$bluetape-workflow/references/liveness-contract.md`; lane 결과와 복구 상태: `$bluetape-workflow/references/topology-contract.md`.
- 구현 및 테스트: `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/`, `spring-boot/core/src/test/kotlin/io/bluetape4k/spring/`
- 사용자 문서: `spring-boot/core/README.md`, `spring-boot/core/README.ko.md`

## 현재 전달 기준

구현 시점에 작성한 계획의 PR 제외 문구는 당시 권한을 기록한다. 이후 PR과 merge 지시를 받으면 별도 전달 계획에 저장소·base·head, PR 단계, exact-head 검증, merge 전 새 승인과 post-merge closeout을 구체적으로 적는다. 이렇게 해야 과거 계획을 다시 쓰지 않고도 최신 사용자 지시와 현재 gate를 연결할 수 있다. 이번 작업의 기준은 [전달 계획 보충](../superpowers/plans/2026-10-11-spring-reified-pr-delivery-addendum.md)이다.

## 작성자 문서 게이트 (SPW-01..05)

- **SPW-01 — PASS:** 독자는 다음 bluetape4k 구현·리뷰를 맡는 유지보수자다. 목적은 사용자 수정과 검증에서 나온 재발 방지 규칙을 다음 작업에 적용하는 것이다. 출처는 #1823, 승인 계획, Spring 구현·테스트·README, 이전 A-08 및 현재 PR delivery receipt, 사용자의 범위 수정, 현재 delivery addendum다. `ApplicationContext.getBean`, `Binder`, `BootstrapContext`, `SpringFactoriesLoader`, commit SHA와 gate ID를 그대로 보존했다. r7의 sequence 18 판정은 이전 문서본에 한정하고 r9 current-version disposition은 sequence 31, r11 command-provenance finding은 sequence 81–82에 연결했다.
- **SPW-02 — PASS:** 문서는 맥락·결과, 잘못된 가정과 드러난 근거, 수정 결정, 재발 방지 규칙, 추적 경로를 연결한다. 새 전달 규칙은 원래 계획의 범위와 후속 사용자 지시를 분리하고 CG-16 승인을 보존한다. lane-start 뒤 spawn 누락, 취소 상태의 완료 불가 결과와 재발 방지 전이 선택도 현재 receipt 근거에 연결했다.
- **SPW-03 — PASS:** 한국어 기술 문체와 일정한 Spring/Kotlin 용어를 유지하고 API·명령·경로·이슈 번호는 식별자 그대로 뒀다. `korean-naturalness-checklist.md`를 적용했다. r11 chronology와 provenance correction을 포함한 다섯 문서의 최신 contextual audit는 `files=5`, `findings=[]`였다.
- **SPW-04 — PASS:** code verification과 여섯 관점 review는 exact HEAD `079e6666805d9cf21d287b66cad16623a9456080`에 연결했다. final review의 Gradle 명령을 testlog의 실제 build/Detekt 실행과 맞추고, r11 finding과 sequence 81–82를 receipt 및 연관 문서와 대조했다. PR exact-head CI는 별도 gate다.
- **SPW-05 — PASS:** lesson과 연관 문서의 최종 Markdown을 다시 읽고 writer DoD를 기록했다. 독립 `final-docs-review`는 writer read-back과 별도 workflow check다. PR CI·CG-16·post-merge는 별도 후속 gate다.

### 15. 필수 check 전에 전체 topology를 등록한다

새 run에서 writer r9의 lane은 sequence 31에 terminal 상태가 됐지만 `final-docs-review` 결과를 먼저 기록하려 하자 helper가 `component check is not registered`로 거부했다. receipt는 변하지 않았다. 이번 run에는 필수 component와 check를 담은 전체 topology를 아직 등록하지 않은 상태였다. 다음에는 required reviewer와 main-verification lane을 모두 만든 뒤 승인된 전체 topology를 한 번 등록하고, 그다음 `check-result`, `component-evidence`, `completion-check`를 순서대로 실행한다. 등록되지 않은 check 결과를 재시도하지 말고 현재 topology와 run plan을 먼저 읽는다.

### 16. 용어 수정 뒤 SPW-03 근거를 새로 감사한다

독립 검토에서 용어 지적을 받으면 표현만 바꾸고 끝내지 않는다. 수정 문서 묶음 전체를 다시 감사하고, 각 SPW-03 기록이 새 결과를 설명하는지도 갱신한다. 이전 감사의 `findings=[]`는 그 뒤 바뀐 문서의 증거가 아니다. 이번 r10 P2도 이 순서로 수정해 다섯 문서 감사에서 `findings=[]`를 확인했다. r11은 별도로 review 문서의 Gradle 명령과 testlog의 실제 옵션이 다른 점을 발견했다. 같은 실행을 설명하는 review·testlog의 명령 문자열은 옵션까지 일치시켜 provenance를 보존한다.


### 17. 재시도 결과와 실패 lane의 증거를 구분한다

사용량 제한으로 실패한 reviewer lane을 같은 reviewer의 뒤늦은 회신만으로 PASS로 바꾸지 않는다. 실패 이벤트를 보존하고, 허용된 inline fallback을 별도 lane으로 끝낸 뒤 original/fallback evidence digest를 연결한다. delayed native reply는 provenance를 분명히 한 supplemental evidence로 남긴다. 시작·ACK·stall 기록도 실제 시각과 helper가 받아들인 schema만 사용하며, 거부된 lifecycle command는 receipt sequence를 바꾸지 않았다고 보고한다.
