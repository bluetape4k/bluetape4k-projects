# Logging Connector Repair Checklist

## 분류와 불변 조건

- Scope: three canonical logging SVG/PNG pairs; derived site snapshot only.
- Required: CL-01..08; DIA-01..08 per asset; common connector rules per asset; architecture rules for `logger-api-map` and `mdc-scope-lifecycle`; sequence rules for `async-channel-sequence`.
- N/A: infrastructure icon changes and review pages; the diagrams are text-only and no separate review page exists.
- User-reported invariant: arrow-tip-to-target-boundary gap = `0px` in SVG coordinates and visibly connected in the scale-2 PNG.
- User-reported marker invariant: architecture primary heads = `14×14`; sequence heads = `16×16`; same-role heads have matching rendered size/color and remain solid on dashed lines.

## 체크리스트 계약

- [x] **CL-01 — Create before mutation**
    - **조치:** Instantiate router, common, and kind items before changing any SVG.
    - **증거:** This checklist exists before connector mutation.
    - **실패 시:** STOP and reconstruct.
- [x] **CL-02 — Classify every item**
    - **조치:** Mark required, conditional, and N/A items.
    - **증거:** Classification section names all applicable families and two concrete N/A cases.
    - **실패 시:** Treat unclassified rows as required.
- [x] **CL-03 — Respect dependency order**
    - **조치:** Complete each asset edit-render-audit-inspect loop sequentially.
    -
  **증거:** Completed logger map, then MDC, then sequence; each asset finished edit-render-audit-inspect before the next SVG changed.
    - **실패 시:** Rerun reordered downstream proof.
- [x] **CL-04 — Record evidence immediately**
    - **조치:** Update each row when its output is read.
    -
  **증거:** Each asset row and ledger were updated before advancing; sequence visual failure was recorded before repair.
    - **실패 시:** Leave row unchecked.
- [x] **CL-05 — Fail closed**
    - **조치:** Stop an asset loop on any failed audit or visible PNG defect.
    -
  **증거:** Logger/MDC passed first final inspection; sequence was stopped after visual detection of two off-lifeline rows, repaired, rerendered, reaudited, and reinspected.
    - **실패 시:** Invalidate downstream work.
- [x] **CL-06 — Repair skipped or reordered work**
    - **조치:** Rerun affected proofs after every final coordinate change.
    - **증거:** Sequence XML/render/common/sequence/targeted/diff proof was rerun after the last coordinate repair.
    - **실패 시:** Remain blocked.
- [x] **CL-07 — Refresh irreversible holds**
    - **조치:** Classify external side effects.
    - **증거:** N/A — no push, PR, merge, deploy, or destructive action requested.
    - **실패 시:** Stop at external boundary.
- [x] **CL-08 — Count before completion**
    - **조치:** Reconcile required, N/A, and blocked totals.
    -
  **증거:** Diagram-stage denominator reconciles to required 64/64, N/A 9, Blocked 0; downstream site publication is tracked in the execution plan, not this diagram checklist.
    - **실패 시:** Completion claim forbidden.

## Asset 1 — logger-api-map (architecture)

- [x] **LAM-DIA-01 — Pin scope and source model**
    - **조치:** Read landing prose, logging source/tests, and related assets.
    -
  **증거:** `docs/manual/ko/modules/bluetape4k-logging.md`, `bluetape4k/logging/src/main`, representative tests, and all three logging PNGs read; reader question is responsibility ownership without floating relations.
    - **실패 시:** Stop unsupported drawing.
- [x] **LAM-DIA-02 — Load common and architecture rules**
    - **조치:** Read `common.md` and `architecture.md`.
    - **증거:** Both references read in full for this defect.
    - **실패 시:** Stop generic editing.
- [x] **LAM-DIA-03 — Complete SVG edit**
    - **조치:** Attach four arrows with perpendicular target entry and separate ports.
    -
  **증거:** Four connector paths only; targeted assertion changed from `missing=4` before repair to `boundary_gaps=0/4 missing=0` after repair.
    - **실패 시:** Stop before render completion.
- [x] **LAM-DIA-04 — Parse and render PNG**
    - **조치:** Run `xmllint` and CairoSVG scale 2.
    - **증거:** `xmllint --noout` PASS; `cairosvg ... -s 2` PASS; `sips` reports 3200×1960.
    - **실패 시:** Stop asset loop.
