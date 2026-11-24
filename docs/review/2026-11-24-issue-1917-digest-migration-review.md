# 이슈 #1917 공통 digest 이전 검토 기록

## 범위와 기준 정보

- Projects 작업 브랜치: `feat/digest-api`
- 초기 Step 6-R 리뷰 기준 HEAD(이 문서의 과거 기록): Projects `9122e244669f1667551d4964eb2867a62e547521`, Workshop `3c86c23eaf95637ee7813560858543928e1a6a6f`
- 현재 Projects 코드 재검토 기준 HEAD: `f2cc3759bba728a688b1bb7ef1cb615b6650d05e`; 이 문서는 이후 문서 전용 커밋에서 갱신됐다.
- 설계·계획 문서 커밋: `fb03e1841fe7c3985c5bc2e4b161ebf18866db1e`
- 구현 전 Projects 기준 커밋: `06634e9ebf985258f8f1c48aac3b6c6ff1853d7a`
- Workshop 소비자 브랜치: `feat/digest-api-consumers`
- Workshop 기준 HEAD: `3c86c23eaf95637ee7813560858543928e1a6a6f`
- 외부 게시·PR·merge·release는 범위 밖이다.

## Projects 기준선

| 확인 항목 | 기준 결과 |
|---|---|
| 작업 트리 | `feat/digest-api` clean, HEAD `fb03e1841fe7c3985c5bc2e4b161ebf18866db1e`; 부모가 구현 기준 `06634e9ebf985258f8f1c48aac3b6c6ff1853d7a` |
| JDK / Gradle / Kotlin | Java `25.0.5.0.3`; Wrapper Gradle `9.8.2`; Kotlin `2.4.10` |
| Tink compile | `./gradlew :bluetape4k-tink:classes` — BUILD SUCCESSFUL |
| 기존 회귀 테스트 | `./gradlew :bluetape4k-tink:test --tests 'io.bluetape4k.tink.digest.TinkDigesterTest'` — 60 tests, failures/errors/skipped 0 |
| ABI 증거 | `/private/tmp/issue-1917-tink-public-before.txt` |

`TinkDigester` 기존 descriptor는 다음과 같다.

| 멤버 | JVM descriptor |
|---|---|
| 생성자 | `(Ljava/lang/String;)V` |
| `getAlgorithmName()` | `()Ljava/lang/String;` |
| `digest(byte[])` | `([B)[B` |
| `digest(String)` | `(Ljava/lang/String;)Ljava/lang/String;` |
| `digestHex(String)` | `(Ljava/lang/String;)Ljava/lang/String;` |
| `matches(byte[], byte[])` | `([B[B)Z` |
| `matches(String, String)` | `(Ljava/lang/String;Ljava/lang/String;)Z` |
| `matchesHex(String, String)` | `(Ljava/lang/String;Ljava/lang/String;)Z` |
| `toString()` | `()Ljava/lang/String;` |

JDK `MessageDigest.getInstance` 기준 `abc` known vector와 provider는 다음과 같다. 기존 `TinkDigester` 소스도 각 계산에서 이 JDK factory를 직접 호출한다.

| 알고리즘 | provider 이름 / class | `abc` lowercase hex |
|---|---|---|
| MD5 | `SUN` / `sun.security.provider.Sun` | `900150983cd24fb0d6963f7d28e17f72` |
| SHA-1 | `SUN` / `sun.security.provider.Sun` | `a9993e364706816aba3e25717850c26c9cd0d89d` |
| SHA-256 | `SUN` / `sun.security.provider.Sun` | `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad` |
| SHA-384 | `SUN` / `sun.security.provider.Sun` | `cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7` |
| SHA-512 | `SUN` / `sun.security.provider.Sun` | `ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f` |

지원하지 않는 이름은 `MessageDigest.getInstance("issue-1917-unsupported")` 호출 시점에 `NoSuchAlgorithmException`을 던진다. 현재 `TinkDigester` 생성자는 이름을 저장할 뿐 factory를 호출하지 않는다.

## 편집 전 경로 journal

아래 Projects 경로는 승인된 계획에 따라 수정 후보로 사전 등록한다. 실제로 수정하지 않는 경로는 완료 때 `미사용`으로 표시한다. Workshop의 29개 대상 경로는 각 모듈의 기준 상태와 parity table을 기록한 뒤, 해당 저장소 소스 수정 전에 별도 표에 등록한다.

| 경로 | 용도 | 상태 |
|---|---|---|
| `docs/review/2026-11-24-issue-1917-digest-migration-review.md` | 기준선·journal·Workshop parity evidence | 작성 완료; 최종 리뷰 evidence 추가 중 |
| `docs/superpowers/plans/2026-11-24-issue-1917-shared-digester-plan.md` | Java URL-safe Base64 기대값 정정 | 수정됨; SHA-256 `9d2f51014d7ee51f008b7b26af2f3b3bf2b26abba0b64a39a9327d5578a83582` |
| `docs/superpowers/reviews/2026-11-24-issue-1917-plan-review.md` | 정정된 계획 해시와 추가 발견 재검토 | 수정됨; 호출자 재검토 PASS, P0/P1 없음 |
| `io/io/src/test/kotlin/io/bluetape4k/io/digest/DigesterTest.kt` | 공통 API known vectors, matcher, 공유 인스턴스 계약 | 작성됨; API 부재 compile RED |
| `io/io/src/test/kotlin/io/bluetape4k/io/digest/DigestSessionTest.kt` | 증분 입력, lifecycle, 범위, 격리 계약 | 작성됨; API 부재 compile RED |
| `io/io/src/test/kotlin/io/bluetape4k/io/digest/Base64CodecTest.kt` | 네 Base64 형식과 canonical 입력 계약 | 작성됨; API 부재 compile RED |
| `io/io/src/test/java/io/bluetape4k/io/digest/DigestersJavaContractTest.java` | Java 호출 및 기본/명시 overload 계약 | 작성됨; API 부재 compile RED |
| `io/io/src/test/kotlin/io/bluetape4k/io/digest/DigestersReadmeExampleTest.kt` | 양 README 예제의 실행 및 코드 본문 일치 | 구현됨; 1/1 GREEN, imports와 함수 본문 일치 |
| `io/io/src/main/kotlin/io/bluetape4k/io/digest/Digester.kt` | 공통 인터페이스 | 구현됨; 최종 API regression 19개(17 Kotlin, 2 Java) GREEN |
| `io/io/src/main/kotlin/io/bluetape4k/io/digest/DigestSession.kt` | 세션 인터페이스와 구현 | 구현됨; 세션 lifecycle 및 동시 격리 테스트 GREEN |
| `io/io/src/main/kotlin/io/bluetape4k/io/digest/JdkDigester.kt` | JDK 구현 | 구현됨; provider 오류 테스트 GREEN |
| `io/io/src/main/kotlin/io/bluetape4k/io/digest/Digesters.kt` | `Digesters.SHA256` 기본 접근점 | 구현됨; Java `@JvmField` fixture GREEN |
| `io/io/src/main/kotlin/io/bluetape4k/io/digest/Base64Codec.kt` | strict Base64 codec | 구현됨; 네 형식과 canonical 입력 테스트 GREEN |
| `io/io/src/main/kotlin/io/bluetape4k/io/digest/Base64Format.kt` | Base64 alphabet/padding enum | 구현됨; 네 형식 테스트 GREEN |
| `io/io/build.gradle.kts` | 역방향 Tink 의존 제거, digest-only benchmark config | 구현됨; dependencyInsight 및 task inventory PASS |
| `io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigester.kt` | 공통 adapter와 기존 API 보존 | 구현됨; 63개 테스트 GREEN, 기존 Base64 허용범위 확인 |
| `io/tink/build.gradle.kts` | `io` 방향 공개 의존성 | 구현됨; dependencyInsight에서 방향 검증 PASS |
| `io/tink/src/test/kotlin/io/bluetape4k/tink/digest/TinkDigesterTest.kt` | adapter 회귀·호환성 검증 | TDD RED 뒤 63/63 GREEN |
| `io/io/README.md` | 영문 API 예제 | 구현됨; 실제 API sample 포함 |
| `io/io/README.ko.md` | 한글 API 예제 | 구현됨; 실제 API sample 포함 |
| `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/DigestBenchmark.kt` | 반복/증분 경로 벤치마크 | 구현됨; compile 및 12개 조합 GREEN |
| `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/README.md` | 재현 명령과 측정 결과 | 구현됨; 환경, payload hashes, 점수와 전체 raw JSON 포함 |
| `docs/lessons/2026-11-24-issue-1917-digesters.md` | 반복 방지 교훈 | 작성 및 SPW-01~05 검토 완료 |

