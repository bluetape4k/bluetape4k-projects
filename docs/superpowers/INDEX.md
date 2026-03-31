# Superpowers Spec/Plan Index

설계(Spec)와 실행계획(Plan) 전체 이력을 관리합니다.

## 상태 범례

| 아이콘 | 의미 |
|--------|------|
| ✅ | 완료 |
| ⏳ | 구현 대기 |
| 🔄 | 진행 중 |
| ❓ | 상태 미확인 |
| — | 해당 파일 없음 |

---

## 전체 목록

| 날짜 | 기능 | Spec | Plan | 상태 | 비고 |
|------|------|------|------|------|------|
| 2026-03-13 | 전체 모듈 심층 코드 리뷰 | [design](specs/2026-03-13-full-module-review-design.md) | — | ✅ | W1~W15 전체 완료 |
| 2026-03-17 | 모듈 통합(consolidation) | [design](specs/2026-03-17-module-consolidation-design.md) | [plan](plans/2026-03-17-module-consolidation.md) | ✅ | |
| 2026-03-17 | Exposed JDBC + Lettuce 시나리오 | — | [plan](plans/2026-03-17-exposed-jdbc-lettuce-scenarios.md) | ❓ | Spec 없음 |
| 2026-03-17 | Exposed R2DBC + Lettuce | — | [plan](plans/2026-03-17-exposed-r2dbc-lettuce.md) | ❓ | Spec 없음 |
| 2026-03-18 | 캐시 일관성 리팩토링 | [design](specs/2026-03-18-cache-consistency-refactoring-design.md) | [plan](plans/2026-03-18-cache-consistency-refactoring.md) | ❓ | |
| 2026-03-18 | NearCache 인터페이스 통일 | [design](specs/2026-03-18-nearcache-unification-design.md) | [plan](plans/2026-03-18-nearcache-unification.md) | ✅ | NearCacheOperations/SuspendNearCacheOperations 완료 |
| 2026-03-18 | Redisson Repository 제네릭 리팩토링 | — | [plan](plans/2026-03-18-redisson-repository-generic-refactoring.md) | ❓ | Spec 없음 |
| 2026-03-20 | Spring Boot 4 모듈 이관 | [design](specs/2026-03-20-spring-boot4-modules-design.md) | [plan](plans/2026-03-20-spring-boot4-modules-plan.md) | ✅ | |
| 2026-03-20 | ULID IdGenerators 통합 | [design](specs/2026-03-20-ulid-idgenerators-integration-design.md) | [plan](plans/2026-03-20-ulid-idgenerators-integration-plan.md) | ❓ | |
| 2026-03-20 | UUID Generator 리팩토링 | [design](specs/2026-03-20-uuid-generator-refactoring-design.md) | [plan](plans/2026-03-20-uuid-generator-refactoring-plan.md) | ✅ | Uuid V1/V4/V5/V6/V7 교체 완료 |
| 2026-03-22 | debop4k 마이그레이션 | [design](specs/2026-03-22-debop4k-migration-design.md) | [plan](plans/2026-03-22-debop4k-migration-plan.md) | ❓ | |
| 2026-03-28 | Exposed BigQuery/DuckDB 마이그레이션 | [design](specs/2026-03-28-exposed-bigquery-duckdb-migration-design.md) | [plan](plans/2026-03-28-exposed-bigquery-duckdb-migration-plan.md) | ❓ | |
| 2026-03-28 | Exposed inet/phone 마이그레이션 | [design](specs/2026-03-28-exposed-inet-phone-migration-design.md) | [plan](plans/2026-03-28-exposed-inet-phone-migration-plan.md) | ❓ | |
| 2026-03-28 | Exposed MySQL8 마이그레이션 | [design](specs/2026-03-28-exposed-mysql8-migration-design.md) | [plan](plans/2026-03-28-exposed-mysql8-migration-plan.md) | ❓ | |
| 2026-03-28 | Hibernate Cache Lettuce 마이그레이션 | [design](specs/2026-03-28-hibernate-cache-lettuce-migration-design.md) | [plan](plans/2026-03-28-hibernate-cache-lettuce-migration-plan.md) | ✅ | bluetape4k-hibernate-cache-lettuce 완료 |
| 2026-03-28 | PostgreSQL 모듈 통합 | [design](specs/2026-03-28-postgresql-module-design.md) | [plan](plans/2026-03-28-postgresql-module-plan.md) | ✅ | postgis/pgvector/tsrange 단일 모듈 완료 |
| 2026-03-29 | Auditable Exposed 설계 | [design](specs/2026-03-29-auditable-exposed-design.md) | [plan](plans/2026-03-29-auditable-exposed-plan.md) | ⏳ | 스펙+플랜 완료, 구현 대기 |
| 2026-03-29 | Hibernate Lettuce 마이그레이션 | [design](specs/2026-03-29-hibernate-lettuce-migration-design.md) | [plan](plans/2026-03-29-hibernate-lettuce-migration-plan.md) | ✅ | spring-boot3/4 hibernate-lettuce 8개 모듈 완료 |
| 2026-03-29 | Spring Data Demo 마이그레이션 | [design](specs/2026-03-29-spring-data-demo-migration-design.md) | [plan](plans/2026-03-29-spring-data-demo-migration-plan.md) | ✅ | exposed-jdbc/r2dbc demo 완료 |
| 2026-03-29 | Spring Data Exposed 마이그레이션 | [design](specs/2026-03-29-spring-data-exposed-migration-design.md) | [plan](plans/2026-03-29-spring-data-exposed-migration-plan.md) | ✅ | exposed-jdbc/r2dbc repository 완료 |

---

## 요약

| 상태 | 건수 |
|------|------|
| ✅ 완료 | 10 |
| ⏳ 구현 대기 | 1 |
| ❓ 상태 미확인 | 9 |
| 합계 | 20 |

---

## 신규 Spec/Plan 작성 규칙

```
docs/superpowers/specs/{YYYY-MM-DD}-{기능명}-design.md   ← Spec
docs/superpowers/plans/{YYYY-MM-DD}-{기능명}-plan.md     ← Plan
```

작성 후 이 INDEX.md의 테이블에 행을 추가하고 상태를 갱신합니다.
