# Logging Connector Repair Checklist

## Classification and invariants

- Scope: three canonical logging SVG/PNG pairs; derived site snapshot only.
- Required: CL-01..08; DIA-01..08 per asset; common connector rules per asset; architecture rules for `logger-api-map` and `mdc-scope-lifecycle`; sequence rules for `async-channel-sequence`.
- N/A: infrastructure icon changes and review pages; the diagrams are text-only and no separate review page exists.
- User-reported invariant: arrow-tip-to-target-boundary gap = `0px` in SVG coordinates and visibly connected in the scale-2 PNG.
- User-reported marker invariant: architecture primary heads = `14×14`; sequence heads = `16×16`; same-role heads have matching rendered size/color and remain solid on dashed lines.

## Checklist contract

- [x] **CL-01 — Create before mutation**
    - **Action:** Instantiate router, common, and kind items before changing any SVG.
    - **Evidence:** This checklist exists before connector mutation.
    - **Failure:** STOP and reconstruct.
- [x] **CL-02 — Classify every item**
    - **Action:** Mark required, conditional, and N/A items.
    - **Evidence:** Classification section names all applicable families and two concrete N/A cases.
    - **Failure:** Treat unclassified rows as required.
- [x] **CL-03 — Respect dependency order**
    - **Action:** Complete each asset edit-render-audit-inspect loop sequentially.
    -
  **Evidence:** Completed logger map, then MDC, then sequence; each asset finished edit-render-audit-inspect before the next SVG changed.
    - **Failure:** Rerun reordered downstream proof.
- [x] **CL-04 — Record evidence immediately**
    - **Action:** Update each row when its output is read.
    -
  **Evidence:** Each asset row and ledger were updated before advancing; sequence visual failure was recorded before repair.
    - **Failure:** Leave row unchecked.
- [x] **CL-05 — Fail closed**
    - **Action:** Stop an asset loop on any failed audit or visible PNG defect.
    -
  **Evidence:** Logger/MDC passed first final inspection; sequence was stopped after visual detection of two off-lifeline rows, repaired, rerendered, reaudited, and reinspected.
    - **Failure:** Invalidate downstream work.
- [x] **CL-06 — Repair skipped or reordered work**
    - **Action:** Rerun affected proofs after every final coordinate change.
    - **Evidence:** Sequence XML/render/common/sequence/targeted/diff proof was rerun after the last coordinate repair.
    - **Failure:** Remain blocked.
- [x] **CL-07 — Refresh irreversible holds**
    - **Action:** Classify external side effects.
    - **Evidence:** N/A — no push, PR, merge, deploy, or destructive action requested.
    - **Failure:** Stop at external boundary.
- [x] **CL-08 — Count before completion**
    - **Action:** Reconcile required, N/A, and blocked totals.
    -
  **Evidence:** Diagram-stage denominator reconciles to required 64/64, N/A 9, Blocked 0; downstream site publication is tracked in the execution plan, not this diagram checklist.
    - **Failure:** Completion claim forbidden.

## Asset 1 — logger-api-map (architecture)

- [x] **LAM-DIA-01 — Pin scope and source model**
    - **Action:** Read landing prose, logging source/tests, and related assets.
    -
  **Evidence:** `docs/manual/ko/modules/bluetape4k-logging.md`, `bluetape4k/logging/src/main`, representative tests, and all three logging PNGs read; reader question is responsibility ownership without floating relations.
    - **Failure:** Stop unsupported drawing.
- [x] **LAM-DIA-02 — Load common and architecture rules**
    - **Action:** Read `common.md` and `architecture.md`.
    - **Evidence:** Both references read in full for this defect.
    - **Failure:** Stop generic editing.
- [x] **LAM-DIA-03 — Complete SVG edit**
    - **Action:** Attach four arrows with perpendicular target entry and separate ports.
    -
  **Evidence:** Four connector paths only; targeted assertion changed from `missing=4` before repair to `boundary_gaps=0/4 missing=0` after repair.
    - **Failure:** Stop before render completion.
- [x] **LAM-DIA-04 — Parse and render PNG**
    - **Action:** Run `xmllint` and CairoSVG scale 2.
    - **Evidence:** `xmllint --noout` PASS; `cairosvg ... -s 2` PASS; `sips` reports 3200×1960.
    - **Failure:** Stop asset loop.
