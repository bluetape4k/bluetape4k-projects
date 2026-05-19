# README Diagram PNG and SVG Assets

Date: 2026-05-19

## Context

README Mermaid diagrams were converted to English Comic Mono SVG infographics. GitHub's SVG rendering can miss local fonts, which can clip diagram text for readers.

## Decision

README files embed PNG diagram assets for stable display, and the adjacent SVG files remain in the repository as reusable source assets.

## Outcome

Each README diagram under `docs/images/readme-diagrams/` has a matching `.svg` source and `.png` display asset.

## Verification

- README Mermaid blocks: 0
- README SVG diagram links: 0
- Referenced PNG diagrams: 479
- SVG files: 479
- PNG files: 479
- `git diff --check --cached` passed for staged documentation and image assets.

## Future Guidance

When a README diagram changes, edit the SVG source and regenerate the matching PNG before updating the README link.
