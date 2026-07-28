# Issue 446 Jackson3 Consumer

## 배경

Spring Boot 4는 Jackson 3을 사용하므로, `bluetape4k-jackson2`만 소비하던 지원
module을 `bluetape4k-jackson3`으로 migration했다.

## 결정

Local code 또는 dependency가 `tools.jackson.*`와 `io.bluetape4k.jackson3.*`를
상대로 compile 가능한 module만 변환한다. Retrofit의 Jackson converter, JJWT Jackson,
Spring Kafka 3.x converter처럼 여전히 Jackson 2가 필요한 upstream integration은
유지한다.

## 결과

지원되는 data, IO, Feign, HTTP, Vert.x, NATS, Micrometer, gRPC, Cassandra,
Hibernate, MongoDB, R2DBC, Geo, Redisson demo dependency는 이제 Jackson3를 사용한다.
Jackson2-only upstream integration은 변경하지 않았다.

## 검증

- `./gradlew :bluetape4k-cassandra:testClasses :bluetape4k-hibernate:testClasses :bluetape4k-hibernate-reactive:testClasses :bluetape4k-mongodb:testClasses :bluetape4k-r2dbc:testClasses :bluetape4k-examples-redisson-demo:testClasses :bluetape4k-micrometer:testClasses :bluetape4k-nats:testClasses :bluetape4k-feign:testClasses :bluetape4k-grpc:testClasses :bluetape4k-http:testClasses :bluetape4k-vertx:testClasses :bluetape4k-geo:testClasses`
- `./gradlew :bluetape4k-geo:testClasses`

## 향후 메모

Module이 Jackson2를 이름으로 참조한다는 이유만으로 migration하지 않는다. 먼저 실제
resolved classpath에서 upstream adapter가 Jackson3 API를 노출하는지 확인한다.
