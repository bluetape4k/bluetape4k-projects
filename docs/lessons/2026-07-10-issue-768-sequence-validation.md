# Issue #768 Sequence Diagram Validation

## Context

Sequence README assets failed validation even when their rendered structure was
valid. The validator depended on generator-specific marker IDs and one exact
label class, while several diagrams also contained real text overflow and
branch-clarity defects.

## Decision

- Validate fixed-size filled markers and visible numbered pill labels by SVG
  structure, not by one generator's names.
- Treat generic message pills as message labels, not footer elements.
- Repair and render each failing asset individually.
- Keep branch outcomes visually distinct when they share one sequence frame.

## Outcome

The sequence family now has zero validator failures. Five SVG/PNG pairs were
corrected without rewriting already-valid sequence semantics.

## Verification

- README diagram validator: `total=268 failed=137`, sequence failures `0`
- Sequence style audit: `PASS sequence_files=5`
- Geometry, endpoint, and mixed-corner audits: zero failures
- Connector audit: PASS for four assets
- `cache-cache-core-sequence-02`: fallback invariant `messages=6`,
  `direct_solid_heads=6`, `participants=4`
- CairoSVG render and PNG visual inspection completed for every changed asset

## Future Guidance

Do not regenerate an entire diagram family to satisfy structural validation.
First separate validator false positives from visible defects, then edit and
inspect one asset at a time. A generic audit reporting `connectors=0` requires
an explicit invariant that proves every visible message has a rendered
arrowhead.
