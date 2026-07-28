# io/io benchmark chart

## 배경

`io/io` README benchmark section은 throughput data임에도 generated `readme-diagrams/io-io-diagram-03..05`
image를 link했다. Generated SVG 하나는 chart syntax를 `ops/s`, `y`, `axis` 같은 component box로
변환했다.

## 결정

Benchmark visual은 `docs/images/readme-charts/`를 사용하고, 깨진 benchmark diagram은 `readme-diagrams`에서
제거한다.

## 결과

README benchmark section은 fast serializer throughput, binary serializer payload comparison,
compressor throughput에 대한 chart image를 link한다. Benchmark naming도 stale `Fury` label 대신 현재
source API name인 `Fory`를 사용한다.

## 검증

- 수정한 chart SVG file에 `xmllint --noout`
- `rsvg-convert` PNG rendering
- `io/io` README image-link scan
- 새 fast serializer chart visual spot-check

## 향후 노트

Mermaid 또는 generated image source가 benchmark value를 encode한다면 component diagram이 아니라
`docs/images/readme-charts/` 아래 chart로 render한다.
