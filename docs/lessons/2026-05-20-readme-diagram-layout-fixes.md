# README Diagram Layout Fixes

## Context

Follow-up visual QA found two layout defects in generated README diagrams:

- some architecture connectors were rendered as very short line segments where only the arrow head was visible
- sequence participant header labels were vertically biased toward the top of the header box

A related sequence issue was also fixed: self-calls previously rendered as zero-length arrows, which looked like a standalone arrow head.

## Decision

Keep the existing diagram style and update only geometry in the generated SVG/PNG assets. Architecture connector line segments must span the visible gap between adjacent cards. Sequence participant labels must use the same vertical-centering baseline as architecture cards. Sequence self-calls should render as a small loop instead of a zero-length line.

## Verification

- README image link check: missing=0, localSvgImageLinks=0, mermaidResidue=0
- PNG/SVG shape check: shapeCandidates=0
- architecture short connector check: shortArch=0
- sequence header alignment check: seqTop=0
- sequence zero-length arrow check: zeroSeq=0
- `git diff --check`
- visual samples reviewed for exposed root architecture and representative sequence diagrams

## Future Guidance

Treat arrow head-only connectors as a failed rendering even when the SVG is syntactically valid. Geometry checks should cover architecture connector length, sequence header baseline, and sequence self-call arrows before PR creation.
