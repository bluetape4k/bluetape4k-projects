# Logging Diagram Connector Repair Design

## Problem

The three logging manual diagrams render arrowheads short of their target card, activation bar, or lifeline. The existing generic audits report valid paths, but the authoritative PNG still reads as disconnected.

## Approved visual contract

- Every arrow tip lands exactly on the target boundary or lifeline.
- A card-to-card connector leaves and enters the touched edge perpendicularly.
- Multiple inbound relations use separate ports; they do not converge into a floating horizontal rail.
- Sequence messages start and end on the participant lifeline or activation-bar edge, not at an arbitrary standoff coordinate.
- Architecture primary-flow arrowheads render at the guide size `14×14`; sequence message arrowheads render at `16×16`.
- Arrowhead color matches its line, the head is solid even on dashed returns, and the connected PNG does not make the head read as a detached small glyph.
- No connector crosses a card, label, frame title, or unrelated line.
- CairoSVG PNG output at scale 2 is authoritative; any visible gap returns the asset to editing even when scripts pass.

## Asset decisions

1. `logger-api-map`: replace the two side-entry floating arrows with rounded orthogonal routes that turn downward into separate top ports on the SLF4J card; extend both vertical arrows to their target card boundaries.
2. `mdc-scope-lifecycle`: extend both horizontal progression arrows to the next card boundary.
3. `async-channel-sequence`: normalize every message endpoint to the corresponding lifeline or activation-bar edge.

No text, color semantics, source model, card placement, or manual prose changes are required. Marker geometry may change only if the authoritative PNG cannot meet the guide-sized visual invariant with the current marker definition.