- [x] **LAM-DIA-05 — Run audits**
    - **조치:** Run common connector/geometry/endpoint/mixed-corner audits and targeted assertions.
    -
  **증거:** markers=1, connectors=4, intrusions=0, crossings=0; geometry_failures=0; endpoint PASS; mixed-corner paths=4 q_bends=4 failures=0; targeted missing=0.
    - **실패 시:** Repair.
- [x] **LAM-DIA-06 — Inspect full-size PNG**
    - **조치:** Inspect labels, endpoints, markers, corners, intrusion, spacing, and whitespace.
    -
  **증거:** 3200×1960 original viewed after final edit: all four tips visibly touch borders; three inbound ports are separate; arrowheads are readable; no sharp corners, clipping, crossing, border riding, or card intrusion.
    - **실패 시:** Return to SVG edit.
- [x] **LAM-DIA-07 — Verify exposure and diff**
    - **조치:** Verify manual embed and diff hygiene.
    - **증거:** Landing embeds `../../assets/logging/logger-api-map.svg`; scoped `git diff --check` PASS.
    - **실패 시:** Repair.
- [x] **LAM-DIA-08 — Record evidence ledger**
    - **조치:** Reconcile every logger-map row.
    -
  **증거:** Logger-map ledger row contains source, XML/render, audit counts, marker size, and original-PNG observations.
    - **실패 시:** Do not claim completion.
- [x] **LAM-COM-01 — Verify source and related set**
    - **조치:** Confirm source-backed relationships and scan all logging SVGs for floating endpoints.
    -
  **증거:** Landing/source/tests read; related scan found the same fixed-gap pattern in MDC and sequence assets, so both remain required.
    - **실패 시:** Stop or widen repair.
- [x] **LAM-COM-02 — Preserve text and theme**
    - **조치:** Keep approved fonts, palette, alignment, and unclipped text.
    -
  **증거:** No text/style/card changes; full-size PNG confirms existing Architects Daughter/Comic Mono family, palette, alignment, and clipping remain intact.
    - **실패 시:** Repair.
- [x] **LAM-COM-03 — Classify icons**
    - **조치:** Check infrastructure-icon applicability.
    - **증거:** N/A — responsibility cards are text-only; no infrastructure icon changes.
    - **실패 시:** Reclassify if scope changes.
- [x] **LAM-COM-04 — Verify markers in PNG**
    - **조치:** Confirm blue 14×14 heads match line color/size/direction.
    -
  **증거:** one fixed `userSpaceOnUse` marker, explicit `14×14`, blue line/head parity; four same-role heads render equal, solid, and correctly directed.
    - **실패 시:** Repair marker.
- [x] **LAM-COM-05 — Verify endpoints and routes**
    - **조치:** Prove 0px boundary gaps, perpendicular entry, separated ports, and no intrusion.
    -
  **증거:** endpoint gaps=0/4; three top entries are perpendicular at x=740/800/860; bottom target y=754; intrusions=0, crossings=0; PNG confirms contact.
    - **실패 시:** Repair coordinates.
- [x] **LAM-COM-06 — Verify bent corners**
    - **조치:** Prove every turn is rounded with clearance.
    -
  **증거:** paths=4, q_bends=4, mixed-corner failures=0; both side routes use two rounded Q turns and render smoothly.
    - **실패 시:** Repair bends.
- [x] **LAM-COM-07 — Synchronize canvas and whitespace**
    - **조치:** Confirm unchanged cards/canvas still have balanced margins.
    -
  **증거:** Cards/canvas unchanged; the new connector rail uses existing corridor with balanced left/right routing and no new whitespace defect.
    - **실패 시:** Repair dependent geometry.
- [x] **LAM-COM-08 — Run required commands**
    - **조치:** Read all required audit output.
    -
  **증거:** Final post-edit block ran XML, CairoSVG, dimensions, connector, geometry, endpoint, mixed-corner, targeted marker/boundary assertions, and diff check; all PASS.
    - **실패 시:** Remain unchecked.
