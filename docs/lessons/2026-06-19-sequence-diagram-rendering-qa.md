# Sequence diagram rendering QA

## Context

The README diagram refresh standardized every sequence SVG/PNG under
`docs/images/readme-diagrams` to the Spring Boot Cassandra best-practices style.
The visual pass exposed several renderer-specific failures that did not show up
as simple XML or SVG-source errors:

- participant boxes could become dark because a CSS selector intended for text
  also matched `rect.participant`;
- call labels could look washed out in PNG because a shared `.label` style was
  applied to both text and pill shapes;
- numbered badges, label text, message lines, and arrowheads could drift into
  different colors when a diagram reused old classes or reordered label groups;
- arrowheads looked acceptable in the raw SVG but became too small after
  CairoSVG conversion;
- stray placeholder number badges such as `?` could survive a broad style pass;
- contact-sheet thumbnails could hide dense-label defects, so risky diagrams
  still needed individual PNG inspection.

## Decision or Finding

Treat the rendered PNG as the authoritative README artifact. SVG source checks
are necessary, but they are not sufficient for sequence diagrams.

For sequence diagrams, each message row must be validated as one unit:

- the message path stroke defines the message color;
- the marker arrowhead must use the same color as the path stroke;
- the call label pill outline, badge circle, and label text must use the same
  message color;
- the badge number text must be white;
- the label shape and label text must not share a class that lets CSS style text
  as a shape or hide text under a pill;
- participant/header rectangles must have explicit light fills and should not
  inherit text-only selector styles;
- dashed `alt` or return lines still need readable arrowheads after PNG
  conversion.

The same rule applies to class/UML diagrams with dashed relationships: dashed
lines must not make hollow triangles or open arrowheads render as dashed,
broken, undersized, or check-like. If marker-based arrowheads inherit
`stroke-dasharray` in PNG, hard-override the marker child path with
`stroke-dasharray="none"` and `style="stroke-dasharray:none"`. If that still
fails, draw the arrowhead as direct `polygon` or `polyline` geometry with a
solid stroke and route the final segment perpendicular to the target edge.

## Outcome

The sequence diagram refresh was fixed by separating text and shape selectors,
adding explicit light participant rectangle styling, using PNG-safe fixed-size
sequence arrow markers, and synchronizing message colors by message number
rather than by nearby XML order alone.

The specific regression that prompted this lesson was
`examples-spring-boot-observability-spring-boot-demo-sequence-01`: participant
boxes were rendered as dark filled boxes in PNG because `.participant` styling
was too broad. The fix was to scope text styling to `text.participant` and set
`rect.participant` to a white fill with an explicit stroke.

## Verification

The final sequence diagram pass used this verification stack:

- XML parse all sequence SVG files.
- Reject placeholder badges such as `>?</text>`.
- Count numbered message badges against marker-ended message paths.
- Verify every marker color matches its message path stroke.
- Verify every numbered badge uses white number text.
- Verify badge, label, line, and arrowhead colors from message number to message
  color, not only from raw SVG source order.
- Render every sequence SVG to PNG with
  `~/.local/bin/cairosvg <diagram>.svg -o <diagram>.png -s 2`.
- Generate contact sheets for the full sequence set and inspect them.
- Open dense or high-risk PNGs individually, especially diagrams with many
  labels, fallback paths, or previously broken participant boxes.
- Run `git diff --check`.

## Future Guidance

Do not claim sequence diagram completion from SVG inspection or render success
alone. Always inspect the PNG, because CairoSVG can change the apparent size,
dash behavior, and readability of markers and text.

When changing one arrowhead or label rule, search for equivalent SVG patterns
across the diagram set before committing. The failure mode is usually systemic:
old classes, shared selectors, dashed marker inheritance, or marker scaling
rules can appear in many files even if only one diagram was reported.

For sequence diagrams, prefer a stable message schema:

- message line: explicit `stroke`, fixed `marker-end`, and rounded line joins;
- marker: `markerUnits="userSpaceOnUse"` when fixed PNG size matters;
- label pill: a rect-specific class such as `labelPill`;
- label text: a text-specific class or explicit fill;
- badge circle: fill and stroke equal to the message color;
- badge number: explicit white fill;
- participant text and participant rectangle styles: separate selectors.

For dashed UML/class relationships, run a dedicated dashed-arrowhead audit
before commit. Search for dashed lines with marker heads, hollow/open marker
definitions, standalone triangle paths, and direct arrowhead geometry. Then
zoom into the rendered PNG and reject any dashed, broken, tiny, or check-like
arrowhead even when the raw SVG looks correct.

Contact sheets are only a broad sweep. If a contact sheet shows a dense area,
long fallback branch, tiny label, or recently edited arrowhead, open that PNG at
full size before accepting it.