## Workshop 워크트리 기준

Type E setup run `20261123T223301Z-696d02e7`로 task worktree를 다시 생성하고 completion check를 통과했다. 앞서 완료로 기록된 `20261123T211835Z-20a5cb8f`에 해당하는 worktree가 실제로 없었던 상태를 발견해 복구했다. canonical Workshop `develop`은 clean HEAD `3c86c23eaf95637ee7813560858543928e1a6a6f`를 유지한다. `feat/digest-api-consumers` worktree도 같은 HEAD에서 clean하게 시작했고 다른 worktree는 보존했다.

Type A consumer run `20261123T223904Z-3e7fbae5`가 활성화됐다. `digest-api-consumers` component에는 `baseline`, `migration`, `dependency-selection`, `sequential-tests`, `rollback`, `seven-tier-review`, `lesson`이 필수 check로 등록됐다. 수정 전 source/build/doc write target 39개에 대한 mutation check가 통과했다. 새 테스트 경로가 필요해지면 추가 전에 별도 mutation check를 한다.

### Workshop pre-migration manifest

기준 HEAD는 아홉 모듈 모두 `3c86c23eaf95637ee7813560858543928e1a6a6f`이며, task worktree `git status --short --untracked-files=all`은 비어 있었다. 아래 task-owned source 번호는 이 계획의 [Workshop migration path appendix](../superpowers/plans/2026-11-24-issue-1917-shared-digester-plan.md)와 대응한다. 8개 모듈의 `build.gradle.kts`도 task-owned 경로다. `pre-generated-voucher-pool`은 이미 `libs.bluetape4k.io`를 선언해 build file을 수정하지 않는다.

| 모듈 경로 | Gradle project | 기준 HEAD | 기준 상태 경로 | task-owned 경로 | Rollback result |
|---|---|---|---|---|---|
| `commerce/concert-ticket-flash-sale` | `:commerce-concert-ticket-flash-sale` | `3c86c23eaf95637ee7813560858543928e1a6a6f` | clean | plan appendix 1–4, `build.gradle.kts` | 변경 전, 적용 대상 없음 |
| `commerce/event-sourced-promotion-voucher-campaign` | `:commerce-event-sourced-promotion-voucher-campaign` | `3c86c23eaf95637ee7813560858543928e1a6a6f` | clean | plan appendix 5–7, `build.gradle.kts`, `EventContractTest.kt` | 변경 전, 적용 대상 없음 |
| `commerce/order-lifecycle-fulfillment` | `:commerce-order-lifecycle-fulfillment` | `3c86c23eaf95637ee7813560858543928e1a6a6f` | clean | plan appendix 8, `build.gradle.kts` | 변경 전, 적용 대상 없음 |
| `commerce/pre-generated-voucher-pool` | `:commerce-pre-generated-voucher-pool` | `3c86c23eaf95637ee7813560858543928e1a6a6f` | clean | plan appendix 9–16, `VoucherPoolRedisSignalsTest.kt`; existing `libs.bluetape4k.io` 유지 | 변경 전, 적용 대상 없음 |
| `commerce/promotion-voucher-campaign` | `:commerce-promotion-voucher-campaign` | `3c86c23eaf95637ee7813560858543928e1a6a6f` | clean | plan appendix 17–21, `build.gradle.kts` | 변경 전, 적용 대상 없음 |
| `commerce/usage-metering-billing-event-sourcing` | `:commerce-usage-metering-billing-event-sourcing` | `3c86c23eaf95637ee7813560858543928e1a6a6f` | clean | plan appendix 22–26, `build.gradle.kts`, `CanonicalEventHashTest.kt`, `CorrectionReconciliationIntegrationTest.kt`, `ProjectionWorkerDigestTest.kt` | 변경 전, 적용 대상 없음 |
| `commerce/usage-metering-billing-ledger` | `:commerce-usage-metering-billing-ledger` | `3c86c23eaf95637ee7813560858543928e1a6a6f` | clean | plan appendix 27, `build.gradle.kts` | 변경 전, 적용 대상 없음 |
| `commerce/shared` | `:commerce-shared` | `3c86c23eaf95637ee7813560858543928e1a6a6f` | clean | plan appendix 28, `build.gradle.kts`, `DigestMigrationParityTest.kt` | 변경 전, 적용 대상 없음 |
| `commerce/reservation-control-plane` | `:commerce-reservation-control-plane` | `3c86c23eaf95637ee7813560858543928e1a6a6f` | clean | plan appendix 29, `build.gradle.kts` | 변경 전, 적용 대상 없음 |

`gradle/libs.versions.toml`의 `bluetape4k-io` alias는 `io.github.bluetape4k:bluetape4k-io`를 가리키고 직접 버전은 지정하지 않는다. Workshop은 `bluetape4k-dependencies` BOM `2.1.0`으로 이 alias 버전을 관리한다. 변경 전 `:commerce-shared:dependencyInsight`에는 해당 dependency가 없었다.

### 35개 digest 호출 기준 parity

아래 `pre-migration`은 기존 JDK SHA-256 호출에 입력 framing과 결과 변환을 적용한 값이다. 재계산 벡터는 별도 명시가 없으면 UTF-8 또는 원시 bytes를 그대로 넣었고, 복합 필드는 각 행에 적은 순서와 separator를 사용했다. 기존 golden은 그대로 기록했다. `post-migration` 열에는 새 `Digesters.SHA256` 호출과 결정적 fixture에서 확인한 실제 결과를 기록했으며, 35개 행 모두 이전 결과 및 표현과 일치한다. 신규 parity fixture는 같은 입력 bytes로 JDK `MessageDigest` oracle과 새 API를 함께 계산해 출력 형식까지 고정한다.

