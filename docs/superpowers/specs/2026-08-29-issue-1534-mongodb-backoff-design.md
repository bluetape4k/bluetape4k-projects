# #1534 ReactiveMongoOperations backoff 경계 설계

- 이슈: [#1534](https://github.com/bluetape4k/bluetape4k-projects/issues/1534)
- 기준 브랜치: `develop`의 `783929c2925b9544cec43c6bc951246eeb3742cd`
- 작업 브랜치: `fix/issue-1534-mongodb-backoff`
- 대상 모듈: `bluetape4k-spring-boot-mongodb`

## 문제

`ReactiveMongoAutoConfiguration`은 생성자 `init`에서 legacy-only
`spring.data.mongodb.uri`를 검사한다. 그러나 fallback 소유권을 나타내는
`@ConditionalOnMissingBean(ReactiveMongoOperations::class)`은 Bean 메서드에만 있다.
그 결과 애플리케이션이 `ReactiveMongoOperations`를 직접 제공해 library fallback을 사용하지
않아도 auto-configuration class가 먼저 생성되어 migration 예외를 던진다.

현재 테스트는 다음 계약을 각각 검증하지만 두 조건을 결합하지 않는다.

- 사용자 소유 `ReactiveMongoOperations`는 fallback `ReactiveMongoTemplate`보다 우선한다.
- legacy URI만 있으면 정확한 migration 예외로 실패한다.
- 새 URI와 legacy URI가 함께 있으면 새 namespace가 우선한다.

누락된 consumer 계약은 사용자 소유 operations와 legacy-only URI가 함께 있을 때 application
context가 정상적으로 시작되고 사용자 Bean이 유지되는지다. 또한 class 조건은 Bean의
provenance를 구분하지 않으므로 Spring Boot가 operations를 먼저 제공한 경로도 같은 backoff
계약에 포함된다.

## 목표

1. legacy URI fail-fast를 library fallback이 실제로 참여하는 경계에만 적용한다.
2. 사용자 또는 Spring Boot가 제공한 `ReactiveMongoOperations`가 있으면
   auto-configuration class 전체를 backoff한다.
3. 사용자 Bean이 없을 때의 legacy-only fail-fast와 dual-key 우선순위는 유지한다.
4. KDoc와 영문·한글 README에 같은 책임 경계와 정확한 rollback 좌표를 기록한다.

## 비목표

- `spring.data.mongodb.uri` 지원 복원
- Spring Boot의 `MongoProperties` binding 변경
- 새 configuration property나 dependency 추가
- MongoDB coroutine/query DSL 변경
- #1535의 전체 manual README 점검 또는 #1537의 ABI 정책 변경

## 선택지

| 선택지 | 장점 | 문제 | 결정 |
| --- | --- | --- | --- |
| A. `@ConditionalOnMissingBean`을 class 경계로 이동 | class 생성과 `init` 검증이 fallback 소유권과 일치 | class 조건의 user-bean 검색 시점을 테스트로 고정해야 함 | **선택** |
| B. legacy 검사를 Bean 메서드 본문으로 이동 | 검사가 fallback 생성 경로에만 존재 | 메서드 인자 해석 실패가 migration 예외보다 먼저 발생할 수 있음 | 제외 |
| C. 전역 fail-fast 유지 | 모든 legacy-only 설정을 강하게 차단 | 사용자 소유 Bean 경계를 침범하고 issue의 호환성 결함을 유지 | 제외 |

## 동작 계약

`@ConditionalOnMissingBean(ReactiveMongoOperations::class)`을 Bean 메서드에서
`ReactiveMongoAutoConfiguration` class로 이동한다.

| 선행 operations | `spring.mongodb.uri` | `spring.data.mongodb.uri` | 결과 |
| --- | --- | --- | --- |
| 있음 | 없음 | 있음 | auto-configuration 전체 backoff, 사용자 Bean 유지 |
| 없음 | 없음 | 있음 | 기존 `IllegalStateException`으로 fail-fast |
| 있음 | 있음 | 있음 | auto-configuration backoff, Boot의 현재 URI binding 유지 |
| 없음 | 없음 | 없음 | factory와 converter가 있으면 fallback template 생성 |

선행 operations에는 애플리케이션이 직접 제공한 Bean과
`DataMongoReactiveAutoConfiguration`이 제공한 Bean이 모두 포함된다. class-level
`@ConditionalOnMissingBean`은 provenance가 아니라 동일 타입 Bean의 존재 여부를 판단한다.

예외 메시지는 호환 계약이므로 변경하지 않는다.

```text
Unsupported legacy MongoDB property 'spring.data.mongodb.uri'; use 'spring.mongodb.uri' on Spring Boot 4.1+
```

## 문서와 rollback 계약

README와 KDoc은 legacy-only fail-fast가 library fallback 참여 시에만 적용된다고 명시한다.
즉시 마이그레이션할 수 없는 사용자는 Maven Central에서 확인된 다음 좌표로 rollback할 수 있다.

```kotlin
implementation("io.github.bluetape4k:bluetape4k-spring-boot-mongodb:1.12.1")
implementation(platform("io.github.bluetape4k:bluetape4k-bom:1.12.1"))
```

`1.12.1` tag의 auto-configuration은 `spring.data.mongodb.uri`를 지원하며, 두 POM은
2026-08-29 기준 Maven Central에서 HTTP 200으로 확인됐다.

## 검증

1. 사용자 operations + legacy-only 테스트를 추가하고 현재 코드에서 의도한 이유로 실패하는지 확인한다.
2. Boot 제공 operations + legacy-only 테스트로 같은 backoff 범위를 고정한다.
3. class-level backoff로 최소 수정한 뒤 두 테스트를 GREEN으로 만든다.
4. 기존 legacy-only, dual-key, fallback 테스트를 포함한 test class 전체를 실행한다.
5. module 전체 테스트와 `detekt`, `git diff --check`를 순서대로 실행한다.
6. 영문·한글 README와 KDoc의 책임 경계, 예외 문자열, rollback 좌표를 대조한다.

## 수용 기준

- 사용자 operations + legacy-only context가 startup failure 없이 사용자 Bean을 반환한다.
- Boot 제공 operations + legacy-only context가 startup failure 없이 Boot template을 유지한다.
- 사용자 operations가 없으면 legacy-only가 기존 예외로 실패한다.
- 새 URI와 legacy URI가 함께 있을 때의 현재 URI 우선순위가 유지된다.
- KDoc과 README locale 두 개가 fail-fast 범위와 `1.12.1` rollback 좌표를 동일하게 설명한다.
- targeted/full module test, detekt, 문서 검증, `git diff --check`가 통과한다.
