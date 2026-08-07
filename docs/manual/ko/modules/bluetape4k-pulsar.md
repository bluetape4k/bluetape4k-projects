---
manualId: bluetape4k-pulsar
title: "Apache Pulsar 클라이언트 확장"
description: "Apache Pulsar Java Client를 Kotlin DSL, suspend 함수, Flow와 Jackson 2·3 JSON Schema로 사용하는 방법과 1.12.1의 수명주기 경계를 설명합니다."
kind: library
group: messaging
learningOrder: 740
---

# Apache Pulsar 클라이언트 확장

## 제공하는 기능 {#problem}

`bluetape4k-pulsar`는 Apache Pulsar Java Client 위에 Kotlin용 편의 API를 더합니다. client·producer·consumer·reader builder를 Kotlin DSL로 구성하고, `CompletableFuture` 기반 작업을 `suspend` 함수로 기다리며, 연속 발행·수신·읽기를 `Flow`로 연결할 수 있습니다. Jackson 2와 Jackson 3용 JSON `Schema<T>`도 선택해서 사용할 수 있습니다.

이 모듈은 broker, topic, subscription 또는 schema registry를 관리하지 않습니다. 재연결, batching, compression, routing, receiver queue와 broker protocol은 Pulsar Client가 맡습니다. 이 매뉴얼은 1.12.1에서 bluetape4k가 얹는 얇은 경계와 호출자가 계속 책임져야 하는 부분을 나눠 설명합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- client를 애플리케이션 수명 동안 공유할지, 짧은 작업 블록에서 만들고 닫을지 정합니다.
- Producer, Consumer, Reader 중 메시지 처리 의미에 맞는 API를 고릅니다. Reader에는 subscription과 ack가 없습니다.
- Consumer의 subscription type과 개별·누적 ack 정책을 먼저 정합니다.
- Jackson 2와 Jackson 3 중 하나를 고르고 producer와 consumer가 같은 JSON·schema 계약을 쓰도록 맞춥니다.
- `Flow`가 병렬 발행이나 자동 ack를 제공한다고 가정하지 않습니다.
- 취소 중 close 완료가 반드시 필요한 서비스라면 1.12.1의 cleanup 한계를 애플리케이션 종료 절차에서 보완합니다.

## 의존성 추가 {#coordinates}

