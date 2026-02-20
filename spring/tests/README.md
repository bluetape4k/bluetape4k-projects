# Module bluetape4k-spring-tests

Spring Boot Application을 테스트 할 때 필요한 유용한 기능을 제공합니다.

## 주요 기능

- `RestClient`, `WebClient`, `WebTestClient` 확장 함수 제공
- 테스트 코드에서 자주 쓰는 HTTP 호출을 간결하게 구성
- `httpbin` 기반 테스트 예제 포함

## 사용 예시

```kotlin
val response = restClient
    .httpGet("/get")
    .toEntity<String>()
    .body
```

```kotlin
val body = webClient
    .httpPost("/post", "hello")
    .awaitBody<String>()
```

```kotlin
webTestClient
    .httpPut("/put", "payload")
    .expectStatus().is2xxSuccessful
```

## 테스트

```bash
./gradlew :bluetape4k-spring-tests:test
```
