# Supabase PostgreSQL 가이드

[English](./README.md) | 한국어

Supabase 프로젝트에서의 PostgreSQL 모범 사례 참고 문서입니다.

## 출처

[supabase/agent-skills](https://github.com/supabase/agent-skills) 기반 — PostgreSQL 성능 최적화 가이드라인.

## 카테고리

| 우선순위 | 카테고리               | 영향도        |
|------|--------------------|------------|
| 1    | 쿼리 성능              | 매우 중요      |
| 2    | 커넥션 관리             | 매우 중요      |
| 3    | 보안 및 RLS           | 매우 중요      |
| 4    | 스키마 설계             | 높음         |
| 5    | 동시성 및 잠금           | 중간-높음      |
| 6    | 데이터 접근 패턴          | 중간         |
| 7    | 모니터링 및 진단          | 낮음-중간      |
| 8    | 고급 기능              | 낮음         |

## 사용처

이 가이드는 다음에서 참조됩니다:

- **Agent**: db-supabase-expert
- **Skill**: supabase-postgres-best-practices

## 외부 리소스

- [Supabase 문서](https://supabase.com/docs)
- [PostgreSQL 문서](https://www.postgresql.org/docs/)
- [Supabase Agent Skills (GitHub)](https://github.com/supabase/agent-skills)
