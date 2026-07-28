# README Mermaid SVG Infographics

## 배경

README file은 Mermaid diagram을 직접 사용했다. 요청된 documentation presentation은
pastel infographic-style SVG image였고, `sequenceDiagram` block은 Mermaid source로 유지했다.

## 결정

README file의 모든 non-sequence Mermaid block을 `docs/images/readme-diagrams/` 아래 SVG로 render한다.
Shared pastel Mermaid theme과 diagram-type-safe render normalization을 사용한다. Rendered Mermaid
block만 relative SVG image link로 교체한다.

## 결과

Flowchart, graph, classDiagram, xychart, bar-derived, gantt, block-beta, state diagram에 대한
SVG asset을 생성했다. README sequence diagram은 Mermaid code block으로 남긴다.

## 검증

Mermaid CLI 11.14.0으로 계획된 모든 SVG file을 render했다. README conversion count를 확인하고
`git diff --check`를 실행했다.

## 향후 가이드

README diagram conversion은 먼저 render하고, 모든 SVG file이 존재하는 것을 확인한 뒤 README edit을
확정한다. Repository-wide documentation rewrite에서 `.worktrees`는 제외 상태로 유지한다.
