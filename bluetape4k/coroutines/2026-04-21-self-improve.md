# bluetape4k-coroutines Self-Improve 결과 — 2026-04-21

## 목표

**coroutines Flow 처리량 +30% 향상** — 7개 벤치마크 기하평균 ≥ 7770.83 ops/s

- 기준선: 5977.562 ops/s (`parallelFlowMapThroughput` = 16.7 ops/s 가 병목)
- 목표: 7770.83 ops/s (+30%)

---

## 결과 요약

| 지표 | 기준선 | 최종 | 변화율 |
|------|--------|------|--------|
| geomean (7 benchmarks) | 5977.562 | **7930.803** | **+32.7%** |
| parallelFlowMapThroughput | 16.7 | **101.2** | **+506%** |
| asyncFlowMapThroughput | 1368.6 | 1422.6 | +3.9% |
| mapParallelThroughput | 12974.9 | 12988.1 | +0.1% |
| concatMapEagerThroughput | 85190.6 | 88256.8 | +3.6% |

**목표 달성: ✅** (7930.803 ≥ 7770.83, 3 라운드 완료)

---

## 라운드별 진행

### Round 1 — Channel 기반 재설계 (geomean: 5977→6723, +12.5%)

**변경**: `FlowParallel` / `FlowSequential` — Resumable 핸드쉐이크 → `Channel(256)` 기반

- **FlowParallel**: N개 rail `Channel` 생성 + 라운드로빈 send
- **FlowSequential**: 공유 `out Channel(256)` + `ChannelWriter` × N

**효과**: `parallelFlowMapThroughput` 16.7 → 32.7 ops/s (+96%)

### Round 2 — AsyncFlow 원자 제거 (geomean: 6723 유지→재확인 6723)

**변경**: `AsyncFlow.LazyDeferred` — `atomic<Deferred<T>?>` 제거, `start()` 직접 `Deferred<T>` 반환

**효과**: `asyncFlowMapThroughput` 1368 → 1442 (+5.4%), geomean noise 범위

### Round 3 — per-rail 채널 + select 기반 fan-in (geomean: 6723→7930, +18.0%)

**변경**: `FlowSequential` — 공유 `out Channel` 병목 제거

**이전**:
```kotlin
val out = Channel<T>(capacity = 256)
val writers = Array<FlowCollector<T>>(n) { ChannelWriter(out) }  // 모두 같은 out → CAS 경쟁
val producer = launch { source.collect(*writers); out.close() }
for (v in out) { collector.emit(v) }
```

**이후**:
```kotlin
// rail별 독립 채널 → 경쟁 없음
val perRailChannels = Array(n) { Channel<T>(capacity = 64) }
val writers = Array<FlowCollector<T>>(n) { i -> ChannelWriter(perRailChannels[i]) }
val producer = launch { source.collect(*writers); perRailChannels.forEach { it.close() } }

// select {} 로 fair 멀티플렉싱
val activeChannels = perRailChannels.toMutableList()
while (activeChannels.isNotEmpty()) {
    select<Unit> {
        for (ch in activeChannels) {
            ch.onReceiveCatching { result ->
                if (result.isClosed) closedChannel = ch
                else { receivedValue = result.getOrThrow(); receivedValuePresent = true }
            }
        }
    }
    if (receivedValuePresent) collector.emit(receivedValue as T)
    else if (closedChannel != null) activeChannels.remove(closedChannel)
}
```

**핵심 원인**: N개 `ChannelWriter`가 모두 같은 `out` 채널로 경쟁 전송 → Channel CAS lock 직렬화.  
rail별 독립 채널로 경쟁 제거 + `select { onReceiveCatching }` 로 fair drain.

**효과**: `parallelFlowMapThroughput` 32.7 → 101.2 ops/s (+3.1x)

---

## 브랜치

- 개선 브랜치: `improve/coroutines_flow_throughput`
- 타깃: `develop`

## 최종 벤치마크 (re-benchmark on improve branch)

```
CoroutinesFlowBenchmark.asyncFlowMapThroughput       thrpt  3   1422.598 ops/s
CoroutinesFlowBenchmark.chunkedFlowThroughput        thrpt  3  24272.313 ops/s
CoroutinesFlowBenchmark.concatMapEagerThroughput     thrpt  3  88256.835 ops/s
CoroutinesFlowBenchmark.flatMapMergeThroughput       thrpt  3  13457.830 ops/s
CoroutinesFlowBenchmark.mapParallelThroughput        thrpt  3  12988.119 ops/s
CoroutinesFlowBenchmark.parallelFlowMapThroughput    thrpt  3    101.177 ops/s
CoroutinesFlowBenchmark.plainMapBaselineThroughput   thrpt  3  36616.388 ops/s
Geomean: 7930.803 ops/s
```