- [x] **LAM-DIA-05 — Run audits**
    - **Action:** Run common connector/geometry/endpoint/mixed-corner audits and targeted assertions.
    -
  **Evidence:** markers=1, connectors=4, intrusions=0, crossings=0; geometry_failures=0; endpoint PASS; mixed-corner paths=4 q_bends=4 failures=0; targeted missing=0.
    - **Failure:** Repair.
- [x] **LAM-DIA-06 — Inspect full-size PNG**
    - **Action:** Inspect labels, endpoints, markers, corners, intrusion, spacing, and whitespace.
    -
  **Evidence:** 3200×1960 original viewed after final edit: all four tips visibly touch borders; three inbound ports are separate; arrowheads are readable; no sharp corners, clipping, crossing, border riding, or card intrusion.
    - **Failure:** Return to SVG edit.
- [x] **LAM-DIA-07 — Verify exposure and diff**
    - **Action:** Verify manual embed and diff hygiene.
    - **Evidence:** Landing embeds `../../assets/logging/logger-api-map.svg`; scoped `git diff --check` PASS.
    - **Failure:** Repair.
- [x] **LAM-DIA-08 — Record evidence ledger**
    - **Action:** Reconcile every logger-map row.
    -
  **Evidence:** Logger-map ledger row contains source, XML/render, audit counts, marker size, and original-PNG observations.
    - **Failure:** Do not claim completion.
- [x] **LAM-COM-01 — Verify source and related set**
    - **Action:** Confirm source-backed relationships and scan all logging SVGs for floating endpoints.
    -
  **Evidence:** Landing/source/tests read; related scan found the same fixed-gap pattern in MDC and sequence assets, so both remain required.
    - **Failure:** Stop or widen repair.
- [x] **LAM-COM-02 — Preserve text and theme**
    - **Action:** Keep approved fonts, palette, alignment, and unclipped text.
    -
  **Evidence:** No text/style/card changes; full-size PNG confirms existing Architects Daughter/Comic Mono family, palette, alignment, and clipping remain intact.
    - **Failure:** Repair.
- [x] **LAM-COM-03 — Classify icons**
    - **Action:** Check infrastructure-icon applicability.
    - **Evidence:** N/A — responsibility cards are text-only; no infrastructure icon changes.
    - **Failure:** Reclassify if scope changes.
- [x] **LAM-COM-04 — Verify markers in PNG**
    - **Action:** Confirm blue 14×14 heads match line color/size/direction.
    -
  **Evidence:** one fixed `userSpaceOnUse` marker, explicit `14×14`, blue line/head parity; four same-role heads render equal, solid, and correctly directed.
    - **Failure:** Repair marker.
- [x] **LAM-COM-05 — Verify endpoints and routes**
    - **Action:** Prove 0px boundary gaps, perpendicular entry, separated ports, and no intrusion.
    -
  **Evidence:** endpoint gaps=0/4; three top entries are perpendicular at x=740/800/860; bottom target y=754; intrusions=0, crossings=0; PNG confirms contact.
    - **Failure:** Repair coordinates.
- [x] **LAM-COM-06 — Verify bent corners**
    - **Action:** Prove every turn is rounded with clearance.
    -
  **Evidence:** paths=4, q_bends=4, mixed-corner failures=0; both side routes use two rounded Q turns and render smoothly.
    - **Failure:** Repair bends.
- [x] **LAM-COM-07 — Synchronize canvas and whitespace**
    - **Action:** Confirm unchanged cards/canvas still have balanced margins.
    -
  **Evidence:** Cards/canvas unchanged; the new connector rail uses existing corridor with balanced left/right routing and no new whitespace defect.
    - **Failure:** Repair dependent geometry.
- [x] **LAM-COM-08 — Run required commands**
    - **Action:** Read all required audit output.
    -
  **Evidence:** Final post-edit block ran XML, CairoSVG, dimensions, connector, geometry, endpoint, mixed-corner, targeted marker/boundary assertions, and diff check; all PASS.
    - **Failure:** Remain unchecked.
