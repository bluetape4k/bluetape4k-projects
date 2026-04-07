# LLM Wiki 시스템 구현 계획

> **For agentic workers:
** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (
`- [ ]`) syntax for tracking.

**Goal:** `.wiki/` 폴더 기반 LLM Wiki 시스템 구축 — specs/plans/git log를 9개 주제별 wiki 페이지로 합성하고, qmd MCP로 검색 가능하게 하며, spec 저장 시
`/wiki-update` 알림 hook을 등록한다.

**Architecture:** `.wiki/pages/`에 주제별 마크다운 페이지를 두고 qmd가 인덱싱한다. spec 저장 시 PostToolUse hook이 알림을 띄우고,
`/wiki-update` 스킬이 관련 페이지를 갱신한다. `docs/superpowers/specs|plans/`와 git log가 원본 소스(raw)다.

**Tech Stack:** qmd 2.1.0 (BM25+vector 검색), Claude Code hooks (PostToolUse), Claude Code skills (SKILL.md)

---

## 파일 맵

| 역할     | 경로                                              |
|--------|-------------------------------------------------|
| Create | `.wiki/WIKI.md`                                 |
| Create | `.wiki/raw/gitlog/version-decisions.md`         |
| Create | `.wiki/pages/exposed-patterns.md`               |
| Create | `.wiki/pages/cache-architecture.md`             |
| Create | `.wiki/pages/spring-boot-integration.md`        |
| Create | `.wiki/pages/kotlin-testing-patterns.md`        |
| Create | `.wiki/pages/module-decisions.md`               |
| Create | `.wiki/pages/database-dialects.md`              |
| Create | `.wiki/pages/auditable-pattern.md`              |
| Create | `.wiki/pages/infrastructure-patterns.md`        |
| Create | `.wiki/pages/dependency-decisions.md`           |
| Create | `~/.claude/hooks/wiki-update-reminder.sh`       |
| Create | `~/.claude/skills/wiki-update/SKILL.md`         |
| Modify | `~/.claude/settings.json` (PostToolUse hook 추가) |
| Modify | `CLAUDE.md` (체크리스트 항목 추가)                       |
| Modify | `.gitignore` (qmd 인덱스 파일 제외)                    |

---

## Task 1: `.wiki/` 폴더 구조 + WIKI.md 스키마 생성

**complexity: low**

**Files:**

- Create: `.wiki/WIKI.md`
- Create: `.wiki/raw/articles/.gitkeep`
- Create: `.wiki/raw/notes/.gitkeep`
- Create: `.wiki/raw/gitlog/.gitkeep`
- Create: `.wiki/pages/.gitkeep`

- [ ] **Step 1: 폴더 구조 생성**

```bash
mkdir -p .wiki/raw/articles .wiki/raw/notes .wiki/raw/gitlog .wiki/pages
touch .wiki/raw/articles/.gitkeep .wiki/raw/notes/.gitkeep .wiki/raw/gitlog/.gitkeep .wiki/pages/.gitkeep
```

- [ ] **Step 2: `.wiki/WIKI.md` 스키마 작성**

`.wiki/WIKI.md` 파일을 아래 내용으로 생성한다:

```markdown
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
|----|----|----|---------|

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
```

- [ ] **Step 3: 폴더 구조 확인**

```bash
find .wiki -type f | sort
```

기대 출력:

```
.wiki/WIKI.md
.wiki/raw/articles/.gitkeep
.wiki/raw/notes/.gitkeep
.wiki/raw/gitlog/.gitkeep
.wiki/pages/.gitkeep
```

- [ ] **Step 4: .gitignore 업데이트**

`.gitignore`에 qmd 인덱스 파일 추가:

```
# qmd search index
.wiki/.qmd/
```

- [ ] **Step 5: 커밋**

```bash
git add .wiki/ .gitignore
git commit -m "chore: .wiki 폴더 구조 및 WIKI.md 스키마 생성"
```

---

## Task 2: wiki-update-reminder Hook 등록

**complexity: low**

**Files:**

- Create: `~/.claude/hooks/wiki-update-reminder.sh`
- Modify: `~/.claude/settings.json`

- [ ] **Step 1: hook 스크립트 작성**

`~/.claude/hooks/wiki-update-reminder.sh` 생성:

