---
manualId: bluetape4k-nats
title: "NATS 클라이언트 확장"
description: "jNATS의 core messaging, JetStream, KeyValue, ObjectStore와 Service API를 Kotlin DSL과 coroutine으로 다루는 방법을 설명합니다."
kind: library
group: messaging
learningOrder: 730
---

# NATS 클라이언트 확장

## 제공하는 기능 {#problem}

`bluetape4k-nats`는 공식 Java client인 jNATS 위에 Kotlin 친화적인 builder와 확장 함수를 더합니다. 문자열 payload 발행, request-reply, `CompletableFuture`를 기다리는 suspend 함수, JetStream stream·consumer 관리, KeyValue·ObjectStore 설정, NATS Service 구성을 간결하게 작성할 수 있습니다.

이 모듈은 broker나 별도의 messaging framework가 아닙니다. connection, reconnect, dispatcher thread, subject routing, JetStream persistence와 acknowledgment의 실제 계약은 jNATS와 NATS server가 담당합니다. 이 매뉴얼은 wrapper가 줄여 주는 코드와 애플리케이션이 여전히 결정해야 하는 운영 경계를 나눠 설명합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- 메시지를 현재 subscriber에게만 전달하는 core NATS로 충분한지, 저장·재전달·acknowledgment가 필요한 JetStream인지 먼저 정합니다.
- `Connection`을 어느 컴포넌트가 만들고 닫을지 정합니다. 이 모듈은 connection singleton이나 Spring bean을 제공하지 않습니다.
- request-reply의 timeout과 no-responder를 정상적인 업무 결과로 볼지 실패로 볼지 호출부에서 결정합니다.
- JetStream stream과 consumer를 애플리케이션 시작 시 생성할지, 별도 운영 배포 단계에서 관리할지 선택합니다.
- KeyValue를 일반 database처럼 쓰지 않습니다. revision과 watch가 필요한 작은 상태에 맞는지 확인합니다.
- Spring Boot가 필요하다면 `nats-spring`을 애플리케이션이 직접 선언하고 lifecycle·properties·health 구성을 소유합니다.

## 의존성 추가 {#coordinates}

사용자는 jNATS 세부 버전을 직접 맞추지 않고 중앙 BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-nats")
}
```

Gradle project path는 `:bluetape4k-nats`, source directory는 `infra/nats`입니다. coroutine과 `nats-spring` 연동은 `compileOnly`이므로 해당 API를 사용하는 애플리케이션이 runtime dependency를 제공해야 합니다.

## 첫 publish와 subscribe {#quick-start}

가장 작은 core NATS 흐름은 connection을 직접 열고 닫습니다.

```kotlin
val options = natsOptions {
    server("nats://localhost:4222")
    maxReconnects(10)
}

