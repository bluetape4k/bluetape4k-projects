# WIKI 스키마

LLM이 이 파일의 규칙에 따라 `.wiki/pages/`를 유지한다.

## 원본 소스 경로

- Specs: `docs/superpowers/specs/`
- Plans: `docs/superpowers/plans/`
- Git log: `.wiki/raw/gitlog/version-decisions.md`
- 임의 자료: `.wiki/raw/articles/`, `.wiki/raw/notes/`

## 페이지 목록 & 주제 매핑

| 페이지 | 담당 주제 | 흡수하는 specs 키워드 |
|--------|---------|-------------------|
| `exposed-patterns.md` | Exposed ORM 패턴 전반 | exposed-core, exposed-dao, exposed-jdbc, exposed-r2dbc, exposed-redis-api, exposed-cache |
| `cache-architecture.md` | NearCache 설계 결정 | nearcache-unification, cache-consistency, redisson-repository |
| `spring-boot-integration.md` | Spring Boot 3/4 통합 | spring-boot3, spring-boot4, hibernate-lettuce, spring-data |
| `kotlin-testing-patterns.md` | 테스트 패턴, KDoc, Testcontainers | kdoc-examples, testcontainers, junit5 |
| `module-decisions.md` | 모듈 통합/분리 결정 | module-consolidation, debop4k-migration, full-module-review |
| `database-dialects.md` | DB 방언 구현 결정 | postgresql, mysql8, bigquery, duckdb, trino |
| `auditable-pattern.md` | Auditable 감사 추적 패턴 | auditable-exposed |
| `infrastructure-patterns.md` | AWS, Testcontainers 인프라 | aws-kotlin, testcontainers-new-servers, testcontainers-refactor |
| `dependency-decisions.md` | 의존성 버전 결정 이력 | git log (downgrade/revert/충돌 커밋) |

## 페이지 표준 형식

각 페이지는 아래 섹션을 포함한다:

```
# {주제명}

> 마지막 업데이트: YYYY-MM-DD | 관련 specs: N개

## 개요
범위 한두 줄.

## 핵심 설계 결정 (ADR)
| 결정 | 이유 | 날짜 | 관련 spec |
|------|------|------|----------|

## 패턴 & 사용법
코드 예시 + 언제 쓰는지.

## 선택하지 않은 방식 / 트레이드오프
왜 다른 접근을 채택하지 않았는지.

## 관련 페이지
- [[다른-페이지]] — 연결 이유
```

## /wiki-update 스킬 동작 규칙

1. `git diff HEAD~1 --name-only | grep 'docs/superpowers'`로 변경 파일 감지
2. 변경 파일 읽기
3. 위 매핑 기준으로 관련 wiki 페이지 파악
4. 해당 페이지의 ADR 테이블 갱신 + 패턴 섹션 보완 + 업데이트 날짜 갱신
5. `git log --oneline -100 | grep -iE 'downgrad|revert|fix.*version|다운그레이드|충돌'`으로 dependency-decisions.md 갱신
6. `qmd index .wiki/pages/` 실행
