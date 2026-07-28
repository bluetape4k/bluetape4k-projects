# 이슈 #694 Spring observability helper

## 배경

Spring Boot 4 observability 작업에는 metrics, tracing, exporter, endpoint에 대한
Spring Boot Actuator와 Micrometer ownership을 보존하면서 service, HTTP handler, event
handler code를 위한 helper API가 필요했다.

## 결정

exporter나 custom endpoint를 auto-configure하지 않고, `spring-boot/core`에 좁은
`ObservationRegistry` helper를 추가한다. Prometheus와 OTLP는 README example에
문서화된 application configuration concern으로 취급한다.

## 결과

`observeSpring`과 `observeSpringSuspending`은 이제 observation lifecycle, exception
recording, cancellation propagation, coroutine scope cleanup을 관리한다. README file도
Spring Boot 4의 Jackson 3 default를 반영한다.

## 검증

- `./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.observability.SpringObservationSupportTest'`
- `./gradlew :bluetape4k-spring-boot-core:test`
- `git diff --check`

## 향후 가드

Spring Boot observability helper는 `ObservationRegistry` 같은 framework-owned object를
받고 backend property를 문서화한다. library 안에서 OpenTelemetry SDK, tracing exporter,
Prometheus endpoint를 instantiate하지 않는다.
