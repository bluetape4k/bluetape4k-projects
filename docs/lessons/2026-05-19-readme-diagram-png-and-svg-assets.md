# README Diagram PNG and SVG Assets

Date: 2026-05-19

## Context

README Mermaid diagrams were converted to English SVG infographics. A first PNG pass used `rsvg-convert`, but Mermaid labels are stored as SVG `foreignObject` HTML and that renderer omitted the text.

## Decision

README files embed PNG diagram assets rendered through macOS Quick Look/WebKit so text is visible, and the adjacent SVG files remain in the repository as reusable source assets.

## Outcome

Each README diagram under `docs/images/readme-diagrams/` has a matching `.svg` source and `.png` display asset. README files reference the PNG display asset.

## Verification

- README Mermaid blocks: 0
- README SVG diagram links: 0
- README PNG diagram links: 479
- SVG files: 479
- PNG files: 479
- Quick Look rendered PNG files from SVG sources.
- `git diff --check --cached` passed for staged documentation and image assets.

## Future Guidance

When a README diagram changes, edit or regenerate the SVG source and render the matching PNG with a WebKit-capable renderer. Avoid `rsvg-convert` for Mermaid SVGs that contain `foreignObject` labels.
