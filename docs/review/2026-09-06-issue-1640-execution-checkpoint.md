# #1640 실행 결과와 선행 정렬 경계

## 현재 상태

후속 승인 `추가해`로 root Jakarta Persistence 정렬과 전역 검증을 추가했다.
추가 계획·리뷰는 `aed17cf76`에 기록했다. 아래 최초 실패는 과거 실행 증거로 유지한다.
현재 root의 catalog 선택은 v32이며, 같은 fixture의 새 실행은 8개 통과다.
세 Spring Boot 모듈 전체 테스트와 전역 build는 실행했다.
신규 fixture 정리 후 최종 재검증과 전체 영향 판정은 아래 후속 결과로 구분한다.

명세 확인 후 구현 계획과 리뷰를 `294045300`에 커밋했다.
구버전 강제 제거와 신규 fixture는 아직 커밋하지 않았다.
제품 코드·전역 catalog·PR은 변경하지 않았다. root build는 승인 후 한 줄 변경했다.

## 실행 증거

| 추가 승인 후 실행 | 결과 | 로그 |
|---|---|---|
| root 수정 전 같은 fixture 재현 | 8개 실패·오류 0·제외 0, FindOption 누락, 종료코드 1 | /tmp/issue-1640-jakarta-red.log |
| root v32 선택 후 같은 fixture | 8개 통과·실패 0·오류 0·제외 0, 종료코드 0 | /tmp/issue-1640-jakarta-green.log |

GREEN은 JPA 기동·실제 저장/조회·executor 선택과 종료의 현재 8개 계약을 검증한다.
Boot 4.1.0의 upstream 순환 의존 재현을 뜻하지 않는다.

### 전역 영향 조사

- 두 Spring Boot 모듈 × 4개 classpath는 모두 Jakarta Persistence 3.2.0이다.
- QueryDSL 예제의 compile/runtime은 3.1.0, testCompile/testRuntime은 기존 force로 3.2.0이다.
- 전체 12개 dependencies 조회는 종료코드 0이며 unresolved/FAILED 항목은 없다. 조회 성공과 버전 정렬 성공은 다르다.
- 원본 로그는 `/tmp/issue-1640-aligned-<모듈>-<configuration>.log`, 종료코드는 각 `.log.exit`다.
- 대표 POM `spring-boot/hibernate-lettuce/build/publications/Bluetape4k/pom-default.xml`의 Jakarta 관리 버전은 3.2.0이다. 생성 종료코드 0, 로그는 `/tmp/issue-1640-aligned-pom.log`다.
- POM 생성은 성공했지만 configuration cache 저장에서 8개 문제(고유 4개)가 보고되어 cache entry가 폐기됐다. cache 호환성까지 통과했다고 보고하지 않는다.

남은 직접 v31 선언은 `examples/jpa-querydsl-demo/build.gradle.kts:67,74`의
`implementation`과 `kapt`다. root 한 줄 정렬은 이 명시적 선언을 제거하지 않는다.
승인된 추가 계획은 이 예제의 선언을 편집하지 않는 조건이므로 현재 그대로 보존했다.
전역 정렬은 이 잔여 불일치 때문에 PASS가 아니다.

### 순차 모듈 검증

| 모듈 | tests | failures / errors / skipped | 결과 로그 |
|---|---:|---|---|
| core | 250 | 0 / 0 / 0 | /tmp/issue-1640-full-core.log |
| Hibernate Lettuce | 38 | 0 / 0 / 0 | /tmp/issue-1640-full-hibernate-lettuce.log |
| demo | 7 | 0 / 0 / 0 | /tmp/issue-1640-full-hibernate-lettuce-demo.log |

각 명령은 cleanTest·no-build-cache·1800초 상한을 사용했고 종료코드 0이다.
JUnit XML 49·5·2개를 합산했다. core에는 controller lifecycle 3개,
Hibernate Lettuce에는 신규 JPA 8개가 포함된다.

최초 정적 검사 명령은 demo에 없는 `detekt` task를 요청하여 실패했다.
root `isSampleOrBenchmarkProject`·`detektExclusionReason`·plugin 적용 조건에 따라
demo는 library 분석에서 제외된다. 잘못된 계획 명령을 사용한 오류이며 제품 결함이 아니다.
demo를 제외한 두 library의 detekt와 세 모듈 compileTestKotlin으로 명령을 정정했다.
demo의 detekt만 N/A이며 compile/test 증거는 위와 같이 실제 실행했다.

### 신규 fixture 정리 계획

