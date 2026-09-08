# measured와 인프라 확장 구현 계획

**목표:** projects #1712–#1717의 단위·SDK 연동 계약을 독립 검증 가능한 변경으로 구현한다.
**구조:** 기존 Units 및 공식 SDK 타입을 재사용한다. measured, OpenFGA, Qdrant, Temporal은 파일 소유권을 분리하고 공통 빌드·카탈로그·CI는 주 세션이 통합한다.
**기술:** Kotlin/JVM 25, kotlinx.coroutines, JUnit 5, MockK, bluetape4k assertions, Testcontainers 및 Temporal 공식 테스트 환경.

## 1. 기준과 격리

- [x] 이슈 6개를 live-read하고 기준 브랜치와 기존 작업을 확인한다.
- [x] `feat/measured-infra-1712-1717` worktree를 생성한다.
- [x] `./gradlew :bluetape4k-measured:test --max-workers=2` — 기존 181개 테스트 통과.
- [x] workflow 및 Kotlin testing/module 규칙을 읽고 실행 범위를 등록한다.

## 2. measured — #1712–#1714

파일: `utils/measured/src/main/kotlin/io/bluetape4k/measured/{Electrical,DataRate,ForceTorque}.kt`, `Units.kt`, 대응하는 `src/test/kotlin/io/bluetape4k/measured/*Test.kt`, README 두 언어.

- [ ] 새 의미 타입의 환산·혼합 스케일·표시 회귀 테스트를 먼저 작성하고 누락 API로 실패함을 확인한다.
- [ ] Electrical.kt에 전류·전하·전압·저항 단위와 숫자 확장 및 정규화 연산을 추가한다.
- [ ] DataRate.kt에 B/s 기준 SI/IEC 단위, 생성 함수, 시간 곱셈·나눗셈과 명시적 표시 정책을 추가한다.
- [ ] ForceTorque.kt에 kg 기반 힘 환원과 수직 모멘트암을 받는 `torqueAt`를 추가한다.
- [ ] Units.kt의 toHuman 분기와 두 README를 확장한다. 기존 비유한 값/음수/0 계약을 유지한다.
- [ ] `./gradlew :bluetape4k-measured:test :bluetape4k-measured:detekt --max-workers=2`를 통과한다.

수치 검증: 1000mA=1A, 2kW/500mA=4000V, 1A×1s=1C, 1MΩ=10⁶Ω, 1MB/s=8Mbit/s, 1MiB/s=1048576B/s, 1MB/500ms=2MB/s, 1000g×1m/s²=1N, 1kN의 2m 모멘트암=2kN·m. Torque와 Energy 표시가 각각 N·m/J인지 확인한다. JVM erasure 충돌은 compile로 검증한다.

## 3. 중앙 SDK 카탈로그

파일: dependencies worktree의 `gradle/libs.versions.toml`, checksum, `build.gradle.kts`, 필요한 생성·동기화 설정.

- [ ] 공식 SDK 릴리스와 소스에서 좌표·JVM 요구·실제 메서드를 확인한다.
- [ ] 원본에 SDK aliases와 BOM constraints를 추가하고 checksum을 갱신한다.
- [ ] 프로젝트 빌드는 `-Pbluetape4kDependenciesCatalogPath=<원본 worktree>/gradle/libs.versions.toml`로 검증한다.
- [ ] 새 발행 모듈은 `sync-managed-catalog.py`의 repository-roots 입력으로 생성한다. 생성 영역을 수동 변경하지 않는다.
- [ ] 중앙 카탈로그 테스트와 BOM build/POM을 검증한다. 원격 통합 전에는 일반 checkout 빌드 가능 상태로 보고하지 않는다.

## 4. OpenFGA — #1715

파일: `infra/openfga/build.gradle.kts`, `src/main/kotlin/io/bluetape4k/openfga/*`, 단위/서버 통합 테스트, 테스트 resources, README 두 언어.

- [ ] 단위 테스트에 allow/deny/exception·batch 부분 실패·store/model 옵션 격리·취소를 먼저 고정한다.
- [ ] 공식 future await 확장, tuple 검증 빌더, 명시적 요청 scope 및 페이지 Flow를 구현한다.
- [ ] 실제 서버에서 모델 생성 → tuple 쓰기 → 권한 검사 → 읽기 흐름을 검증한다.
- [ ] 클라이언트는 호출자 소유이며 확장이 종료하지 않음을 검증한다. 오류 로그에 tuple/인증 정보를 남기지 않는다.
- [ ] `./gradlew :bluetape4k-openfga:test :bluetape4k-openfga:detekt --max-workers=1` 통과.

## 5. Qdrant — #1716

파일: `infra/qdrant/build.gradle.kts`, `src/main/kotlin/io/bluetape4k/qdrant/*`, 단위/서버 테스트, 테스트 resources, README 두 언어.