```bash
#!/bin/bash
# wiki-update-reminder.sh
# PostToolUse(Write): docs/superpowers/specs/ 에 새 spec 저장 시 /wiki-update 안내

set -euo pipefail

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | python3 -c \
  "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('file_path',''))" \
  2>/dev/null || echo "")

# specs/ 경로가 아니면 조용히 종료
echo "$FILE_PATH" | grep -q 'docs/superpowers/specs/' || exit 0

# -design.md 로 끝나는 파일만 (spec 파일)
[[ "$FILE_PATH" == *-design.md ]] || exit 0

echo "💡 [wiki-update] 새 spec이 추가됐습니다: $(basename "$FILE_PATH")"
echo "   /wiki-update 를 실행해 .wiki/pages/ 를 업데이트하세요."
exit 2
```

- [ ] **Step 2: 실행 권한 부여**

```bash
chmod +x ~/.claude/hooks/wiki-update-reminder.sh
```

- [ ] **Step 3: settings.json PostToolUse 섹션에 hook 추가**

`~/.claude/settings.json`의 `"PostToolUse"` 배열에서 `"matcher": "Edit|Write"` 블록의 `"hooks"` 배열에 다음 항목을 추가한다:

```json
{
  "type": "command",
  "command": "bash /Users/debop/.claude/hooks/wiki-update-reminder.sh"
}
```

- [ ] **Step 4: hook 동작 확인**

```bash
echo '{"tool_input":{"file_path":"/Users/debop/work/bluetape4k/bluetape4k-projects/docs/superpowers/specs/2026-04-07-test-design.md"}}' \
  | bash ~/.claude/hooks/wiki-update-reminder.sh
```

기대 출력 (exit 2):

```
💡 [wiki-update] 새 spec이 추가됐습니다: 2026-04-07-test-design.md
   /wiki-update 를 실행해 .wiki/pages/ 를 업데이트하세요.
```

- [ ] **Step 5: chezmoi re-add**

```bash
chezmoi re-add ~/.claude/hooks/wiki-update-reminder.sh
chezmoi re-add ~/.claude/settings.json
```

---

## Task 3: git log 버전 결정 이력 추출 → dependency-decisions.md

**complexity: medium**

**Files:**

- Create: `.wiki/raw/gitlog/version-decisions.md`
- Create: `.wiki/pages/dependency-decisions.md`

- [ ] **Step 1: git log 버전 관련 커밋 추출**

```bash
cd /Users/debop/work/bluetape4k/bluetape4k-projects
git log --format="%h %as %s" | grep -iE 'downgrad|revert|fix.*version|버전.*다운|다운그레이드|충돌|classpath' \
  > .wiki/raw/gitlog/version-decisions.md
```

- [ ] **Step 2: version-decisions.md 확인**

```bash
cat .wiki/raw/gitlog/version-decisions.md
```

기대: lettuce downgrade, rest-assured downgrade, Jackson downgrade, aws-kotlin downgrade 등 포함.

- [ ] **Step 3: dependency-decisions.md 합성**

`.wiki/pages/dependency-decisions.md`를 아래 형식으로 작성한다.
`.wiki/raw/gitlog/version-decisions.md`의 각 커밋을 읽고, 커밋 메시지에서 라이브러리명·버전·이유를 추출해 ADR 테이블로 정리한다:

```markdown
# 의존성 버전 결정 이력

> 마지막 업데이트: 2026-04-07 | 소스: git log

## 개요
버전 다운그레이드, 충돌 해결, 의존성 제거 등 의존성 관련 주요 결정 이력.
새로운 버전 문제 발생 시 여기서 선례를 먼저 확인한다.

## 핵심 설계 결정 (ADR)

| 라이브러리 | 결정 | 버전 변경 | 이유 | 날짜 | 커밋 |
|-----------|------|---------|------|------|------|
| lettuce | 다운그레이드 | 7.5.0 → 6.8.2 | NearCache 호환성 문제 | 2026-03 | 8ad00ab94 |
| rest-assured | 다운그레이드 | — | Jackson 버전 충돌 | — | 9edeb194f |
| Jackson | 다운그레이드 | — | 버전 충돌 | — | 5f25ae9e5 |
| aws-kotlin | 다운그레이드 | — | 호환성 문제 | — | 1775790b0 |
| httpclient5 | 다운그레이드 | — | Apache HttpComponents 호환 | — | 7e264f368 |
| hibernate-lettuce | 충돌 수정 | — | SB3/SB4 classpath 충돌 | — | acec909ec |

*(추출된 git log 내용에 따라 행 추가)*

## 패턴 & 교훈

- **lettuce 7.x**: NearCache(CLIENT TRACKING RESP3)와 비호환 — 6.8.x 유지
- **rest-assured**: Jackson 버전을 프로젝트 BOM과 맞춰야 함
- **Spring Boot 3/4 동시 지원**: classpath 격리 주의

## 관련 페이지
- [[cache-architecture]] — lettuce 다운그레이드 배경
- [[spring-boot-integration]] — SB3/SB4 classpath 충돌
```

