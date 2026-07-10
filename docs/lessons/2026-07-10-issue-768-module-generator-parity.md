# Issue #768 Module Diagram Generator Parity

## Context

The Redis umbrella diagram had correct visible content but lacked source-backed
validation metadata. Running its generator exposed a second defect: the
generator no longer reproduced the approved rounded connectors, direct heads
for dashed routes, or icon provenance stored in the committed SVG.

## Decision

- Derive diagram intent from the umbrella Gradle dependency declaration, README,
  and the separate Spring Redis serializer source.
- Store the evidence in the generated SVG root.
- Encode rounded routes and Cairo-safe direct arrowheads in the owning generator
  before accepting regenerated output.
- Keep the PNG unchanged when metadata and generator parity do not alter pixels.

## Outcome

The module diagram validator failure was removed without changing the rendered
image. The generator now reproduces the committed SVG and PNG idempotently.

## Verification

- README diagram validator: `total=268 failed=136`
- Target row: `failures=[]`
- Generator and PNG render SHA checks: idempotent
- Connector audit: `connectors=4`, `intrusions=0`, `crossings=0`
- Geometry and endpoint audits: PASS
- Mixed-corner audit: `q_bends=12`, `failures=0`

## Future Guidance

When adding validation metadata to a generated asset, regenerate before editing
the SVG manually. If the output differs beyond metadata, repair generator parity
first and verify that the PNG remains visually equivalent.
