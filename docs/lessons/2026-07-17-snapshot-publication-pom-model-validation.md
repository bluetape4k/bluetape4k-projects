# Snapshot publication POM model validation

## 배경

Generated publication POM에는 version 없는 Spring Boot와 Jackson BOM import가 있었다.
그래서 Maven은 해당 import 없이는 version을 resolve할 수 없는 일반 Spring dependency를
포함해 25개 module POM을 거부했다.

## 결정

Published BOM import의 version source로 중앙 `bt4k` catalog를 사용한다. CI, snapshot
publishing, release publishing 전에 모든 generated POM을 구조적으로 검증한 뒤 effective
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

수정 뒤에는 모든 publication POM을 강제 생성하고 `ruby scripts/publication/validate_poms.rb`로
effective Maven model을 확인한다. 중복 좌표 검사는
`ruby scripts/publication/publication_pom_audit_test.rb`에 회귀 테스트를 둔다.
2026-10-07 검증에서는 82개 POM, 33,111개 dependency가 통과했다.
