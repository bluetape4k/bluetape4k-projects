# Issue 416 idgenerator Spring Boot demo

## Context

#416은 `bluetape4k-idgenerators`를 Spring Boot에서 바로 참고할 수 있는 예제로 제공하는 작업이다. #419의 Ktor 예제와 범위를 나누되, endpoint 스타일은 명시적 `/ids/*`와 generic `/idgen/{type}`을 모두 제공하는 방향으로 정리했다.

## Decision

- 새 예제는 기존 Spring Boot demo 계열과 맞춰 `spring-boot/idgenerator-demo`에 둔다.
- `IdGeneratorConfiguration`에서 generator Bean 등록 방식을 드러내고, `IdGeneratorRegistry`가 REST type과 generator를 매핑한다.
- 사용자가 선호한 명시적 endpoint를 유지하면서, 재사용성이 좋은 `/idgen/{type}`도 함께 제공한다.
- unique ID 검증은 endpoint 테스트에서 `SuspendedJobTester`를 사용해 병렬 요청으로 증명한다.

## Outcome

Spring Boot 애플리케이션, REST controller/service/registry/configuration, README.md/README.ko.md, 테스트 리소스와 lesson 문서를 추가했다.

## Verification Evidence

- `./gradlew :bluetape4k-spring-boot-idgenerator-demo:compileKotlin :bluetape4k-spring-boot-idgenerator-demo:compileTestKotlin --parallel` 성공.
- `./gradlew :bluetape4k-spring-boot-idgenerator-demo:test --parallel` 성공, `IdGeneratorDemoApplicationTest` 7개 통과.

## Future Guidance

idgenerator 예제는 endpoint를 줄이지 말고 명시적 endpoint와 generic endpoint를 함께 유지한다. 테스트에는 단순 응답 확인뿐 아니라 병렬 unique ID 검증을 포함한다.