- [x] **LAM-COM-09 — Verify review exposure**
    - **조치:** Check review-page applicability.
    - **증거:** N/A — no separate review page; canonical manual embeds the asset.
    - **실패 시:** Reclassify if a review page appears.
- [x] **LAM-ARC-01 — Confirm architecture semantics**
    - **조치:** Keep the static responsibility/ownership model.
    -
  **증거:** Static map answers which layer owns logger creation, event construction, MDC context, SLF4J facade, and provider policy; no ordered call semantics added.
    - **실패 시:** Route ordered behavior elsewhere.
- [x] **LAM-ARC-02 — Match visual family**
    - **조치:** Preserve the approved logging/manual visual family.
    -
  **증거:** Compared full-size logging family PNGs; palette, cards, type, border, and marker color remain unchanged.
    - **실패 시:** Repair style drift.
- [x] **LAM-ARC-03 — Verify layout choice**
    - **조치:** Preserve balanced horizontal responsibilities and vertical ownership tiers.
    -
  **증거:** Horizontal responsibilities and vertical ownership tiers remain balanced; separate top ports remove the prior ambiguous floating rail.
    - **실패 시:** Repair layout.
- [x] **LAM-ARC-04 — Verify architecture connectors**
    - **조치:** Prove rounded orthogonal target attachment and standoff.
    -
  **증거:** orthogonal/rounded routes, perpendicular target entry, 0px gaps, marker clearance, intrusions=0, crossings=0, full-size PNG PASS.
    - **실패 시:** Repair.

## Asset 2 — mdc-scope-lifecycle (architecture)

- [x] **MDC-DIA-01 — Pin scope and source model**
    - **조치:** Read scoped-MDC prose, implementation/tests, and related assets.
    -
  **증거:** scoped-MDC chapter, `MdcSupport.kt`, `MdcSupportTest.kt`, and related logging PNGs read; reader question is save/install/restore ownership with unambiguous progression.
    - **실패 시:** Stop unsupported drawing.
- [x] **MDC-DIA-02 — Load common and architecture rules**
    - **조치:** Read `common.md` and `architecture.md`.
    - **증거:** Both references read in full.
    - **실패 시:** Stop generic editing.
- [x] **MDC-DIA-03 — Complete SVG edit**
    - **조치:** Attach both arrows edge-to-edge.
    -
  **증거:** two endpoint coordinates only; targeted assertion changed from `missing=2` to `boundary_gaps=0/2 missing=0`.
    - **실패 시:** Stop.
- [x] **MDC-DIA-04 — Parse and render PNG**
    - **조치:** Run XML and CairoSVG scale 2.
    - **증거:** XML PASS; CairoSVG scale 2 PASS; `sips` reports 3200×1960.
    - **실패 시:** Stop.
- [x] **MDC-DIA-05 — Run audits**
    - **조치:** Run common and targeted audits.
    -
  **증거:** markers=1, connectors=2, intrusions=0, crossings=0; geometry_failures=0; endpoint PASS; paths=2 q_bends=0 failures=0; targeted missing=0.
    - **실패 시:** Repair.
- [x] **MDC-DIA-06 — Inspect full-size PNG**
    - **조치:** Inspect every visual gate.
    -
  **증거:** 3200×1960 original viewed after final edit: both tips touch target borders, start at source borders, remain centered/perpendicular, and show no clipping, intrusion, overlap, or spacing regression.
    - **실패 시:** Return to edit.
- [x] **MDC-DIA-07 — Verify exposure and diff**
    - **조치:** Verify embed and diff hygiene.
    -
  **증거:** scoped-MDC chapter embeds `../../../assets/logging/mdc-scope-lifecycle.svg`; scoped `git diff --check` PASS.
    - **실패 시:** Repair.
- [x] **MDC-DIA-08 — Record evidence ledger**
    - **조치:** Reconcile all MDC rows.
    -
  **증거:** MDC ledger row contains source, render, counts, marker size, endpoint gaps, and original-PNG observations.
    - **실패 시:** No completion claim.
- [x] **MDC-COM-01 — Verify source and related set**
    - **조치:** Confirm save/install/restore semantics and related pattern scan.
    -
  **증거:** `MdcSupport.kt`/test confirm capture-install-finally restore/remove; related scan confirms sequence asset still needs repair.
    - **실패 시:** Stop.
