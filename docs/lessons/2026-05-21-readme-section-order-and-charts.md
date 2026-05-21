# README section order and benchmark charts

## Context

Several module READMEs had architecture diagrams near the end of the document, and benchmark tables without adjacent chart images. One generated cache-lettuce KO flowchart also contained placeholder-like nodes instead of explaining the cache stability contracts.

## Decision

Move Architecture Diagrams near the top of each touched README, keep benchmark tables as numeric source of truth, and add chart images immediately after benchmark result tables.

## Outcome

The data, infra, cache, and IO READMEs touched in this pass now show overview architecture before usage detail and show benchmark values as charts instead of confusing diagrams or table-only sections.

## Verification

- `xmllint --noout` on touched SVG assets
- `rsvg-convert` PNG rendering for generated charts and diagrams
- README and Benchmark image-link scan
- Visual spot-check for generated chart/diagram assets
- `git diff --check`

## Future note

Audit README visuals by section meaning: architecture belongs near the top; benchmark results belong in `docs/images/readme-charts`; generated diagrams with placeholder nodes should be replaced, not relabeled.