사용자는 Pulsar Client와 Jackson의 세부 버전을 직접 맞추지 않고 중앙 BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-pulsar")

    // 둘 중 실제로 사용하는 JSON 계열만 추가합니다.
    implementation("io.github.bluetape4k:bluetape4k-jackson2")
    // implementation("io.github.bluetape4k:bluetape4k-jackson3")
}
```

Gradle project path는 `:bluetape4k-pulsar`, source directory는 `infra/pulsar`입니다. Jackson 2·3 연동은 `compileOnly`이므로 사용하는 애플리케이션이 해당 runtime dependency를 제공해야 합니다.

## 첫 client와 메시지 {#quick-start}

```kotlin
withPulsarClient("pulsar://localhost:6650") {
    withProducer(Schema.STRING, { topic("persistent://public/default/orders") }) {
        val messageId = sendSuspend {
            value("order-1")
            key("order-1")
        }
    }
}
```

`withPulsarClient`와 `withProducer`는 블록이 끝나면 `closeAsync()`를 호출합니다. 1.12.1 구현은 close 실패를 경고로 남기고 삼키며, 취소된 coroutine에서 close를 `NonCancellable`로 감싸지는 않습니다. 이 코드는 scope를 간결하게 만드는 helper이지 종료를 끝까지 보장하는 장치는 아닙니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| client 생성 | `pulsarClient`, `withPulsarClient` | URL 또는 setup의 `serviceUrl`이 필요합니다. |
| producer 생성·발행 | `producer`, `withProducer`, `sendSuspend` | builder 설정과 발행 의미는 Pulsar Client 계약입니다. |
| `Flow<T>` 순차 발행 | `sendAsFlow` | 한 번에 한 메시지를 기다리며 병렬화하지 않습니다. |
| consumer 생성·수신 | `consumer`, `withConsumer`, `receiveSuspend` | subscription name과 type을 호출자가 정합니다. |
| 연속 수신·ack | `receiveAsFlow`, `acknowledgeSuspend` | Flow는 자동 ack하거나 Consumer를 닫지 않습니다. |
| 누적 ack | `acknowledgeCumulativeSuspend` | Shared subscription에서는 사용할 수 없습니다. |
| subscription 없는 읽기 | `reader`, `withReader`, `readNextSuspend` | 시작 위치를 지정하며 ack하지 않습니다. |
| 현재 읽을 수 있는 메시지 drain | `readAsFlow` | `hasMessageAvailable()`이 false면 종료하는 유한 Flow입니다. |
| JSON payload schema | `jacksonSchema`, `jackson3Schema` | mapper 계열과 설정을 양쪽에서 맞춰야 합니다. |

## 학습 경로 {#concepts}

아래 여섯 장은 1.12.1 배포 소스와 테스트를 따라 client 소유권에서 wire contract, 메시지 처리와 운영 한계까지 이어집니다. 각 장에는 실제 API 조합, 이 모듈이 하지 않는 일, 실패·취소 시 확인할 항목이 함께 있습니다.

1. [Client 구성과 수명주기](./bluetape4k-pulsar/client-lifecycle-configuration.md) — URL·builder 구성, 직접 소유와 block scope, 1.12.1 cleanup 한계를 확인합니다.
2. [Jackson 2·3 Schema와 wire 호환성](./bluetape4k-pulsar/jackson-schemas-wire-compatibility.md) — `SchemaInfo`, mapper 선택, runtime dependency와 교차 버전 검증을 다룹니다.
3. [Producer와 coroutine 발행](./bluetape4k-pulsar/producers-coroutine-send.md) — 단건·메시지 DSL·순차 Flow 발행과 실패 경계를 설명합니다.
4. [Consumer, Flow와 ack](./bluetape4k-pulsar/consumers-flow-acknowledgement.md) — 무한 수신 Flow, 개별·누적 ack, subscription type을 연결합니다.
5. [Reader와 시작 위치](./bluetape4k-pulsar/readers-and-positions.md) — subscription 없는 읽기, `MessageId`와 유한 drain 동작을 설명합니다.
6. [취소, 실패, 테스트와 운영](./bluetape4k-pulsar/cancellation-failures-testing-operations.md) — future 취소, close 제한, Testcontainers 검증과 관측 항목을 정리합니다.

처음 도입한다면 1→3→4 순서로 단일 topic의 발행·수신을 완성합니다. 도메인 타입을 전송할 때 2장을 추가하고, replay나 점검 도구를 만들 때 5장을 읽습니다.

## 권장 패턴 {#patterns}

`PulsarClient`는 연결과 thread를 가진 장기 resource이므로 메시지마다 만들지 않습니다. 애플리케이션이 공유 client를 소유하고, producer·consumer도 처리 컴포넌트의 수명에 맞춰 재사용합니다. `with*` helper는 짧고 경계가 분명한 batch·도구 작업에 잘 맞습니다.

Consumer에서는 업무 처리가 끝난 뒤 ack합니다. 처리와 ack 사이의 장애, redelivery, idempotency를 애플리케이션 계약으로 다룹니다. Producer의 `sendAsFlow`는 순서를 보존하는 직렬 발행에 사용하고, throughput을 높이려면 Pulsar batching과 제한된 coroutine concurrency를 별도로 설계합니다.

## 연동 {#integrations}

모듈은 `pulsar-client`를 `api`로 노출하므로 builder option과 native type을 그대로 사용할 수 있습니다. `bluetape4k-core`, `bluetape4k-coroutines`와 coroutine core가 기본 경계입니다.

Jackson 2와 Jackson 3 지원은 선택 사항입니다. 두 schema 구현은 Pulsar의 JSON schema metadata를 사용하지만 payload bytes는 각 `ObjectMapper`가 만듭니다. 이름 전략, module, 날짜·숫자 설정이 다르면 같은 Kotlin class라도 wire 결과가 달라질 수 있으므로 교차 producer·consumer 테스트로 확인합니다.

## 설정 {#configuration}

모듈 전용 property나 resource는 없습니다. endpoint, authentication, TLS, timeout과 connection option은 `ClientBuilder`, topic·batching·compression은 `ProducerBuilder`, subscription·queue·ack timeout은 `ConsumerBuilder`, 시작 위치는 `ReaderBuilder`에서 공식 Pulsar 방식으로 설정합니다.

```kotlin
val client = pulsarClient {
    serviceUrl("pulsar+ssl://broker:6651")
    tlsTrustCertsFilePath("/run/secrets/pulsar-ca.pem")
}
```

`pulsarClient()`에 빈 URL을 주고 setup에서도 `serviceUrl`을 지정하지 않으면 `build()`가 실패합니다. credential과 token을 log에 남기지 않습니다.

## 실패 동작 {#failures}

생성, 발행, 수신, ack와 읽기 실패는 Pulsar Client 예외로 전파됩니다. helper는 retry, dead-letter routing, transaction 또는 domain exception 변환을 추가하지 않습니다. `sendAsFlow`, `receiveAsFlow`, `readAsFlow`도 upstream이나 Pulsar 작업이 실패하면 종료합니다.

연속 Flow는 대기 중인 future에서 `CancellationException`을 받으면 `future.cancel(true)`를 호출하고 취소를 다시 던집니다. 다만 broker가 이미 받은 발행을 되돌리거나 server-side 작업 종료를 증명하지는 않습니다. block-scoped close는 실패를 경고로만 남기므로 운영 종료 절차에서는 닫힘 상태와 timeout을 별도로 확인해야 합니다.

## 운영 {#operations}

client connection, producer send latency와 pending queue, consumer backlog·redelivery·unacked count, ack failure, reader lag를 관찰합니다. topic과 subscription은 제한된 metric label로 사용하고 message key나 tenant처럼 cardinality가 계속 늘어나는 값은 label에 넣지 않습니다.

재시도는 처리의 idempotency와 함께 설계합니다. shutdown 시 신규 발행·수신을 먼저 막고 in-flight 작업을 제한 시간 동안 기다린 뒤 producer, consumer, reader, client 순서로 닫습니다. 1.12.1 `with*`만으로 취소 상황의 cleanup 완료를 보장하지 않습니다.

## 테스트 {#testing}

release test는 `PulsarContainer`를 사용하는 `AbstractPulsarTest`에 의존합니다. client, producer, consumer, reader와 broker round trip을 함께 검증하므로 다른 Testcontainers suite와 순차 실행합니다.

```bash
./gradlew :bluetape4k-pulsar:test --no-configuration-cache
```

Jackson schema의 encode/decode와 clone은 broker 없이도 검증할 수 있는 핵심 계약입니다. 애플리케이션에서는 mapper 설정별 round trip unit test와 실제 broker의 schema compatibility·redelivery·subscription type integration test를 나눕니다.

## 워크숍 {#workshops}

전용 workshop은 manual manifest에 등록되어 있지 않습니다. 대신 release test가 작은 실행 예제를 제공합니다. `ProducerExtensionsTest`의 단건·DSL·Flow 발행, `ConsumerExtensionsTest`의 개별·누적 ack와 Shared 실패, `ReaderExtensionsTest`의 earliest·latest 동작을 학습 순서대로 실행해 볼 수 있습니다.

운영 예제로 발전시킬 때는 인증·TLS, schema evolution, idempotent handler, redelivery와 graceful shutdown 검증을 추가합니다. 테스트 fixture인 `AbstractPulsarTest`는 published API가 아닙니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 release commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`의 1.12.1 소스와 테스트를 기준으로 합니다. 이후 branch에는 `PulsarCloseSupport`와 취소 중 cleanup 검증이 추가되었지만 1.12.1에는 없습니다.