| ID | 파일 / 함수 | 입력과 기존 framing | 결과 표현 | deterministic test / oracle | pre-migration bytes/value | post-migration |
|---:|---|---|---|---|---|---|
| 1 | `TicketMigrationRunner.read` | `V001__concert_ticket_flash_sale.sql` 원시 bytes (11,334 bytes) | 소문자 hex | `TicketMigrationRunnerTest`; SQL 고정 byte vector | `bc482be8992260ed4526213dae53147cd16661504e0836151cd604cfe8220dc6` | `bc482be8992260ed4526213dae53147cd16661504e0836151cd604cfe8220dc6` |
| 2 | ticket `IdempotencyFingerprint.sha256` | `ticket-idempotency-request-v1\nPOST\n/purchase\ngrade=GENERAL\nquantity=2` UTF-8 | URL-safe unpadded Base64 | `PurchaseServiceIntegrationTest`; HTTP 요청 고정 fixture | `b8d11f5970f163b8a71c6f9af085a68e570471b0e4f82714916ad42191c7b28c` → `uNEfWXDxY7inHG-a8IWmjlcEcbDk-CcUkWrUIZHHsow` | `b8d11f5970f163b8a71c6f9af085a68e570471b0e4f82714916ad42191c7b28c` → `uNEfWXDxY7inHG-a8IWmjlcEcbDk-CcUkWrUIZHHsow` |
| 3 | `PurchaseService.effectDigest` | `ticket-effect-v1:00000000-0000-0000-0000-000000000001:issued` UTF-8 | raw digest bytes | `TicketEffectIntegrationTest`; fixed operation ID vector | `996fa58d80db2ac2180c7694f92e35cce4b6ff5617d856efb1a69eb2b302cf6d` | `996fa58d80db2ac2180c7694f92e35cce4b6ff5617d856efb1a69eb2b302cf6d` |
| 4 | `PurchaseService.guardAdvisoryKey` | `ticket-active-guard-v1\0<saleId=...0002>\0USER\0<subjectId=...0003>` UTF-8 | digest 앞 8 bytes big-endian signed `Long` | `PurchaseServiceIntegrationTest`; fixed sale/subject UUID vector | digest `d44003f9d74575d31e4a6ea921b5679ebbddd7b7409daacfb15b6f2a6d857ca8` → `-3152515367565953581` | digest `d44003f9d74575d31e4a6ea921b5679ebbddd7b7409daacfb15b6f2a6d857ca8` → `-3152515367565953581` |
| 5 | `RefundService.apply` | `refund-effect-v1:00000000-0000-0000-0000-000000000004` UTF-8 | raw digest bytes | `RefundRaceIntegrationTest`; fixed operation ID vector | `36f83ce7dd6be2945e7c00c7c7720ed364e152dc48f352c041bf65a523eaea5e` | `36f83ce7dd6be2945e7c00c7c7720ed364e152dc48f352c041bf65a523eaea5e` |
| 6 | `EventContract.canonicalChecksum` | 고정 envelope의 순서 있는 15개 field를 `\|`로 join: event ID, tenant, stream type/id/version, global position, event type/schema, occurred/recorded time, correlation, empty causation, actor, HMAC key version, canonical JSON | 소문자 hex | `EventContractTest`; 고정 UUID/time/payload fixture (기존 assertion은 self-equality라 별도 golden 추가 대상) | `b8e8da6a87c77c533bc5663e6396e0ebebaa18df8778acf3fc34f7c7186ecdfe` | `b8e8da6a87c77c533bc5663e6396e0ebebaa18df8778acf3fc34f7c7186ecdfe` |
| 7 | `EventSourcedIdempotency.ReceiptDigest.sha256` | `projection-generation` UTF-8 | lowercase hex `ReceiptDigest` | `EventSourcedIdempotencyContractTest` | `ba9d4a603bb5fbaea56c72f5e64d46f0471471ed92d8bea2ce261bbdb7812530` | `ba9d4a603bb5fbaea56c72f5e64d46f0471471ed92d8bea2ce261bbdb7812530` |
| 8 | `EventSnapshots` digest | `state-250` UTF-8 | 소문자 hex | `EventSnapshotRepositoryIntegrationTest`; fixed canonical state | `5376bdeb268fd4f8f2faec1130b635fb20b553b529ec99ac50bd783ee4eabc2f` | `5376bdeb268fd4f8f2faec1130b635fb20b553b529ec99ac50bd783ee4eabc2f` |
| 9 | order `IdempotencyFingerprint.request` | sorted lines `sku-a=1\nsku-b=2` UTF-8 | 소문자 hex | `IdempotencyFingerprintTest`; sorted item fixture | `2996a469ea9235cc36630802dd50014a025959206060d751eac4d55d43373996` | `2996a469ea9235cc36630802dd50014a025959206060d751eac4d55d43373996` |
| 10 | `VoucherPoolRedisSignalKeyFactory.admissionKey` | `field(domain) || field(RESERVE) || field(01 02 03)`; each field is `Int32BE(length) || bytes` | `v1:` + lowercase hex | 신규 `VoucherPoolRedisSignalsTest`; 고정 namespace/principal vector | `v1:7c0a94daefea195252543926ebd649f5d66da41517774f9b6c11c19aae16a81c` | `v1:7c0a94daefea195252543926ebd649f5d66da41517774f9b6c11c19aae16a81c` |
| 11 | `CampaignBatchCommandService.initialCheckpoint` | `voucher-pool-batch-final-checkpoint-v1 || 32×01 || Int64BE(1)` | raw `DigestValue` bytes | `DigestMigrationParityTest`; 명시적 checkpoint vector | `b47b6ce47012921f5f56e6d2832f664abf03fc4ce42805cb0b677d0d5a34c272` | `b47b6ce47012921f5f56e6d2832f664abf03fc4ce42805cb0b677d0d5a34c272` |
| 12 | `CampaignBatchCommandService.advanceCheckpoint` | `voucher-pool-batch-checkpoint-step-v1 || 32×01 || Int64BE(1) || 32×02` | raw `DigestValue` bytes | `CampaignBatchCommandServiceTest` / parity vector | `6e37655c2e1953870a7aeef188b2af64c4aeae34b5bf7d4034ee556913279586` | `6e37655c2e1953870a7aeef188b2af64c4aeae34b5bf7d4034ee556913279586` |
| 13 | `PreparedChunk.requestDigest` | `voucher-pool-batch-chunk-fingerprint-v1 || Int64BE(first=0) || Int64BE(source=0) || stableDigest(32×01)`; rejection 없음 | 소문자 hex | `CampaignBatchCommandServiceTest` / parity vector | `230b8a400441dff728447a25095201d8a9dd5e3dc5e6e884a6a31171a96a349f` | `230b8a400441dff728447a25095201d8a9dd5e3dc5e6e884a6a31171a96a349f` |
| 14 | `VoucherPoolRevocationService.previewTokenDigest` | `single-use-preview-token` UTF-8 | 소문자 hex | `VoucherPoolRevocationServiceTest`; token fixture | `1a329ac5336b9bf46302c8c2e3d79685cedaea81034d5683ab17c6d09159fb26` | `1a329ac5336b9bf46302c8c2e3d79685cedaea81034d5683ab17c6d09159fb26` |
| 15 | `VoucherPoolRevocationService.impactDigest` | `BATCH\|00000000-0000-0000-0000-000000000001\|2\|AVAILABLE=2\|RESERVED=0\|ALLOCATED=0\|REDEEMED=0\|RELEASED=0\|REVOKED=0\|EXPIRED=0\|2\|0\|0\|0\|2` UTF-8 | raw digest bytes | `OperatorVoucherPoolWebIntegrationTest`; fixed impact-count vector | `0dd14b9a99b173a47e9b876ac35c405704341e87763a80157108e0efc228d081` | `0dd14b9a99b173a47e9b876ac35c405704341e87763a80157108e0efc228d081` |
| 16 | `VoucherPoolKeyMaterialConfiguration.isKnownTestFixture` | 32 raw bytes `01..20` | known-fixture membership `true`; digest bytes are zeroized in `finally` | `VoucherPoolKeyMaterialConfigurationTest`; seed 1 fixture | fingerprint `ae216c2ef5247a3782c135efa279a3e4cdc61094270f5d2be58c6204b7a612c9` → `true` | fingerprint `ae216c2ef5247a3782c135efa279a3e4cdc61094270f5d2be58c6204b7a612c9` → `true` |
| 17 | `VoucherPoolMigrationRunner.read` | `V001__voucher_pool.sql` 원시 bytes (31,925 bytes) | lowercase hex checksum | `VoucherPoolMigrationRunnerTest`; SQL fixed byte vector | `8d252f8af2b146918957a2c134c7cfbb4e70b03d90b407d88eb51577cb07f631` | `8d252f8af2b146918957a2c134c7cfbb4e70b03d90b407d88eb51577cb07f631` |
| 18 | `VoucherPoolFingerprint.command` | domain + `reserve`; UTF-8 sorted `campaignId=campaign-1`, `quantity=1`; each field is Int32BE-length-prefixed, with `01` value marker | raw `CommandFingerprint` bytes | `VoucherPoolFingerprintTest`; fixed command/field fixture | `4db577fe1919da3089bc862ac0da862f3d232236f3e8e73a7aadf49ff5cc512c` | `4db577fe1919da3089bc862ac0da862f3d232236f3e8e73a7aadf49ff5cc512c` |
| 19 | `VoucherPoolIdempotencyRepository.sha256` owner token | `voucher-pool-idempotency-owner-v1 || 00 || tokenBytes(01..20)` | raw digest bytes | `VoucherPoolIdempotencyRepositoryTest`; fixed token-byte vector | `f67431e5339987bdc4420247d123b3a8d9dd6221628c8f88db0f548d9e105666` | `f67431e5339987bdc4420247d123b3a8d9dd6221628c8f88db0f548d9e105666` |
| 20 | `OperatorAccessFilter` remote digest | `127.0.0.1` UTF-8 | raw digest bytes | `OperatorAccessFilterIntegrationTest`; fixed remote address | `12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0` | `12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0` |
| 21 | `OperatorAccessFilter` secret digest | `test-operator-secret-0000000000000001` UTF-8 (`null` maps to empty UTF-8) | raw digest bytes | `OperatorAccessFilterIntegrationTest`; fixed secret fixture | `52ce602bd3afc05fc4ee9b846279ef005a73572ed19c692626b550e6f4a03788` | `52ce602bd3afc05fc4ee9b846279ef005a73572ed19c692626b550e6f4a03788` |
| 22 | `AllocationService.voucherUserDigest` | `voucher-user-v1\0tenant-a\0user-1` UTF-8 | lowercase hex | `AllocationServiceTest`; fixed tenant/user fixture | `cdbba4bb482789b128121bb44c75e24940f7a8b0a39a7f1a9303e3fce4c215e3` | `cdbba4bb482789b128121bb44c75e24940f7a8b0a39a7f1a9303e3fce4c215e3` |
| 23 | `VoucherMigrationRunner.read` | `V001__voucher_campaign.sql` 원시 bytes (8,675 bytes) | lowercase hex checksum | `VoucherMigrationRunnerTest`; SQL fixed byte vector | `d981d87195ce50f45f4a0ad24c394b9e4ad89f48a4ee31b1947faf9cc04f26c6` | `d981d87195ce50f45f4a0ad24c394b9e4ad89f48a4ee31b1947faf9cc04f26c6` |
| 24 | promotion `IdempotencyFingerprint` | fixed `POST /api/v1/campaigns/c-1/vouchers`, resource `c-1`, canonical JSON `{"amount":1.00,"userRef":"u1"}` plus schema defaults | URL-safe unpadded Base64 | `IdempotencyFingerprintTest`; existing golden | `bstRVOnLhqvLrUzLUwckQDSUtGTyEnowlYcsQPXSzds` | `bstRVOnLhqvLrUzLUwckQDSUtGTyEnowlYcsQPXSzds` |
| 25 | `EventInboxRepository.lockAggregate` | `tenant-a:CAMPAIGN:018f1f2e-3d4c-7b6a-8f90-1234567890ab` UTF-8 | digest 앞 8 bytes big-endian signed `Long` | `VoucherRepositoryTest`; fixed inbox record | digest `496fe945d9e2fc8d2fca64d743debda291d9bb423dc027cf0c512fbd5ecb5109` → `5291704573401169037` | digest `496fe945d9e2fc8d2fca64d743debda291d9bb423dc027cf0c512fbd5ecb5109` → `5291704573401169037` |
| 26 | `VoucherCodeService.checksum` | `V7-2JNj1cRbsKnoHUCJNpSTTm` UTF-8 | 첫 두 digest bytes mod `58²`, Base58 two-char checksum | `VoucherCodeServiceTest`; full code golden `V7-2JNj1cRbsKnoHUCJNpSTTmjR` | digest `b470d84d2da2395dbd791181c41c112fedbd46dc2f3b257a2787377406f6b561` → `jR` | digest `b470d84d2da2395dbd791181c41c112fedbd46dc2f3b257a2787377406f6b561` → `jR` |
| 27 | `BillingCommandServices.deterministicUsageId` | `gateway\0atomic` UTF-8 | 소문자 hex | `CommandReceiptPostgresIntegrationTest`; fixed source fixture | `c2858ddbeef57e375d7055f1566af39da4cbd89cdd2f7cb7aaa1a8ea1cffb31e` | `c2858ddbeef57e375d7055f1566af39da4cbd89cdd2f7cb7aaa1a8ea1cffb31e` |
| 28 | `CorrectionReconciliationServices.sha256` | `tenant-a\|billing\|1\|2\|3.00\|4.0000` UTF-8; generation 1/checkpoint 2, expected 3.00/actual 4.0000 | lowercase hex | `CorrectionReconciliationIntegrationTest`; reset fixture, two events in global positions 1 and 2 | `a84870c4bc34776e9fed0820d725ce7d191c93a6ae1a77b71e1ca3274ba10fc2` | `a84870c4bc34776e9fed0820d725ce7d191c93a6ae1a77b71e1ca3274ba10fc2` |
| 29 | `CanonicalEventHash.sha256` | fixed event fixture: tenant-a / Usage / usage-1 / version 1 / usage.accepted / schema 1 / payload `{"quantity":10}` / metadata `{"actor":"tenant-a"}` / previous hash null | lowercase hex | `CanonicalEventHashTest`; canonical event fixture | `8169d82413c0355319a0a42f41adc49f392309da1ab94c263d5cb956e479f2eb` | `8169d82413c0355319a0a42f41adc49f392309da1ab94c263d5cb956e479f2eb` |
| 30 | event-sourcing `CommandFingerprint.request` | `meter-register` plus sorted `code=api_calls`, `unit=request` canonical newline fields | lowercase hex | `CommandFingerprintTest`; field-order fixture | `97f0ceb0cc416fd32f1f42a52e560eb608b40e69d33f431bbbb8b2d38586918a` | `97f0ceb0cc416fd32f1f42a52e560eb608b40e69d33f431bbbb8b2d38586918a` |
| 31 | `ProjectionWorker.failureDigest` | `java.lang.IllegalStateException:fixture failure` UTF-8 | lowercase hex | 신규 `ProjectionWorkerDigestTest`; fixed exception fixture | `70e1afa66135205173e0b2c8a9a36a766c1e13fd0e18c1d09ed68a7aca172bb6` | `70e1afa66135205173e0b2c8a9a36a766c1e13fd0e18c1d09ed68a7aca172bb6` |
| 32 | ledger `CommandFingerprint.request` | sorted canonical fields `occurredAt=2026-07-01T00:00:00Z\nquantity=1.0` with `usage-ingest` domain framing | lowercase hex | `CommandFingerprintTest`; canonical decimal/instant fixture | `6b870763e8ffab7b842299c8fb30b5bca277ba454fac235b97dd6104b0ff8402` | `6b870763e8ffab7b842299c8fb30b5bca277ba454fac235b97dd6104b0ff8402` |
| 33 | `PayloadDigest.of` | `""`; `한글과 emoji🙂`; `{"tenantId":"tenant-a","quantity":"2.5"}` UTF-8 | lowercase hex | `CommerceIntegrationContractsTest`; independent test-only JDK oracle | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`; `00133dcfa86f0acf5806b269eeaca01efb734495640938f9303bb094436fd3cf`; `0c028c2f2f4d771a5beaab4398a07067b0eec849c11d91345b462d11b5777e27` | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`; `00133dcfa86f0acf5806b269eeaca01efb734495640938f9303bb094436fd3cf`; `0c028c2f2f4d771a5beaab4398a07067b0eec849c11d91345b462d11b5777e27` |
| 34 | `PayloadDigest.matches` | payload `payload` UTF-8; compare expected hex to lowercase, uppercase, malformed, truncated, and different values | boolean | `CommerceIntegrationContractsTest`; expected digest and rejection cases | hash `239f59ed55e737c77147cf55ad0c1b030b6d7ee748a7426952f9b852d5a935e5`; matches: `true`, `false`, `false`, `false`, `false` | hash `239f59ed55e737c77147cf55ad0c1b030b6d7ee748a7426952f9b852d5a935e5`; matches: `true`, `false`, `false`, `false`, `false` |
| 35 | reservation `IdempotencyFingerprint` | existing ASCII/Unicode key and request vectors; NUL/domain framing preserved | lowercase hex | `IdempotencyFingerprintTest`; four existing goldens | `c8c08a6aaf042cca74e5079aff177fbb55b4b93b5c7f00fff0a274f57c46fd00`; `742ef157b884b48ed4570007635cafb4f48a001e60c95714cab7b6a5fbd94ed5`; `d4a37e2e79bf58dcc917efd8a200d290f0e6e9f85d0187f1090f32ccf79f2b1e`; `e12be6d71fd10a584c8235b7b0a33e2aa5f3d1c8455821af836c186b340092fe` | `c8c08a6aaf042cca74e5079aff177fbb55b4b93b5c7f00fff0a274f57c46fd00`; `742ef157b884b48ed4570007635cafb4f48a001e60c95714cab7b6a5fbd94ed5`; `d4a37e2e79bf58dcc917efd8a200d290f0e6e9f85d0187f1090f32ccf79f2b1e`; `e12be6d71fd10a584c8235b7b0a33e2aa5f3d1c8455821af836c186b340092fe` |