수정한 정적 검사 명령은 종료코드 0이지만 `ignoreFailures=true` 때문에 지적이 남았다.
신규 fixture의 LongMethod·NestedBlockDepth·ThrowingExceptionFromFinally·MaxLineLength·ThrowsCount
5개 지적은 이 작업에서 수정한다. 기존 core 및 cache 코드 지적은 별도로 남긴다.

1. 이미 통과한 JPA 8개·모듈 38개로 동작을 고정한다.
2. 동일성 비교 줄을 분리하고 실제 저장/조회·Future 검증을 이름 있는 작은 함수로 분리한다.
3. 최초 실패를 보관하고 종료 검증 실패를 suppressed로 연결한 뒤 finally 밖에서 다시 던진다. 실패를 정상값으로 바꾸는 우회는 추가하지 않는다.
4. fixture 8개·Hibernate Lettuce 전체·Detekt를 다시 실행하고 신규 파일 지적이 0인지 XML에서 확인한다.

전역 build가 읽는 파일과 경합하지 않도록 종료를 확인한 뒤 코드 정리를 시작한다.

### 전역 build와 정리 후 검증

- 전체 `build -x test --no-parallel`: 종료코드 0, 2분 33초, 686 tasks(438 executed, 168 from cache, 80 up-to-date).
- 로그: `/tmp/issue-1640-global-build.log`. configuration cache 문제 12개로 entry가 폐기됐다.
- `-x test`는 이름이 다른 전용 task를 제외하지 않는다. 실제 coordinationLockPerformanceTest 등이 실행됐으므로 이 결과를 전체 테스트 통과나 테스트 완전 제외로 표현하지 않는다.
- 신규 fixture의 5개 Detekt 지적을 수정했고 `/tmp/issue-1640-cleanup-green.log`에서 8개 통과·종료코드 0을 확인했다. 신규 파일 지적은 없으며 기존 cache 코드 지적은 6개 남는다.
- 음성 대조: 잘못된 bean 이름은 3개 실패, expectedVirtual 반전은 3개 실패, 닫힌 EMF의 isOpen=true 검사는 8개 실패했다. 모두 종료코드 1이며 각각 `/tmp/issue-1640-negative-name.log`, `-virtual.log`, `-close.log`에 보존했다.
- 음성 대조 변경을 복구한 최종 `/tmp/issue-1640-final-hibernate.log`는 전체 38개 통과·실패/오류/제외 0·종료코드 0이다. Detekt XML에서 신규 fixture 지적 0개, 기존 파일 지적 6개를 확인했다.
- 전역 build는 테스트 함수 정리 전 실행이다. 정리 후에는 해당 모듈 compile·전체 테스트·Detekt를 새로 검증했다. 전체 테스트 및 exact-head Full Nightly는 미실행이다.
- 구현 diff는 root와 두 모듈 build·신규 fixture에 한정되며 아직 커밋하지 않았다. 추가 계획 커밋은 `aed17cf76`이다. PR·push·merge·dispatch·삭제는 수행하지 않았다.

| 실행 | 실제 결과 | 로그 |
|---|---|---|
| 임시 실행기 성공·실패·timeout | 종료코드 0·7·124 확인, timeout process group 50592의 사후 ps 결과 비어 있음 | 현재 세션 도구 출력 |
| 기존 Boot dependencyInsight | testRuntimeClasspath에서 Boot 4.0.3 선택, 구버전 rule 확인 | /tmp/issue-1640-version-red-graph.log |
| 최소 Boot 버전 assertion RED | 1개 실패, 실제 4.0.3과 기대 4.1.1 불일치, 종료코드 1 | /tmp/issue-1640-version-red.log |
| 두 모듈 force 제거 후 같은 assertion GREEN | 1개 통과, 종료코드 0 | /tmp/issue-1640-version-green.log |
| 실제 JPA fixture | compileTestKotlin 성공, 8개 실행·8개 실패·오류 0·제외 0, 종료코드 1 | /tmp/issue-1640-fixture.log |
| 현재 testRuntimeClasspath | Boot 4.1.1, Spring ORM 7.0.9, Hibernate 7.4.7.Final, Jakarta Persistence 3.1.0 | /tmp/issue-1640-classpath-diagnosis.log |
| 현재 runtimeClasspath dependencyInsight | Jakarta Persistence 3.2.0 → 3.1.0, Selected by rule, 종료코드 0 | /tmp/issue-1640-jakarta-runtime.log |

RED/GREEN의 임시 버전 assertion은 최종 fixture로 교체했다.
이 결과는 Boot 버전 불일치를 검출·해소한 증거이며 upstream 순환 의존을 직접 재현한 증거가 아니다.