- [ ] 고정 point와 pending future로 query/upsert/delete await·취소·오류 전달 테스트를 먼저 작성한다.
- [ ] scroll cold Flow와 개수/직렬화 크기를 제한한 순차 배치 입력을 구현한다.
- [ ] 단일 초과 point 거부, 배치 경계, 느린 소비자·take·반복 cursor·재수집을 검증한다.
- [ ] 실제 서버에서 컬렉션 생성 → upsert → 필터 query → scroll → 삭제를 검증한다. 숫자/UUID ID와 vector 차원·이름 및 deadline을 확인한다.
- [ ] `./gradlew :bluetape4k-qdrant:test :bluetape4k-qdrant:detekt --max-workers=1` 통과.

## 6. Temporal — #1717

파일: `infra/temporal/build.gradle.kts`, `src/main/kotlin/io/bluetape4k/temporal/*`, SDK 테스트 환경 기반 테스트/주문 보상 예제, 테스트 resources, README 두 언어.

- [ ] pending result Future 대기 취소가 remote cancel을 호출하지 않는 테스트를 먼저 작성한다.
- [ ] 외부 클라이언트 start/signal/query의 IO 경계와 결과 await, 명시적 원격 취소·종료를 구현한다.
- [ ] SDK DSL을 재사용하며 Workflow 코드 내부에는 일반 코루틴을 추가하지 않는다.
- [ ] 공식 테스트 환경에서 시간 제어, 대기 취소 후 계속 실행, 명시적 remote cancel, Activity 재시도/멱등성/보상과 history replay를 검증한다.
- [ ] WorkerFactory 종료의 기한·강제 종료·소유권·스레드 종료를 검증한다.
- [ ] `./gradlew :bluetape4k-temporal:test :bluetape4k-temporal:detekt --max-workers=1` 통과.

## 7. 통합과 검토

- [ ] root README locale 및 AGENTS 모듈 목록, CI path filters와 테스트 작업·summary needs, Nightly, Kover artifact 수집, BOM/POM 등록을 갱신한다.
- [ ] `./gradlew projects`로 세 모듈 자동 등록을 검증한다. workflow는 actionlint와 기존 matrix 검증기를 사용한다.
- [ ] spec/plan 및 구현을 성능·안정성·보안·운영·개발 API·호출자 관점으로 검토한다. P0/P1은 수정 후 관련 검증을 다시 실행한다.
- [ ] Kotlin checklist와 문서 SPW-01–05 및 한국어 용어 검사를 완료한다.
- [ ] 교훈 문서에 단위 기준 혼동, SDK 취소와 원격 취소 차이, 중앙 카탈로그 도입 제약을 기록한다.
- [ ] `git diff --check`, scoped status, 이슈별 수용 기준을 최종 확인하고 변경을 commit한다.

## 복구 및 전달 범위

### 검토 보강 테스트

- measured: `DataRateFormat` 세 정책과 명시적 toHuman(format), Force/Acceleration→Mass 역변환, 기존 wildcard equals 제약 문서화. nullable API 도입은 N/A(기존 생성 API 없음).
- 페이지 Flow: maxPages 양수 검증, 동일 cursor 거부, A→B→A는 페이지 상한에서 실패, 정상 마지막 페이지는 상한 안에서 성공.

- OpenFGA: configured request timeout 및 전체 operation withTimeout, 수집 전 요청 0, 두 collector 독립 token, 빈 token 종료·반복 token 오류, take 후 요청 0, SDK close API 없음 확인.
- Qdrant: in-flight cancel, 실제 gRPC deadline, injected client 미종료, batch2 실패 전 batch1 성공 결과 전달, maxInFlight=1과 batch cap 초과 요청 0.
- Temporal: 실제 job 취소와 withTimeout 후 Workflow 계속 실행을 별개 테스트, shutdown 정상/timeout/force/반복 호출 및 service stubs 미종료.
- 공통: KLoggingChannel 상태 로그에 token/tuple/payload/workflow 인자 및 SDK 예외 원문이 포함되지 않음을 코드 검토한다. 서버 임시 자원 finally 정리와 실패 시 잔여물 확인을 테스트 실행 후 수행한다.

실패 시 해당 모듈의 수정·테스트 단계로 돌아간다. 단위 엔진이나 공유 SDK 상태를 우회 수정하지 않는다. 중앙 카탈로그와 consumer 변경은 독립 commit으로 유지한다. 컨테이너 테스트는 순차 실행한다. 이번 실행 권한은 구현·검증이며 PR 생성, 통합 브랜치 병합, 발행은 별도 전달 결정 전 수행하지 않는다. 미완료 이슈는 번호와 검증 공백을 명시한다.
