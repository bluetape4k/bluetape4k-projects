# 이슈 #768 Module diagram generator parity

## 배경

Redis umbrella diagram은 visible content가 올바랐지만 source-backed validation
metadata가 없었다. Generator를 실행하자 두 번째 결함이 드러났다. Generator가 더 이상
승인된 rounded connector, dashed route의 direct head, committed SVG에 저장된 icon
provenance를 재현하지 못했다.

## 결정

- Umbrella Gradle dependency declaration, README, 별도 Spring Redis serializer source에서 diagram intent를 도출한다.
- Evidence를 generated SVG root에 저장한다.
- Regenerated output을 승인하기 전에 owning generator에 rounded route와 Cairo-safe direct arrowhead를 encode한다.
- Metadata와 generator parity가 pixel을 바꾸지 않으면 PNG는 그대로 둔다.

## 결과

Rendered image를 바꾸지 않고 module diagram validator failure를 제거했다. Generator는
이제 committed SVG와 PNG를 idempotent하게 재현한다.

## 검증

- README diagram validator: `total=268 failed=136`
- Target row: `failures=[]`
- Generator and PNG render SHA checks: idempotent
- Connector audit: `connectors=4`, `intrusions=0`, `crossings=0`
- Geometry and endpoint audits: PASS
- Mixed-corner audit: `q_bends=12`, `failures=0`

## 향후 지침

Generated asset에 validation metadata를 추가할 때는 SVG를 수동 편집하기 전에 먼저
regenerate한다. Output이 metadata 이상으로 달라지면 generator parity를 먼저 고치고
PNG가 시각적으로 동등하게 유지되는지 검증한다.
