# Module bluetape4k-spring-webflux

Spring Webflux 를 사용하는 프로젝트에서 사용할 수 있는 Kotlin 확장 기능을 제공합니다.

## 주요 기능

- 커스텀 이벤트 루프를 사용하는 `WebClient` 설정
- 요청 정보를 Reactor Context에 저장/조회하는 WebFilter 제공
- 루트 경로 리다이렉트를 위한 WebFilter 제공
- 코루틴 컨트롤러용 기본 CoroutineScope 추상 클래스 제공

## WebClient 설정 예시

```kotlin
@SpringBootApplication
class CustomWebClientConfig: AbstractWebClientConfig() {
    override val threadCount: Int = 8
    override val responseTimeout: Duration = Duration.ofSeconds(5)
}
```

## 요청 정보 접근 예시

```kotlin
val request = HttpRequestHolder.getHttpRequest().awaitSingleOrNull()
```

## 리다이렉트 필터 예시

```kotlin
@Component
class RedirectToSwaggerWebFilter: AbstractRedirectWebFilter("/swagger-ui.html")
```

## 테스트

```bash
./gradlew :bluetape4k-spring-webflux:test
```
