# Issue 659 README Graphviz Diagrams

## Context

Issue #659 needed the root README catalog refreshed and the Ktor module README
pages made easier to understand. Existing active README diagrams had SVG/PNG
assets but no committed Graphviz `.dot`, `.plain`, or sketch evidence.

## Decision

Regenerate the active root README diagrams and the Ktor idgenerator demo
diagram with Graphviz evidence beside the final SVG/PNG assets. Add new Ktor
module diagrams for core, observability, and testing, and embed the same
English-label PNGs in both English and Korean README files.

## Outcome

The root overview, root module structure, Ktor core architecture, Ktor
observability component, Ktor testing sequence, and idgenerator Ktor demo
architecture diagrams now have `.dot`, `.plain`, `-sketch.svg`, final `.svg`,
and final `.png` assets. The stale Spring Boot idgenerator demo README link now
points to the current `examples/spring-boot/idgenerator-spring-boot-demo`
location.

## Verification

- Generated Graphviz `.plain` and sketch SVG assets for each touched diagram.
- Saved `docs/images/readme-diagrams/issue-659-graphviz-summary.md` with
  node/edge comparison counts and zero missing nodes or edges.
- Rendered and inspected a contact sheet of all six PNGs at README scale.

## Future Guidance

For README diagrams with nodes and connectors, do not update only SVG/PNG.
Keep Graphviz `.dot`, `.plain`, and sketch SVG evidence beside the final
assets, and inspect the rendered PNG before reporting completion.
