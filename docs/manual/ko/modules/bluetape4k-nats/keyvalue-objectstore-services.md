---
title: KeyValue, ObjectStore와 Service API
description: JetStream 기반 KeyValue revision, ObjectStore chunk·digest, NATS Service endpoint 구성과 용도 경계를 설명합니다.
manualId: bluetape4k-nats
chapterId: keyvalue-objectstore-services
---

# KeyValue, ObjectStore와 Service API

## 세 기능의 공통점과 차이

KeyValue와 ObjectStore는 JetStream을 사용하지만 사용 목적은 다릅니다. KeyValue는 revision을 가진 작은 값을 읽고 watch하는 데 맞고, ObjectStore는 큰 byte stream을 chunk로 저장합니다. NATS Service API는 request-reply endpoint와 service metadata를 구성합니다.

이 세 API는 database transaction, S3 호환 storage, HTTP framework를 대체하지 않습니다. 요구가 맞을 때 NATS connection과 subject 체계 안에서 구성 요소 수를 줄이는 선택지입니다.

## KeyValue bucket

```kotlin
val config = keyValueConfiguration("profiles") {
    storageType(StorageType.File)
    history(10)
    replicas(3)
}

val management = connection.keyValueManagement()
management.createOrUpdate(config)
val profiles = connection.keyValue("profiles")

val revision = profiles.put("user-42.color", "blue")
profiles.update("user-42.color", "green", revision)
```

`createOrUpdate`는 먼저 update를 시도하고 bucket이 없다는 JetStream 오류일 때만 create합니다. authorization이나 network 오류는 숨기지 않습니다.

revision은 optimistic concurrency에 쓸 수 있습니다. 전체 bucket의 stream sequence를 기반으로 하므로 각 key가 독립적으로 1부터 증가한다고 가정하지 않습니다. watcher는 변경 event를 받을 뿐, 느린 consumer와 handler 실패 정책을 대신 정해 주지 않습니다.

## ObjectStore

```kotlin
val objectConfig = objectStoreConfiguration("reports") {
    storageType(StorageType.File)
}
connection.objectStoreManagement().create(objectConfig)

val store = connection.objectStore("reports")
Files.newInputStream(reportPath).use { input ->
    val meta = objectMeta("daily.csv") {
        description("daily settlement report")
        chunkSize(128 * 1024)
    }
    store.put(meta, input)
}
```

ObjectStore는 object를 chunk로 나누고 metadata와 digest를 관리합니다. 파일 system의 atomic rename, S3 lifecycle·replication·IAM과 같은 기능을 자동으로 제공하지 않습니다. 최대 object 크기, stream storage와 disk capacity를 NATS server 운영 기준에 포함합니다.

`ObjectStoreManagement.tryDelete`는 bucket이 이미 없을 때만 오류를 무시합니다. bucket 삭제는 저장 object 전체에 영향을 주므로 test cleanup이나 승인된 운영 명령에만 둡니다.

## object link와 metadata

`objectLinkOf(bucket)`은 bucket link를, `objectLinkOf(bucket, objectName)`은 특정 object link를 만듭니다. `objectMeta`와 `objectMetaOptions` DSL은 jNATS builder를 감쌉니다. 이름 blank 검증 외의 metadata 정책과 content type contract는 애플리케이션 몫입니다.

## NATS Service

Service DSL은 connection, name, version과 endpoint를 묶습니다.

```kotlin
val echoEndpoint = serviceEndpointOf {
    endpointName("echo")
    endpointSubject("tools.echo")
    handler { message ->
        message.respond(connection, message.data)
    }
}

val service = natsServiceOf(
    connection,
    name = "tool-service",
    version = "1.0.0",
    echoEndpoint,
) {
    description("Small internal tools")
}

service.startService()
```

`natsServiceOf`는 전달된 endpoint를 순서대로 builder에 추가합니다. name이나 version의 사전 검증을 별도로 넣지 않으므로 jNATS builder가 최종 계약을 결정합니다. handler concurrency, timeout, error response schema와 service stop은 호출자가 설계합니다.

## 선택 기준

- 값의 revision과 wildcard watch가 필요하면 KeyValue를 검토합니다.
- binary object를 NATS 운영 범위 안에서 전달·보관하려면 ObjectStore를 검토합니다.
- 이미 NATS request-reply를 사용하며 discoverable service metadata가 필요하면 Service API를 사용합니다.
- relational query, 대규모 object lifecycle, HTTP gateway가 핵심이면 각각의 전용 system을 우선합니다.

## Source와 examples

- [`KeyValueManagement.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/KeyValueManagement.kt)
- [`KeyValueConfiguration.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/api/KeyValueConfiguration.kt)
- [`ObjectStreamManagement.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/ObjectStreamManagement.kt)
- [`ObjectStoreConfiguration.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/api/ObjectStoreConfiguration.kt)
- [`Service.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/service/Service.kt)
- [`ServiceEndpoint.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/service/ServiceEndpoint.kt)
- [`KeyValueIntroExamples.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/KeyValueIntroExamples.kt)
- [`ObjectStoreExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/ObjectStoreExample.kt)
- [`ServiceExtensionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/service/ServiceExtensionsTest.kt)