- [ ] **Step 4: 커밋**

```bash
git add .wiki/raw/gitlog/ .wiki/pages/dependency-decisions.md
git commit -m "docs(wiki): dependency-decisions.md — git log 버전 결정 이력 합성"
```

---

## Task 4: exposed-patterns.md 합성

**complexity: high**

**Files:**

- Create: `.wiki/pages/exposed-patterns.md`
- Read: `docs/superpowers/specs/` 중 exposed 관련 파일들

- [ ] **Step 1: 관련 spec 파일 목록 확인**

```bash
ls docs/superpowers/specs/ | grep -iE 'exposed|cache-repository|auditable'
```

- [ ] **Step 2: 관련 spec 파일 읽기**

아래 파일들을 순서대로 읽는다:

- `docs/superpowers/specs/2026-04-07-exposed-cache-repository-unification-design.md`
- `docs/superpowers/specs/2026-03-18-cache-consistency-refactoring-design.md`
- `docs/superpowers/specs/2026-03-29-spring-data-exposed-migration-design.md`
- `docs/superpowers/specs/2026-03-29-spring-data-demo-migration-design.md`

- [ ] **Step 3: `.wiki/pages/exposed-patterns.md` 합성**

읽은 내용을 바탕으로 아래 형식으로 작성한다:

```markdown
# Exposed ORM 패턴

> 마지막 업데이트: 2026-04-07 | 관련 specs: 6개

## 개요
bluetape4k에서 Jetbrains Exposed ORM을 사용하는 핵심 패턴.
Repository 추상화, 캐시 통합, Spring Data 연동, 감사 추적을 다룬다.

## 핵심 설계 결정 (ADR)

| 결정 | 이유 | 날짜 | 관련 spec |
|------|------|------|----------|
| Repository generic: `<ID, E>` (테이블 타입 제거) | 테이블 타입 노출 시 캡슐화 파괴, 사용 복잡도 증가 | 2026-03 | redisson-repository-generic |
| exposed-redis-api 신규 모듈 (공통 캐시 인터페이스) | lettuce/redisson 구현체 간 인터페이스 불일치 → 테스트 중복 | 2026-04 | exposed-cache-repository-unification |
| CacheMode / CacheWriteMode 열거형 통합 | lettuce WriteMode와 redisson CacheMode+WriteMode 조합이 달라 혼란 | 2026-04 | exposed-cache-repository-unification |
| testFixtures 시나리오 공유 | 동일 캐시 시나리오를 lettuce/redisson 양쪽에서 재사용 | 2026-04 | exposed-cache-repository-unification |

## 패턴 & 사용법

### Repository 기본 패턴
```kotlin
// ✅ 올바른 패턴: ID, Entity 두 타입만
class MyRepository : AbstractJdbcRepository<Long, MyEntity>() {
    override val table = MyTable
    override fun extractId(entity: MyEntity) = entity.id
}
```

### 캐시 통합 패턴 (exposed-redis-api)

```kotlin
// JdbcCacheRepository — sync
class ArticleCacheRepository : AbstractJdbcLettuceRepository<Long, ArticleRecord>()

// SuspendedJdbcCacheRepository — coroutines
class ArticleSuspendRepository : AbstractSuspendedJdbcLettuceRepository<Long, ArticleRecord>()
```

### CacheMode 선택 기준

| 모드              | 언제                    |
|-----------------|-----------------------|
| `READ_THROUGH`  | 조회 시 캐시 미스 → DB 자동 로드 |
| `WRITE_THROUGH` | 쓰기 즉시 캐시 갱신 (일관성 우선)  |
| `WRITE_BEHIND`  | 쓰기 비동기 반영 (성능 우선)     |

## 선택하지 않은 방식 / 트레이드오프

- **테이블 타입을 generic에 포함**: `SoftDeleted*` 제외하고 제거 — 대부분의 사용처에서 테이블 직접 접근 불필요
- **lettuce/redisson 별도 인터페이스 유지**: 구현체 교체 시 코드 변경 불가피 → exposed-redis-api 통합 인터페이스 채택

## 관련 페이지

- [[cache-architecture]] — NearCache 백엔드 선택
- [[auditable-pattern]] — 감사 추적 통합
- [[spring-boot-integration]] — Spring Data Exposed 연동

```

