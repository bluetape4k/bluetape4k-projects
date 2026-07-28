# README section order와 benchmark chart

## 배경

여러 module README에서 architecture diagram이 문서 끝부분에 있었고, benchmark table 옆에는 chart image가
없었다. Generated cache-lettuce KO flowchart 하나는 cache stability contract를 설명하지 않고
placeholder-like node를 포함했다.

## 결정

Architecture Diagrams를 touched README의 상단 근처로 옮기고, benchmark table은 numeric source of truth로
유지하며, benchmark result table 바로 뒤에 chart image를 추가한다.

## 결과

이번 pass에서 수정한 data, infra, cache, IO README는 usage detail보다 먼저 overview architecture를 보여주고,
benchmark value는 혼란스러운 diagram이나 table-only section 대신 chart로 보여준다.

## 검증

- 수정한 SVG asset에 `xmllint --noout`
- Generated chart/diagram에 `rsvg-convert` PNG rendering
- README 및 Benchmark image-link scan
- Generated chart/diagram asset visual spot-check
- `git diff --check`

## 향후 노트

README visual은 section meaning 기준으로 audit한다. Architecture는 상단 근처에 두고, benchmark result는
`docs/images/readme-charts`에 둔다. Placeholder node를 가진 generated diagram은 label만 바꾸지 말고 교체한다.