기준 벡터의 소스는 결정적 테스트 fixture와 SQL resource bytes다. #28은 fixture reset 후 global positions 1, 2이며 actual 합계가 scale `4.0000`으로 남는다는 integration test 관찰값을 기준으로 고정했다. 먼저 사용한 `4.00` 예상값은 실패한 assertion의 원인이어서 폐기했다. #6의 기존 assertion은 checksum을 자기 자신과 비교하므로 고정값으로 교체했다. #10과 #31은 등록된 신규 fixture에서 기존 JDK 계산을 실행한다. #29의 provisional hash `561612...`도 canonical test fixture와 불일치해 폐기했고, targeted baseline test가 반환한 `8169d824...`를 고정했다. key-material의 임시 digest zeroization, voucher code checksum 변환, constant-time verifier 비교는 별도 보안/API 검토 항목이다.

#### 신규 baseline parity fixtures

| 확인 | 실행과 결과 | 실패·재시도 분류 |
|---|---|---|
| #10 admission key | `:commerce-pre-generated-voucher-pool:test --tests '*VoucherPoolRedisSignalsTest'` — 1/1 PASS | 없음; 기존 JDK 구현의 key output 고정 |
| #11–13 checkpoint/chunk framing | `:commerce-shared:test --tests '*DigestMigrationParityTest'` — 1/1 PASS, 세 JDK vector 일치 | 없음; source 호출 순서에서 유래한 byte vectors 고정 |
| #31 quarantine failure digest | `:commerce-usage-metering-billing-event-sourcing:test --tests '*ProjectionWorkerDigestTest'` — 1/1 PASS | 첫 compile은 test의 잘못된 imports (`ProjectionEventApplier`, MockK DSL imports)로 실패; 같은 fixture에서 imports 정리 후 compile/test 1/1 PASS. production 코드 변경 전 실패이며 test assertion failure는 없었음 |
| #6 event contract checksum | `:commerce-event-sourced-promotion-voucher-campaign:test --tests '*EventContractTest'` — 4/4 PASS | 없음; 기존 self-equality assertion을 고정 checksum으로 강화 |
| #29 canonical event hash | `:commerce-usage-metering-billing-event-sourcing:test --tests '*CanonicalEventHashTest'` — 2/2 PASS | provisional `561612...`은 첫 targeted run에서 실제 `8169d824...`와 달라 실패. canonical fixed fixture를 확인해 expected 값을 수정한 뒤 2/2 PASS; 이전 provisional 값은 폐기 |
| #28 reconciliation digest | 일반 `test`에서는 integration tag 제외로 0 tests였음. `:commerce-usage-metering-billing-event-sourcing:integrationTest --tests '*CorrectionReconciliationIntegrationTest.projection corruption*'` — 1/1 PASS | 첫 integration assertion에서 예상 `4.00`과 실제 `4.0000`의 scale 차이 확인. material과 expected hash를 actual `4.0000`에 맞춘 뒤 통합 테스트 1/1 PASS |

