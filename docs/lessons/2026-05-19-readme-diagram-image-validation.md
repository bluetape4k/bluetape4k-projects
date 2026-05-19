# README Diagram Image Validation

## Context

README Mermaid diagrams were replaced with pastel infographic PNG images while
preserving matching SVG sources.

## Decision

Use English-only diagram labels, PNG README embeds, preserved SVG assets, and
content-driven dimensions instead of fixed-size Mermaid recolors.

Large labels use `Architects Daughter`; detail labels use the clearest
Comic-style fallback available to the renderer.

## Outcome

The refined renderer produced architecture, class, sequence, and module-stack
images without fixed height constraints. Grouped architecture diagrams use
content-sized sections and masonry placement where needed.

## Verification

- Full regeneration: `rendered=477`, `missing=[]`.
- README image links: `readmes=169`, `missing=0`, `svgLinks=0`.
- Asset counts: `png=415`, `svg=415`.
- Mermaid README blocks: `0`.
- Shape sanity check: `shapeCandidates=0`.
- Whitespace check: `git diff --check`.

## Future Guidance

Do not rely on link checks alone. Always run visual-shape checks and inspect
known-risk diagrams such as JUnit5, Testcontainers, wide class hierarchies, and
sequence diagrams before opening PRs.
