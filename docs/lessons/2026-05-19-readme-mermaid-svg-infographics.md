# README Mermaid SVG infographics

## Context

README files used Mermaid diagrams directly. The requested documentation
presentation was pastel infographic-style SVG images, while keeping
`sequenceDiagram` blocks as Mermaid source.

## Decision

Render every non-sequence Mermaid block in README files to SVG under
`docs/images/readme-diagrams/`, using a shared pastel Mermaid theme and
diagram-type-safe render normalization. Replace only the rendered Mermaid
blocks with relative SVG image links.

## Outcome

Generated SVG assets for flowchart, graph, classDiagram, xychart, bar-derived,
gantt, block-beta, and state diagrams. README sequence diagrams remain as
Mermaid code blocks.

## Verification

Rendered all planned SVG files with Mermaid CLI 11.14.0. Verified the README
conversion counts and ran `git diff --check`.

## Future Guidance

For README diagram conversion, render first, finalize README edits only after
all SVG files exist, and keep `.worktrees` excluded from repository-wide
documentation rewrites.
