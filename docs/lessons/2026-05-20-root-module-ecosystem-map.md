# Root Module Ecosystem Map

## Context

The root README module diagram was tall and grouped modules loosely by recovered
Mermaid output. It did not clearly explain the current root repository boundary
versus split bluetape4k repositories.

## Decision

Replace `root-readme-en-diagram-01.{svg,png}` with a compact pastel ecosystem
map titled as the Bluetape4k framework rather than the narrower Projects repo.
The new map centers the BOM, groups root modules by foundation, I/O, data,
infrastructure, Spring integration, testing, utilities, and examples, and shows
AWS, Exposed, Image, Text, Graph, JaVers, and Leader as split repositories.

## Outcome

The root README now presents the module structure as an architecture map rather
than a long module list image.

## Verification

- SVG XML parsed successfully.
- PNG rendered with `rsvg-convert` at 1400x980.
- Visual inspection confirmed readable labels and no obvious overlap.

## Future Note

Keep the root module map focused on architecture boundaries. Detailed per-module
diagrams belong in module READMEs.
Use `boot4` for Spring integration labels; do not use older Spring Boot line
labels in current README visuals.
