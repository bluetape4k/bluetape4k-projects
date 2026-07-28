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
