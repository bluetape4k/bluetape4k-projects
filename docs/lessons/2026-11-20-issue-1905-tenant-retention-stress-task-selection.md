# tenant retention stress 전용 task의 테스트 선택 계약 교훈

## 배경

PR [#1904](https://github.com/bluetape4k/bluetape4k-projects/pull/1904)의
merge commit `b0e270adec9e964c5604e455923f8b8d222819ad`에서
`TenantContextRetentionStressTest`의 `@Tag("tenant-retention-stress")`가
제거됐다. `bluetape4k/tenant/build.gradle.kts`의 기본 `test` task는 이 tag를
제외하고, `tenantRetentionStress` task는 이 tag만 포함하므로, 전용 task가
실행할 테스트를 잃었다.

그러나 Gradle `Test` task의 기본 no-match 동작은 선택된 테스트가 0개여도
성공으로 끝날 수 있다. 그 결과 nightly/CI가 120초 retention stress 검증을
실행하지 않고도 성공했다. 이 회귀는 [이슈 #1905](https://github.com/bluetape4k/bluetape4k-projects/issues/1905)에
기록했다.

## 결정

- stress class에 `@Tag("tenant-retention-stress")`를 복원해 JUnit 선택 계약을
  원래대로 되돌린다.
- `tenantRetentionStress` 전용 `Test` task에
  `filter { isFailOnNoMatchingTests = true }`를 설정해 선택 결과가 0개면
  빌드를 실패시킨다.
- class의 tag가 다시 제거되는 회귀를 조기에 잡도록
  `TenantRetentionStressConfigurationTest`에서 annotation 값을 검증한다.
- 기본 `test`와 전용 stress task의 실행 범위는 분리한다. stress 검증을 기본
  suite에 넣어 timeout·GC 비용을 상시 부담하지 않는다.

## 결과

전용 task가 실제 `TenantContextRetentionStressTest` 2개를 실행하고, 선택
결과가 비어 있으면 성공으로 남지 않도록 계약을 고정했다. 기본 tenant test는
stress tag를 계속 제외하며 모듈 회귀 테스트를 실행한다. 공개 API와 ABI는
변경하지 않았다.

## 검증

- pre-fix RED: `TenantRetentionStressConfigurationTest`가 tag 제거 상태에서
  `Expected <null> to equal to "tenant-retention-stress"`로 실패했다.
- post-fix GREEN: configuration regression test 1개 통과.
- `./gradlew :bluetape4k-tenant:tenantRetentionStress --no-configuration-cache --no-daemon`:
  2개 테스트 통과.
- no-match guard 회귀 확인:
  `--tests 'io.bluetape4k.tenant.NoSuchTenantRetentionStressTest'` 실행이
  `0 passing` 뒤 `No tests found for given includes`로 실패했다.
- `./gradlew :bluetape4k-tenant:test --no-configuration-cache --no-daemon`:
  18개 테스트 통과, 실패·오류·skip 없음.
- `./gradlew :bluetape4k-tenant:detekt --no-configuration-cache --no-daemon`:
  `BUILD SUCCESSFUL`. `TenantContextRetentionStressTest.kt:130`의
  `ExplicitGarbageCollectionCall` 진단은 base에도 있던 기존 코드이며 이번
  변경으로 추가되지 않았다.
- `git diff --check` 통과.

## 놓친 점과 주의사항

Gradle task가 `BUILD SUCCESSFUL`이라는 사실만으로 필수 테스트가 실행됐다고
판단할 수 없다. tag·include/exclude filter를 사용하는 전용 task는 선택 결과와
no-match 정책을 함께 확인해야 한다. hosted CI가 새 head에서 실제 테스트 수와
retention report를 생성하는지는 PR 검증에서 다시 확인한다.

## 향후 지침

- 새 JUnit tag 전용 Gradle `Test` task에는 `isFailOnNoMatchingTests = true`를
  기본으로 둔다.
- tag 또는 Gradle include/exclude 규칙을 변경하면 annotation 계약 테스트와
  전용 task 실행 결과를 같은 변경에서 갱신한다.
- CI/nightly read-back에서는 `BUILD SUCCESSFUL`뿐 아니라 선택된 테스트 수와
  필수 report 산출 여부를 확인한다.

## 문서 SPW 감사

- SPW-01: PASS — 독자는 Gradle/JUnit task와 CI를 유지보수하는 개발자이며,
  근거는 #1905, PR #1904 merge commit, 현재 build script와 테스트다.
- SPW-02: PASS — 배경, 결정, 결과, 검증, 놓친 점, 향후 지침을 기록했다.
- SPW-03: PASS — 한국어 기술 문장을 사용하고 명령·식별자·정확한 오류를
  보존했다. KO-01~KO-05 자연스러움 검사를 완료했다.
- SPW-04: PASS — tag 선택, no-match 정책, 실행 수, detekt 진단을 현재 소스와
  테스트 출력에 대조했다.
- SPW-05: PASS — 최종 Markdown을 다시 읽었고, SPW-01~SPW-05와 KO-01~KO-07
  결과를 기록했다.
