# 2026-05-20 — README overview visual placement

## Context

README diagrams and charts need to be treated as source-backed documentation,
not as decorative generated assets. The current pass used the 2026 reference
documents and the shared README diagram style guide, but source code and build
layout remained the authority for module names and grouping.

## Decision

Add English-only SVG+PNG README overview visuals for the root README and place
the overview diagram before installation, usage, or build instructions. Move
existing Architecture/Diagram sections upward when they were appended after
usage examples.

## Outcome

`bluetape4k-projects` now has a root README overview diagram and module composition chart, and
its README visual placement follows the overview-first rule. Generated labels
avoid localized text inside the images.

## Verification

- Generated SVG files parsed with `xmllint --noout`.
- Generated PNG files rendered with `rsvg-convert`.
- Workspace README image-link scan reported zero missing local images.
- Workspace Architecture/Diagram ordering scan reported zero remaining sections
  behind Installation, Usage, Examples, or Build headings.
- Generated root overview SVG text contained no non-ASCII characters.

## Future Note

Do not append architecture diagrams to the end of README files. Keep overview
or architecture diagrams near the top, then place class, sequence, ERD, or flow
diagrams beside the section they explain.

Root overview diagrams and composition charts place BOM first when present and Examples or Additional examples last when present; middle groups keep the source-backed orientation order unless a repo-specific README calls for alphabetic grouping.