- [x] **LAM-COM-09 — Verify review exposure**
    - **Action:** Check review-page applicability.
    - **Evidence:** N/A — no separate review page; canonical manual embeds the asset.
    - **Failure:** Reclassify if a review page appears.
- [x] **LAM-ARC-01 — Confirm architecture semantics**
    - **Action:** Keep the static responsibility/ownership model.
    -
  **Evidence:** Static map answers which layer owns logger creation, event construction, MDC context, SLF4J facade, and provider policy; no ordered call semantics added.
    - **Failure:** Route ordered behavior elsewhere.
- [x] **LAM-ARC-02 — Match visual family**
    - **Action:** Preserve the approved logging/manual visual family.
    -
  **Evidence:** Compared full-size logging family PNGs; palette, cards, type, border, and marker color remain unchanged.
    - **Failure:** Repair style drift.
- [x] **LAM-ARC-03 — Verify layout choice**
    - **Action:** Preserve balanced horizontal responsibilities and vertical ownership tiers.
    -
  **Evidence:** Horizontal responsibilities and vertical ownership tiers remain balanced; separate top ports remove the prior ambiguous floating rail.
    - **Failure:** Repair layout.
- [x] **LAM-ARC-04 — Verify architecture connectors**
    - **Action:** Prove rounded orthogonal target attachment and standoff.
    -
  **Evidence:** orthogonal/rounded routes, perpendicular target entry, 0px gaps, marker clearance, intrusions=0, crossings=0, full-size PNG PASS.
    - **Failure:** Repair.

## Asset 2 — mdc-scope-lifecycle (architecture)

- [x] **MDC-DIA-01 — Pin scope and source model**
    - **Action:** Read scoped-MDC prose, implementation/tests, and related assets.
    -
  **Evidence:** scoped-MDC chapter, `MdcSupport.kt`, `MdcSupportTest.kt`, and related logging PNGs read; reader question is save/install/restore ownership with unambiguous progression.
    - **Failure:** Stop unsupported drawing.
- [x] **MDC-DIA-02 — Load common and architecture rules**
    - **Action:** Read `common.md` and `architecture.md`.
    - **Evidence:** Both references read in full.
    - **Failure:** Stop generic editing.
- [x] **MDC-DIA-03 — Complete SVG edit**
    - **Action:** Attach both arrows edge-to-edge.
    -
  **Evidence:** two endpoint coordinates only; targeted assertion changed from `missing=2` to `boundary_gaps=0/2 missing=0`.
    - **Failure:** Stop.
- [x] **MDC-DIA-04 — Parse and render PNG**
    - **Action:** Run XML and CairoSVG scale 2.
    - **Evidence:** XML PASS; CairoSVG scale 2 PASS; `sips` reports 3200×1960.
    - **Failure:** Stop.
- [x] **MDC-DIA-05 — Run audits**
    - **Action:** Run common and targeted audits.
    -
  **Evidence:** markers=1, connectors=2, intrusions=0, crossings=0; geometry_failures=0; endpoint PASS; paths=2 q_bends=0 failures=0; targeted missing=0.
    - **Failure:** Repair.
- [x] **MDC-DIA-06 — Inspect full-size PNG**
    - **Action:** Inspect every visual gate.
    -
  **Evidence:** 3200×1960 original viewed after final edit: both tips touch target borders, start at source borders, remain centered/perpendicular, and show no clipping, intrusion, overlap, or spacing regression.
    - **Failure:** Return to edit.
- [x] **MDC-DIA-07 — Verify exposure and diff**
    - **Action:** Verify embed and diff hygiene.
    -
  **Evidence:** scoped-MDC chapter embeds `../../../assets/logging/mdc-scope-lifecycle.svg`; scoped `git diff --check` PASS.
    - **Failure:** Repair.
- [x] **MDC-DIA-08 — Record evidence ledger**
    - **Action:** Reconcile all MDC rows.
    -
  **Evidence:** MDC ledger row contains source, render, counts, marker size, endpoint gaps, and original-PNG observations.
    - **Failure:** No completion claim.
- [x] **MDC-COM-01 — Verify source and related set**
    - **Action:** Confirm save/install/restore semantics and related pattern scan.
    -
  **Evidence:** `MdcSupport.kt`/test confirm capture-install-finally restore/remove; related scan confirms sequence asset still needs repair.
    - **Failure:** Stop.
