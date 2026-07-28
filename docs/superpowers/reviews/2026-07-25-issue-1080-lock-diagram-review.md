# Issue #1080 Lock Diagram 검토

## 범위와 공통 근거

- 대상 문서: `infra/lettuce/README.md`, `infra/lettuce/README.ko.md`,
  `infra/lettuce/CoordinationLocks.md`, `infra/lettuce/CoordinationLocks.ko.md`
- 구현 근거: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/` 및 `internal/`
- Architecture 기준: `docs/images/readme-diagrams/infra-lettuce-diagram-02.png`
- Sequence 기준 1:
  `/Users/debop/work/bluetape4k/bluetape4k-wiki/docs/diagrams/best-practices/assets/leader-redis-lettuce-sequence-02.png`
- Sequence 기준 2: `docs/images/readme-diagrams/infra-lettuce-sequence-01.png`
- 규칙: `bluetape-diagram/SKILL.md`, `references/common.md`, `references/architecture.md`,
  `references/sequence.md`
- 공통 실행: `diagram-svg-text-normalize.py`, `xmllint --noout`, `cairosvg -s 2`,
  `diagram-connector-audit.py`, `diagram-geometry-audit.py --fail-diagonal`,
  `diagram-endpoint-audit.py`, `diagram-mixed-corner-audit.py`; sequence에는
  `diagram-sequence-style-audit.py` 추가

## `infra-lettuce-diagram-03`

SVG/PNG는 각각 `docs/images/readme-diagrams/infra-lettuce-diagram-03.svg`와 `.png`이며, 독자 질문은
“어떤 Lock 객체를 선택하며 어떤 infrastructure를 공유하는가?”이다.

| ID | 결과와 근거 |
|---|---|
| DIA-01 | PASS — README/구현을 읽고 6개 객체군, 3개 API 표면, runtime, Redis/Lua, observation, legacy 경계를 고정했다. |
| DIA-02 | PASS — common + architecture 규칙과 가장 가까운 `infra-lettuce-diagram-02.png`를 사용했다. |
| DIA-03 | PASS — 영문 자산 하나만 편집하고 선택/공유/호환 경계 invariant를 유지했다. |
| DIA-04 | PASS — XML parse, CairoSVG `-s 2`; PNG `3800x2160`. |
| DIA-05 | PASS — text hazards `0`, code-without-highlight `0`; markers `3`, connectors `5`, cards `6`; geometry/endpoint/mixed-corner failures `0`, `q_bends=2`. |
| DIA-06 | PASS — 최종 좌표 뒤 원본 크기 PNG를 열어 clipping, tofu, 겹침, endpoint, arrowhead, whitespace 이상 없음을 확인했다. |
| DIA-07 | PASS — 영문 README/guide가 정확한 PNG를 한 번씩 embed하며 locale sibling 기하가 같다. |
| DIA-08 | PASS — 이 표에 명령, count, 크기, inspection, source/reference를 기록했다. |
| DIA-COM-01 | PASS — source/readme 및 related Lettuce diagram set을 확인했다. |
| DIA-COM-02 | PASS — 읽을 수 있는 theme/font, hazards `0`, full-size alignment PASS. |
| DIA-COM-03 | N/A — logo가 아닌 text/card architecture이며 invented icon이 없다. |
| DIA-COM-04 | PASS — 3개 fixed-color marker가 PNG에서 방향/색/크기를 유지한다. |
| DIA-COM-05 | PASS — connectors `5`, intrusions/crossings/shared segments/label collisions 모두 `0`; endpoint PASS. |
| DIA-COM-06 | PASS — mixed-corner paths `5`, `q_bends=2`, failures `0`. |
| DIA-COM-07 | PASS — `1900x1080` canvas에서 lane/margin/footer와 bottom whitespace가 균형을 이룬다. |
| DIA-COM-08 | PASS — 필수 명령 전부 exit `0`; generic count는 cards/connectors로 의미가 있다. |
| DIA-COM-09 | PASS — 이 review ledger와 영문 README/guide가 canonical output을 가리킨다. |
| DIA-ARC-01 | PASS — 정적 responsibility/ownership/dependency 질문이며 호출 순서를 담지 않는다. |
| DIA-ARC-02 | PASS — 인접 Lettuce architecture의 muted palette/card/legend 계열을 유지했다. |
| DIA-ARC-03 | PASS — 선택 객체군에서 공통 runtime/Redis/observation으로 읽히는 수평 구조다. |
| DIA-ARC-04 | PASS — rounded orthogonal route, standoff, arrowhead clearance; crossing/card intrusion `0`. |

## `infra-lettuce-diagram-03-ko`

SVG/PNG는 `docs/images/readme-diagrams/infra-lettuce-diagram-03-ko.svg`와 `.png`이며 영문과 같은
`1900x1080` 기하를 사용한다.

| ID | 결과와 근거 |
|---|---|
| DIA-01 | PASS — 한국어 README/guide와 같은 구현 source에 범위를 고정했다. |
| DIA-02 | PASS — common + architecture 규칙과 영문 sibling/인접 architecture를 사용했다. |
| DIA-03 | PASS — 한국어 자산만 편집하고 기술 identifier와 기하 parity를 유지했다. |
| DIA-04 | PASS — XML parse, CairoSVG `-s 2`; PNG `3800x2160`. |
| DIA-05 | PASS — text hazards `0`, code-without-highlight `0`; markers `3`, connectors `5`, cards `6`; geometry/endpoint/mixed-corner failures `0`, `q_bends=2`. |
| DIA-06 | PASS — full-size PNG에서 한국어 font, clipping, tofu, 겹침, endpoint, whitespace 이상이 없다. |
| DIA-07 | PASS — 한국어 README/guide가 `-ko.png`를 정확히 한 번씩 embed한다. |
| DIA-08 | PASS — 이 표에 locale/source/count/render/inspection 근거를 기록했다. |
| DIA-COM-01 | PASS — 한국어 prose, 구현 source, sibling set을 확인했다. |
| DIA-COM-02 | PASS — `goorm Sans` 계열과 locale label이 full-size에서 읽힌다. |
| DIA-COM-03 | N/A — text/card architecture이며 icon을 사용하지 않는다. |
| DIA-COM-04 | PASS — 3개 marker가 raster에서 solid color와 방향을 유지한다. |
| DIA-COM-05 | PASS — connectors `5`; intrusion/crossing/shared/label collision `0`; endpoint PASS. |
| DIA-COM-06 | PASS — mixed-corner paths `5`, `q_bends=2`, failures `0`. |
| DIA-COM-07 | PASS — 영문 sibling과 canvas/lane/whitespace 기하가 같다. |
| DIA-COM-08 | PASS — XML/render/common/architecture 감사가 모두 exit `0`. |
| DIA-COM-09 | PASS — review ledger와 한국어 README/guide가 canonical `-ko` output을 가리킨다. |
| DIA-ARC-01 | PASS — 객체 선택과 공통 infrastructure라는 정적 architecture 질문에 답한다. |
| DIA-ARC-02 | PASS — 영문 sibling과 같은 approved visual family를 사용한다. |
| DIA-ARC-03 | PASS — 영문과 같은 수평 구조와 균형 margin을 유지한다. |
| DIA-ARC-04 | PASS — connectors/endpoints/corners/crossings raster inspection PASS. |

## `infra-lettuce-sequence-02`

SVG/PNG는 `docs/images/readme-diagrams/infra-lettuce-sequence-02.svg`와 `.png`이며, 독자 질문은
“동일 request identity가 획득부터 모호한 결과 복구와 handle 해제까지 어떻게 이동하는가?”이다.

| ID | 결과와 근거 |
|---|---|
| DIA-01 | PASS — caller/facade/runtime/Lua-Redis와 acquire, contention, watchdog, reconcile, release, close 순서를 source에서 고정했다. |
| DIA-02 | PASS — common + sequence 규칙과 위의 authoritative PNG 두 개를 원본 크기로 열었다. |
| DIA-03 | PASS — 영문 sequence 하나만 편집하고 chronological invariant를 유지했다. |
| DIA-04 | PASS — XML parse, CairoSVG `-s 2`; PNG `3800x3120`. |
| DIA-05 | PASS — hazards `0`; markers `5`, connectors `9`; geometry/endpoint/mixed-corner failures `0`, paths `9`; sequence style PASS. |
| DIA-06 | PASS — full-size PNG에서 4 participants, lifelines, activations, 1..8 labels, frames, markers, footer를 확인했다. |
| DIA-07 | PASS — 영문 README/guide embed와 한국어 sibling 기하 parity를 확인했다. |
| DIA-08 | PASS — 이 표에 reference, counts, dimensions, full-size inspection을 기록했다. |
| DIA-COM-01 | PASS — final guide/source와 related sequence set을 확인했다. |
| DIA-COM-02 | PASS — readable row/label/theme, hazards `0`, clipping/tofu `0`. |
| DIA-COM-03 | N/A — participant text와 Redis label만 사용하며 logo icon이 없다. |
| DIA-COM-04 | PASS — 5개 fixed per-color triangle marker가 PNG에서 solid/일관 방향이다. |
| DIA-COM-05 | PASS — connectors `9`, intrusions/crossings/shared segments/label collisions `0`; endpoint PASS. |
| DIA-COM-06 | PASS — straight chronological messages, mixed-corner paths `9`, failures `0`. |
| DIA-COM-07 | PASS — `1900x1560` canvas, participant lanes, footer, bottom whitespace가 균형을 이룬다. |
| DIA-COM-08 | PASS — XML/render/common/sequence 명령이 모두 exit `0`. |
| DIA-COM-09 | PASS — ledger와 영문 README/guide가 canonical PNG를 가리킨다. |
| DIA-SEQ-01 | PASS — best-practices Redis sequence와 repo-local Lettuce sequence를 full-size로 비교했다. |
| DIA-SEQ-02 | PASS — participants `4`, lifelines `4`, activations `3`, numbered labels `1..8`, chronological frames `2`. |
| DIA-SEQ-03 | PASS — muted blue/green/amber/red/purple와 5개 matching marker를 유지했다. |
| DIA-SEQ-04 | PASS — 번호 `1..8`이 연속이며 label이 각 message 위에 있고 line과 겹치지 않는다. |
| DIA-SEQ-05 | PASS — 획득/경합 및 ambiguous cancellation frame은 투명하고 시간 순서와 footer clearance를 유지한다. |
| DIA-SEQ-06 | PASS — sequence style audit `PASS sequence_files=1`; full-size PNG와 모순이 없다. |

## `infra-lettuce-sequence-02-ko`

SVG/PNG는 `docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.svg`와 `.png`이며 영문과 같은
`1900x1560` 기하를 사용한다.

| ID | 결과와 근거 |
|---|---|
| DIA-01 | PASS — 한국어 guide/source에서 같은 lifecycle participant와 순서를 고정했다. |
| DIA-02 | PASS — common + sequence 규칙, authoritative reference 2개, 영문 sibling을 사용했다. |
| DIA-03 | PASS — 한국어 label만 자연스럽게 번역하고 기술 identifier/기하를 유지했다. |
| DIA-04 | PASS — XML parse, CairoSVG `-s 2`; PNG `3800x3120`. |
| DIA-05 | PASS — hazards `0`; markers `5`, connectors `9`; geometry/endpoint/mixed-corner failures `0`, paths `9`; sequence style PASS. |
| DIA-06 | PASS — 최초 tofu를 `goorm Sans` 계열로 교정한 최종 full-size PNG에서 font/clipping/overlap 이상이 없다. |
| DIA-07 | PASS — 한국어 README/guide가 정확한 `-ko.png`를 한 번씩 embed하며 영문 기하와 같다. |
| DIA-08 | PASS — 이 표에 locale 교정과 최종 검증 근거를 기록했다. |
| DIA-COM-01 | PASS — 한국어 prose, 구현 source, related sequence를 확인했다. |
| DIA-COM-02 | PASS — Korean-capable font, hazards `0`, full-size readability PASS. |
| DIA-COM-03 | N/A — text participant만 사용하고 icon은 없다. |
| DIA-COM-04 | PASS — 5개 marker의 solid color/크기/방향이 raster에서 일치한다. |
| DIA-COM-05 | PASS — connectors `9`, intrusion/crossing/shared/label collision `0`; endpoint PASS. |
| DIA-COM-06 | PASS — mixed-corner paths `9`, failures `0`. |
| DIA-COM-07 | PASS — 영문 sibling과 lane/canvas/whitespace 기하가 같다. |
| DIA-COM-08 | PASS — XML/render/common/sequence 명령이 모두 exit `0`. |
| DIA-COM-09 | PASS — ledger와 한국어 README/guide가 canonical `-ko` PNG를 가리킨다. |
| DIA-SEQ-01 | PASS — authoritative reference 2개와 영문 sibling을 full-size로 비교했다. |
| DIA-SEQ-02 | PASS — participants `4`, lifelines `4`, activations `3`, 번호 `1..8`, frames `2`. |
| DIA-SEQ-03 | PASS — 영문과 동일한 muted palette/5개 marker를 유지한다. |
| DIA-SEQ-04 | PASS — 번호 `1..8`, label-over-line, 충분한 row height를 확인했다. |
| DIA-SEQ-05 | PASS — 두 chronological frame의 transparency, padding, divider, footer clearance를 확인했다. |
| DIA-SEQ-06 | PASS — sequence style audit `PASS sequence_files=1`; 최종 PNG와 모순이 없다. |

## 최종 집계

`Required checks: 84/84; N/A: 4; Blocked: 0`