위 targeted baseline은 모두 Workshop task worktree에서 순차 실행했다. 첫 `ProjectionWorkerDigestTest` compile 실패, 잘못된 #29 provisional value, #28 scale mismatch는 test-only 증거 수집 단계에서 발견·교정했다. 별도의 test task 0 tests는 tagged integration을 실행하지 않았으므로 pass로 세지 않았다.

| 모듈 | 기준 테스트 결과 |
|---|---|
| `:commerce-concert-ticket-flash-sale:test` | 97 tests, BUILD SUCCESSFUL |
| `:commerce-event-sourced-promotion-voucher-campaign:test` | 65 tests, BUILD SUCCESSFUL |
| `:commerce-order-lifecycle-fulfillment:test` | 46 tests, BUILD SUCCESSFUL |
| `:commerce-pre-generated-voucher-pool:test` | 373 tests, BUILD SUCCESSFUL; Testcontainers 포함 |
| `:commerce-promotion-voucher-campaign:test` | 189 tests, BUILD SUCCESSFUL |
| `:commerce-usage-metering-billing-event-sourcing:test` | 20 tests, BUILD SUCCESSFUL |
| `:commerce-usage-metering-billing-ledger:test` | 24 tests, BUILD SUCCESSFUL |
| `:commerce-shared:test` | 6 tests, BUILD SUCCESSFUL |
| `:commerce-reservation-control-plane:test` | 60 tests, BUILD SUCCESSFUL |

총 880 tests가 순차 실행됐으며 test failure/error/skipped는 없었다. Gradle 9.8.2의 기존 deprecated-features 경고는 출력됐다. 각 모듈 테스트는 `context-mode` shell runner에서 task별로 하나씩 실행했다.

## 구현 및 이전 결과

### TDD RED

- 명령: `./gradlew :bluetape4k-io:test --tests 'io.bluetape4k.io.digest.*' --console=plain`
- 결과: 새 API가 없는 기준 커밋에서 Kotlin 테스트 컴파일이 `Digesters`, `JdkDigester`, `Base64Codec`, `Base64Format` 미정의로 실패했다. 나머지 진단은 이 타입 부재에서 이어진 추론 오류이며 별도의 문법/테스트 의존성 오류는 확인되지 않았다. 테스트는 실행되지 않았다.
- 원시 출력: `/private/tmp/issue-1917-digest-api-red.log`

### Tink adapter TDD RED 준비

- `TinkDigesterTest.kt`에 기존 `TinkDigester` API와 다섯 preset의 golden vector, URL-safe unpadded legacy matcher 동작, 공통 `Digester` 경로 및 unsupported algorithm 예외 계약을 추가했다.
- 구현과 `io/tink -> io/io` 의존성을 추가하기 전 실행 결과를 아래에 기록한다.

### Tink adapter TDD RED

- 명령: `./gradlew :bluetape4k-tink:test --tests 'io.bluetape4k.tink.digest.TinkDigesterTest' --console=plain`
- 결과: 예상한 compile RED. 테스트가 아직 `io.bluetape4k-io`에 의존하지 않아 공통 API imports 및 새 interface 멤버가 unresolved였다. 별도 의존성 다운로드나 기존 API의 컴파일 오류는 없었다.
- 원시 출력: `/private/tmp/issue-1917-tink-adapter-red.log`

### 공용 API GREEN

- 명령: `./gradlew :bluetape4k-io:test --tests 'io.bluetape4k.io.digest.*' --console=plain`
- 초기 구현 검증 결과: BUILD SUCCESSFUL; 18개 테스트 통과. Kotlin digest/matcher, session lifecycle·범위 예외·동시 격리, 네 Base64 형식/canonical 거부, Java field와 overload 계약을 확인했다. 최종 API 재검증은 아래 Step 6-R 기록에 별도로 적었다.
- 원시 출력: `/private/tmp/issue-1917-digest-api-green.log`

### Tink adapter GREEN 및 ABI

- TDD RED: 새 test import와 공통 멤버가 없는 dependency baseline에서 unresolved로 실패했다. 원시 출력은 `/private/tmp/issue-1917-tink-adapter-red.log`다.
- 처음 adapter 컴파일에서 `matches(ByteArray, ByteArray)`의 `override` 누락이 검출되어 수정했다. 이후 `./gradlew :bluetape4k-tink:test --tests 'io.bluetape4k.tink.digest.TinkDigesterTest' --console=plain` 및 `--tests 'io.bluetape4k.tink.digest.*'`가 각각 63개 테스트 GREEN으로 끝났다. 마지막 원시 출력은 `/private/tmp/issue-1917-tink-adapter-all.log`다.
- 기존 URL-safe Base64 matcher가 unpadded 기대값을 허용하는 호환 동작도 테스트했다. 공통 `matchesBase64`는 설계대로 canonical 형식만 수용한다.
- ABI: `javap -public -s` baseline/after는 각각 `/private/tmp/issue-1917-tink-public-before.txt`, `/private/tmp/issue-1917-tink-public-after.txt`. 비교 결과 기존 TinkDigester public symbol 10개 유지, 허용한 신규 method descriptor 13개 정확히 추가, `TinkDigesters` public 표면 동일이다.
- 의존성: `/private/tmp/issue-1917-io-no-tink-dependency.txt`에서 `:bluetape4k-io:compileClasspath`에 `bluetape4k-tink` 미포함; `/private/tmp/issue-1917-tink-io-dependency.txt`에서 `:bluetape4k-tink:compileClasspath`가 `project :bluetape4k-io`를 선택했다.