- [x] **MDC-COM-02 — Preserve text and theme**
    - **Action:** Keep fonts, palette, alignment, and readability.
    -
  **Evidence:** No text/style/card changes; full-size PNG preserves approved logging palette, fonts, alignment, and readable labels.
    - **Failure:** Repair.
- [x] **MDC-COM-03 — Classify icons**
    - **Action:** Check icon applicability.
    - **Evidence:** N/A — text-only operation cards.
    - **Failure:** Reclassify if scope changes.
- [x] **MDC-COM-04 — Verify markers in PNG**
    - **Action:** Confirm blue 14×14 heads.
    -
  **Evidence:** one fixed blue marker, explicit 14×14; both heads render equal, solid, line-colored, and correctly directed.
    - **Failure:** Repair.
- [x] **MDC-COM-05 — Verify endpoints and routes**
    - **Action:** Prove both 0px target gaps and no intrusion.
    -
  **Evidence:** target gaps=0/2 at x=605 and x=1120; straight perpendicular attachment; intrusions=0, crossings=0; PNG confirms contact without overshoot.
    - **Failure:** Repair.
- [x] **MDC-COM-06 — Verify bent corners**
    - **Action:** Classify bend applicability.
    - **Evidence:** N/A — both connectors are straight horizontal segments.
    - **Failure:** Reclassify if routes bend.
- [x] **MDC-COM-07 — Synchronize canvas and whitespace**
    - **Action:** Confirm unchanged layout remains balanced.
    - **Evidence:** Canvas/cards unchanged; equal connector corridors and existing lower note spacing remain balanced.
    - **Failure:** Repair.
- [x] **MDC-COM-08 — Run required commands**
    - **Action:** Read every required audit output.
    -
  **Evidence:** Final post-edit block ran XML, CairoSVG, dimensions, connector, geometry, endpoint, mixed-corner, targeted marker/boundary assertions, and diff check; all PASS.
    - **Failure:** Remain unchecked.
- [x] **MDC-COM-09 — Verify review exposure**
    - **Action:** Check review-page applicability.
    - **Evidence:** N/A — no separate review page.
    - **Failure:** Reclassify if present.
- [x] **MDC-ARC-01 — Confirm architecture semantics**
    - **Action:** Keep the static operation-state responsibility view.
    -
  **Evidence:** Cards describe three operation-state responsibilities backed by implementation/tests; no new call/return or branch semantics added.
    - **Failure:** Reclassify if time ordering dominates.
- [x] **MDC-ARC-02 — Match visual family**
    - **Action:** Preserve approved logging palette/type.
    -
  **Evidence:** Compared logger-map and MDC PNGs; existing logging palette, typography, shadows, and 14×14 heads remain consistent.
    - **Failure:** Repair.
- [x] **MDC-ARC-03 — Verify layout choice**
    - **Action:** Preserve three balanced operation cards.
    -
  **Evidence:** Three equal cards and equal horizontal corridors remain balanced; no detour or additional whitespace introduced.
    - **Failure:** Repair.
- [x] **MDC-ARC-04 — Verify architecture connectors**
    - **Action:** Prove straight perpendicular edge attachment.
    -
  **Evidence:** straight orthogonal/perpendicular attachment, 0px gaps, intrusions=0, crossings=0, endpoint/geometry PASS, original PNG PASS.
    - **Failure:** Repair.

## Asset 3 — async-channel-sequence (sequence)

- [x] **ACS-DIA-01 — Pin scope and source model**
    - **Action:** Read async-channel prose, implementation/tests, and reference sequences.
    -
  **Evidence:** async-channel chapter, `KLoggingChannel.kt`/test, and two full-size sequence references read; reader question is send/collect/close/post-close ownership over time.
    - **Failure:** Stop unsupported drawing.
- [x] **ACS-DIA-02 — Load common and sequence rules**
    - **Action:** Read `common.md` and `sequence.md`.
    - **Evidence:** Both references read in full.
    - **Failure:** Stop generic editing.
- [x] **ACS-DIA-03 — Complete SVG edit**
    - **Action:** Attach every message to lifeline/activation edges.
    -
  **Evidence:** eight message endpoints only; initial targeted `missing=8`; first render exposed two semantically off-lifeline rows; final assertion reports attached_messages=8/8 missing=0.
    - **Failure:** Stop.
