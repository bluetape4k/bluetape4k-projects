# 이슈 #1917 공통 digest 이전 검토 기록

## 범위와 기준 정보

- Projects 작업 브랜치: `feat/digest-api`
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
| `docs/review/2026-11-24-issue-1917-digest-migration-review.md` | 기준선·journal·Workshop parity evidence | 기준선 기록; 이후 증거 추가 예정 |
| `docs/superpowers/plans/2026-11-24-issue-1917-shared-digester-plan.md` | Java URL-safe Base64 기대값 정정 | 수정됨; SHA-256 `9d2f51014d7ee51f008b7b26af2f3b3bf2b26abba0b64a39a9327d5578a83582` |
| `docs/superpowers/reviews/2026-11-24-issue-1917-plan-review.md` | 정정된 계획 해시와 추가 발견 재검토 | 수정됨; 호출자 재검토 PASS, P0/P1 없음 |
| `io/io/src/test/kotlin/io/bluetape4k/io/digest/DigesterTest.kt` | 공통 API known vectors, matcher, 공유 인스턴스 계약 | 등록됨; 미편집 |
| `io/io/src/test/kotlin/io/bluetape4k/io/digest/DigestSessionTest.kt` | 증분 입력, lifecycle, 범위, 격리 계약 | 등록됨; 미편집 |
| `io/io/src/test/kotlin/io/bluetape4k/io/digest/Base64CodecTest.kt` | 네 Base64 형식과 canonical 입력 계약 | 등록됨; 미편집 |
| `io/io/src/test/java/io/bluetape4k/io/digest/DigestersJavaContractTest.java` | Java 호출 및 기본/명시 overload 계약 | 등록됨; 미편집 |
| `io/io/src/main/kotlin/io/bluetape4k/io/digest/Digester.kt` | 공통 인터페이스 | 등록됨; 미편집 |
| `io/io/src/main/kotlin/io/bluetape4k/io/digest/DigestSession.kt` | 세션 인터페이스와 구현 | 등록됨; 미편집 |
| `io/io/src/main/kotlin/io/bluetape4k/io/digest/JdkDigester.kt` | JDK 구현 | 등록됨; 미편집 |
| `io/io/src/main/kotlin/io/bluetape4k/io/digest/Digesters.kt` | `Digesters.SHA256` 기본 접근점 | 등록됨; 미편집 |
| `io/io/src/main/kotlin/io/bluetape4k/io/digest/Base64Codec.kt` | strict Base64 codec | 등록됨; 미편집 |
| `io/io/src/main/kotlin/io/bluetape4k/io/digest/Base64Format.kt` | Base64 alphabet/padding enum | 등록됨; 미편집 |
| `io/io/build.gradle.kts` | 역방향 Tink 의존 제거 | 등록됨; 미편집 |
| `io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigester.kt` | 공통 adapter와 기존 API 보존 | 등록됨; 미편집 |
| `io/tink/build.gradle.kts` | `io` 방향 공개 의존성 | 등록됨; 미편집 |
| `io/tink/src/test/kotlin/io/bluetape4k/tink/digest/TinkDigesterTest.kt` | adapter 회귀·호환성 검증 | 등록됨; 미편집 |
| `io/io/README.md` | 영문 API 예제 | 등록됨; 미편집 |
| `io/io/README.ko.md` | 한글 API 예제 | 등록됨; 미편집 |
| `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/DigestBenchmark.kt` | 반복/증분 경로 벤치마크 | 등록됨; 미편집 |
| `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/README.md` | 재현 명령과 측정 결과 | 등록됨; 미편집 |
| `docs/lessons/2026-11-24-issue-1917-digesters.md` | 반복 방지 교훈 (적용 필요성은 최종 diff 검토 뒤 결정) | 후보 등록; 미편집 |

## Workshop 워크트리 기준

Type E setup run `20261123T211835Z-20a5cb8f`는 완료됐다. canonical Workshop `develop`은 clean HEAD `3c86c23eaf95637ee7813560858543928e1a6a6f`를 유지한다. 새 `feat/digest-api-consumers` worktree가 같은 HEAD에서 생성됐고 `git status --short --branch`는 clean이다. 이전에 존재하던 worktree 두 개도 보존됐다.

## 구현 및 이전 결과

이 절은 RED/GREEN 결과와 35개 호출 parity, module별 기준 manifest, dependency substitution, 테스트 및 최종 리뷰 증거를 각 단계에서 추가한다. 결과 바이트·문자열·wire 값이 기준과 달라지는 경우 해당 모듈 이전을 중단한다.
