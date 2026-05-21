# io/io benchmark charts

## Context

The `io/io` README benchmark section linked generated `readme-diagrams/io-io-diagram-03..05` images even though the content was throughput data. One generated SVG converted chart syntax into component boxes such as `ops/s`, `y`, and `axis`.

## Decision

Use `docs/images/readme-charts/` for benchmark visuals and remove the broken benchmark diagrams from `readme-diagrams`.

## Outcome

The README benchmark section now links chart images for fast serializer throughput, binary serializer payload comparison, and compressor throughput. Benchmark naming also uses the current `Fory` source API name instead of the stale `Fury` label.

## Verification

- `xmllint --noout` on touched chart SVG files
- `rsvg-convert` PNG rendering
- README image-link scan for `io/io`
- Visual spot-check of the new fast serializer chart

## Future note

When a Mermaid or generated image source encodes benchmark values, render it as a chart under `docs/images/readme-charts/`, not as a component diagram.
