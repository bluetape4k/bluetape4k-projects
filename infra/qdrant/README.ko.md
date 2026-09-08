# bluetape4k-qdrant

공식 Qdrant Java client의 protobuf 요청을 유지하면서 suspend 조회·쓰기와 cold Flow를 제공합니다.

## 사용

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-qdrant")
}
```

버전은 bluetape4k BOM에서 관리합니다. 클라이언트·gRPC 채널·인증·TLS·최대 수신 메시지 크기는 호출자가 설정하고 종료합니다.

```kotlin
val request = QueryPoints.newBuilder()
    .setCollectionName("documents")
    .setQuery(QueryFactory.nearest(0.1f, 0.2f, 0.3f))
    .setLimit(10)
    .build()
val matches = client.querySuspending(request, Duration.ofSeconds(3))

client.scrollAsFlow(
    ScrollPoints.newBuilder().setCollectionName("documents").setLimit(100).build()
).take(200).collect { point -> process(point) }

val template = UpsertPoints.newBuilder().setCollectionName("documents").setWait(true).build()
client.upsertBatches(pointsFlow, template, maxBatchItems = 256, maxBatchBytes = 4 * 1024 * 1024)
    .collect { result -> record(result) }
```

`QueryFactory`, `ConditionFactory`, `ValueFactory`, `VectorsFactory` 등 공식 SDK 빌더를 그대로 사용합니다. delete는 `DeletePoints`를 받는 `deleteSuspending`으로 ID·필터 선택과 옵션을 보존합니다.

포인트 ID는 `PointIdFactory.id(1L)` 또는 `PointIdFactory.id(uuid)`로 구분합니다. 벡터 길이는 collection 생성 시의 차원과 일치해야 합니다. 이름 있는 벡터는 `VectorsFactory.namedVectors(mapOf("embedding" to VectorFactory.vector(1f, 0f, 0f)))`로 저장하고 query의 `setUsing("embedding")`으로 선택합니다. 차원·이름 불일치 오류는 서버 응답 그대로 전파합니다. 일관성·쓰기 순서·wait 옵션도 원본 protobuf 요청에서 지정합니다.

## 실행 계약

| API | 동작 |
| --- | --- |
| `querySuspending`, `upsertSuspending`, `deleteSuspending` | Future 완료를 콜백으로 기다리고 원래 실패 원인을 전파 |
| `scrollAsFlow` | 수집마다 독립 실행, 페이지당 limit 1..1000, 한 페이지 소비 뒤 다음 요청 |
| `upsertBatches` | 배치당 최대 항목 수와 전체 protobuf 요청 바이트 상한, 동시 요청 1개 |

RPC별 `Duration`은 SDK에 그대로 전달합니다. 전체 수집 시간은 `withTimeout`으로 제한할 수 있습니다. 취소하면 대기 중인 Future도 취소하지만 이미 서버에 반영된 쓰기를 되돌리지는 않습니다. 클라이언트와 채널은 자동 종료하지 않습니다.

scroll의 기본 `maxPages`는 10000입니다. 같은 cursor가 즉시 반복되거나 다음 페이지가 남은 상태에서 상한에 도달하면 실패합니다. 모든 cursor를 저장하지 않습니다. 페이지 하나의 payload 크기는 채널 수신 상한으로 제한해야 합니다.

배치 template에는 포인트가 없어야 합니다. 단일 포인트가 바이트 상한을 넘으면 그 항목의 요청 전에 거부합니다. 성공한 배치 응답은 이미 소비자에게 전달되므로 후속 오류 시 부분 성공을 고려해야 합니다. 자동 재시도·롤백은 없습니다. 입력 오류나 취소 시 남은 배치는 보내지 않습니다. 소비자가 느리면 다음 배치 전송도 기다립니다.

인가에 필요한 tenant 필터는 호출자가 요청에 넣어야 합니다. 로그에는 동작과 실패 상태만 남기며 payload·벡터·토큰·SDK 오류 원문을 기록하지 않습니다.

## 검증

```bash
./gradlew :bluetape4k-qdrant:test -PexcludeIntegrationTests=true
./gradlew :bluetape4k-qdrant:test
```

두 번째 명령은 Docker에서 Qdrant 1.19.0을 실행하여 저장, tenant 필터 검색, 페이지 조회, 삭제를 검증합니다. 테스트별 collection과 client는 종료 시 정리합니다. 개발 중 중앙 카탈로그 변경이 병합되기 전에는 `-Pbluetape4kDependenciesCatalogPath=<dependencies-worktree>/gradle/libs.versions.toml`이 필요합니다.
