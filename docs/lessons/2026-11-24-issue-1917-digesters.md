# 이슈 #1917: 공통 digest API 이름과 소비자 이전

## 맥락

공통 SHA-256 기능을 Tink adapter보다 아래 계층인 bluetape4k-io에서 제공하려는 작업이다. 초안의 이름을 DigestUtils로만 바꾸는 요청으로 잘못 축소하지 않고, 사용자의 정정에 따라 기존 설계 문서에 Digesters를 반영한 뒤 이슈 #1917의 승인 범위를 구현했다.

## 결정

- 새 접근점은 io.bluetape4k.io.digest.Digesters.SHA256으로 통일하고, 미배포 API에 이전 이름 별칭을 추가하지 않는다.
- io/tink는 기존 TinkDigester와 TinkDigesters 표면을 유지하며 공통 Digester adapter로 동작한다.
- Workshop 이전은 기존 framing, sorting, separator, byte order, digest 출력 형식과 MAC 경계를 보존한다.
- composite 검증에서 Spring mavenBom이 로컬 Gradle BOM project 치환으로 실패하면 source/build 설정을 넓히기 전에 임시 init script로 BOM 치환만 제외한다. local io project 치환은 계속 검증한다.
- 일반 module test와 tagged integrationTest를 같은 증거로 취급하지 않는다. 변경한 integration fixture는 대상 task를 직접 실행한다.

## 결과와 증거

최종 재검증에서 README 실행 fixture를 포함한 Projects API Kotlin 테스트 17개와 Java 계약 테스트 2개, Tink adapter 63개가 통과했다. ABI 비교는 기존 TinkDigester descriptor 10개를 보존하고 승인된 descriptor 13개만 추가했으며 TinkDigesters는 동일했다. Workshop 9개 모듈을 순차 검증해 883개 일반 테스트와 대상 integrationTest 2개가 모두 통과했다. 모든 module dependencyInsight는 local Projects bluetape4k-io를 선택했다. benchmark는 12개 조합을 forks=2, fork당 5회 실행했다.

## 놓친 점과 다음 작업자용 가드

1. 사용자가 목표를 바로잡았는데도 작업을 이름 치환으로만 처리하면 설계·이슈 범위를 놓친다. 구현 전 요청, 이슈, 기존 설계를 함께 읽고 이름 변경이 반영된 승인 계획을 다시 확인한다. 사용자 정정은 계획과 기록에 반영한다.
2. parity test가 기존 JDK oracle만 계산하면 새 API를 실제로 검증하지 않는다. 같은 ordered bytes를 JDK 기준 구현과 새 Digesters API 양쪽에서 계산하고 hard-coded golden과 서로의 결과를 비교한다.
3. Spring Dependency Management Plugin의 mavenBom resolver와 Gradle composite substitution은 local Java-platform project를 POM으로 읽을 수 없다. 먼저 published BOM 소비와 local library substitution을 분리하고, 승인 범위를 넘는 platform DSL 변경이나 local publish를 임의로 추가하지 않는다.
4. 일반 test task는 tagged integration test를 실행하지 않을 수 있다. 실제 integrationTest XML 결과가 0 tests인지 확인하고, 영향받은 class를 명시적으로 필터링해 실행한다.
5. 다중 모듈 hash migration은 각 모듈에서 compileKotlin, dependencyInsight, test를 순서대로 완료한 다음 다음 모듈로 이동한다. 오류가 나면 해당 모듈을 중단하고 framing, 결과 표현, security-sensitive path를 확인한다.
6. 보안 계약은 단순 unkeyed digest 호출로 바꾸지 않는다. HMAC/Tink-MAC, MessageDigest.isEqual, key zeroization, Base58 checksum, coroutine cancellation과 기존 byte framing을 별도 검토한다.
7. 신규 Kotlin 테스트는 `io.bluetape4k.assertions`의 목적별 matcher와 `assertFailsWith`를 사용하고, 동시성 검증은 저장소의 `MultithreadingTester`를 우선한다. Step 6-R에서 발견된 assertion/harness P2를 최종 리뷰 뒤 테스트 관례에 맞춰 수정하고 해당 모듈 테스트와 API 리뷰를 다시 통과시켰다.

전체 검증 근거와 실패·재시도 구분은 docs/review/2026-11-24-issue-1917-digest-migration-review.md에 기록했다. GitHub CI는 PR을 만들지 않아 실행하지 않았다.
