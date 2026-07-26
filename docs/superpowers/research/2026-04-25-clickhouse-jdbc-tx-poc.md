# ClickHouse JDBC 0.9.5 트랜잭션 PoC 결과

- Date: 2026-04-25
- Target: ClickHouse JDBC 0.9.5 (`com.clickhouse:clickhouse-jdbc:0.9.5`)
- Container: clickhouse/clickhouse-server:25.4

## 검증 항목

### C1. raw commit ()/rollback () 동작

**결과**: autoCommit=true 상태에서 commit ()/rollback () 모두 예외 없이 반환 (no-op). ClickHouseConnectionWrapper의 no-op 구현이 타당함.

테스트:

- `raw commit does not throw exception` — PASS
- `raw rollback does not throw exception` — PASS

### C2. requiresAutoCommitOnCreateDrop

**결과**: ClickHouseDialect에서 `requiresAutoCommitOnCreateDrop = true` 설정. ClickHouseServer는 기본적으로 autoCommit=true이므로 DDL 실행에 문제 없음. ConnectionWrapper가 autoCommit을 true로 강제하므로 이 flag는 안전망 역할.

테스트:

- `autoCommit is always true in wrapper` — PASS
- `SELECT 1 works in transaction` — PASS

## 결론

- C1: **RESOLVED** — no-op 구현 유지 (`ClickHouseConnectionWrapper.commit()` / `rollback()`)
- C2: **RESOLVED** — `requiresAutoCommitOnCreateDrop = true` 유지 (ConnectionWrapper와 이중 보장)

## 관련 구현

- `ClickHouseConnectionWrapper` — commit ()/rollback () no-op, autoCommit 항상 true
- `ClickHouseDialect.requiresAutoCommitOnCreateDrop` — true