*(실제 파일 읽은 내용으로 ADR 행과 패턴 섹션을 보완한다)*

- [ ] **Step 4: 커밋**

```bash
git add .wiki/pages/exposed-patterns.md
git commit -m "docs(wiki): exposed-patterns.md 초기 합성"
```

---

## Task 5: cache-architecture.md 합성

**complexity: high**

**Files:**

- Create: `.wiki/pages/cache-architecture.md`
- Read: `docs/superpowers/specs/2026-03-18-nearcache-unification-design.md`,
  `2026-03-18-cache-consistency-refactoring-design.md`, `2026-03-18-redisson-repository-generic-refactoring-design.md`

- [ ] **Step 1: 관련 spec 읽기**

위 3개 파일을 읽는다.

- [ ] **Step 2: `.wiki/pages/cache-architecture.md` 합성**

```markdown
# NearCache 아키텍처

> 마지막 업데이트: 2026-04-07 | 관련 specs: 3개

## 개요
bluetape4k cache 모듈의 NearCache 추상화 설계.
Lettuce(RESP3 CLIENT TRACKING), Redisson(RLocalCachedMap), Hazelcast 백엔드를
동일 인터페이스로 교체 가능하게 설계한다.

## 핵심 설계 결정 (ADR)

| 결정 | 이유 | 날짜 | 관련 spec |
|------|------|------|----------|
| `NearCacheOperations<V>` / `SuspendNearCacheOperations<V>` 단일 인터페이스 | 백엔드 교체 시 코드 변경 없이 swap 가능 | 2026-03 | nearcache-unification |
| Resilience4j Decorator 분리 | retry/fallback 로직을 구현체에서 분리 → 단일 책임 | 2026-03 | nearcache-unification |
| lettuce 6.8.2 고정 (7.5.0 사용 안 함) | RESP3 CLIENT TRACKING NearCache 호환성 문제 | 2026-03 | — (git log) |
| `*Of` 팩토리 함수 패턴 | 생성자 직접 호출 대신 DSL 스타일 | 2026-03 | nearcache-unification |

## 패턴 & 사용법

```kotlin
// 팩토리 함수로 생성
val cache = lettuceNearCacheOf<MyValue>(redisClient, codec, config)

// Resilience 래핑
val resilient = cache.withResilience {
    retryMaxAttempts = 3
    retryWaitDuration = 100.milliseconds
}

// suspend 사용
val value = cache.getOrLoad(key) { fetchFromDb(key) }
```

## 선택하지 않은 방식 / 트레이드오프

- **lettuce 7.5.0**: NearCache CLIENT TRACKING 비호환 → 6.8.2 고정
- **Hazelcast Near Cache 내장 기능 직접 사용**: 인터페이스 불통일 → 래핑

## 관련 페이지

- [[exposed-patterns]] — DB + 캐시 통합 레이어
- [[dependency-decisions]] — lettuce 버전 다운그레이드 배경

```

- [ ] **Step 3: 커밋**

```bash
git add .wiki/pages/cache-architecture.md
git commit -m "docs(wiki): cache-architecture.md 초기 합성"
```

---

## Task 6: 나머지 7개 wiki 페이지 합성

**complexity: high**

각 페이지마다 동일한 패턴(관련 spec 읽기 → 합성 → 커밋)을 따른다.

**Pages & 대응 spec 파일:**

| 페이지                          | 읽을 spec 파일                                                                                                                                                                                |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `spring-boot-integration.md` | `2026-03-20-spring-boot4-modules-design.md`, `2026-03-29-hibernate-lettuce-migration-design.md`, `2026-03-29-spring-data-exposed-migration-design.md`                                     |
| `kotlin-testing-patterns.md` | `2026-04-04-kdoc-examples-all-modules-design.md`, `2026-04-03-testcontainers-design.md`, `2026-04-02-testcontainers-new-servers-design.md`                                                |
| `module-decisions.md`        | `2026-03-13-full-module-review-design.md`, `2026-03-17-module-consolidation-design.md`, `2026-03-22-debop4k-migration-design.md`                                                          |
| `database-dialects.md`       | `2026-03-28-postgresql-module-design.md`, `2026-03-28-exposed-mysql8-migration-design.md`, `2026-03-28-exposed-bigquery-duckdb-migration-design.md`, `2026-04-03-exposed-trino-design.md` |
| `auditable-pattern.md`       | `2026-03-29-auditable-exposed-design.md`                                                                                                                                                  |
| `infrastructure-patterns.md` | `2026-04-02-testcontainers-new-servers-design.md`, `2026-03-28-hibernate-cache-lettuce-migration-design.md`                                                                               |

