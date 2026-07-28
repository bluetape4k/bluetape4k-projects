# 2026-05-24 — README Diagram Geometry Regeneration

## 배경

README diagram asset은 workspace diagram style rule에 맞춰 regeneration이 필요했다. Sequence diagram은
outer canvas/frame margin은 유지하면서 internal message-to-arrow gap을 줄이는 explicit geometry cleanup도
필요했다.

## 결정

먼저 SVG source를 normalize한 뒤 그 source에서 PNG를 regenerate한다. Sequence asset은 compact vertical
message spacing, 80px body-side margin, centered participant header, non-collapsed self-call check를
사용한다. Class/component asset은 unlabeled UML header가 class name을 중앙에 둘 수 있도록 empty stereotype
row를 제거한다.

## 결과

모든 `docs/images/readme-diagrams/*.svg` source를 normalize했고 matching PNG asset을 모두 regenerate했다.
README link는 PNG asset만 embed하고 SVG file은 editable source로 남긴다.

## 검증

- SVG parse: 415개 SVG file에 대해 `xmllint --noout` 통과.
- PNG render: `rsvg-convert`로 415개 PNG file regenerate.
- Geometry audit: sequence outer margin, sequence label-arrow gap, self-call, participant header baseline,
  empty stereotype, marker-only check 모두 failure 0.
- README image scan: README file 172개, missing link 0, SVG embed 0, Mermaid residue 0.
- `git diff --check` 통과.
- `/tmp/bluetape4k-readme-qa/projects-diagrams-20260524.png` visual QA montage review.

## 향후 가이드

README diagram을 regenerate할 때는 syntax와 geometry를 모두 검증한다. Sequence diagram은 넉넉한 outer
margin과 compact internal message spacing을 함께 가져야 한다. Component/class diagram은 class name을
중앙에서 밀어내는 empty stereotype row를 유지하지 않는다.
