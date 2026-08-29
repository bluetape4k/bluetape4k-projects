# #1534 Spring auto-configuration 검증 경계 교훈

## 배경

`ReactiveMongoAutoConfiguration`은 legacy-only `spring.data.mongodb.uri`를 생성자에서
검사했지만, 사용자 소유 `ReactiveMongoOperations`를 보호하는
`@ConditionalOnMissingBean`은 Bean 메서드에만 있었다. Bean 등록 조건이 있으면
auto-configuration의 모든 동작이 backoff할 것이라는 가정이 잘못됐다.

## 실패 증거

사용자 `ReactiveMongoOperations`와 legacy-only URI를 함께 등록한
`ApplicationContextRunner` 테스트는 Bean 메서드가 실행되기 전에 auto-configuration
생성자에서 실패했다. RED 결과는 `tests=1`, `failures=1`, `skipped=0`이었고 startup
failure는 `ReactiveMongoAutoConfiguration` 생성자 예외였다.

## 결정

사용자 Bean이 소유권 경계를 결정한다면 missing-bean 조건도 validation이 실행되는 가장
바깥 auto-configuration scope에 둔다. #1534에서는
`@ConditionalOnMissingBean(ReactiveMongoOperations::class)`을 Bean 메서드에서 class로
이동했다. 이로써 사용자 Bean이 있을 때 생성자 legacy 검사까지 backoff하고, library
fallback이 참여할 때는 기존 fail-fast가 유지된다.

class-level 조건은 Bean의 provenance를 구분하지 않는다. Spring Boot의
`DataMongoReactiveAutoConfiguration`이 먼저 만든 `ReactiveMongoOperations`도 동일한
backoff 대상이다. 이 경로를 별도 consumer test로 고정해 “사용자 소유”라는 문구가 실제
조건보다 좁게 해석되지 않도록 했다.

검사를 Bean 메서드 본문으로 옮기는 방법은 선택하지 않았다. Spring이 메서드 인자를 먼저
해석하면 `ReactiveMongoDatabaseFactory`나 `MongoConverter` 누락이 migration 예외보다
앞서 나타날 수 있기 때문이다.

## 재발 방지 규칙

1. auto-configuration의 생성자, `init`, class property가 환경이나 migration 계약을
   검사하면 Bean 메서드 조건만으로 backoff를 표현하지 않는다.
2. 사용자 소유 Bean이 auto-configuration 전체를 비활성화해야 하는지 먼저 결정하고,
   그렇다면 ownership condition을 class scope에 둔다.
3. `ApplicationContextRunner` 테스트는 사용자 Bean과 validation trigger를 결합해 class
   생성 전 backoff를 직접 검증한다. 사용자 Bean 테스트와 validation 테스트를 따로 두는
   것만으로는 이 경계를 증명하지 못한다.
4. `@ConditionalOnMissingBean`은 provenance가 아니라 타입 존재 여부를 판단한다. 선행
   framework auto-configuration이 같은 타입을 만들 수 있으면 그 경로도 별도 테스트한다.
5. validation을 Bean 메서드로 내릴 때는 dependency resolution이 의도한 오류보다 먼저
   실패하지 않는지 확인한다.

## 검증

- 회귀 테스트 RED: `tests=1`, `failures=1`, `skipped=0`
- 최소 수정 뒤 회귀 테스트 GREEN: `1 passing`
- Boot 제공 Bean 회귀 테스트 RED: `tests=1`, `failures=1`, `skipped=0`
- class-level 조건 복원 뒤 Boot 제공 Bean 회귀 테스트 GREEN: `1 passing`
- `ReactiveMongoAutoConfigurationTest`: `12 passing`
- `bluetape4k-spring-boot-mongodb` 전체: `tests=94`, `failures=0`, `errors=0`, `skipped=0`
- detekt task: `BUILD SUCCESSFUL`; 변경 밖의 기존 `TooManyFunctions` 2건만 출력
- 한국어 용어 감사와 `git diff --check`: 통과