Nats.connect(options).use { connection ->
    val subscription = connection.subscribe("orders.created")

    connection.publish("orders.created", "{\"orderId\":\"O-100\"}")
    connection.flush(2.seconds)

    val message = subscription.nextMessage(2.seconds)
    println(message?.data?.toUtf8String())
}
```

`flush`는 server가 이전 protocol command를 처리했음을 확인하는 round trip이지, JetStream 저장 acknowledgment가 아닙니다. subscriber가 잠시 없던 메시지를 나중에 받아야 한다면 core subscription 대신 stream과 consumer를 설계합니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| connection option 구성 | `natsOptions`, `natsOptionsOf` | builder만 제공하며 connection을 만들거나 닫지 않습니다. |
| core publish·request | `Connection.publish`, `requestAsync`, `requestSuspending` | core publish에는 저장 ack가 없습니다. |
| message와 subscription | `natsMessageOf`, `Subscription.nextMessage` | timeout이면 다음 message는 `null`입니다. |
| callback dispatch | jNATS `createDispatcher` | callback thread와 unsubscribe·drain 순서를 호출자가 관리합니다. |
| JetStream publish | `JetStream.publishSuspending` | `PublishAck`가 성공해야 stream 저장을 확인할 수 있습니다. |
| stream·consumer 관리 | `JetStreamManagement` 확장, `consumerContextOf` | replace·purge·delete는 운영 상태를 바꾸는 명령입니다. |
| KeyValue와 ObjectStore | configuration DSL과 management 확장 | 둘 다 JetStream을 기반으로 하며 server 설정의 영향을 받습니다. |
| service endpoint 구성 | `endpointOf`, `serviceEndpointOf`, `natsServiceOf` | service 시작·중지와 handler 실패 정책은 호출자가 소유합니다. |

## 학습 경로 {#concepts}

아래 여섯 장은 1.12.1 배포 소스와 테스트를 따라 connection ownership에서 core messaging, JetStream과 운영 경계까지 이어집니다. 각 장에는 바로 실행 흐름을 만들 수 있는 예제와 실패 조건, 다음에 읽을 source가 함께 있습니다.

1. [Connection과 option 소유권](./bluetape4k-nats/connection-options-ownership.md) — option DSL, connection lifecycle, reconnect와 shutdown 책임을 정합니다.
2. [Core pub-sub와 request-reply](./bluetape4k-nats/core-pubsub-request-reply.md) — 휘발성 전달, flush, timeout과 no-responder를 구분합니다.
3. [Message, subscription과 dispatcher](./bluetape4k-nats/messages-subscriptions-dispatchers.md) — message 구성, blocking pull, callback dispatch와 drain 순서를 다룹니다.
4. [JetStream stream과 consumer](./bluetape4k-nats/jetstream-streams-consumers.md) — publish ack, stream reconciliation, durable consumer와 fetch 제한을 설명합니다.
5. [KeyValue, ObjectStore와 Service API](./bluetape4k-nats/keyvalue-objectstore-services.md) — revision, watch, object chunk와 service endpoint의 용도를 구분합니다.
6. [실패, 테스트, 운영과 생태계 경계](./bluetape4k-nats/failures-testing-operations-ecosystem.md) — not-found 처리, Testcontainers, 관측 항목과 Spring 통합 책임을 정리합니다.

처음 도입한다면 1→2→3 순서로 core NATS를 먼저 확인합니다. 저장과 재처리가 필요할 때 4장으로 넘어가고, JetStream 기반의 상태·파일·서비스 기능이 필요할 때만 5장을 추가합니다.

## 권장 패턴 {#patterns}

하나의 애플리케이션 lifecycle에 connection 하나를 연결하고, operation마다 `Nats.connect`를 반복하지 않습니다. `use`는 짧은 batch와 test에 적합하지만 장기 실행 서비스에서는 시작 시 만들고 graceful shutdown에서 `drain`과 `close` 순서를 명시합니다.

core NATS와 JetStream을 전달 보장이라는 한 문장으로 섞지 않습니다. 즉시 처리하고 유실을 허용할 notification은 core subject가 단순합니다. 재시작 후 재처리, durable consumer, replay와 acknowledgment가 필요하면 stream을 만들고 retention·storage·replicas를 운영 설정으로 관리합니다.

## 연동 {#integrations}

모듈은 `jnats`를 API dependency로 노출합니다. coroutine bridge와 `nats-spring`은 `compileOnly`입니다. `requestSuspending`, `publishSuspending`, `drainSuspending`을 사용하면 coroutine runtime을 애플리케이션에 추가해야 합니다.

Spring Boot 연동은 이 artifact가 자동 구성하지 않습니다. main source에는 `@AutoConfiguration`, bean factory, properties class, health indicator가 없고 `src/main/resources`도 없습니다. Spring을 쓸 때는 `nats-spring`과 필요한 설정을 직접 선언하고, 동일한 `Connection`을 누가 생성·종료하는지 하나로 맞춥니다.

## 설정 {#configuration}

모듈 전용 property나 YAML namespace는 없습니다. server URL, authentication, TLS, reconnect, buffer, ping, error listener는 jNATS `Options.Builder`에서 설정합니다.

```kotlin
val options = natsOptions {
    servers(arrayOf(primaryUrl, secondaryUrl))
    maxReconnects(20)
    connectionListener { connection, event ->
        logger.info { "NATS event=$event servers=${connection.servers}" }
    }
}
```

credential이나 token이 포함된 URL을 log에 남기지 않습니다. JetStream의 stream configuration과 consumer configuration은 connection option과 별도 배포·운영 설정으로 다룹니다.

## 실패 동작 {#failures}

subject, stream name, bucket name처럼 필수 문자열이 blank면 wrapper는 `IllegalArgumentException`을 던집니다. 나머지 connection·timeout·protocol·JetStream 오류는 jNATS 예외나 `CompletableFuture` 실패로 그대로 전달합니다.

`forcedDelete*`, `get*OrNull`, `exists*` 계열은 JetStream의 not-found 코드만 정상 흐름으로 바꿉니다. network `IOException`, permission 오류와 다른 API error는 숨기지 않습니다. 이름에 `forced`가 붙어도 무조건 성공시키는 함수가 아닙니다.

## 운영 {#operations}

connection state와 reconnect 횟수, slow consumer, dropped message, request timeout, dispatcher backlog를 관찰합니다. JetStream에서는 publish ack latency, consumer pending·redelivery·ack floor, stream bytes·messages와 storage 상태를 별도로 봅니다.

stream replace, purge, consumer delete, KeyValue/ObjectStore bucket delete는 파괴적인 제어 명령입니다. 요청 처리 경로에서 습관적으로 실행하지 말고 시작 단계의 제한된 reconciliation 또는 별도 운영 도구에 둡니다.

## 테스트 {#testing}

builder와 위임 계약은 MockK 기반 unit test로 확인할 수 있지만, 실제 publish·request·JetStream·KeyValue·ObjectStore 예제는 `NatsServer` Testcontainer를 사용합니다.

```bash
./gradlew :bluetape4k-nats:test --no-configuration-cache
```

`AbstractNatsTest`는 `src/test` fixture이며 published API가 아닙니다. Testcontainers suite는 다른 infrastructure test와 순차 실행합니다. 장애 테스트에는 server 중단, reconnect, request timeout과 consumer redelivery를 의도적으로 포함합니다.

## 워크숍과 실행 예제 {#workshops}

모듈 test source가 작은 workshop 역할을 합니다. `PubSubExample`과 `RequestReplyExample`은 core messaging을, `KeyValueIntroExamples`는 revision·watch를, `ObjectStoreExample`은 chunked upload와 digest 검증을 보여 줍니다. JetStream의 `simple` 예제는 `ConsumerContext`에서 `next`, `fetch`, iterable consumer로 발전하는 흐름을 담고 있습니다.

예제를 그대로 production 설정으로 옮기지는 않습니다. test는 memory storage와 짧은 timeout을 주로 사용하므로 실제 retention, replicas, file storage와 shutdown 기준은 서비스 요구에 맞춰 다시 정합니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 release commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`의 1.12.1 소스와 테스트를 기준으로 합니다. module은 jNATS builder DSL과 작은 extension을 제공하며 자체 broker, schema registry, serializer contract, retry framework, outbox, tracing instrumentation을 제공하지 않습니다.

