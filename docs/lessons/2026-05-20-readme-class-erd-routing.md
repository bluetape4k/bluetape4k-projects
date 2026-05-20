# README Class/ERD Routing

## Context

README class and ERD images were regenerated across the bluetape4k workspace for reuse in documentation, blog posts, and presentations.

## Decision

Use orthogonal connector routing with blocker-aware lane selection for class and ERD diagrams. Keep pastel colors and existing typography, but avoid cubic curves and connector paths that cross component interiors.

## Outcome

The regenerated class/ERD SVGs use relation-aware component placement, straight horizontal/vertical lanes, smaller arrow markers, and top/bottom ports with vertical first and final segments, and horizontal lanes placed near row midlines instead of component edges.

## Verification

- `node --check .omx/scripts/refine-readme-diagrams.mjs`
- Changed class/ERD SVGs: cubic connector count `0`
- Changed class/ERD SVGs: card-interior crossing candidates `0`

## Future Guidance

When diagrams are regenerated, preserve the blocker-aware route scoring and inspect contact sheets before accepting broad image churn.