- [x] **MDC-COM-02 — Preserve text and theme**
    - **조치:** Keep fonts, palette, alignment, and readability.
    -
  **증거:** No text/style/card changes; full-size PNG preserves approved logging palette, fonts, alignment, and readable labels.
    - **실패 시:** Repair.
- [x] **MDC-COM-03 — Classify icons**
    - **조치:** Check icon applicability.
    - **증거:** N/A — text-only operation cards.
    - **실패 시:** Reclassify if scope changes.
- [x] **MDC-COM-04 — Verify markers in PNG**
    - **조치:** Confirm blue 14×14 heads.
    -
  **증거:** one fixed blue marker, explicit 14×14; both heads render equal, solid, line-colored, and correctly directed.
    - **실패 시:** Repair.
- [x] **MDC-COM-05 — Verify endpoints and routes**
    - **조치:** Prove both 0px target gaps and no intrusion.
    -
  **증거:** target gaps=0/2 at x=605 and x=1120; straight perpendicular attachment; intrusions=0, crossings=0; PNG confirms contact without overshoot.
    - **실패 시:** Repair.
- [x] **MDC-COM-06 — Verify bent corners**
    - **조치:** Classify bend applicability.
    - **증거:** N/A — both connectors are straight horizontal segments.
    - **실패 시:** Reclassify if routes bend.
- [x] **MDC-COM-07 — Synchronize canvas and whitespace**
    - **조치:** Confirm unchanged layout remains balanced.
    - **증거:** Canvas/cards unchanged; equal connector corridors and existing lower note spacing remain balanced.
    - **실패 시:** Repair.
- [x] **MDC-COM-08 — Run required commands**
    - **조치:** Read every required audit output.
    -
  **증거:** Final post-edit block ran XML, CairoSVG, dimensions, connector, geometry, endpoint, mixed-corner, targeted marker/boundary assertions, and diff check; all PASS.
    - **실패 시:** Remain unchecked.
- [x] **MDC-COM-09 — Verify review exposure**
    - **조치:** Check review-page applicability.
    - **증거:** N/A — no separate review page.
    - **실패 시:** Reclassify if present.
- [x] **MDC-ARC-01 — Confirm architecture semantics**
    - **조치:** Keep the static operation-state responsibility view.
    -
  **증거:** Cards describe three operation-state responsibilities backed by implementation/tests; no new call/return or branch semantics added.
    - **실패 시:** Reclassify if time ordering dominates.
- [x] **MDC-ARC-02 — Match visual family**
    - **조치:** Preserve approved logging palette/type.
    -
  **증거:** Compared logger-map and MDC PNGs; existing logging palette, typography, shadows, and 14×14 heads remain consistent.
    - **실패 시:** Repair.
- [x] **MDC-ARC-03 — Verify layout choice**
    - **조치:** Preserve three balanced operation cards.
    -
  **증거:** Three equal cards and equal horizontal corridors remain balanced; no detour or additional whitespace introduced.
    - **실패 시:** Repair.
- [x] **MDC-ARC-04 — Verify architecture connectors**
    - **조치:** Prove straight perpendicular edge attachment.
    -
  **증거:** straight orthogonal/perpendicular attachment, 0px gaps, intrusions=0, crossings=0, endpoint/geometry PASS, original PNG PASS.
    - **실패 시:** Repair.

## Asset 3 — async-channel-sequence (sequence)

- [x] **ACS-DIA-01 — Pin scope and source model**
    - **조치:** Read async-channel prose, implementation/tests, and reference sequences.
    -
  **증거:** async-channel chapter, `KLoggingChannel.kt`/test, and two full-size sequence references read; reader question is send/collect/close/post-close ownership over time.
    - **실패 시:** Stop unsupported drawing.
- [x] **ACS-DIA-02 — Load common and sequence rules**
    - **조치:** Read `common.md` and `sequence.md`.
    - **증거:** Both references read in full.
    - **실패 시:** Stop generic editing.
- [x] **ACS-DIA-03 — Complete SVG edit**
    - **조치:** Attach every message to lifeline/activation edges.
    -
  **증거:** eight message endpoints only; initial targeted `missing=8`; first render exposed two semantically off-lifeline rows; final assertion reports attached_messages=8/8 missing=0.
    - **실패 시:** Stop.
