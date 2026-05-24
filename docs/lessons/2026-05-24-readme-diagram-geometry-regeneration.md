# 2026-05-24 — README Diagram Geometry Regeneration

## Context

The README diagram assets needed regeneration under the workspace diagram style
rules. Sequence diagrams also needed explicit geometry cleanup: preserve outer
canvas/frame margin while reducing internal message-to-arrow gaps.

## Decision

Normalize SVG sources first, then regenerate PNGs from those sources. Sequence
assets now use compact vertical message spacing, 80px body-side margins, centered
participant headers, and non-collapsed self-call checks. Class/component assets
remove empty stereotype rows so unlabeled UML headers center their class names.

## Outcome

All `docs/images/readme-diagrams/*.svg` sources were normalized and all matching
PNG assets were regenerated. README links now embed PNG assets only while SVG
files remain as editable sources.

## Verification

- SVG parse: `xmllint --noout` passed for 415 SVG files.
- PNG render: `rsvg-convert` regenerated 415 PNG files.
- Geometry audit: sequence outer margin, sequence label-arrow gap, self-call,
  participant header baseline, empty stereotype, and marker-only checks all
  reported zero failures.
- README image scan: 172 README files, missing links 0, SVG embeds 0, Mermaid
  residue 0.
- `git diff --check` passed.
- Visual QA montage reviewed at
  `/tmp/bluetape4k-readme-qa/projects-diagrams-20260524.png`.

## Future Guidance

When regenerating README diagrams, validate both syntax and geometry. Sequence
diagrams should have generous outer margins but compact internal message
spacing; component/class diagrams should not keep empty stereotype rows that
push class names off center.
