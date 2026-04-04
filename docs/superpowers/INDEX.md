# Superpowers Spec/Plan Index

설계(Spec)와 실행계획(Plan) 전체 이력을 관리합니다.

## 상태 범례

| 아이콘 | 의미 |
|--------|------|
| ✅ | 완료 (git 커밋 확인) |
| ⏳ | 구현 대기 |
| 🔄 | 진행 중 |
| — | 해당 파일 없음 |

---

## 신규 Spec/Plan 작성 규칙

```
docs/superpowers/specs/{YYYY-MM-DD}-{기능명}-design.md   ← Spec
docs/superpowers/plans/{YYYY-MM-DD}-{기능명}-plan.md     ← Plan
```

작성 후 이 INDEX.md의 테이블에 **맨 위 행으로** 추가하고, 완료 시 git 커밋 해시를 기록합니다.

---

## 요약

| 상태      | 건수 |
|---------|----|
| ✅ 완료    | 26 |
| ⏳ 구현 대기 | 0  |
| 합계      | 26 |

---

## 전체 목록 (최신순)

| 날짜         | 기능                                         | Spec                                                                   | Plan                                                                | 상태 | 완료 커밋                                                                                                                                                                                                                                           |
|------------|--------------------------------------------|------------------------------------------------------------------------|---------------------------------------------------------------------|----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-04-04 | bluetape4k-rule-engine (DSL/MVEL/SpEL/Script) | [design](specs/2026-04-04-rule-engine-states-design.md)             | [plan](plans/2026-04-04-rule-engine-states-plan.md)                 | ✅  | [`69de83742`](https://github.com/bluetape4k/bluetape4k-projects/commit/69de83742)                                                                                                                                                               |
| 2026-04-04 | bluetape4k-states (FSM — 동기/코루틴)            | [design](specs/2026-04-04-rule-engine-states-design.md)                | [plan](plans/2026-04-04-rule-engine-states-plan.md)                 | ✅  | [`3e9be7d25`](https://github.com/bluetape4k/bluetape4k-projects/commit/3e9be7d25)                                                                                                                                                               |
| 2026-04-03 | testcontainers 리팩토링 (계약 강화/키 통일)           | [design](specs/2026-04-03-testcontainers-design.md)                    | [plan](plans/2026-04-03-testcontainers-plan.md)                     | ✅  | [`cc0d7204`](https://github.com/bluetape4k/bluetape4k-projects/commit/cc0d7204) [`e32e8c4b`](https://github.com/bluetape4k/bluetape4k-projects/commit/e32e8c4b)                                                                                 |
| 2026-04-03 | Exposed Trino 모듈                           | [design](specs/2026-04-03-exposed-trino-design.md)                     | [plan](plans/2026-04-03-exposed-trino-plan.md)                      | ✅  | [`7816a3ca`](https://github.com/bluetape4k/bluetape4k-projects/commit/7816a3ca) [`28dab07f`](https://github.com/bluetape4k/bluetape4k-projects/commit/28dab07f) [`4ce6750b`](https://github.com/bluetape4k/bluetape4k-projects/commit/4ce6750b) |
| 2026-04-02 | testcontainers 신규 Server 추가                | [design](specs/2026-04-02-testcontainers-new-servers-design.md)        | [plan](plans/2026-04-02-testcontainers-new-servers-plan.md)         | ✅  | [`3b0e5af8`](https://github.com/bluetape4k/bluetape4k-projects/commit/3b0e5af8) [`a01b688e`](https://github.com/bluetape4k/bluetape4k-projects/commit/a01b688e)                                                                                 |
| 2026-04-01 | bluetape4k-science (GIS/Shapefile/PostGIS) | [design](specs/2026-04-01-bluetape4k-science-design.md)                | [plan](plans/2026-04-01-bluetape4k-science-plan.md)                 | ✅  | [`a32d243b`](https://github.com/bluetape4k/bluetape4k-projects/commit/a32d243b)                                                                                                                                                                 |
| 2026-03-29 | Spring Data Exposed 마이그레이션                 | [design](specs/2026-03-29-spring-data-exposed-migration-design.md)     | [plan](plans/2026-03-29-spring-data-exposed-migration-plan.md)      | ✅  | [`08c6711a`](https://github.com/bluetape4k/bluetape4k-projects/commit/08c6711a)                                                                                                                                                                 |
| 2026-03-29 | Spring Data Demo 마이그레이션                    | [design](specs/2026-03-29-spring-data-demo-migration-design.md)        | [plan](plans/2026-03-29-spring-data-demo-migration-plan.md)         | ✅  | [`3b3b2729`](https://github.com/bluetape4k/bluetape4k-projects/commit/3b3b2729) [`4e339dbe`](https://github.com/bluetape4k/bluetape4k-projects/commit/4e339dbe)                                                                                 |
| 2026-03-29 | Hibernate Lettuce 마이그레이션                   | [design](specs/2026-03-29-hibernate-lettuce-migration-design.md)       | [plan](plans/2026-03-29-hibernate-lettuce-migration-plan.md)        | ✅  | [`1f0eae55`](https://github.com/bluetape4k/bluetape4k-projects/commit/1f0eae55) [`93d9d10e`](https://github.com/bluetape4k/bluetape4k-projects/commit/93d9d10e) [`1ebdaa3c`](https://github.com/bluetape4k/bluetape4k-projects/commit/1ebdaa3c) |
| 2026-03-29 | Auditable Exposed 설계                       | [design](specs/2026-03-29-auditable-exposed-design.md)                 | [plan](plans/2026-03-29-auditable-exposed-plan.md)                  | ✅  | [`207bcbca`](https://github.com/bluetape4k/bluetape4k-projects/commit/207bcbca) [`6954b380`](https://github.com/bluetape4k/bluetape4k-projects/commit/6954b380)                                                                                 |
| 2026-03-28 | PostgreSQL 모듈 통합                           | [design](specs/2026-03-28-postgresql-module-design.md)                 | [plan](plans/2026-03-28-postgresql-module-plan.md)                  | ✅  | [`06c5087f`](https://github.com/bluetape4k/bluetape4k-projects/commit/06c5087f) [`b9c0b838`](https://github.com/bluetape4k/bluetape4k-projects/commit/b9c0b838)                                                                                 |
| 2026-03-28 | Hibernate Cache Lettuce 마이그레이션             | [design](specs/2026-03-28-hibernate-cache-lettuce-migration-design.md) | [plan](plans/2026-03-28-hibernate-cache-lettuce-migration-plan.md)  | ✅  | [`de7cf96d`](https://github.com/bluetape4k/bluetape4k-projects/commit/de7cf96d) [`3a061b72`](https://github.com/bluetape4k/bluetape4k-projects/commit/3a061b72)                                                                                 |
| 2026-03-28 | Exposed MySQL8 마이그레이션                      | [design](specs/2026-03-28-exposed-mysql8-migration-design.md)          | [plan](plans/2026-03-28-exposed-mysql8-migration-plan.md)           | ✅  | [`5a9e32d7`](https://github.com/bluetape4k/bluetape4k-projects/commit/5a9e32d7) [`d7607df2`](https://github.com/bluetape4k/bluetape4k-projects/commit/d7607df2)                                                                                 |
| 2026-03-28 | Exposed inet/phone 마이그레이션                  | [design](specs/2026-03-28-exposed-inet-phone-migration-design.md)      | [plan](plans/2026-03-28-exposed-inet-phone-migration-plan.md)       | ✅  | [`dd520942`](https://github.com/bluetape4k/bluetape4k-projects/commit/dd520942)                                                                                                                                                                 |
| 2026-03-28 | Exposed BigQuery/DuckDB 마이그레이션             | [design](specs/2026-03-28-exposed-bigquery-duckdb-migration-design.md) | [plan](plans/2026-03-28-exposed-bigquery-duckdb-migration-plan.md)  | ✅  | [`d7acd494`](https://github.com/bluetape4k/bluetape4k-projects/commit/d7acd494) [`9e30f1f8`](https://github.com/bluetape4k/bluetape4k-projects/commit/9e30f1f8)                                                                                 |
| 2026-03-22 | debop4k 마이그레이션                             | [design](specs/2026-03-22-debop4k-migration-design.md)                 | [plan](plans/2026-03-22-debop4k-migration-plan.md)                  | ✅  | [`2ea5ab1c`](https://github.com/bluetape4k/bluetape4k-projects/commit/2ea5ab1c) [`694a8340`](https://github.com/bluetape4k/bluetape4k-projects/commit/694a8340)                                                                                 |
| 2026-03-20 | UUID Generator 리팩토링                        | [design](specs/2026-03-20-uuid-generator-refactoring-design.md)        | [plan](plans/2026-03-20-uuid-generator-refactoring-plan.md)         | ✅  | [`945b7444`](https://github.com/bluetape4k/bluetape4k-projects/commit/945b7444) [`db55831f`](https://github.com/bluetape4k/bluetape4k-projects/commit/db55831f)                                                                                 |
| 2026-03-20 | ULID IdGenerators 통합                       | [design](specs/2026-03-20-ulid-idgenerators-integration-design.md)     | [plan](plans/2026-03-20-ulid-idgenerators-integration-plan.md)      | ✅  | [`a40a3392`](https://github.com/bluetape4k/bluetape4k-projects/commit/a40a3392) [`cd345b11`](https://github.com/bluetape4k/bluetape4k-projects/commit/cd345b11)                                                                                 |
| 2026-03-20 | Spring Boot 4 모듈 이관                        | [design](specs/2026-03-20-spring-boot4-modules-design.md)              | [plan](plans/2026-03-20-spring-boot4-modules-plan.md)               | ✅  | [`f7618b85`](https://github.com/bluetape4k/bluetape4k-projects/commit/f7618b85) [`a722b511`](https://github.com/bluetape4k/bluetape4k-projects/commit/a722b511) [`89e35b65`](https://github.com/bluetape4k/bluetape4k-projects/commit/89e35b65) |
| 2026-03-18 | Redisson Repository 제네릭 리팩토링               | —                                                                      | [plan](plans/2026-03-18-redisson-repository-generic-refactoring.md) | ✅  | [`0f458af5`](https://github.com/bluetape4k/bluetape4k-projects/commit/0f458af5) [`7f6b0a34`](https://github.com/bluetape4k/bluetape4k-projects/commit/7f6b0a34) [`77ca32bb`](https://github.com/bluetape4k/bluetape4k-projects/commit/77ca32bb) |
| 2026-03-18 | NearCache 인터페이스 통일                         | [design](specs/2026-03-18-nearcache-unification-design.md)             | [plan](plans/2026-03-18-nearcache-unification.md)                   | ✅  | NearCacheOperations/SuspendNearCacheOperations 완료                                                                                                                                                                                               |
| 2026-03-18 | 캐시 일관성 리팩토링                                | [design](specs/2026-03-18-cache-consistency-refactoring-design.md)     | [plan](plans/2026-03-18-cache-consistency-refactoring.md)           | ✅  | [`188163b4`](https://github.com/bluetape4k/bluetape4k-projects/commit/188163b4) [`79db098f`](https://github.com/bluetape4k/bluetape4k-projects/commit/79db098f) [`d4cbd1dd`](https://github.com/bluetape4k/bluetape4k-projects/commit/d4cbd1dd) |
| 2026-03-17 | Exposed R2DBC + Lettuce                    | —                                                                      | [plan](plans/2026-03-17-exposed-r2dbc-lettuce.md)                   | ✅  | [`d3f0f542`](https://github.com/bluetape4k/bluetape4k-projects/commit/d3f0f542) [`ba6fbec2`](https://github.com/bluetape4k/bluetape4k-projects/commit/ba6fbec2)                                                                                 |
| 2026-03-17 | Exposed JDBC + Lettuce                     | —                                                                      | [plan](plans/2026-03-17-exposed-jdbc-lettuce-scenarios.md)          | ✅  | [`d842890e`](https://github.com/bluetape4k/bluetape4k-projects/commit/d842890e) [`ba6fbec2`](https://github.com/bluetape4k/bluetape4k-projects/commit/ba6fbec2) [`bd20cbc2`](https://github.com/bluetape4k/bluetape4k-projects/commit/bd20cbc2) |
| 2026-03-17 | 모듈 통합(consolidation)                       | [design](specs/2026-03-17-module-consolidation-design.md)              | [plan](plans/2026-03-17-module-consolidation.md)                    | ✅  | [`953321bf`](https://github.com/bluetape4k/bluetape4k-projects/commit/953321bf) [`a722b511`](https://github.com/bluetape4k/bluetape4k-projects/commit/a722b511)                                                                                 |
| 2026-03-13 | 전체 모듈 심층 코드 리뷰                             | [design](specs/2026-03-13-full-module-review-design.md)                | —                                                                   | ✅  | W1~W15 완료                                                                                                                                                                                                                                       |
