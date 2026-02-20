# Module bluetape4k-grpc

gRPC 서버/클라이언트 구현을 위한 Kotlin 확장 라이브러리입니다.

## 개요

`bluetape4k-grpc`는 [gRPC](https://grpc.io/) 서버와 클라이언트를 Kotlin 환경에서 쉽게 구현할 수 있도록 추상 클래스와 확장 함수를 제공합니다. 또한 Protobuf 메시지 처리를 위한 유틸리티도 포함되어 있습니다.

### 주요 기능

- **gRPC 서버 추상화**: 서버 시작/중지/상태 관리
- **gRPC 클라이언트 추상화**: 채널 관리 및 호출
- **In-process 서버/클라이언트**: 테스트용 인메모리 통신
- **인터셉터 지원**: 서버 인터셉터 보조
- **Protobuf 유틸리티**: Timestamp, Duration, Money 등 변환
- **Protobuf 직렬화**: Protobuf 기반 직렬화기

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-grpc:${version}")

    // gRPC
    implementation("io.grpc:grpc-netty-shaded:1.68.0")
    implementation("io.grpc:grpc-protobuf:1.68.0")
    implementation("io.grpc:grpc-stub:1.68.0")
}
```

## 기본 사용법

### 1. gRPC 서버 구현

```kotlin
import io.bluetape4k.grpc.AbstractGrpcServer

class MyGrpcServer(
    private val port: Int = 50051
): AbstractGrpcServer() {

    override fun start() {
        // 서버 시작 로직
        server = ServerBuilder.forPort(port)
            .addService(MyService())
            .build()
            .start()
    }

    override fun stop() {
        server?.shutdown()
    }

    override fun blockUntilShutdown() {
        server?.awaitTermination()
    }
}

// 사용
val server = MyGrpcServer(50051)
server.start()
server.blockUntilShutdown()
```

### 2. gRPC 클라이언트 구현

```kotlin
import io.bluetape4k.grpc.AbstractGrpcClient

class MyGrpcClient(
    private val host: String = "localhost",
    private val port: Int = 50051
): AbstractGrpcClient() {

    private lateinit var channel: ManagedChannel
    private lateinit var stub: MyServiceGrpc.MyServiceBlockingStub

    override fun connect() {
        channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()
        stub = MyServiceGrpc.newBlockingStub(channel)
    }

    override fun close() {
        channel.shutdown()
    }

    fun doSomething(request: Request): Response {
        return stub.doSomething(request)
    }
}
```

### 3. In-process 서버/클라이언트 (테스트용)

```kotlin
import io.bluetape4k.grpc.inprocess.AbstractGrpcInprocessServer
import io.bluetape4k.grpc.inprocess.AbstractGrpcInprocessClient

// 테스트용 서버
class TestGrpcServer: AbstractGrpcInprocessServer("test-server") {
    override fun start() {
        server = InProcessServerBuilder.forName(serverName)
            .addService(MyService())
            .build()
            .start()
    }
}

// 테스트용 클라이언트
class TestGrpcClient: AbstractGrpcInprocessClient("test-server") {
    override fun connect() {
        channel = InProcessChannelBuilder.forName(serverName).build()
        stub = MyServiceGrpc.newBlockingStub(channel)
    }
}
```

### 4. Protobuf 유틸리티

```kotlin
import io.bluetape4k.protobuf.*

// Timestamp 변환
val timestamp = now().toTimestamp()
val instant = timestamp.toInstant()

// Duration 변환
val duration = 5.minutes().toDuration()
val kotlinDuration = duration.toKotlinDuration()

// Money 변환 (Google Money 프로토콜)
val money = Money.newBuilder()
    .setCurrencyCode("KRW")
    .setUnits(10000)
    .build()
val decimal = money.toBigDecimal()
```

## 주요 파일/클래스 목록

### gRPC Core

| 파일                         | 설명                |
|----------------------------|-------------------|
| `GrpcServer.kt`            | gRPC 서버 인터페이스     |
| `AbstractGrpcServer.kt`    | gRPC 서버 추상 클래스    |
| `AbstractGrpcClient.kt`    | gRPC 클라이언트 추상 클래스 |
| `ServerSupport.kt`         | 서버 확장 함수          |
| `ManagedChannelSupport.kt` | 채널 확장 함수          |

### In-process (inprocess/)

| 파일                               | 설명         |
|----------------------------------|------------|
| `AbstractGrpcInprocessServer.kt` | 인메모리 서버    |
| `AbstractGrpcInprocessClient.kt` | 인메모리 클라이언트 |

### Interceptor (interceptor/)

| 파일                            | 설명         |
|-------------------------------|------------|
| `ServerInterceptorSupport.kt` | 서버 인터셉터 확장 |

### Protobuf (protobuf/)

| 파일                                  | 설명            |
|-------------------------------------|---------------|
| `TimestampSupport.kt`               | Timestamp 변환  |
| `DurationSupport.kt`                | Duration 변환   |
| `DateTimeSupport.kt`                | 날짜/시간 변환      |
| `MoneySupport.kt`                   | Money 변환      |
| `MessageSupport.kt`                 | 메시지 유틸리티      |
| `serializers/ProtobufSerializer.kt` | Protobuf 직렬화기 |

## 테스트

```bash
./gradlew :bluetape4k-grpc:test
```

## 참고

- [gRPC](https://grpc.io/)
- [gRPC Kotlin](https://grpc.io/docs/languages/kotlin/)
- [Protocol Buffers](https://protobuf.dev/)