## 원인

첫 JPA 실패는 `NoClassDefFoundError: jakarta/persistence/FindOption`이다.
나머지 사례는 같은 초기화 실패 이후의 `HibernateJpaDialect` 초기화 오류다.
`BeanCurrentlyInCreationException` 회귀 재현으로 분류하지 않는다.

- `build.gradle.kts:791`은 모든 하위 프로젝트의 dependencyManagement에서 `rootBt4k.jakarta.persistence.v31`을 등록한다.
- 실제 catalog ref `9698c9d66bea6fcba373143ee8fa5bfbd9812d4b`에는 v31=3.1.0, v32=3.2.0이 모두 있다.
- Boot BOM과 Hibernate가 요구하는 3.2.0이 runtimeClasspath에서도 3.1.0으로 낮아진다. 테스트 전용 문제로 한정되지 않는다.
- 로컬 공식 artifact의 `jar tf` 비교에서 3.1.0에는 FindOption.class가 없고 3.2.0에는 있다.

이전 테스트 force는 Jakarta 3.2.0을 강제하여 이 전역 불일치를 가렸다.
버전 상수나 새 test-only force를 추가하면 production runtime 불일치는 남는다.
따라서 이번 테스트에서만 우회하지 않고 root의 기존 catalog alias 선택을 정렬하는 선행 변경을 제안한다.
최초 실행에서는 승인 명세가 제외한 범위여서 중단했다.
이후 사용자가 추가를 승인했으므로 기존 catalog v32 정렬을 적용했다.

## 보존 상태와 다음 검증

- 변경: root v32 정렬, Hibernate Lettuce와 demo build의 강제 블록 제거, Hibernate Lettuce에 test-only core 연결, JpaExecutorContractTest 8개 사례와 함수 정리.
- 완료: fixture GREEN·음성 대조, 두 모듈 8개 classpath 대조, controller 포함 세 모듈 전체 테스트, 전역 build, 신규 fixture Detekt 지적 해소.
- 미완료: QueryDSL 예제 production 정렬, 전체 테스트 증거, 기존 Detekt·configuration-cache 지적 처리, 최종 코드 리뷰, lesson·wiki 보존, PR·CI.
- 독립 리뷰 생성 실패 이력은 삭제하지 않았으며, 주 담당자의 직접 검토 결과와 runtime resolution을 연결했다. 독립 리뷰로 바꾸어 집계하지 않는다.
- 조사 중 context-mode의 짧은 제한으로 dependencyInsight 도구 호출이 timeout을 보고했지만 실제 Gradle 로그는 완료했다. 같은 조회를 재시도하지 않았고 이후 빌드는 외부 상한을 유지한 background 실행·별도 종료코드 파일로 확인했다.
- 후속 수정은 root catalog alias의 전체 영향 범위와 필요한 모든 모듈 검증을 포함해야 한다. workflow dispatch·병합·태그·배포·삭제 승인을 포함하지 않는다.

## 문서 검증

SPW-01–04: 한국어 실행 기록, 정확한 파일·버전·종료코드·원인과 승인 경계를 확인했다.
KO-01–06: 코드 토큰·수치·오류를 유지하고 문서 검토와 테스트 통과를 분리했다.
SPW-05·KO-07: 최종 재독과 용어 감사 통과, findings=0. 문서 SPW 5/5·KO 7/7이며 구현 검증 통과를 뜻하지 않는다.

## QueryDSL 직접 선언 정렬 후 검증

사용자가 implementation·kapt 두 선언의 catalog v32 정렬을 승인했고,
후속 `계속해`로 검증 진행을 지시했다. 두 줄을 변경했으며 기존 테스트
버전 강제 블록은 유지했다. 저장소 Gradle 파일에서 직접 v31 선언은
더 이상 검색되지 않는다.

- `compileClasspath`, `runtimeClasspath`, `testCompileClasspath`,
  `testRuntimeClasspath`, `kapt` 모두 Jakarta Persistence 3.2.0을 선택했다.
  각 dependencies 실행은 BUILD SUCCESSFUL이며 FAILED 항목은 없다.
- `:bluetape4k-examples-jpa-querydsl-demo:cleanTest :bluetape4k-examples-jpa-querydsl-demo:test --no-build-cache --no-parallel`:
  종료코드 0, JUnit 45개 중 44개 통과·실패 0·오류 0·제외 1.
  제외된 `insert operations()`는 기존
  `@Disabled("단순 entity insert 는 불가하다")`이며 이번 변경으로 제외하지 않았다.
  따라서 45개 전체 통과로 보고하지 않는다.
