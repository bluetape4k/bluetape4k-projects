# 2026-05-20 — Benchmark result charts

## Context

Benchmark result documents were hard to scan because several pages used ASCII
bars or Markdown tables only. Some benchmark results also lived in linked
documents outside module README files.

## Decision

Add static SVG + PNG charts under `docs/images/readme-charts/` and link the PNG
from benchmark result documents. Keep the numeric tables as the source of truth.
Use English labels inside the images, pastel bars, Architects Daughter titles,
and Comic-style body labels.

## Outcome

Charts now cover Lettuce near-cache, Lettuce/Redisson codecs, I/O serializer and
compressor results, ID generator throughput, and the FastFory codec uplift note.

## Verification

- `xmllint --noout docs/images/readme-charts/*.svg`
- `identify docs/images/readme-charts/*.png`
- Manual visual spot-check for the near-cache and ID generator charts after
  increasing chart height to avoid clipping.

## Future

When adding benchmark numbers, prefer a chart next to the result table. Use log
scale when values differ by orders of magnitude.
