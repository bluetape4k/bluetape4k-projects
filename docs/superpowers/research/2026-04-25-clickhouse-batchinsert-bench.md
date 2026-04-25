# ClickHouse BatchInsert 성능 측정 (T13)

- **Date**: 2026-04-25
- **Target**: ClickHouse JDBC 0.9.5 batchInsert via Exposed 1.2
- **Container**: clickhouse/clickhouse-server:25.4
- **Decision**: C7 — Exposed 기본 batchInsert 사용 확정

## 측정 목표

100,000행 대량 삽입 성능 검증:
- 반복: 3회
- 임계치: 10,000행당 500ms 이내 (avg 5ms/row)

## 측정 방법

```kotlin
val durations = (1..3).map {
  transaction {
    Events.batchInsert(events) { ... }  // 100K rows
  }
}
avg_ms_per_10k = avg(durations) * 10_000 / 100_000
```

## 결과 (실행 후 기록)

(테스트 실행 시 아래 항목 채우기)

| Round | Duration (ms) | Note |
|-------|---------------|------|
| 1     | -             |      |
| 2     | -             |      |
| 3     | -             |      |
| **평균** | **-** | **-ms/10K** |

## 결론

- ✅ 임계치 이내 → **C7: Exposed 기본 batchInsert 확정**
- ❌ 임계치 초과 → 후속 이슈 등록 (배치 크기 최적화 / 커넥션 풀 튜닝)