- 로그: `/tmp/issue-1640-querydsl-{configuration}.log`,
  `/tmp/issue-1640-querydsl-verify.log`, 종료코드 파일
  `/tmp/issue-1640-querydsl-verify.exit`.
- 전역 build 재실행은 종료코드 0으로 성공했다. 1분 42초,
  686개 task 중 98개 실행·588개 up-to-date이며 configuration-cache
  문제 12건으로 cache entry가 폐기됐다.
  로그는 `/tmp/issue-1640-querydsl-global-build.log`와 같은 이름의
  `.exit` 파일이다. `-x test`로 custom test task까지 제외되지는
  않았으며, 이 결과를 전역 전체 테스트 통과로 집계하지 않는다.
- `test --no-build-cache --no-parallel` 첫 전역 테스트는
  cache-core 문서 검사 3개에서 환경변수 누락으로 실패했다.
  종료코드 1, 오류는 `BLUETAPE4K_MANUAL_ROOT must point to the central manual checkout`다.
  로그는 `/tmp/issue-1640-global-tests.log`다.
- GNO 기록과 현행 CI 설정·테스트 reader를 대조하여
  `/Users/debop/work/bluetape4k/bluetape4k.github.io/docs/manual/bluetape4k-projects`를
  실제 manual root로 확인했다. 중앙 저장소 HEAD는
  `6a1b441980fd8b8ccbb4dd7123c58401ceedc967`이다.
  환경변수만 지정한 `NearJCacheDocumentationTest` 재검증은 6개 통과·
  실패/오류/제외 0·종료코드 0이다. 소스나 사용자 shell 설정은 변경하지 않았다.
- 같은 환경의 전역 테스트는 4분 46초 후 core에서 종료코드 1로 실패했다.
  core JUnit은 1,674개 중 실패 1·오류 0·제외 0이었다.
  `ParallelIterateSupportTest.kt:62`의 `병렬 방식으로 숫자 세기()`가
  `Expected <2386> to be less than <1246>, but was not.`로 실패했다.
  로그는 `/tmp/issue-1640-global-tests-manual.log`다.
- 해당 테스트는 두 count 결과를 먼저 확인한 뒤 `measureTimeMillis`로
  측정한 병렬 시간이 순차 시간보다 작다는 조건을 검사한다. 이번 diff에
  core 소스 변경은 없다. 같은 클래스의 수정 없는 단독 재실행은 13개
  통과·실패/오류/제외 0·종료코드 0이었다.
  로그는 `/tmp/issue-1640-core-timing-diagnostic.log`다.
  실행 조건에 따라 시간 비교 결과가 달라졌음을 확인했지만 정확한
  스케줄링 원인까지 입증하지는 않았다. 재실행 통과로 전역 실패를 대체하지 않는다.
- GNO와 live GitHub에서 `ParallelIterateSupportTest` 검색 결과는 없었다.
  이것만으로 모든 유사 이슈가 없다고 단정하지 않는다.
  core 테스트 안정화는 현재 승인 범위 밖이므로 수정하지 않았다.
  전역 검증 실패가 남아 최종 리뷰·lesson·PR·CI 진행을 보류한다.

계획 추가 승인 절과 이 실행 기록의 문서 검증:
SPW-01–04는 사용자 승인·두 줄 diff·5개 graph·JUnit XML에 대조했다.
KO-01–06은 버전·명령·수치·미검증 항목을 유지하며 한국어 기술 문체로 재독했다.
SPW-05 재독과 KO-07 용어 감사는 두 문서 모두 통과했으며 findings=0이다.
문서 SPW 5/5·KO 7/7은 전역 구현 검증 통과를 뜻하지 않는다.

## DoD Status

### core 시간 비교 안정화 추가 승인 및 실행

사용자의 후속 `추가해`로 앞서 보류한 core 테스트 안정화를 승인 범위에
포함했다. `ParallelIterateSupportTest.kt` 한 파일의 count 검사만
변경했으며, 제품 구현과 다른 성능 예제는 유지했다.

- 기존 RED: 전역 실행의 2386ms < 1246ms 실패. 정상 계산 결과와 속도
  비교 실패를 구분한다.
- 변경: sleep·속도 비교를 제거하고 입력 크기 0·1·31·32·33·1000,
  batchSize=32에서 혼합·전체 일치·전체 불일치의 정확한 count를 검사한다.
- GREEN: 클래스 18개 통과·실패/오류/제외 0·종료코드 0.
  `/tmp/issue-1640-core-count-green.log`.
