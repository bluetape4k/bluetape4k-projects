# 2026-05-21 — README Diagram Semantics

## Context

README visuals had been generated from existing diagram-like content too
literally. That preserved stale or weak semantics in places where another
visual form was more appropriate.

## Decision

Choose the visual type from the section's meaning, not from the previous asset
type. Architecture sections should use module/flow diagrams, state and workflow
sections should expose control transitions, and benchmark result sections
should use charts.

## Outcome

The `io/http` cache benchmark now uses a log-scale throughput chart based on
the documented JMH results. `infra/elasticsearch` and `infra/micrometer` use
architecture diagrams near the top of their READMEs. `utils/workflow` diagrams
now separate transient paths, terminal states, and strategy-specific branches.

## Verification

- SVG files parsed with `xmllint --noout`.
- PNG files rendered with `rsvg-convert`.
- README image-link scan passed.
- `git diff --check` passed.
- Visual QA montage reviewed at `/tmp/bluetape4k-readme-qa/projects-targeted-diagrams-v2.png`.

## Future Note

Do not blindly convert existing Mermaid or ASCII blocks. Read the README section
and source/test evidence first, then decide whether the correct artifact is a
diagram, state diagram, sequence diagram, chart, or no image at all.
