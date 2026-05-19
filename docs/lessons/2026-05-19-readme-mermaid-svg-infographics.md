# README Mermaid SVG infographics

## Context

README files used live Mermaid diagrams. Some Korean diagram labels were clipped
in generated images, and sequence diagrams also needed the same SVG treatment.

## Decision

Render every README Mermaid block to SVG under `docs/images/readme-diagrams/`
using English diagram labels and a shared Mermaid theme that prefers Comic Mono.
Korean README diagrams use the matching English README Mermaid source when
available.

## Outcome

Generated checked-in SVG assets for all README Mermaid diagrams and replaced the
Mermaid blocks with relative image links.

## Verification

Rendered SVG assets with Mermaid CLI 11.14.0, verified SVG link/file counts,
verified zero remaining README Mermaid blocks, checked generated SVGs for Korean
text, and ran `git diff --check`.

## Future Guidance

Render from the base branch source when updating existing diagram-image PRs, so
previous image-link conversions do not hide the original Mermaid blocks.