- [x] **ACS-DIA-04 — Parse and render PNG**
    - **Action:** Run XML and CairoSVG scale 2.
    - **Evidence:** final XML PASS; CairoSVG scale 2 PASS; `sips` reports 3600×2240.
    - **Failure:** Stop.
- [x] **ACS-DIA-05 — Run audits**
    - **Action:** Run common, sequence-style, and targeted audits.
    -
  **Evidence:** markers=3, connectors=8, intrusions=0, crossings=0; geometry_failures=0; endpoint PASS; paths=8 q_bends=0 failures=0; sequence-style PASS; targeted missing=0.
    - **Failure:** Repair.
- [x] **ACS-DIA-06 — Inspect full-size PNG**
    - **Action:** Inspect all message rows, labels, markers, frames, and spacing.
    -
  **Evidence:** First original-PNG review rejected rows 6/7 as off-lifeline; final 3600×2240 review confirms all eight paths touch activation/lifeline, seven labels remain clear, dashed heads are solid, and frame/footer spacing is intact.
    - **Failure:** Return to edit.
- [x] **ACS-DIA-07 — Verify exposure and diff**
    - **Action:** Verify embed and diff hygiene.
    -
  **Evidence:** async chapter embeds `../../../assets/logging/async-channel-sequence.svg`; final scoped `git diff --check` PASS.
    - **Failure:** Repair.
- [x] **ACS-DIA-08 — Record evidence ledger**
    - **Action:** Reconcile all sequence rows.
    -
  **Evidence:** Sequence ledger row contains source, references, final rerun counts, marker proof, failure/repair, and original-PNG observations.
    - **Failure:** No completion claim.
- [x] **ACS-COM-01 — Verify source and related set**
    - **Action:** Confirm send/collect/close/post-close relationships and scan related assets.
    -
  **Evidence:** `KLoggingChannel.kt`/test confirm flow, collector, provider, cancel, injected-scope, and post-close semantics; related-set scan covered all three logging SVGs.
    - **Failure:** Stop.
- [x] **ACS-COM-02 — Preserve text and theme**
    - **Action:** Keep fonts, palette, labels, and readability.
    -
  **Evidence:** No text/style/frame/card changes; final PNG preserves approved fonts, muted semantic palette, label alignment, and readable spacing.
    - **Failure:** Repair.
- [x] **ACS-COM-03 — Classify icons**
    - **Action:** Check icon applicability.
    - **Evidence:** N/A — participants are code roles, not infrastructure cards.
    - **Failure:** Reclassify if scope changes.
- [x] **ACS-COM-04 — Verify markers in PNG**
    - **Action:** Confirm explicit blue/green/red 16×16 heads match lines.
    -
  **Evidence:** three explicit per-color `userSpaceOnUse` markers, each 16×16; blue/green/red line-head parity; marker defs contain no dash; final PNG shows solid equal-sized heads on solid and dashed paths.
    - **Failure:** Repair.
- [x] **ACS-COM-05 — Verify endpoints and routes**
    - **Action:** Prove lifeline/activation attachment and no intrusion.
    -
  **Evidence:** attached_messages=8/8 at lifeline/activation coordinates; intrusions=0, crossings=0, geometry=0, endpoint PASS; final PNG confirms no floating or overshooting rows.
    - **Failure:** Repair.
- [x] **ACS-COM-06 — Verify bent corners**
    - **Action:** Classify bend applicability.
    - **Evidence:** N/A — message connectors are straight horizontal lanes.
    - **Failure:** Reclassify if routes bend.
- [x] **ACS-COM-07 — Synchronize canvas and whitespace**
    - **Action:** Confirm unchanged frames and canvas remain balanced.
    -
  **Evidence:** Canvas, participant headers, frame, footer, and row heights unchanged; final PNG retains balanced whitespace and label clearance.
    - **Failure:** Repair.
- [x] **ACS-COM-08 — Run required commands**
    - **Action:** Read every required audit output.
    -
  **Evidence:** Final post-repair block ran XML, CairoSVG, dimensions, connector, geometry, endpoint, mixed-corner, sequence-style, targeted marker/attachment assertions, and diff check; all PASS.
    - **Failure:** Remain unchecked.