### README 및 benchmark 검증

- README fixture는 먼저 marker 선택 로직의 테스트 결함을 고친 뒤, 문서 marker가 없는 상태에서 의도된 assertion RED를 확인했다. 두 README에 import와 같은 compileable 함수 예제를 추가한 뒤 `./gradlew :bluetape4k-io:test --tests 'io.bluetape4k.io.digest.DigestersReadmeExampleTest' --console=plain`이 1/1 GREEN으로 통과했다. fixture는 샘플 함수를 실제 호출하고 양 문서의 code body가 동일한지 확인한다.
- `./gradlew :bluetape4k-io:tasks --all`에서 `testBenchmarkCompile`, `testBenchmark` 및 새 `testDigestBenchmark` task를 확인했다. 기본 JMH configuration이 여러 기존 workload를 포함하므로 `io/io/build.gradle.kts`에 전용 `digest` configuration을 등록해 네 API 경로만 필터링했다. `advanced("jvmForks", "definedByJmh")`는 plugin 기본 1 fork가 `@Fork(2)`를 덮어쓰지 않도록 한다.
- 첫 실행 `./gradlew :bluetape4k-io:testDigestBenchmark --console=plain`은 task 성공이었지만 JSON에 `forks=1`, `Cnt=5`를 기록해 승인된 2-fork 조건에 미달했다. 결과를 사용하지 않았다. 플러그인 0.4.17 source에서 runner 기본 `jvmForks=1`을 확인하고 설정을 고친 뒤 재실행했다.
- 최종 compile: `./gradlew :bluetape4k-io:testBenchmarkCompile --console=plain` — BUILD SUCCESSFUL.
- 최종 run: `./gradlew -Didea.active=false :bluetape4k-io:testDigestBenchmark --console=plain` — BUILD SUCCESSFUL, 12개 조합 모두 `forks=2`, 각 fork 5회 측정. 원본 JMH JSON report 전체는 benchmark README에 삽입했다. summary는 README의 12행 표에 있고 payload별 SHA-256, JDK/Gradle/Kotlin/JMH, OS/kernel/CPU, JVM arguments, timed region을 기록했다.
- JMH 1.37이 JDK 25에서 `sun.misc.Unsafe.objectFieldOffset` terminal-deprecation warning을 출력했다. benchmark task 실패나 digest source 오류는 없었다.

이 리뷰는 README/benchmark 결과와 35개 호출 parity, module별 기준 manifest, dependency substitution, 테스트 및 최종 리뷰 증거를 순서대로 기록한다. 결과 바이트·문자열·wire 값이 기준과 달라지는 경우 해당 모듈 이전을 중단한다.

## 최종 이전 검증

### 35개 논리 호출의 post-migration 결과

35개 표 행의 마지막 열에는 이전 열과 동일한 실제 결과를 기록했다. 결과 표현도 기존 계약과 일치한다. 테스트 증거는 각 행의 결정적 fixture와 호출자 테스트이며, 신규 parity fixture는 새 API와 JDK 기준 구현을 같은 입력 bytes로 비교한다. 특히 #10 admission key, #11–13 checkpoint/chunk framing, #28 correction reconciliation, #29 canonical event hash, #31 projection failure digest, #33–34 payload digest/matcher는 신규 또는 강화된 결정적 검증으로 고정했다. #6 event contract도 자기 비교 assertion을 고정 checksum으로 바꿨다.

### Workshop 모듈별 검증

Projects composite build 안의 local module은 dependencyInsight 결과가 모두 project ':feat-digest-api:bluetape4k-io'를 선택했다. 호출한 configuration은 compileClasspath다.

| 순서 | Gradle project | compileKotlin | dependencyInsight | test 결과 |
|---:|---|---|---|---|
| 1 | :commerce-concert-ticket-flash-sale | 성공 | local Projects bluetape4k-io 선택 | 97 tests, failures/errors/skipped 0 |
| 2 | :commerce-event-sourced-promotion-voucher-campaign | 성공 | local Projects bluetape4k-io 선택 | 65 tests, failures/errors/skipped 0 |
| 3 | :commerce-order-lifecycle-fulfillment | 성공 | local Projects bluetape4k-io 선택 | 46 tests, failures/errors/skipped 0 |
| 4 | :commerce-pre-generated-voucher-pool | 성공 | local Projects bluetape4k-io 선택 | 374 tests, failures/errors/skipped 0 |
| 5 | :commerce-promotion-voucher-campaign | 성공 | local Projects bluetape4k-io 선택 | 189 tests, failures/errors/skipped 0 |
| 6 | :commerce-usage-metering-billing-event-sourcing | 성공 | local Projects bluetape4k-io 선택 | 21 tests, failures/errors/skipped 0 |
| 7 | :commerce-usage-metering-billing-ledger | 성공 | local Projects bluetape4k-io 선택 | 24 tests, failures/errors/skipped 0 |
| 8 | :commerce-shared | 성공 | local Projects bluetape4k-io 선택 | 7 tests, failures/errors/skipped 0 |
| 9 | :commerce-reservation-control-plane | 성공 | local Projects bluetape4k-io 선택 | 60 tests, failures/errors/skipped 0 |

아홉 모듈의 일반 test task는 한 번에 하나씩 실행해 총 883개 테스트를 통과했다. XML 결과에서도 failures=0, errors=0, skipped=0이다. #28 전용 검증은 일반 test가 아니라 :commerce-usage-metering-billing-event-sourcing:integrationTest --tests '*CorrectionReconciliationIntegrationTest'로 따로 실행했으며 2개 테스트가 통과했다. 이 결과도 failures/errors/skipped가 모두 0이다. 다른 모듈의 전체 integrationTest suite는 실행하지 않았다.

Projects 쪽 검증은 앞 절의 결과를 다시 확인했다. 공용 API 최종 재검증은 Kotlin 17개와 Java 계약 2개, Tink adapter는 63개 테스트가 통과했다. README 실행 fixture, benchmark compile, JMH 12개 조합도 통과했고 각 조합은 forks=2, fork당 5회 측정했다. ABI 비교에서는 기존 TinkDigester public symbol 10개를 보존했고 승인한 신규 descriptor 13개만 추가했다. TinkDigesters public 표면은 기준과 동일하다.

### Composite build의 BOM 해결과 재시도

첫 composite compile은 Kotlin compilation에 들어가기 전에 실패했다. Gradle이 included Projects BOM을 project ':feat-digest-api:bluetape4k-bom'으로 치환했고 Spring Dependency Management Plugin의 mavenBom resolver가 필요한 POM을 찾지 못해 “Could not find bluetape4k-bom.pom”을 출력했다. detached configuration에서 global substitution을 끄는 첫 실험도 Spring plugin의 resolver를 제어하지 못했다.

재시도에서는 저장소 밖의 임시 init script /private/tmp/issue-1917-use-published-bom-with-composite.init.gradle만 사용했다. 해당 invocation에서 included Projects의 bluetape4k-bom project group을 org.codex.temporary로 바꿔 BOM 치환을 제외했다. Workshop은 공개 BOM 2.1.0의 POM을 읽고, bluetape4k-io는 계속 local included project로 치환됐다. 아홉 모듈 모두 이 구성에서 compileKotlin, dependencyInsight, test를 통과했다. source, settings, catalog는 변경하지 않았고 artifact를 publishToMavenLocal로 게시하지 않았다.

