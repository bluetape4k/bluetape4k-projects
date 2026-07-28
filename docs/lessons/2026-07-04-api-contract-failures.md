# API contract failure는 호출자 context를 보존해야 한다

## 배경

이슈 #951과 #954는 public contract가 호출자 기대와 어긋난 helper API를 발견했다.

- Ktor Swagger UI는 nested specification path를 basename으로 접었다.
- RestClient coroutine helper는 `!!`를 사용해 empty body를 raw
  `NullPointerException` failure로 바꿨다.

## 결정

- File source와 Swagger UI remote path 모두에서 호출자가 제공한 relative Swagger
  specification path를 보존한다.
- RestClient coroutine `!!` assertion을 HTTP method, URI, target type을 포함하는
  명시적인 non-null body contract error로 바꾼다.

## 검증

- `./gradlew :bluetape4k-ktor-openapi:test --tests 'io.bluetape4k.ktor.openapi.KtorOpenApiRoutesTest'`
- `./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.http.RestClientCoroutinesDslTest'`
- `git diff --check`

## 향후 지침

Route helper default는 문서화된 path contract를 source-verified 상태로 유지해야
한다. Blocking client를 감싸는 coroutine wrapper는 Kotlin null assertion에 기대지
말고 호출자에게 보이는 명시적인 contract error를 드러내야 한다.
