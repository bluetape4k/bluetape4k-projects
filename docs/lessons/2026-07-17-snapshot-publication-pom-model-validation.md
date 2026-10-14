# Gradle -SNAPSHOT POM 모델 검증

## 배경

Generated publication POM에는 version 없는 Spring Boot와 Jackson BOM import가 있었다.
그래서 Maven은 해당 import 없이는 version을 resolve할 수 없는 일반 Spring dependency를
포함해 25개 module POM을 거부했다.

## 결정

Published BOM import의 version source로 중앙 `bt4k` catalog를 사용한다. CI 및
`-SNAPSHOT`·release 게시 전에 모든 generated POM을 구조적으로 검증한 뒤 effective
Maven model을 build한다.

## 결과

77개 generated publication POM은 versioned dependency-management import를 가지며,
versionless regular dependency는 versioned BOM이나 같은 POM이 관리할 때 계속 유효하다.

## 검증

- `ruby scripts/publication/publication_pom_audit_test.rb`
- `./gradlew generatePomFileForBluetape4kPublication -PsnapshotVersion=-SNAPSHOT --no-daemon --no-configuration-cache --no-build-cache`
- `ruby scripts/publication/validate_poms.rb`

## 향후 지침

모든 regular dependency에 direct version을 요구하지 않는다. BOM import에는 version을
요구하고, 각 versionless regular dependency가 실제로 관리되는지는 Maven
effective-model validation으로 증명한다.

## 중복 dependencyManagement 항목 (2026-10-07)

Maven 3.10.0의 effective-model 검증은 같은 `groupId:artifactId:type:classifier`를
가진 `dependencyManagement` 항목을 거부한다. Gradle이 생성한 POM에서 겹치는 BOM과
platform 제약이 같은 좌표를 반복할 수 있으므로, 구조 검사는 이 좌표를 기준으로
중복을 검출해야 한다. `type`이 없으면 `jar`, `classifier`가 없으면 빈 값으로
정규화한다.

POM 생성 중복을 정리할 때는 전체 항목의 의미가 같을 때만 하나를 남기고, 버전이나
scope가 다른 항목은 충돌로 실패시킨다. `XmlProvider.asNode()`의 `Node.name()`에는
namespace가 포함되므로, 하위 노드는 namespace를 제거한 local name으로 찾는다.
모듈 자체가 특정 BOM 버전을 가져오는 경우에는 전역 BOM을 중복 추가하지 말고 해당
모듈의 우선순위를 유지한다.

항목 fingerprint는 구분자만 연결해서 만들지 않는다. POM 값에 구분자가 포함되면
서로 다른 텍스트와 자식 경계가 같은 문자열이 되어 exclusion이 다른 선언을 중복으로
지울 수 있다. 요소 종류, 속성 키·값, 텍스트와 자식 요소를 길이 접두 방식으로
인코딩하고 속성과 자식 개수를 기록한다. 구분자 충돌 회귀 테스트에서 서로 다른
exclusion이 제거되지 않고 충돌로 실패하는지 검증한다.

수정 뒤에는 모든 publication POM을 강제 생성하고 `ruby scripts/publication/validate_poms.rb`로
effective Maven model을 확인한다. 중복 좌표 검사는
`ruby scripts/publication/publication_pom_audit_test.rb`에 회귀 테스트를 둔다.
2026-10-07 검증에서는 82개 POM, 33,111개 dependency가 통과했다.

모듈별 호환성 BOM을 유지하는 경우 BOM import만 분리하지 않는다. 같은 모듈의 직접
관리 artifact 버전도 BOM의 호환성 선에 맞추고, 생성 POM에서 둘이 일치하는지 검증한다.
Testcontainers는 Netty core를 `netty-bom` 4.1.x에 맞춘다. `netty-tcnative*`는 별도
2.0.x 계열이므로 Netty core 버전과 비교하지 않는다. Netty 4.1.136 BOM은 core artifact에
project version을 사용하고 tcnative에는 별도 `tcnative.version`을 사용한다
([공식 BOM POM](https://raw.githubusercontent.com/netty/netty/netty-4.1.136.Final/bom/pom.xml)).

## Issue #1831: 최종 POM 산출물에서 정규화한다 (2026-10-15)

### 맥락

root `build.gradle.kts`와 BuildSrc의 `configurePublishingSigning` 경로에
`dependencyManagement` 정규화가 각각 있었다. Maven POM XML callback이 끝난 뒤에도
dependency-management plugin이 항목을 추가할 수 있으므로, callback에서 정리한 결과만으로는
최종 POM의 중복과 충돌을 보장할 수 없다. 기존 fixture는 모든 기여자를 helper 설정 전에
등록해 이 실행 순서 차이를 드러내지 못했다. #1831의 독립 리뷰에서 이 공백을 확인했다.

### 결정

정규화 경로를 최종 `GenerateMavenPom` 후처리 action으로 단일화한다. root의 중복
`pom.withXml` callback은 제거하고, BuildSrc가 모든 기여가 반영된 파일을 다시 읽어 중복을
정리하거나 의미가 다른 같은 좌표를 실패시킨다.

회귀 fixture는 기본 POM 기여자를 등록하고 `configurePublishingSigning`을 호출한 다음,
두 번째 `pom.withXml` 기여자를 등록한다. 실제 `GenerateMavenPom` action을 실행한 파일에서
늦게 추가한 동일 항목이 첫 등장 순서만 남기는지, 같은 좌표에 exclusions가 다르면 충돌로
실패하는지 확인한다. callback 등록 순서나 helper 호출만 검사하는 테스트는 최종 산출물의
계약을 증명하지 못한다.

### 결과와 검증

`:buildSrc:test`를 필수 CI에 추가하고 Nightly 표준·전체 실행에서 실행한다. `run_standard`
출력이 `false`인 smoke 실행은 이 단계를 건너뛴다. 로컬 BuildSrc 전체 테스트는 다음 명령으로
실행해 37개 테스트가 실패·오류·건너뜀 없이 통과했다.

```bash
./gradlew :buildSrc:cleanTest :buildSrc:test --no-configuration-cache --no-daemon --no-build-cache --console=plain
```

핵심 회귀 테스트의 결과는
`buildSrc/build/test-results/test/TEST-io.bluetape4k.gradle.PublishingSigningSupportTest.xml`에
기록된다. 수정 workflow 두 개도 다음 명령으로 검사했다.

```bash
actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml
```

이 검증은 로컬 결과이며 원격 CI 실행을 뜻하지 않는다.

### 향후 지침

- `dependencyManagement` 정규화는 최종 `GenerateMavenPom` 파일에서 한 번만 수행한다.
- 회귀 fixture는 production callback 뒤에 새 기여자를 등록하고 실제 task 출력에서 결과를
  확인한다. 동일 항목은 첫 등장 순서를 보존하고, exclusions나 다른 의미가 다르면 조용히
  삭제하지 않고 실패시킨다.
