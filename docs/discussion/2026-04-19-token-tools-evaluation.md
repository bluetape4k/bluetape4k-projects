---
date: 2026-04-19
session_id: 9483f29f-beb9-49dc-8612-198be1b06988
cwd: /Users/debop/work/bluetape4k/bluetape4k-projects
tags: [ claude-chat, token-optimization, tools-evaluation ]
---

# 토큰 절약 도구들 평가: OMC 생태계 검토

## 주요 결정 및 변경사항

### 평가 대상 도구 4개

1. **OMNI** (fajarhide/omni) — 터미널 출력 필터링으로 최대 90% 토큰 절약
2. **OpenWolf** (cytostack/openwolf) — 파일 인덱스, 학습 메모리, 토큰 모니터링으로 65-80% 절약
3. **Caveman** (JuliusBrussee/caveman) — 원시인 말투로 출력 토큰 65-75% 압축
4. **Evolver** (EvoMap/evolver) — GEP 기반 AI 에이전트 자기진화 엔진

### 각 도구별 평가 결과

| 도구         | 주요 기능            | 현재 환경 충돌                                | 권장                       |
|--------------|----------------------|-----------------------------------------------|----------------------------|
| **OMNI**     | 입력 노이즈 필터링   | RTK와 중복 (60-90% 절약 동일)                 | ❌ 패스                    |
| **OpenWolf** | 파일 인덱스 + 메모리 | code-review-graph + episodic-memory로 커버됨  | ❌ 패스 (hook 충돌 리스크) |
| **Caveman**  | 출력 토큰 압축       | 한국어 응답 필수 + OMC 간결성 규칙 기존       | ❌ 패스                    |
| **Evolver**  | 에이전트 자기진화    | self-improve 스킬 기존 + 보안 (외부 네트워크) | ❌ 패스                    |

### 최종 결론

**"OMC 쓰는 게 속 편한 거구나"** — 현재 설치된 OMC 생태계 (50+ 플러그인)가 이미 모든 주요 영역을 커버하고 있음.

- 토큰 절약: RTK (MCP) + context-mode (MCP) ✓
- 파일 추적: code-review-graph (MCP) ✓
- 메모리: episodic-memory (MCP) + OMC auto-memory ✓
- 자기진화: oh-my-claudecode:self-improve 스킬 ✓

## 설치·설정·파일 변경

**변경사항 없음** — 모든 도구 설치 및 적용 불가

추가 검토를 원한다면:

```bash
# 현재 OMC 플러그인 목록 확인
ls ~/.claude/plugins/cache/omc/oh-my-claudecode/*/skills/
```

## 다음 세션 기억사항

1. **외부 도구 평가 시 체크리스트**
    - OMC 기존 커버리지 우선 확인
    - 한국어 환경 지원 여부 확인
    - Hook/MCP 충돌 가능성 검토
    - 보안 (프라이빗 코드 외부 노출) 확인

2. **현재 스택의 강점**
    - 통합 에코시스템 (충돌 최소)
    - 자동 메모리 및 학습 기능
    - 다국어 지원 (한국어 우선)
    - 오프라인 우선 설계

3. **다음 개선 방향**
    - 기존 도구들의 조합/통합 활용
    - OMC 플러그인 커스터마이징
    - 사용자 스킬 작성 검토
