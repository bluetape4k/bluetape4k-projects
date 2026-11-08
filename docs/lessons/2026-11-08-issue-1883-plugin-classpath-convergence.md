# Issue #1883: plugin classpath의 취약 전이 의존성을 중앙 보안 트레인에 수렴한다

## 맥락

Issue [#1883](https://github.com/bluetape4k/bluetape4k-projects/issues/1883)의
live Dependency Submission SBOM에는 프로젝트 runtime classpath가 아닌
Gradle plugin classpath에서 유입된 취약 좌표가 남아 있었다. Dokka/Kover
경로에는 FreeMarker 2.3.32와 jsoup 1.16.1이, Avro plugin 경로에는 Jackson
2.14.2가, Jib/Gatling plugin 경로에는 Guava 32.1.2와 Jackson 2.22.2가
관찰됐다. 중앙 catalog 자체는 FreeMarker 2.3.35, jsoup 1.23.2, Jackson
2.22.4, Guava 33.7.2-jre, Bouncy Castle 1.86으로 이미 갱신되어 있었다.

## 원인

root project의 'allprojects' 및 Dokka configuration 보정은 일반 project
configuration에만 적용되고, 'plugins { }'를 로드하는 각 project의
'buildscript' 'classpath' configuration에는 적용되지 않았다. 따라서
catalog가 최신이어도 Dependency Submission이 plugin classpath의 오래된
전이 좌표를 별도로 수집했다.
MinIO testcontainers compile graph의 Bouncy Castle 요청 버전도 project
metadata 경로에 별도로 남을 수 있어 같은 수렴 범위로 확인했다.

## 결정

- Gradle 'buildscript.dependencies.constraints'에 Issue #1883의 보안 좌표를
  build-only 범위로 선언한다.
- root plugin classpath에는 직접 constraint를 적용한다.
- 'project.gradle.beforeProject'에서 같은 constraint map을 모든
  subproject 'buildscript' classpath에 적용한다. 전역
  'resolutionStrategy.force'나 published runtime dependency 강제는 사용하지
  않는다.
- hosted graph가 원본 plugin metadata의 취약 요청 버전을 보존하지 않도록
  Dokka의 'dokka-core', 'dokka-base', 'analysis-markdown',
  'templating-plugin', Avro plugin, Jib plugin, Gatling enterprise commons,
  Kover의 'coverage-report'에만 component metadata rule을 적용한다. 각
  rule은 해당 모듈의 취약 direct dependency/constraint를 제거하고 중앙
  catalog 버전을 다시 추가한다.
- Jackson BOM/annotation과 Bouncy Castle BOM도 함께 수렴시켜 동일 graph에서
  구버전 BOM이 남지 않도록 한다.
- root와 subproject buildscript classpath뿐 아니라 allprojects의 project
  dependency handler에도 metadata rule을 등록한다. Dokka base의 FreeMarker와
  Jackson constraint, MinIO의 bcprov-jdk18on 1.84 요청 edge를 제거하고
  중앙 catalog 버전을 다시 추가한다.
- WireMock의 Guava 33.5.0-jre와 Elasticsearch Java client의 Jackson 2.22.2
  요청 edge도 일반 project configuration에서 발견될 수 있으므로 같은
  metadata rule로 중앙 catalog 버전을 다시 추가한다.
- workflow contract test는 constraint가 'plugins { }'보다 먼저 선언되고,
  'beforeProject' callback이 동일 목록을 재사용하며, global force가 없음을
  검증한다.
- workflow contract test는 project metadata rule과 MinIO 보정 범위도
  확인하며, global force를 허용하지 않는다.
- 'issue1883DependencyGraphCheck' Gradle task는 대표 plugin classpath와
  일반 project의 runtime/test configuration을 project별 task에서 각각
  해석하고, build-only/runtime/test-only/published 범위를 로그에 남긴 뒤
  forbidden requested coordinates를 실제 ResolutionResult에서 검사한다.

## 검증

- 수정 전 RED: 새 plugin-classpath contract test가 constraint 부재로 실패했다.
- 수정 후 GREEN: 'tests.test_catalog_workflows' 143개 테스트 통과.
- './gradlew buildEnvironment :bluetape4k-avro:buildEnvironment :bluetape4k-mock-web-server:buildEnvironment :bluetape4k-mock-webflux-server:buildEnvironment --no-configuration-cache --no-daemon' 통과.
- 위 네 buildEnvironment 출력의 취약 요청 버전('1.16.1', '2.3.32',
  '2.15.3', '2.14.2', '2.22.2', '32.1.2', '1.11.3', '1.84')은 모두
  관찰되지 않았고, Gatling Jackson은 2.22.4, Kover FreeMarker는 2.3.35로
  선택됐다.
- 'issue1883DependencyGraphCheck' 통과.
- project graph를 확장한 첫 실행에서 WireMock의 Guava '33.5.0-jre'와
  Elasticsearch Java client의 Jackson '2.22.2' 요청 edge가 발견됐다. 두
  모듈의 metadata rule을 추가한 뒤 7개 대표 project의
  'compileClasspath', 'runtimeClasspath', 'testCompileClasspath',
  'testRuntimeClasspath' 검사가 모두 통과했다. 'apiElements'와
  'runtimeElements'는 published 범위로 분류했지만 Gradle에서
  non-resolvable이므로 hosted SBOM read-back 대상으로 남겼다.
- 'bluetape4k-testcontainers:compileClasspath' dependencyInsight에서
  MinIO의 'bcprov-jdk18on:1.84' 요청 edge가 사라지고 1.86이 선택됨을
  확인했다.
- 'bluetape4k-core:dokkaHtmlGeneratorRuntimeResolver~internal' 출력에서
  FreeMarker 2.3.35, jsoup 1.23.2, Jackson 2.22.4만 남고 기존
  Jackson 2.15.3 constraint가 사라짐을 확인했다.
- Avro plugin의 'jackson-core/databind:2.20.0'와 MinIO의 Guava
  '33.6.0-jre' 요청 edge도 동일한 metadata 검사에서 제거됐다.
- Avro 및 두 mock-server module의 compile/classes 작업 통과.
- 'TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock'를 확인된
  Colima 환경에만 적용해 ':bluetape4k-testcontainers:k8sTest' 9개가
  통과했고, 같은 환경의 './gradlew build -x test -x k8sTest'도 통과했다.
- ':bluetape4k-jwt:dependencies --configuration runtimeClasspath'는 Jackson
  2.22.4를, ':bluetape4k-nats:dependencies --configuration runtimeClasspath'는
  Bouncy Castle 'bcprov-lts8on:2.73.13'을 계속 선택했다.
- 'git diff --check' 통과.
- 환경 변수를 상속하지 않은 첫 전체 빌드는 Docker socket을 찾지 못해
  'k8sTest'와 'coordinationLockPerformanceTest'가 실패했으나, Colima가
  정상 실행 중임을 'colima status', 'docker context show', 'docker info'로
  확인한 뒤 동일 작업을 재실행해 해결했다.
- Dokka HTML 생성은 변경 파일과 무관한 기존
  'bluetape4k/core/README.md'의 'Wrong AST Tree' 오류로 실패했다. 이
  문서 파서는 이번 보안 수정 범위 밖이므로 별도 결함으로 남긴다.

## 잔여 검증

Dependency Submission 재실행 뒤 exact-head live SBOM과 Dependabot alerts
'#235', '#231' read-back이 필요하다. plugin-only 좌표의 alert scope와
runtime 영향은 hosted graph에서 다시 확인하며, 로컬 'buildEnvironment'
성공만으로 live SBOM 수렴을 주장하지 않는다.

## Live alert 좌표와 잔여 위험

아래 표는 2026-11-08에 default branch에서 읽은 17개 open alert를 요청
좌표별로 묶은 것이다. `local candidate`는 현재 branch의 대표 graph에서
확인한 후보 범위이고, `hosted pending`은 exact-head Dependency Submission
후에만 확정할 수 있는 project/configuration 경계다.

| Alert | 요청 좌표 | local candidate | 잔여 위험 |
|---|---|---|---|
| #231, #233 | `org.bouncycastle:bcprov-jdk18on:1.84` | runtime/test-only 후보(MinIO/testcontainers)와 direct catalog | hosted graph가 direct alert를 계속 보존할 수 있음(hosted pending) |
| #235 | `org.freemarker:freemarker:2.3.32` | build-only plugin(Dokka/coverage) | plugin-only라도 SBOM alert가 남을 수 있음(hosted pending) |
| #236, #238, #241, #244, #246, #252, #259, #268 | Jackson `2.14.2` | build-only plugin 및 project transitive 후보 | 각 hosted project/configuration 경계와 runtime 영향은 SBOM 재검증 필요 |
| #247, #253, #254, #256 | Jackson `2.22.2` | build-only Jib 및 test-only Elasticsearch client 후보 | local rule로 2.22.4를 선택하지만 requested edge의 hosted 제거는 미확정 |
| #262 | `org.jsoup:jsoup:1.16.1` | build-only plugin(Dokka) | plugin-only 범위와 SBOM 잔존 여부는 hosted pending |
| #269 | `com.google.guava:guava:33.6.0-jre` | build-only Jib 및 test-only WireMock 후보 | WireMock 33.5.0 edge는 제거했지만 direct catalog alert read-back 필요 |

현재 branch에서 확인된 모든 candidate에는 패치 버전이 존재한다. 따라서
`no-patch` 예외는 기록하지 않았고, plugin-only 좌표의 비실행 위험을
보안 수렴의 증거로 오인하지 않는 것이 핵심 운영 guard다.

## 향후 지침

새 Gradle plugin이 추가되거나 plugin 버전이 바뀌면 root catalog 버전만
확인하지 말고 해당 project의 'buildEnvironment', project configuration의
requested edge, Dependency Submission graph를 함께 확인한다. 취약 plugin을
즉시 교체할 수 없을 때는 plugin metadata rule과 hosted SBOM 검증을 함께
추가하고, published/runtime configuration에는 별도의 근거 없이 force rule을
확장하지 않는다.