1.12.1은 admin API, topic·tenant provisioning, schema migration, transaction orchestration, retry·dead-letter policy, health indicator와 metrics exporter를 제공하지 않습니다. `with*` close는 취소 불가능한 context에서 실행되지 않으며 close 실패도 호출자에게 다시 던지지 않습니다.

## Source와 tests {#sources}

- [`build.gradle.kts`](../../../../infra/pulsar/build.gradle.kts)
- [`PulsarClientSupport.kt`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/PulsarClientSupport.kt)
- [`JacksonSchema.kt`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/codec/JacksonSchema.kt)
- [`Jackson3Schema.kt`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/codec/Jackson3Schema.kt)
- [`ProducerExtensions.kt`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/producer/ProducerExtensions.kt)
- [`ConsumerExtensions.kt`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/consumer/ConsumerExtensions.kt)
- [`ReaderExtensions.kt`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/reader/ReaderExtensions.kt)
- [`PulsarClientSupportTest.kt`](../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/PulsarClientSupportTest.kt)
- [`ProducerExtensionsTest.kt`](../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/producer/ProducerExtensionsTest.kt)
- [`ConsumerExtensionsTest.kt`](../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/consumer/ConsumerExtensionsTest.kt)
- [`ReaderExtensionsTest.kt`](../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/reader/ReaderExtensionsTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### pulsar 클래스 구조도

[![pulsar 클래스 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-pulsar-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-pulsar-diagram-01.svg)

_배포본 README: [`infra/pulsar/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/pulsar/README.ko.md)_

<!-- release-readme-diagrams:end -->
