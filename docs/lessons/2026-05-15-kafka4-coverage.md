# Lessons Learned — kafka4 coverage (2026-05-15)

**관련 PR**: #464
**영향 모듈**: `:bluetape4k-kafka4` (`infra/kafka4`)

## L1: Kafka 4의 StreamPartitioner는 Optional<Set<Integer>>를 반환한다

### 문제
`StreamPartitioner` 람다에서 `Int`를 반환하는 코드를 작성했더니 컴파일 에러 발생.
Kafka 4에서 `StreamPartitioner.partitions()` 반환 타입이 `Optional<Set<Integer>>`로 변경되었다.

### 교훈
Kafka Streams kstream DSL 테스트 작성 시, `StreamPartitioner` 람다는
`Optional.of(setOf(partition))` 형태로 반환해야 한다.

```kotlin
val partitioner = StreamPartitioner<String, String> { _, key, _, numPartitions ->
    Optional.of(setOf(Math.abs(key.hashCode()) % numPartitions))
}
```

---

## L2: KafkaOperationExtensions.kt의 함수는 통합 테스트가 필요하다

### 문제
`suspendSend`, `sendFlowAsParallel`, `sendAndForget` 등 `suspend inline` 확장 함수들은
MockK로 단위 테스트하기 어렵다. `KafkaOperations.execute()` 내부에서
콜백 방식으로 동작하기 때문이다.

### 교훈
`suspendCancellableCoroutine` + `execute { producer -> producer.send(...) { ... } }` 패턴의
함수는 반드시 실제 Kafka 브로커(Testcontainers)를 이용한 통합 테스트로 커버해야 한다.
`KafkaOperationsExtensionsTest` 패턴(AbstractKafkaTest + KafkaServer.Launcher.Spring.getStringProducerFactory)을 재사용한다.

---

## L3: 커버리지 목표 달성에는 낮은 패키지 먼저 파악 후 우선순위 선정이 효율적

### 문제
커버리지 보고서의 전체 수치만 보면 어느 파일이 병목인지 알기 어렵다.

### 교훈
`koverXmlReport` 생성 후 Python으로 XML 파싱하여 패키지/파일별 미커버 라인 수를
내림차순으로 정렬하면, 최소한의 테스트로 최대 커버리지 향상을 달성할 수 있다.

```python
import xml.etree.ElementTree as ET
tree = ET.parse("build/reports/kover/report.xml")
# counter[@type='LINE'] per sourcefile 추출 후 missed 내림차순 정렬
```

---

## L4: MCP GitHub 플러그인은 private repo에서 404를 반환한다

### 문제
`mcp__plugin_github_github__create_pull_request` 도구가 private repo에서 404 에러 반환.

### 교훈
bluetape4k-projects는 private repo이므로 모든 GitHub 작업은 `gh` CLI를 사용해야 한다.
`gh pr create`, `gh pr view`, `gh issue create` 등을 활용한다.
