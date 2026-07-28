# 2026-05-21 — README Diagram Semantics

## 배경

README visual은 기존 diagram-like content를 너무 문자 그대로 변환했다. 그 결과 다른 visual form이 더
적절한 곳에서도 stale하거나 약한 semantics가 보존되었다.

## 결정

이전 asset type이 아니라 section의 의미로 visual type을 선택한다. Architecture section은 module/flow
diagram을 사용하고, state/workflow section은 control transition을 드러내며, benchmark result section은
chart를 사용한다.

## 결과

`io/http` cache benchmark는 documented JMH result 기반 log-scale throughput chart를 사용한다.
`infra/elasticsearch`와 `infra/micrometer`는 README 상단 근처에 architecture diagram을 둔다.
`utils/workflow` diagram은 transient path, terminal state, strategy-specific branch를 분리한다.

## 검증

- SVG file을 `xmllint --noout`으로 parse.
- PNG file을 `rsvg-convert`로 render.
- README image-link scan 통과.
- `git diff --check` 통과.
- `/tmp/bluetape4k-readme-qa/projects-targeted-diagrams-v2.png` visual QA montage review.

## 향후 노트

기존 Mermaid나 ASCII block을 맹목적으로 변환하지 않는다. README section과 source/test evidence를 먼저
읽고 diagram, state diagram, sequence diagram, chart, 또는 image 없음 중 올바른 artifact를 결정한다.
