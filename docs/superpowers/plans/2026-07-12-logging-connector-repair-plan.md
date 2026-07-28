# Logging Diagram Connector 수리 구현 계획

> **agentic worker용:** 필수 sub-skill: 이 계획은 superpowers:executing-plans로 task별 구현한다. 진행 상태는 checkbox(`- [ ]`) syntax로 추적한다.

**목표:** 모든 logging diagram arrow가 target boundary에 시각적으로도, 기하학적으로도 연결되게 만든다.

**아키텍처:** 기존 source-backed diagram model은 보존하고 SVG connector 좌표와 route만 수정한다. Derived site snapshot을 동기화하기 전에 각 canonical SVG/PNG pair를 독립적으로 XML validation, CairoSVG rendering, scripted audit, targeted endpoint assertion, full-size PNG inspection까지 통과시킨다.

**기술 스택:** SVG, CairoSVG CLI, bluetape diagram audit script, Astro site snapshot tooling.

---

### Task 1: logger responsibility-map connector 수리

**파일:**

- 수정: `docs/manual/assets/logging/logger-api-map.svg`
- 재생성: `docs/manual/assets/logging/logger-api-map.png`

- [ ] 왼쪽과 오른쪽 input을 분리된 rounded top port로 routing하고 모든 arrow tip을 target boundary까지 연장한다.
- [ ] XML, CairoSVG, connector, geometry, endpoint, mixed-corner, targeted boundary assertion, `14×14` primary-marker assertion을 실행한다.
- [ ] 3200×1960 PNG를 full size로 검사한다. gap, sharp corner, intrusion, ambiguous convergence가 남아 있으면 반복한다.

### Task 2: MDC lifecycle connector 수리

**파일:**

- 수정: `docs/manual/assets/logging/mdc-scope-lifecycle.svg`
- 재생성: `docs/manual/assets/logging/mdc-scope-lifecycle.png`

- [ ] 두 progression arrow를 source card edge에서 다음 card edge까지 연장한다.
- [ ] XML, CairoSVG, connector, geometry, endpoint, mixed-corner, targeted boundary assertion, `14×14` primary-marker assertion을 실행한다.
- [ ] 3200×1960 PNG를 full size로 검사한다. arrow가 떨어져 보이거나 card 안으로 들어가면 반복한다.

### Task 3: async sequence message endpoint 수리

**파일:**

- 수정: `docs/manual/assets/logging/async-channel-sequence.svg`
- 재생성: `docs/manual/assets/logging/async-channel-sequence.png`

- [ ] 번호가 붙은 7개 message row와 post-close return을 lifeline/activation edge에 맞춘다.
- [ ] XML, CairoSVG, common connector audit, sequence-style audit, targeted message endpoint assertion, 명시적 `16×16` per-color marker assertion을 실행한다.
- [ ] 3600×2240 PNG를 full size로 검사한다. message가 floating되거나 overshoot하거나 label을 지나거나 inconsistent arrowhead를 쓰면 반복한다.

### Task 4: derived site snapshot 게시와 검증

**파일:**

- 재생성: `public/manual-assets/bluetape4k-projects/logging/*`
- 재생성: `src/data/manual/bluetape4k-projects.snapshot.json`
- 재생성: `src/content/docs/**/manual/bluetape4k-projects/` 아래 synchronized manual metadata

- [ ] 모든 checklist row가 통과한 뒤 canonical Projects diagram을 commit한다.
- [ ] 최종 Projects commit에서 Site snapshot을 동기화하고 snapshot test, Astro diagnostics/build, browser rendering, console check를 실행한다.
- [ ] 두 worktree가 clean인지 확인하고 local preview server를 유지한다.