Spring Boot auto-configuration과 Spring Cloud Stream binder도 포함하지 않습니다. `nats-spring`은 compile-time API edge일 뿐이며 애플리케이션이 dependency와 lifecycle을 직접 구성해야 합니다.

## Source와 tests {#sources}

- [`build.gradle.kts`](../../../../infra/nats/build.gradle.kts)
- [`Options.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/Options.kt)
- [`ConnectionExtensions.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/ConnectionExtensions.kt)
- [`SubscriptionExtensions.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/SubscriptionExtensions.kt)
- [`JetStream.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStream.kt)
- [`JetStreamManagement.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStreamManagement.kt)
- [`KeyValueManagement.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/KeyValueManagement.kt)
- [`ObjectStreamManagement.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/ObjectStreamManagement.kt)
- [`Service.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/service/Service.kt)
- [`SimplePublishExample.kt`](../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/SimplePublishExample.kt)
- [`NatsManagementExtensionsTest.kt`](../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/NatsManagementExtensionsTest.kt)
- [`ServiceExtensionsTest.kt`](../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/service/ServiceExtensionsTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### nats 클래스 구조도

[![nats 클래스 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-nats-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-nats-diagram-01.svg)

_배포본 README: [`infra/nats/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/nats/README.ko.md)_

<!-- release-readme-diagrams:end -->
