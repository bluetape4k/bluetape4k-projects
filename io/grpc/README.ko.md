# Module bluetape4k-grpc

[English](./README.md) | 한국어

gRPC 서버/클라이언트 구현을 위한 Kotlin 확장 라이브러리입니다.

## 개요

`bluetape4k-grpc`는 [gRPC](https://grpc.io/) 서버와 클라이언트를 Kotlin 환경에서 쉽게 구현할 수 있도록 추상 클래스와 확장 함수를 제공합니다. Protobuf 유틸리티는 [
`bluetape4k-protobuf`](../protobuf/README.ko.md) 모듈로 분리되었습니다.

## 아키텍처

### 클래스 계층

```mermaid
classDiagram
    class GrpcServer {
        <<interface>>
        +start()
        +stop()
        +blockUntilShutdown()
    }

    class AbstractGrpcServer {
        #server: Server?
        +start()
        +stop()
        +blockUntilShutdown()
    }

    class AbstractGrpcClient {
        #channel: ManagedChannel?
        +connect()
        +close()
    }

    class AbstractGrpcInprocessServer {
        #serverName: String
        +start()
        +stop()
    }

    class AbstractGrpcInprocessClient {
        #serverName: String
        +connect()
        +close()
    }

    GrpcServer <|.. AbstractGrpcServer
    AbstractGrpcServer <|-- AbstractGrpcInprocessServer
    AbstractGrpcClient <|-- AbstractGrpcInprocessClient

    style GrpcServer fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style AbstractGrpcServer fill:#1976D2,stroke:#1565C0,color:#FFFFFF
    style AbstractGrpcClient fill:#1976D2,stroke:#1565C0,color:#FFFFFF
    style AbstractGrpcInprocessServer fill:#00897B,stroke:#00695C,color:#FFFFFF
    style AbstractGrpcInprocessClient fill:#00897B,stroke:#00695C,color:#FFFFFF
```

### 컴포넌트 개요

```mermaid
flowchart TD
    subgraph bluetape4k-grpc
        subgraph Server["서버 측"]
            GS[GrpcServer 인터페이스]
            AGS[AbstractGrpcServer]
            AGIS[AbstractGrpcInprocessServer]
            SI[ServerInterceptorSupport]
            SS[ServerSupport]
        end

        subgraph Client["클라이언트 측"]
            AGC[AbstractGrpcClient]
            AGIC[AbstractGrpcInprocessClient]
            MCS[ManagedChannelSupport]
        end
    end

    subgraph External["외부 / gRPC 런타임"]
        SB[ServerBuilder]
        MCB[ManagedChannelBuilder]
        IPSB[InProcessServerBuilder]
        IPCB[InProcessChannelBuilder]
    end

    GS <|.. AGS
    AGS <|-- AGIS
    AGC <|-- AGIC
    AGS --> SB
    AGIS --> IPSB
    AGC --> MCB
    AGIC --> IPCB

    classDef coreStyle fill:#1B5E20,stroke:#1B5E20,color:#FFFFFF,font-weight:bold
    classDef serviceStyle fill:#1565C0,stroke:#1565C0,color:#FFFFFF
    classDef utilStyle fill:#E65100,stroke:#E65100,color:#FFFFFF
    classDef extStyle fill:#37474F,stroke:#37474F,color:#FFFFFF

    class GS serviceStyle
    class AGS,AGC serviceStyle
    class AGIS,AGIC coreStyle
    class SB,MCB,IPSB,IPCB extStyle
```

### gRPC 서버-클라이언트 통신 시퀀스

```mermaid
sequenceDiagram
    box rgb(232, 245, 233) Client
        participant C as GrpcClient
        participant CH as ManagedChannel
    end
    box rgb(227, 242, 253) Server
        participant S as GrpcServer
        participant SVC as ServiceImpl
    end

    C->>CH: ManagedChannelBuilder.forAddress(host, port)
    CH->>S: TCP 연결 수립
    S->>SVC: 서비스 등록

    C->>CH: stub.doSomething(request)
    CH->>S: HTTP/2 요청 전송
    S->>SVC: 메서드 호출
    SVC-->>S: 응답 생성
    S-->>CH: HTTP/2 응답
    CH-->>C: Response 반환

    C->>CH: channel.shutdown()
    CH->>S: 연결 종료
```

### In-process 테스트 시퀀스

```mermaid
sequenceDiagram
    box rgb(232, 245, 233) 테스트
        participant T as 테스트 코드
    end
    box rgb(227, 242, 253) In-process 서버
        participant IS as InprocessServer
        participant SVC as ServiceImpl
    end
    box rgb(237, 231, 246) In-process 클라이언트
        participant IC as InprocessClient
    end

    T->>IS: InProcessServerBuilder.forName("test-server")
    IS->>SVC: 서비스 등록 및 시작
    T->>IC: InProcessChannelBuilder.forName("test-server")
    IC->>IS: 인메모리 채널 연결

    T->>IC: stub.call(request)
    IC->>IS: 인메모리 전송 (네트워크 없음)
    IS->>SVC: 메서드 호출
    SVC-->>IC: 응답
    IC-->>T: Response 반환

    T->>IS: server.shutdown()
    T->>IC: channel.shutdown()
```

## 주요 기능

- **gRPC 서버 추상화**: 서버 시작/중지/상태 관리
- **gRPC 클라이언트 추상화**: 채널 관리 및 호출
- **In-process 서버/클라이언트**: 테스트용 인메모리 통신
- **인터셉터 지원**: 서버 인터셉터 보조
- **입력 검증**: host/target/name 은 blank를 허용하지 않고 port 는 `1..65535` 범위를 즉시 검증

## 사용 예시

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

## 관련 모듈

- **[bluetape4k-protobuf](../protobuf/README.ko.md)**: Protobuf 유틸리티 (Timestamp/Duration/Money 변환, ProtobufSerializer)

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-grpc:${version}")
    // bluetape4k-protobuf가 전이적으로 포함됩니다
}
```

## 테스트

```bash
./gradlew :bluetape4k-grpc:test
```

## 참고

- [gRPC](https://grpc.io/)
- [gRPC Kotlin](https://grpc.io/docs/languages/kotlin/)
- [Protocol Buffers](https://protobuf.dev/)
