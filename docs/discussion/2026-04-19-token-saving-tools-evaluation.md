---
date: 2026-04-19
session_id: 9483f29f-beb9-49dc-8612-198be1b06988
cwd: /Users/debop/work/bluetape4k/bluetape4k-projects
tags: [ claude-chat, token-optimization, tools-evaluation ]
---

# 토큰 절약 도구 평가: OMNI, OpenWolf, Caveman

## 주요 결정 및 변경사항

세 가지 인기 있는 Claude Code 토큰 절약 도구를 평가한 결과, **모두 OMC 생태계에 이미 구현된 기능과 중복**되는 것으로 확인되었습니다.

### 평가 결과

#### 1. OMNI (fajarhide/omni)

- **기능**: 터미널 출력 필터링을 통해 60~90% 토큰 절약
- **기존 도구**: RTK (Rust Token Killer)가 이미 설치되어 있고 자동 적용 중
- **결론**: 중복 설치 불필요

#### 2. OpenWolf (cytostack/openwolf)

- **기능**: 파일 인덱스, 학습 메모리, 토큰 모니터링 (6개 hook 스크립트)
- **기존 대응**:
    - 파일 인덱스 → `code-review-graph` MCP
    - 학습 메모리 → `episodic-memory` + OMC auto-memory
    - 토큰 모니터링 → `context-mode` MCP
- **우려**: OMC가 이미 많은 PostToolUse/PreToolUse hooks를 사용 중 → **충돌 가능성**
- **결론**: 현재 스택으로 충분하며, hook 충돌 리스크로 패스 권장

#### 3. Caveman (JuliusBrussee/caveman)

- **기능**: 응답을 "원시인 말투"로 압축하여 출력 토큰 65~75% 절약
- **입력 vs 출력**: RTK/OpenWolf는 입력 노이즈 필터링, Caveman은 **출력 압축**
- **문제점**:
    - 한국어 응답이 필수인 환경에서 caveman-speak 적용 시 **가독성 저하**
    - OMC가 이미 응답 간결성 규칙 적용 중 (`≤100 words`, `≤25 words between tool calls`)
- **결론**: 영어 전용 프로젝트에는 흥미로우나, 한국어+OMC 환경에선 불편함 > 이득

## 설치·설정·파일 변경

**변경사항 없음** — 기존 도구들이 모두 충분하므로 추가 설치 불필요

## 다음 세션 기억사항

- OMC 생태계가 50+ 플러그인으로 광범위하게 구성되어 있음
- 토큰 절약을 위해 **새로운 도구 추가**보다 **기존 도구 최적화**가 우선
- RTK, code-review-graph, episodic-memory, context-mode가 이미 모든 주요 영역을 커버 중
- Hook 충돌 가능성 있으므로, 새 도구 추가 시 기존 hooks와의 호환성 검토 필수