- [x] **ACS-COM-09 — Verify review exposure**
    - **Action:** Check review-page applicability.
    - **Evidence:** N/A — no separate review page.
    - **Failure:** Reclassify if present.
- [x] **ACS-SEQ-01 — Open two authoritative references**
    - **Action:** Inspect one best-practices and one nearest repo-local sequence PNG.
    -
  **Evidence:** Full-size `sequence-workflow-sample.png` and nearest `bluetape4k-coroutines-sequence-01.png` opened; both show calls terminating on activation edges/lifelines and guide the repair.
    - **Failure:** Stop sequence editing.
- [x] **ACS-SEQ-02 — Preserve sequence signals**
    - **Action:** Keep headers, lifelines, activations, messages, numbered labels, and chronological frame.
    -
  **Evidence:** participant headers=4, lifelines=4, activations=2, message paths=8, numbered labels=7, chronological frame=1; final PNG PASS.
    - **Failure:** Repair.
- [x] **ACS-SEQ-03 — Verify palette and markers**
    - **Action:** Preserve muted semantic color parity.
    -
  **Evidence:** muted blue/green/red line, head, label border/text, and badge families remain paired; all heads explicit 16×16 and solid.
    - **Failure:** Repair.
- [x] **ACS-SEQ-04 — Verify numbered rows**
    - **Action:** Prove seven visible ordered labels above continuous message lines.
    -
  **Evidence:** visible numbered pills 1..7 remain above their continuous lines with no label-line, frame, or lifeline overlap.
    - **Failure:** Increase spacing.
- [x] **ACS-SEQ-05 — Verify branch frame**
    - **Action:** Preserve transparent padded close frame and outside footer.
    -
  **Evidence:** close frame remains `fill=none`, padded, titled inside, with rows 5/6 contained and post-close row/footer outside.
    - **Failure:** Repair.
- [x] **ACS-SEQ-06 — Run sequence proof**
    - **Action:** Run sequence-style audit and reconcile with PNG.
    -
  **Evidence:** final sequence-style audit PASS reconciled with references, 7 labels, 1 frame, 3 marker colors, 8 attached paths, and original-PNG PASS.
    - **Failure:** Repair.

## Evidence ledger

| Asset                    | Source and kind                                                         | XML/render                         | Connector and marker proof                                                                                                                 | Original PNG inspection                                                                                                                       |
|--------------------------|-------------------------------------------------------------------------|------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `logger-api-map`         | landing + logging source/tests; architecture                            | XML PASS; CairoSVG 3200×1960       | markers=1, connectors=4, intrusions=0, crossings=0, geometry=0, endpoint PASS, paths=4, q_bends=4, mixed failures=0, gaps=0/4, heads=14×14 | four borders visibly connected; three separate inbound ports; smooth corners; no clipping/crossing/intrusion                                  |
| `mdc-scope-lifecycle`    | scoped-MDC chapter + `MdcSupport.kt`/test; architecture                 | XML PASS; CairoSVG 3200×1960       | markers=1, connectors=2, intrusions=0, crossings=0, geometry=0, endpoint PASS, paths=2, q_bends=0, gaps=0/2, heads=14×14                   | both source/target borders visibly connected; equal corridors; no overshoot/clipping/intrusion                                                |
| `async-channel-sequence` | async chapter + `KLoggingChannel.kt`/test; sequence; two reference PNGs | final XML PASS; CairoSVG 3600×2240 | markers=3, connectors=8, intrusions=0, crossings=0, geometry=0, endpoint PASS, paths=8, sequence PASS, attached=8/8, heads=16×16           | first review rejected rows 6/7; final review confirms lifeline/activation contact, solid dashed heads, labels 1..7 clear, frame/footer intact |

`Required checks: 64/64; N/A: 9; Blocked: 0` — every diagram row has fresh final evidence; no unchecked IDs.

Post-ledger repository proof: the first exporter invocation used a nonexistent filename and was rejected. The corrected `set -e` rerun passed `validate_manuals_test.rb` 14/41, `export_manifest_test.rb` 2/7, `generate_manuals_test.rb` 1/23, `validate_manuals.rb` alignment, and `git diff --check`.