원인과 제한은 upstream 문서/이슈와 일치한다. [Gradle composite substitution](https://docs.gradle.org/current/userguide/composite_builds.html#included_builds), [Gradle substitution limitations](https://docs.gradle.org/current/userguide/composite_builds.html#cases_where_included_build_substitutions_wont_work), [Spring mavenBom import](https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/#dependency-management-configuration-importing-a-maven-bom), [upstream issue #272](https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/272#issuecomment-600027601). Spring plugin requirements는 Gradle 6.x, 7.x, 8.x를 문서화하지만 이 저장소는 Gradle 9.8.2를 사용한다. 이번 해결은 검증 invocation에 한정했으며 plugin/toolchain 호환성은 별도 위험으로 남긴다.

### 최종 7-tier 검토

| 검토 관점 | 근거와 판정 |
|---|---|
| Source와 경계 | 29 production files 및 8 build files를 이전했다. pre-generated-voucher-pool build file은 이미 io alias를 사용해 수정하지 않았다. quality reviewer의 정적 재검토에서 parity fixture가 실제 Digesters API를 호출하지 않는 점과 reservation의 미사용 Tink dependency를 발견해 수정한 뒤 APPROVE를 받았다. |
| 실패·재시도·skip된 테스트 | 사전 기준선은 880 tests, 최종은 883 tests다. 이전 표에 기록한 테스트-only import 오류, provisional #29 hash, #28 decimal scale, 첫 benchmark fork 설정, 첫 composite BOM compile 실패는 각각 수정하고 재검증했다. 최종 일반 테스트 883개와 대상 통합 테스트 2개에는 failure, error, skipped가 없다. 예상된 TDD compile RED는 실행 오류가 아니라 API 부재를 확인한 단계로 구분했다. |
| CI와 toolchain | JDK 25.0.5.0.3, Gradle 9.8.2, Kotlin 2.4.10에서 로컬 검증했다. PR을 만들지 않았으므로 hosted CI는 실행하지 않았고 결과를 로컬 통과와 분리한다. Gradle 10 호환성 deprecation warning과 Spring plugin의 문서 지원범위 초과를 기록한다. |
| API와 ABI | Projects API/Java fixture가 통과했다. TinkDigester 기존 descriptor 10개와 TinkDigesters 표면을 보존했고, 허용된 신규 descriptor 13개만 추가했다. README 예제 fixture 1/1, benchmark compile 및 12개 benchmark 조합도 통과했다. |
| lifecycle과 security | session 독립성 및 범위 예외 계약을 API 테스트로 검증했다. 사용자 digest는 HMAC/Tink-MAC 경로와 분리했다. constant-time MessageDigest.isEqual, key zeroization, Base58 checksum, byte framing, cancellation 처리는 독립 spec/quality 재검토에서 보존을 확인했다. |
| migration, dependency, rollback | 모든 module의 compileClasspath가 local io project를 선택했다. 9개 모듈 테스트가 차례로 통과했고 task-owned migration 외 catalog/settings 변화는 없다. 실패 후 source rollback은 필요하지 않았다. 운영 rollback 절차는 plan에 기록한 상태로 유지한다. |
| issue/history와 외부 경계 | 작업 범위는 issue #1917과 승인된 Digesters 설계/계획이다. 사용자 정정에 따라 이름을 기존 설계와 구현에서 Digesters로 통일했다. PR, push, issue close, merge, tag, publication/release는 실행하지 않았다. |

spec reviewer의 최종 재검토는 PASS였다. quality reviewer의 재검토는 두 수정사항 반영 후 APPROVE였으며, 현재 P0=0, P1=0이다. 정적 리뷰 결과와 위 로컬 테스트 증거를 분리해 보존한다.

### 초기 Step 6-R 6관점 리뷰 기록 (이전 기준)

Projects `feat/digest-api`의 리뷰 기준 HEAD는 `9122e244669f1667551d4964eb2867a62e547521`, Workshop `feat/digest-api-consumers`의 기준 HEAD는 `3c86c23eaf95637ee7813560858543928e1a6a6f`다. Workshop 변경은 production source 29개, Gradle 8개, test 6개, README 4개로 총 47개 경로다. 두 브랜치의 현재 변경 diff를 11개 모듈 슬라이스별로 성능, 안정성/lifecycle, 보안, 운영, 개발자/API, 사용자/호출자 관점에서 독립 검토했다. 표의 각 값은 `P0/P1/P2/P3` 건수다.

아래 표는 해당 과거 기준의 검토 이력이다. 현재 Projects 코드 후보 `f2cc3759bba728a688b1bb7ef1cb615b6650d05e`의 사전 PR 검토 결과는 다음 절에 별도로 기록하며, 아래의 `9122...`/`3c86...` 값으로 현재 코드나 Workshop 상태를 판정하지 않는다.

| 모듈 슬라이스 | 성능 | 안정성 | 보안 | 운영 | 개발자/API | 사용자/호출자 | 통합 |
|---|---:|---:|---:|---:|---:|---:|---:|
| Projects `bluetape4k-io` | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 |
| Projects `bluetape4k-tink` | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 |
| Workshop `commerce-concert-ticket-flash-sale` | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 |
| Workshop `commerce-event-sourced-promotion-voucher-campaign` | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 |
| Workshop `commerce-order-lifecycle-fulfillment` | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 |
| Workshop `commerce-pre-generated-voucher-pool` | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 |
| Workshop `commerce-promotion-voucher-campaign` | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 |
| Workshop `commerce-usage-metering-billing-event-sourcing` | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 |
| Workshop `commerce-usage-metering-billing-ledger` | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 |
| Workshop `commerce-shared` | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 |
| Workshop `commerce-reservation-control-plane` | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 |

첫 개발자/API 검토에서 Projects Kotlin 테스트의 JUnit assertion 사용과 수동 동시성 harness에 P2 세 건이 보고됐다. 테스트를 `io.bluetape4k.assertions` matcher와 `assertFailsWith`로 전환하고 `MultithreadingTester`를 사용했다. 재검토는 세 항목 종결과 새 finding 없음으로 승인했다. 변경 후 `:bluetape4k-io:test`의 digest Kotlin 테스트 17개, `DigestersJavaContractTest` 2개, `:bluetape4k-tink:test --tests 'io.bluetape4k.tink.digest.TinkDigesterTest'` 63개가 순차 통과했다.

안정성 reviewer는 동시성 harness 변경 이후 `DigestSessionTest`를 다시 검토했다. `MultithreadingTester().workers(8).rounds(8)`가 독립 session을 총 64회 실행하고 누락·중복 없이 완료하는지 확인했으며, reset/reuse·범위 예외 후 재사용·입력 배열 변경 계약도 유지됐다. 재검토 결과 P0=0, P1=0, P2=0, P3=0이다.

독립 성능 리뷰는 기존 직접 JDK 경로 대비 관찰 가능한 회귀를 찾지 못했다. 측정은 단일 스레드 JMH 12개 조합이며 할당량과 contention을 프로파일링하지 않았으므로 이를 동시 처리 성능 개선 주장으로 해석하지 않는다. 보안 리뷰는 HMAC/Tink-MAC, constant-time 비교, key zeroization, framing을 유지했다고 판정했다. 운영 리뷰에서 관측성·health/readiness·배포 설정 변경은 없었다. 호출자 리뷰는 README 예제, Base64 형식, 기존 Tink matcher 호환성에 추가 finding이 없음을 확인했다.

통합 결과는 11개 슬라이스 전부 P0=0, P1=0, P2=0, P3=0이다. P2 유예 항목은 없다. GitHub CI와 Workshop 전체 `integrationTest` matrix는 실행하지 않았으며, 일반 test 및 지정 대상 통합 테스트 증거와 구분한다. composite BOM resolver workaround 및 Gradle/Spring plugin 호환성 경계는 위 절의 위험으로 유지한다.

## Projects 사전 PR 재검토 (2026-11-24)

### 범위와 검증 기준

- Projects 브랜치 `feat/digest-api`, 구현 코드 HEAD `f2cc3759bba728a688b1bb7ef1cb615b6650d05e`, 비교 기준 `905b9efa1ac164ff33c8f935112c7c63bc2feebe`.
- 대상은 Projects `io/io`, `io/tink`, benchmark 기록과 이슈 #1917 승인 설계·계획의 추적성이다. Workshop 구현과 기존 소비자 parity 결과는 위 과거 절의 저장소별 증거로 분리한다.
- GitHub 이슈 #1917은 `OPEN`, milestone `2.2.0`, assignee `debop`이며 label은 `enhancement`, `test`, `design`, `infra/io`다. 이 재검토 시점에는 feature remote ref와 Projects PR이 없었다.
- 로컬 검증: `:bluetape4k-tink:test --tests io.bluetape4k.tink.digest.TinkDigesterTest` — 63 tests, failures=0, errors=0, skipped=0. 공개 API 검증은 Kotlin 17개와 Java 2개, ABI 비교는 기존 TinkDigester symbol 10개 보존 및 승인된 13개 추가를 기록한다.
- Workshop 9개 모듈은 앞 절의 composite 검증에서 순차 실행해 883개 일반 테스트와 대상 `CorrectionReconciliationIntegrationTest` 2개가 통과했다. 전체 Workshop integrationTest matrix는 실행하지 않았다.

### 정확한 HEAD에서 확인한 리뷰와 조치

| 관점 | exact HEAD 결과 | 근거와 조치 |
|---|---|---|
| 개발자/API | P0/P1/P2/P3 = 0/0/0/0 | 독립 재검토에서 Kotlin/Java 공개 API, 기존 Tink ABI, Java fixture와 두 README 예제를 확인했다. |
| 안정성 | P0/P1/P2/P3 = 0/0/0/0 | 독립 세션, reset/reuse, 범위 오류 후 재사용, 동시 격리 및 provider 실패 순서를 확인했다. |
| 보안 | P0/P1/P2/P3 = 0/0/0/0 | malformed Base64 경계, constant-time 비교, 일반 digest와 MAC/signature 분리 및 key-material 부재를 확인했다. |
| 호출자 | P0/P1/P2/P3 = 0/0/0/0 | 35행 migration parity, README·Java 예제, legacy URL-safe unpadded 입력과 malformed 입력 계약을 확인했다. |
| 성능 | P0/P1/P2/P3 = 0/0/0/0 | benchmark source 및 raw JSON 일치, 12개 조합의 fork·measurement 설정과 Tink matcher 경로를 확인했다. |
| 운영 | 초기 exact-head 재검토에서 P1=1 | 구현·Gradle 경계에는 이슈가 없었다. 이전 review 문서와 workflow receipt가 오래된 HEAD를 현재 근거처럼 보이게 한 점을 지적했다. 아래 조치와 새 receipt를 반영한 뒤 운영 관점 재검토를 수행한다. |

기존 matcher는 malformed Base64를 decode한 뒤 `false`를 반환하느라 provider 계산을 생략할 수 있었다. 이는 지원하지 않는 알고리즘의 `NoSuchAlgorithmException`을 가렸다. `TinkDigester.matches(String, String)`이 digest를 먼저 계산하도록 순서를 바로잡았고, 지원하지 않는 알고리즘과 malformed Base64를 함께 전달하는 회귀 테스트를 추가했다. 지원되는 알고리즘의 malformed 입력은 계속 `false`를 반환하며 기존 URL-safe unpadded decode 허용 범위를 유지한다. 수정과 테스트는 코드 커밋 `f2cc3759bba728a688b1bb7ef1cb615b6650d05e`에 있다.

성능·운영 초기 리뷰는 benchmark README가 `DigestBenchmark.kt`가 존재하지 않는 `9122e244...`를 실행 HEAD로 적은 P1을 찾았다. benchmark를 `ac67509466d88f348725be9dcf45eb9a4cb2d369`에서 다시 실행하고 README를 그 실행 커밋 및 source SHA-256 `61796235ba9c9549...`로 고쳤다. 커밋에는 benchmark source가 존재하며 source hash가 일치한다. README의 내장 JSON은 raw report `io/io/build/reports/benchmarks/digest/2026-11-24T14.49.54.414591/test.json`과 구조·수치가 동일하고, 12개 조합 각각 `forks=2`, 1 thread, warmup 2회, 측정 5회다. 성능 리뷰는 이 증거를 exact branch에서 독립 확인했다.

운영 리뷰가 지적한 이전 기준 문서 표시는 이 갱신에서 과거 기록으로 명확히 구분했다. 기존 workflow receipt는 당시 구현 단계 기록으로 보존하며 PR delivery 증거로 사용하지 않는다. 불완전한 구현 단계 receipt는 그 범위와 미완료 gate를 남긴 채 종료하고, PR 진행은 현재 branch/head를 명시한 별도 delivery receipt로 추적한다. 새 receipt에는 현재 exact-head 리뷰를 결속하며, 운영 관점 재검토 결과가 PASS가 될 때까지 PR 생성을 보류한다.

### 통합 판단

현재 코드 후보에서 P0=0, P1=1, P2=0, P3=0이다. 남은 P1은 코드 동작이 아니라 이전 리뷰 문서와 receipt의 기준 표시가 최신 코드 HEAD에 맞지 않던 문제다. 이 문서는 과거 리뷰를 이력으로 구분하고 새 Projects 검토 기준을 추가했다. 기존 구현 receipt를 PR 근거로 승격하지 않고 구현 단계 기록으로 종료한 다음, 현재 HEAD용 PR delivery receipt를 생성하고 운영 리뷰를 다시 수행한다. 이 순서가 끝날 때까지 PR 생성은 pending이다.

### 아직 수행하지 않은 외부 검증

PR은 생성 전이므로 hosted CI, GitHub review/thread read-back, mergeability, merge, canonical `develop` SNAPSHOT publication은 아직 실행하지 않았다. branch는 당시 `origin/develop`보다 2 commits 뒤였으므로 PR CI가 최신 base와의 merge result를 검증해야 한다. Workshop은 Projects artifact와 중앙 Dependencies BOM을 공개 SNAPSHOT에서 해석하기 전까지 게시 artifact를 사용하지 않는다.

## 문서와 실행 경계

이 리뷰 기록은 기준 설계와 승인 계획을 수정하지 않고 실제 구현·검증 결과만 추가한다. 변경 경로는 Projects 및 Workshop task worktree에 한정했다. PR 생성은 사용자가 승인했지만, 정확한 리뷰 근거와 delivery receipt 갱신 전까지 보류한다. 전체 Workshop integrationTest matrix와 GitHub CI는 아직 실행하지 않았으며 로컬 통과로 간주하지 않는다.


## SPW 문서 검증

| artifact | SPW-01 근거 고정 | SPW-02 문서 계약 | SPW-03 한국어·용어 | SPW-04 기술 추적성 | SPW-05 최종 확인 |
|---|---|---|---|---|---|
| 이 리뷰 기록 | #1917, 승인 계획, 설계, source diff, 모듈 테스트 XML, ABI 결과, 독립 재검토를 근거로 고정 | 과거 6-R 기록과 현재 Projects exact-head addendum를 분리하고 finding·조치·외부 검증 대기를 포함 | KO-01~KO-07 수동 점검, `Digesters`와 기존 `TinkDigesters` 구분; term audit 2개 파일, findings=0 (clinic-appointment 규칙만 적용) | 35행 parity, 9개 모듈, API/ABI, f2cc3759 code review 결과와 연결 | 본문·표 read-back PASS; P1과 필요한 receipt/운영 후속 절차를 분명히 기록 |
| 이슈 #1917 lesson | 다음 Kotlin 작업자를 독자로 정하고 사용자 정정, benchmark provenance, provider 오류 순서의 근거를 고정 | 맥락·결정·결과·놓친 점·재발 방지 guard를 포함 | KO-01~KO-07 수동 점검, 설계·계획·구현에서 `Digesters` 이름 대조; term audit 2개 파일, findings=0 (clinic-appointment 규칙만 적용) | 설계·계획·검토 기록과 성공/실패 명령 결과에 연결 | 최종 파일을 다시 읽고 diff whitespace를 확인한다 |

기존 문서 본문과 lesson은 SPW-01부터 SPW-05까지 PASS였다. 이번 갱신에서도 두 파일을 다시 읽고 SPW-01~05를 통과했다. term audit는 2개 파일에서 findings=0을 반환했지만 설치된 규칙 집합은 `clinic-appointment` 전용이므로 프로젝트 용어 계약 검증으로 계산하지 않았다. 설계·계획·구현·양 README에서 승인 이름 `Digesters`와 기존 `TinkDigesters` API를 직접 대조했다. 문서 작성 DoD는 통과했으며, workflow의 기존 P1과 PR 전 운영 재검토는 별도 pending이다.