- [x] **ACS-DIA-04 — Parse and render PNG**
    - **조치:** Run XML and CairoSVG scale 2.
    - **증거:** final XML PASS; CairoSVG scale 2 PASS; `sips` reports 3600×2240.
    - **실패 시:** Stop.
- [x] **ACS-DIA-05 — Run audits**
    - **조치:** Run common, sequence-style, and targeted audits.
    -
  **증거:** markers=3, connectors=8, intrusions=0, crossings=0; geometry_failures=0; endpoint PASS; paths=8 q_bends=0 failures=0; sequence-style PASS; targeted missing=0.
    - **실패 시:** Repair.
- [x] **ACS-DIA-06 — Inspect full-size PNG**
    - **조치:** Inspect all message rows, labels, markers, frames, and spacing.
    -
  **증거:** First original-PNG review rejected rows 6/7 as off-lifeline; final 3600×2240 review confirms all eight paths touch activation/lifeline, seven labels remain clear, dashed heads are solid, and frame/footer spacing is intact.
    - **실패 시:** Return to edit.
- [x] **ACS-DIA-07 — Verify exposure and diff**
    - **조치:** Verify embed and diff hygiene.
    -
  **증거:** async chapter embeds `../../../assets/logging/async-channel-sequence.svg`; final scoped `git diff --check` PASS.
    - **실패 시:** Repair.
- [x] **ACS-DIA-08 — Record evidence ledger**
    - **조치:** Reconcile all sequence rows.
    -
  **증거:** Sequence ledger row contains source, references, final rerun counts, marker proof, failure/repair, and original-PNG observations.
    - **실패 시:** No completion claim.
- [x] **ACS-COM-01 — Verify source and related set**
    - **조치:** Confirm send/collect/close/post-close relationships and scan related assets.
    -
  **증거:** `KLoggingChannel.kt`/test confirm flow, collector, provider, cancel, injected-scope, and post-close semantics; related-set scan covered all three logging SVGs.
    - **실패 시:** Stop.
- [x] **ACS-COM-02 — Preserve text and theme**
    - **조치:** Keep fonts, palette, labels, and readability.
    -
  **증거:** No text/style/frame/card changes; final PNG preserves approved fonts, muted semantic palette, label alignment, and readable spacing.
    - **실패 시:** Repair.
- [x] **ACS-COM-03 — Classify icons**
    - **조치:** Check icon applicability.
    - **증거:** N/A — participants are code roles, not infrastructure cards.
    - **실패 시:** Reclassify if scope changes.
- [x] **ACS-COM-04 — Verify markers in PNG**
    - **조치:** Confirm explicit blue/green/red 16×16 heads match lines.
    -
  **증거:** three explicit per-color `userSpaceOnUse` markers, each 16×16; blue/green/red line-head parity; marker defs contain no dash; final PNG shows solid equal-sized heads on solid and dashed paths.
    - **실패 시:** Repair.
- [x] **ACS-COM-05 — Verify endpoints and routes**
    - **조치:** Prove lifeline/activation attachment and no intrusion.
    -
  **증거:** attached_messages=8/8 at lifeline/activation coordinates; intrusions=0, crossings=0, geometry=0, endpoint PASS; final PNG confirms no floating or overshooting rows.
    - **실패 시:** Repair.
- [x] **ACS-COM-06 — Verify bent corners**
    - **조치:** Classify bend applicability.
    - **증거:** N/A — message connectors are straight horizontal lanes.
    - **실패 시:** Reclassify if routes bend.
- [x] **ACS-COM-07 — Synchronize canvas and whitespace**
    - **조치:** Confirm unchanged frames and canvas remain balanced.
    -
  **증거:** Canvas, participant headers, frame, footer, and row heights unchanged; final PNG retains balanced whitespace and label clearance.
    - **실패 시:** Repair.
- [x] **ACS-COM-08 — Run required commands**
    - **조치:** Read every required audit output.
    -
  **증거:** Final post-repair block ran XML, CairoSVG, dimensions, connector, geometry, endpoint, mixed-corner, sequence-style, targeted marker/attachment assertions, and diff check; all PASS.
    - **실패 시:** Remain unchecked.
