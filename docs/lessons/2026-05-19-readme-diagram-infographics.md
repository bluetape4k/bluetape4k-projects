# README Diagram Infographics

## 배경

README file은 architecture, class, sequence, ERD 등 여러 diagram에 Mermaid code block을 사용했다.
Workspace-wide visual direction은 검토된 pastel infographic PNG로 바뀌었고, 재사용을 위해 SVG source
asset을 함께 보존하기로 했다.

## 결정

README Mermaid block을 generated PNG image link로 교체하고, matching SVG source를 PNG file 옆에
저장한다. Diagram text는 영어만 사용하고, 큰 label에는 Architects Daughter, detail text에는
Comic Mono를 사용하며, architecture/class/sequence/ERD diagram마다 전용 layout을 쓴다.

## 결과

bluetape4k.github.io/docs/readme-diagram-samples의 shared 2026-05-19 style guide로 README diagram을
render했다. Root README asset은 repo-local asset placement rule이 있으면 그 rule을 따른다.

## 검증

Cross-repository conversion pass에서 rsvg-convert로 PNG/SVG asset을 생성하고 README link를 확인했다.

## 향후 가이드

README diagram은 SVG source를 함께 둔 PNG embed로 유지한다. Visual consistency가 중요할 때 raw
Mermaid나 단순 Mermaid theme recoloring으로 되돌리지 않는다.
