# README 병렬 최신화 설계

**날짜**: 2026-04-04  
**작성자**: debop  
**목표**: 모든 모듈 README.md를 claude-session-driver 워커로 병렬 최신화

---

## 범위

- **포함**: `bluetape4k/`, `data/`, `infra/`, `io/`, `aws/`, `spring-boot3/`, `spring-boot4/`, `virtualthread/` 하위 모듈
- **제외**: `x-obsoleted/`, `build/`, 테스트 리소스 내 README, `examples/`, `guides/`

---

## 접근법: 그룹 배치 병렬 (12 워커)

각 워커는 다음 절차로 README를 최신화한다:

1. `git log -- <module>/` 로 README 마지막 수정 이후 커밋 파악
2. 변경된 파일/클래스/API 확인
3. README에 누락·오래된 내용만 반영 (전면 재작성 금지)
4. KDoc은 한국어, 사용 예시는 현행 API 기준 유지

---

## 워커 그룹 배정

| 워커  | 담당 모듈                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|-----|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| W1  | `bluetape4k/core`, `bluetape4k/coroutines`, `bluetape4k/logging`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| W2  | `data/exposed-core`, `data/exposed-dao`, `data/exposed-jdbc`, `data/exposed-r2dbc`, `data/exposed`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| W3  | `data/exposed-jdbc-lettuce`, `data/exposed-r2dbc-lettuce`, `data/exposed-jdbc-redisson`, `data/exposed-r2dbc-redisson`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| W4  | `data/exposed-postgresql`, `data/exposed-mysql8`, `data/exposed-duckdb`, `data/exposed-trino`, `data/exposed-bigquery`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| W5  | `data/exposed-jackson2`, `data/exposed-jackson3`, `data/exposed-fastjson2`, `data/exposed-jasypt`, `data/exposed-tink`, `data/exposed-measured`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| W6  | `data/hibernate`, `data/hibernate-reactive`, `data/hibernate-cache-lettuce`, `data/cassandra`, `data/jdbc`, `data/mongodb`, `data/r2dbc`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| W7  | `infra/lettuce`, `infra/redisson`, `infra/redis`, `infra/cache`, `infra/cache-core`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| W8  | `infra/cache-hazelcast`, `infra/cache-redisson`, `infra/cache-lettuce`, `infra/kafka`, `infra/bucket4j`, `infra/resilience4j`, `infra/micrometer`, `infra/opentelemetry`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| W9  | `io/io`, `io/okio`, `io/jackson2`, `io/jackson3`, `io/fastjson2`, `io/json`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| W10 | `io/feign`, `io/retrofit2`, `io/grpc`, `io/protobuf`, `io/avro`, `io/netty`, `io/http`, `io/csv`, `io/tink`, `io/crypto`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| W11 | `aws/aws`, `aws/aws-kotlin`, `virtualthread/api`, `virtualthread/jdk21`, `virtualthread`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| W12 | `spring-boot3/core`, `spring-boot3/redis`, `spring-boot3/r2dbc`, `spring-boot3/mongodb`, `spring-boot3/cassandra`, `spring-boot3/exposed-jdbc`, `spring-boot3/exposed-r2dbc`, `spring-boot3/hibernate-lettuce`, `spring-boot3/exposed-jdbc-demo`, `spring-boot3/exposed-r2dbc-demo`, `spring-boot3/cassandra-demo`, `spring-boot3/hibernate-lettuce-demo`, `spring-boot4/core`, `spring-boot4/redis`, `spring-boot4/r2dbc`, `spring-boot4/mongodb`, `spring-boot4/cassandra`, `spring-boot4/exposed-jdbc`, `spring-boot4/exposed-r2dbc`, `spring-boot4/hibernate-lettuce`, `spring-boot4/exposed-jdbc-demo`, `spring-boot4/exposed-r2dbc-demo`, `spring-boot4/cassandra-demo`, `spring-boot4/hibernate-lettuce-demo` |

---

## 성공 기준

- 각 모듈 README가 현재 코드 구조와 일치
- git history 기반으로 신규 클래스/함수/설정이 반영됨
- 기존 스타일(한국어 설명, 코드 예시) 유지
- 12개 워커 모두 stop 이벤트 수신 후 완료 확인

---

## 제약

- SCRIPTS 경로: `/Users/debop/.claude/plugins/cache/superpowers-marketplace/claude-session-driver/1.0.1/scripts`
- 프로젝트 경로: `/Users/debop/work/bluetape4k/bluetape4k-projects`
- tmux + jq 필요 (설치 확인됨)