- 음성 대조: 혼합 count의 기대값을 1 증가시키면 입력 6개 모두 실패한다.
  클래스 18개 중 실패 6·오류/제외 0·종료코드 1.
  `/tmp/issue-1640-core-count-negative.log`. 대조 변경은 복구했다.
- 복구 후 core `cleanTest test detekt --no-build-cache --no-parallel`:
  1,679개 통과·실패/오류/제외 0·종료코드 0.
  `/tmp/issue-1640-core-count-full.log`.
  Detekt 전체 지적은 287개이며 변경한 테스트 파일 지적은 0개다.
  root의 ignoreFailures 설정 때문에 종료코드만으로 정적 검사 전체 통과를
  주장하지 않는다.
- 중앙 manual 환경을 지정한 전역 테스트 재실행은 Logging 완료 후
  Math 시작 직전 1,800초 상한으로 종료됐다(종료코드 124).
  `/tmp/issue-1640-global-tests-core-fixed.log`에 기록했다.
  전역 성공으로 집계하지 않는다. 실행 소유 process group 13944의
  사후 조회 결과는 비어 있었다.
- ID 생성기의 장시간 구간을 조사한 thread dump에서 UUID stress test가
  StructuredTaskScopeTester로 작업을 생성하는 것을 확인했다.
  이후 통과 출력이 다시 갱신됐으며, 교착으로 판단하거나 코드를 수정하지 않았다.
- 완료된 task의 최신 상태 검사를 유지하며 같은 전역 명령을 다시 실행했다.
  테스트 제외 옵션은 추가하지 않았고 동일한 1,800초 상한을 적용했다.
  후속 로그는 `/tmp/issue-1640-global-tests-core-resumed.log`다.
- 후속 전역 실행은 `BUILD SUCCESSFUL in 20m 35s`, 종료코드 0으로 완료됐다.
  631개 task 중 156개 실행·475개 up-to-date다. 앞선 완료 task를 재사용한
  증분 검증이며 모든 테스트를 한 번에 clean 실행한 결과는 아니다.
  configuration-cache 문제 12건으로 cache entry가 폐기됐다.
- 완료 직후 현재 `**/build/test-results/test/TEST-*.xml` 2,287개를 합산한
  결과는 tests=21,630, failures=0, errors=0, skipped=108이다.
  이는 작업 트리에 남은 일반 test 보고서의 합계이며 해당 명령에서 새로
  실행한 테스트 수나 모든 custom test task의 합계로 해석하지 않는다.
  제외 108개를 통과로 집계하지 않는다. 이번 core 변경과 JPA fixture는
  각각 1,679개와 8개 모두 제외 없이 통과했다.

### core 변경의 한정된 검토

독립 test-engineer 검토자를 생성했으나 90초 무응답 제한 안에 유효한
결과를 받지 못했다. native interrupt의 이전 상태는 running이었고,
실패 lane 기록을 보존했다. 아래 결과는 **inline fallback review**이며
독립 검토나 전체 이슈의 최종 리뷰가 아니다.

| 관점 | 근거와 판단 |
|---|---|
| 정확성 | size=0·1과 32 전후의 입력, 혼합의 (size+1)/2·전체 size·불일치 0을 검증한다. |
| 안정성 | count 검사에서 sleep·wall-clock 대소 조건을 제거했다. 새 executor·동기화·자원 소유권은 없다. |
| 호환성과 범위 | 제품 코드·공개 API·의존성은 그대로다. 기존 JUnit parameterization과 assertions를 재사용했다. |
| 증거 한계 | count 결과 계약만 검증한다. 실제 worker 수·속도 우위·성능 향상은 증명하지 않는다. |
| 판정 | 이 한 파일의 승인된 안정화 변경에는 P0=0/P1=0. 전역 테스트와 전체 이슈 리뷰는 별도 PENDING이다. |

KT-TEST-01·05는 기존 테스트 도구·음성 대조·core 전체 결과로 확인했다.
KT-TEST-02–04의 새 stress harness·cancellation·HTTP·container 변경은 없다.
계획 추가 절과 이 실행 기록은 한국어 기술 문서로 승인·diff·XML·로그에
대조했다. 전역 종료 결과와 증분 실행·제외 집계의 한계를 구분했다.
최종 재독과 계획·실행 기록·수락 기준 검증 문서 3개 용어 감사는
findings=0으로 통과했다. 이번 추가 문서의 SPW 5/5·KO 7/7을 확인했다.

**PENDING — core 시간 비교 안정화·모듈 검증·전역 테스트 완료, 최종 리뷰 대기.**
전체 이슈 완료나 PR 준비 완료가 아니다.