- [ ] **Step 1: 각 페이지 합성 및 커밋 (페이지당 1 커밋)**

각 페이지에 대해:

1. 대응 spec 파일 읽기
2. WIKI.md의 표준 형식(개요 / ADR 테이블 / 패턴 & 사용법 / 트레이드오프 / 관련 페이지)으로 합성
3. `.wiki/pages/{페이지명}.md` 저장
4. `git commit -m "docs(wiki): {페이지명} 초기 합성"`

---

## Task 7: `/wiki-update` 스킬 작성

**complexity: medium**

**Files:**

- Create: `~/.claude/skills/wiki-update/SKILL.md`

- [ ] **Step 1: 스킬 디렉토리 생성**

```bash
mkdir -p ~/.claude/skills/wiki-update
```

- [ ] **Step 2: `SKILL.md` 작성**

`~/.claude/skills/wiki-update/SKILL.md`:

```markdown
---
name: wiki-update
description: 최근 변경된 spec/plan을 읽고 .wiki/pages/ 관련 페이지를 업데이트한 뒤 qmd를 재인덱싱한다
triggers:
  - /wiki-update
---

# Wiki Update

spec/plan이 추가되거나 변경됐을 때 .wiki/pages/ 를 최신 상태로 유지한다.

## 실행 절차

1. **변경 파일 감지**
   ```bash
   git diff HEAD~1 --name-only | grep 'docs/superpowers'
   ```

변경된 파일이 없으면 git status로 untracked 확인.

2. **변경 파일 읽기**
   감지된 spec/plan 파일을 Read 도구로 읽는다.

3. **관련 wiki 페이지 파악**
   `.wiki/WIKI.md`의 "페이지 목록 & 주제 매핑" 테이블 기준으로 해당 spec이 어느 페이지에 속하는지 파악.

4. **wiki 페이지 업데이트**
    - ADR 테이블: 새 결정 행 추가 또는 기존 행 갱신
    - 패턴 섹션: 새 패턴/코드 예시 보완
    - 헤더의 "마지막 업데이트" 날짜 갱신
    - "관련 specs: N개" 카운트 갱신

5. **dependency-decisions.md 갱신** (의존성 관련 커밋이 있을 경우)
   ```bash
   git log --oneline -100 | grep -iE 'downgrad|revert|fix.*version|다운그레이드|충돌'
   ```
   새 커밋이 있으면 ADR 테이블에 행 추가.

6. **qmd 재인덱싱**
   ```bash
   qmd index .wiki/pages/
   ```

7. **커밋**
   ```bash
   git add .wiki/pages/
   git commit -m "docs(wiki): {업데이트된 페이지명} 갱신 — {spec명}"
   ```

```

- [ ] **Step 3: chezmoi re-add**

```bash
chezmoi re-add ~/.claude/skills/wiki-update/SKILL.md
```

---

## Task 8: qmd 초기 인덱싱 + CLAUDE.md 업데이트

**complexity: low**

- [ ] **Step 1: qmd 초기 인덱싱**

```bash
cd /Users/debop/work/bluetape4k/bluetape4k-projects
qmd index .wiki/pages/
```

기대: 9개 페이지 인덱싱 완료 메시지.

- [ ] **Step 2: qmd 검색 동작 확인**

```bash
qmd query "NearCache lettuce 다운그레이드"
```

기대: `cache-architecture.md` 또는 `dependency-decisions.md` 반환.

- [ ] **Step 3: CLAUDE.md 체크리스트 업데이트**

`CLAUDE.md`의 "코드 변경 후" 체크리스트 섹션에 추가:

```markdown
- [ ] spec/plan 새로 작성 시: `/wiki-update` 실행
```

- [ ] **Step 4: 최종 커밋**

```bash
git add CLAUDE.md
git commit -m "docs: CLAUDE.md wiki-update 체크리스트 항목 추가"
```
