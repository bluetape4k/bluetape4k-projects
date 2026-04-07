# LLM Wiki 시스템 설계 스펙

## 1. 개요

Karpathy의 LLM Wiki 패턴을 bluetape4k-projects에 적용한다.
`docs/superpowers/specs/plans`의 작업 기록과 git log의 버전 결정 이유를 LLM이 주제별 wiki 페이지로 합성·유지하여, 재발견 없이 누적되는 지식 베이스를 구축한다.

### 목표

- RAG 방식(쿼리마다 재발견)이 아닌, LLM이 유지하는 **영속 wiki** 구축
- 설계 결정 근거(ADR), 패턴 카탈로그, 모듈 레퍼런스, 의존성 결정 이력 통합
- spec 추가 시 자동 알림 + `/wiki-update` 스킬로 wiki 갱신
- `qmd` MCP 서버로 Claude Code에서 wiki 검색 가능

---

## 2. 폴더 구조

```
.wiki/
├── WIKI.md              ← 스키마: 페이지 목록, 주제 매핑, 업데이트 규칙
├── raw/                 ← 원본 소스 (immutable, 사용자가 직접 투입)
│   ├── articles/        ← 웹 아티클, PDF, 외부 자료 등 임의 투입
│   ├── notes/           ← 메모, 아이디어, 비정형 자료
│   └── gitlog/          ← git log 추출본 (wiki-update 시 자동 갱신)
└── pages/               ← LLM 합성 위키 페이지 (주제별, qmd 인덱싱 대상)
    ├── exposed-patterns.md
    ├── cache-architecture.md
    ├── spring-boot-integration.md
    ├── kotlin-testing-patterns.md
    ├── module-decisions.md
    ├── database-dialects.md
    ├── auditable-pattern.md
    ├── infrastructure-patterns.md
    └── dependency-decisions.md
```

---

## 3. Wiki 페이지 형식 (표준 템플릿)

각 페이지는 ADR + 패턴 카탈로그 + 결정 근거를 조합한다.

```markdown
# {주제명}

> 마지막 업데이트: YYYY-MM-DD | 관련 specs: N개

## 개요
이 페이지가 다루는 범위 한두 줄 요약.

## 핵심 설계 결정 (ADR)
| 결정 | 이유 | 날짜 | 관련 spec |
|------|------|------|----------|
| ... | ... | ... | ... |

## 패턴 & 사용법
실제 코드 예시 + 언제 어떻게 쓰는지.

## 선택하지 않은 방식 / 트레이드오프
왜 다른 접근을 채택하지 않았는지.

## 관련 페이지
- [[cache-architecture]] — NearCache 백엔드 연동
```

---

## 4. 초기 wiki 페이지 9개 — 주제 매핑

| 페이지 파일                       | 흡수할 specs/plans                                                                               | 보조 소스                                        |
|------------------------------|-----------------------------------------------------------------------------------------------|----------------------------------------------|
| `exposed-patterns.md`        | exposed-core, exposed-dao, exposed-jdbc, exposed-r2dbc, exposed-redis-api, exposed-cache 관련   | —                                            |
| `cache-architecture.md`      | nearcache-unification, cache-consistency-refactoring, redisson-repository-generic-refactoring | —                                            |
| `spring-boot-integration.md` | spring-boot3/4 modules, hibernate-lettuce, spring-data-exposed                                | —                                            |
| `kotlin-testing-patterns.md` | kdoc-examples, kdoc-examples-all-modules, testcontainers, testcontainers-new-servers          | —                                            |
| `module-decisions.md`        | module-consolidation, debop4k-migration, full-module-review                                   | —                                            |
| `database-dialects.md`       | postgresql, mysql8, exposed-bigquery-duckdb, exposed-trino                                    | —                                            |
| `auditable-pattern.md`       | auditable-exposed                                                                             | —                                            |
| `infrastructure-patterns.md` | aws-kotlin (client pattern), testcontainers refactor                                          | —                                            |
| `dependency-decisions.md`    | —                                                                                             | git log (downgrade/revert/fix version 패턴 커밋) |

---

## 5. Hook 설계

### PostToolUse Hook (spec 저장 알림)

`~/.claude/settings.json`에 추가:

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Write",
        "hooks": [
          {
            "type": "command",
            "command": "echo \"$TOOL_INPUT_FILE_PATH\" | grep -q 'docs/superpowers/specs/' && echo '💡 wiki-update: 새 spec이 추가됐습니다. /wiki-update 를 실행해 wiki를 업데이트하세요.' && exit 2 || exit 0"
          }
        ]
      }
    ]
  }
}
```

- `exit 2` = Claude 대화창에 메시지 표시 (LLM 호출 없음)
- `docs/superpowers/specs/` 경로에 Write 발생 시에만 트리거
- **구현 시 확인**: Claude Code PostToolUse hook에서 파일 경로를 담는 환경변수명 (`TOOL_INPUT_FILE_PATH` 또는 다른 변수) 실제 지원 여부 검증 필요

---

## 6. `/wiki-update` 스킬 동작

```
1. 최근 변경 spec 감지
   git diff HEAD~1 --name-only | grep 'docs/superpowers'

2. 변경 파일 읽기
   각 spec/plan 파일 내용 파악

3. WIKI.md 매핑 기준으로 관련 wiki 페이지 파악

4. 관련 wiki 페이지 업데이트
   - ADR 테이블에 새 행 추가 또는 기존 행 갱신
   - 패턴/사용법 섹션 보완
   - 마지막 업데이트 날짜 갱신

5. git log 스캔 → dependency-decisions.md 갱신
   git log --oneline -100 | grep -iE 'downgrad|revert|fix.*version|버전|다운그레이드|충돌'

6. qmd index .wiki/pages/ 실행 (검색 인덱스 갱신)
```

---

## 7. WIKI.md 스키마 (핵심 내용)

`.wiki/WIKI.md`는 LLM이 wiki를 유지할 때 따르는 규칙을 정의한다:

- 각 페이지의 담당 주제 범위
- spec → 페이지 매핑 규칙
- 페이지 내 ADR 테이블 작성 기준
- `dependency-decisions.md` 업데이트 트리거 조건

---

## 8. CLAUDE.md 체크리스트 추가

"코드 변경 후" 섹션에 추가:

```
- [ ] spec/plan 새로 작성 시: `/wiki-update` 실행
```

---

## 9. 구현 순서

1. `.wiki/` 폴더 구조 + `WIKI.md` 스키마 생성
2. 56개 specs/plans + git log 읽고 9개 wiki 페이지 초기 합성
3. PostToolUse hook 등록 (`~/.claude/settings.json`)
4. `/wiki-update` 스킬 파일 작성 (`.claude/skills/wiki-update/SKILL.md`)
5. `qmd index .wiki/pages/` 실행 (초기 인덱싱)
6. CLAUDE.md 체크리스트 업데이트
