# README Diagram Infographics

## Context

README files used Mermaid code blocks for architecture, class, sequence, ERD, and other diagrams. The workspace-wide visual direction changed to reviewed pastel infographic PNGs with SVG source assets kept for reuse.

## Decision

Replace README Mermaid blocks with generated PNG image links and store matching SVG sources next to the PNG files. Use English-only diagram text, Architects Daughter for large labels, Comic Mono for detail text, and diagram-specific layouts for architecture, class, sequence, and ERD diagrams.

## Outcome

Rendered README diagrams with the shared 2026-05-19 style guide from bluetape4k.github.io/docs/readme-diagram-samples. Root README assets follow repo-local asset placement rules when present.

## Verification

Generated PNG/SVG assets with rsvg-convert and checked README links during the cross-repository conversion pass.

## Future Guidance

Keep README diagrams as PNG embeds with SVG sources for editing. Do not fall back to raw Mermaid or simple Mermaid theme recoloring when visual consistency matters.
