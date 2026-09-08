# OpenFGA·Qdrant 공용 테스트 서버 설계

## 목적과 실행 범위

사용자의 “새로 추가된 이슈 검토해서 Testcontainers 용 Server 구현해서 사용” 요청에 따라 #1722, #1723을 구현한다. 기존 `feat/measured-infra-1712-1717` 작업 트리와 PR #1721에서 공용 서버 구현을 먼저 검증하고 infra 테스트가 사용하도록 변경한다. 발행·병합은 이번 실행 범위에 포함하지 않는다.

## 근거와 선택

현재 Spring Boot BOM은 Testcontainers 2.0.5를 관리한다. 공식 `org.testcontainers:testcontainers-openfga`, `org.testcontainers:testcontainers-qdrant`는 BOM 관리 대상이다. 버전 없는 로컬 alias와 compileOnly 의존성을 추가하고 사용하는 infra 테스트는 해당 공식 모듈을 testImplementation으로 선언한다.

- 공식 컨테이너 상속: 이미지 호환성 및 프로토콜 readiness를 재사용하므로 채택한다.
- GenericContainer 직접 구현: 동일한 readiness·명령을 중복 관리하므로 채택하지 않는다.
- infra 전용 fixture 유지: 다른 모듈이 재사용할 수 없으므로 채택하지 않는다.

근거: `ConsulServer`, `ChromaDBServer`, `PropertyExportingServer`, 기존 OpenFgaIntegrationTest/QdrantIntegrationTest.
공식 소스: https://github.com/testcontainers/testcontainers-java/tree/2.0.5/modules/openfga 및 https://github.com/testcontainers/testcontainers-java/tree/2.0.5/modules/qdrant

## API와 수명

`infra.OpenFgaServer`는 OpenFGAContainer, `storage.QdrantServer`는 QdrantContainer를 상속하고 GenericServer와 PropertyExportingServer를 구현한다. 이미지 기본값은 기존 통합 테스트의 `openfga/openfga:v1.8.2`, `qdrant/qdrant:v1.19.0`를 보존한다. 빈 이미지·태그는 즉시 거부한다.

기본값은 임의 호스트 포트, `reuse=false`이다. `useDefaultPort=true`는 HTTP·gRPC 표준 포트를 바인딩한다. `port`는 HTTP 포트이며 `url`은 실제 host와 mapped HTTP 포트이다. HTTP·gRPC 포트와 endpoint는 시작 후에만 읽는다. 속성 namespace는 openfga/qdrant, 키는 host/port/url/http-port/grpc-port이다. 공식 상위 클래스 endpoint와 중복되는 프로퍼티는 다시 선언하지 않는다.

HTTP readiness는 공식 모듈의 OpenFGA `/healthz` SERVING 검사, Qdrant `/readyz` 검사를 보존한다. 시작 제한은 2분이다. 별도 polling/retry 루프를 추가하지 않는다. image-gate 대표 workload에서 mapped gRPC 포트에 TCP 연결하고, Qdrant SDK gRPC 작업과 OpenFGA HTTP 작업을 검증한다. TCP 연결은 OpenFGA gRPC RPC 의미 검증을 대체하지 않으며 이번 HTTP SDK 소비 범위에서는 endpoint 도달성만 보장한다. OpenFGA의 불필요한 playground 포트는 `setExposedPorts(listOf(8080, 8081))`로 기본 목록을 교체하여 노출하지 않는다.

명시적 인스턴스는 호출자가 use/close로 종료한다. Launcher의 lazy singleton은 ShutdownQueue가 소유하고 테스트가 직접 종료하지 않는다. start는 기존 패턴대로 JVM 속성을 export한다. stop이 전역 속성을 자동 복원하지 않는 기존 계약을 문서화하며 테스트는 이전 속성을 보존·복원한다.

## 실패 모드와 완화

1. localhost·표준 포트 고정으로 원격 Docker 접속 실패: host/getMappedPort만 사용하고 실제 endpoint를 검사한다.
2. 시작 전 endpoint 조회 또는 readiness 실패: Testcontainers 예외를 보존하고 제한 시간 안에 실패한다.
3. 공유 Launcher 종료나 데이터 충돌: 호출자 종료 금지, 고유 store/collection 생성 및 finally 정리.
4. compileOnly 모듈 누락으로 소비자 링크 실패: infra 테스트에 공식 컨테이너 의존성을 명시한다. 공용 Server는 infra SDK 모듈에 의존하지 않는다.
5. 실제 네트워크 작업을 runTest로 실행: 소비자 통합 테스트를 runSuspendIO로 변경한다.

## 수용 기준

- AC1: 시작 전 설정·빈 입력·기본 비재사용·표준 포트 옵션을 검증한다.
- AC2: 실제 start, mapped HTTP/gRPC endpoint, 준비 상태, 속성 export, stop을 검증한다.
- AC3: Launcher 동일 인스턴스와 실행 상태를 검증한다.
- AC4: OpenFGA store/model/tuple/check allow·deny 및 Qdrant collection/upsert/query/filter/delete를 실서버로 검증한다.
- AC5: 기존 infra 통합 테스트가 공용 Launcher를 사용하고 직접 GenericContainer를 제거한다. 기존 pagination, named vector, UUID ID, dimension 오류 시나리오는 보존한다.
- AC6: 한국어 KDoc, README 양 언어, 이미지 게이트 manifest와 저장소 계약 검사를 갱신한다. family 53개, release-required 49개, 4-shard [14, 13, 13, 13], README 양 언어 태그·개수, Nightly infra wildcard 및 storage-search QdrantServerTest 등록을 검증한다. 소비자는 공식 컨테이너 모듈을 testImplementation으로 함께 선언해야 한다는 예시를 제공한다.
- AC7: 관련 테스트·detekt·이미지 게이트 정적 검증과 독립 리뷰를 통과한다. 컨테이너 실행은 전체 세션에서 순차로 수행한다.

## DoD

설계 근거 확인 완료. 구현·실서버 테스트·문서·독립 리뷰는 계획에서 추적한다. 발행과 병합은 미수행 상태로 유지한다.

## 복구

Server, 공식 컨테이너 의존성, 소비자 adoption, README와 image-gate/Nightly 등록을 함께 되돌린다. 기존 GenericContainer fixture는 직전 head에서 복원할 수 있다.

## 사용 예시

```kotlin
OpenFgaServer().use { server ->
    server.start()
    val httpUrl = server.url
}
val shared = QdrantServer.Launcher.qdrant
val grpcPort = shared.grpcPort // shared는 ShutdownQueue가 종료한다.
```

인증 없는 테스트 전용 설정이며 신뢰할 수 있는 Docker 호스트에서만 사용한다. Docker의 host binding 정책을 따르므로 임의 포트도 loopback 전용을 의미하지 않는다. 인증 설정과 secret export는 추가하지 않는다.