- [x] **ACS-COM-09 — Verify review exposure**
    - **조치:** Check review-page applicability.
    - **증거:** N/A — no separate review page.
    - **실패 시:** Reclassify if present.
- [x] **ACS-SEQ-01 — Open two authoritative references**
    - **조치:** Inspect one best-practices and one nearest repo-local sequence PNG.
    -
  **증거:** Full-size `sequence-workflow-sample.png` and nearest `bluetape4k-coroutines-sequence-01.png` opened; both show calls terminating on activation edges/lifelines and guide the repair.
    - **실패 시:** Stop sequence editing.
- [x] **ACS-SEQ-02 — Preserve sequence signals**
    - **조치:** Keep headers, lifelines, activations, messages, numbered labels, and chronological frame.
    -
  **증거:** participant headers=4, lifelines=4, activations=2, message paths=8, numbered labels=7, chronological frame=1; final PNG PASS.
    - **실패 시:** Repair.
- [x] **ACS-SEQ-03 — Verify palette and markers**
    - **조치:** Preserve muted semantic color parity.
    -
  **증거:** muted blue/green/red line, head, label border/text, and badge families remain paired; all heads explicit 16×16 and solid.
    - **실패 시:** Repair.
- [x] **ACS-SEQ-04 — Verify numbered rows**
    - **조치:** Prove seven visible ordered labels above continuous message lines.
    -
  **증거:** visible numbered pills 1..7 remain above their continuous lines with no label-line, frame, or lifeline overlap.
    - **실패 시:** Increase spacing.
- [x] **ACS-SEQ-05 — Verify branch frame**
    - **조치:** Preserve transparent padded close frame and outside footer.
    -
  **증거:** close frame remains `fill=none`, padded, titled inside, with rows 5/6 contained and post-close row/footer outside.
    - **실패 시:** Repair.
- [x] **ACS-SEQ-06 — Run sequence proof**
    - **조치:** Run sequence-style audit and reconcile with PNG.
    -
  **증거:** final sequence-style audit PASS reconciled with references, 7 labels, 1 frame, 3 marker colors, 8 attached paths, and original-PNG PASS.
    - **실패 시:** Repair.

## Evidence ledger

| Asset                    | Source and kind                                                         | XML/render                         | Connector and marker proof                                                                                                                 | Original PNG inspection                                                                                                                       |
|--------------------------|-------------------------------------------------------------------------|------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `logger-api-map`         | landing + logging source/tests; architecture                            | XML PASS; CairoSVG 3200×1960       | markers=1, connectors=4, intrusions=0, crossings=0, geometry=0, endpoint PASS, paths=4, q_bends=4, mixed failures=0, gaps=0/4, heads=14×14 | four borders visibly connected; three separate inbound ports; smooth corners; no clipping/crossing/intrusion                                  |
| `mdc-scope-lifecycle`    | scoped-MDC chapter + `MdcSupport.kt`/test; architecture                 | XML PASS; CairoSVG 3200×1960       | markers=1, connectors=2, intrusions=0, crossings=0, geometry=0, endpoint PASS, paths=2, q_bends=0, gaps=0/2, heads=14×14                   | both source/target borders visibly connected; equal corridors; no overshoot/clipping/intrusion                                                |
| `async-channel-sequence` | async chapter + `KLoggingChannel.kt`/test; sequence; two reference PNGs | final XML PASS; CairoSVG 3600×2240 | markers=3, connectors=8, intrusions=0, crossings=0, geometry=0, endpoint PASS, paths=8, sequence PASS, attached=8/8, heads=16×16           | first review rejected rows 6/7; final review confirms lifeline/activation contact, solid dashed heads, labels 1..7 clear, frame/footer intact |

`Required checks: 64/64; N/A: 9; Blocked: 0` — every diagram row has fresh final evidence; no unchecked IDs.

Post-ledger repository proof: the first exporter invocation used a nonexistent filename and was rejected. The corrected `set -e` rerun passed `validate_manuals_test.rb` 14/41, `export_manifest_test.rb` 2/7, `generate_manuals_test.rb` 1/23, `validate_manuals.rb` alignment, and `git diff --check`.
