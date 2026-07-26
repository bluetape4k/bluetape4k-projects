# Logging Diagram Connector Repair Implementation Plan

> **For agentic
workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make all logging diagram arrows visibly and geometrically connect to their target boundaries.

**Architecture:** Preserve the existing source-backed diagram models and repair only SVG connector coordinates and routes. Process each canonical SVG/PNG pair independently through XML validation, CairoSVG rendering, scripted audits, targeted endpoint assertions, and full-size PNG inspection before syncing the derived site snapshot.

**Tech Stack:** SVG, CairoSVG CLI, bluetape diagram audit scripts, Astro site snapshot tooling.

---

### Task 1: Repair logger responsibility-map connectors

**Files:**

- Modify: `docs/manual/assets/logging/logger-api-map.svg`
- Regenerate: `docs/manual/assets/logging/logger-api-map.png`

- [ ] Route the left and right inputs through separate rounded top ports and extend all arrow tips to target boundaries.
- [ ] Run XML, CairoSVG, connector, geometry, endpoint, mixed-corner, targeted boundary assertions, and a `14×14` primary-marker assertion.
- [ ] Inspect the 3200×1960 PNG at full size; repeat if any gap, sharp corner, intrusion, or ambiguous convergence remains.

### Task 2: Repair MDC lifecycle connectors

**Files:**

- Modify: `docs/manual/assets/logging/mdc-scope-lifecycle.svg`
- Regenerate: `docs/manual/assets/logging/mdc-scope-lifecycle.png`

- [ ] Extend both progression arrows from the source card edge to the next card edge.
- [ ] Run XML, CairoSVG, connector, geometry, endpoint, mixed-corner, targeted boundary assertions, and a `14×14` primary-marker assertion.
- [ ] Inspect the 3200×1960 PNG at full size; repeat if either arrow appears detached or enters a card.

### Task 3: Repair async sequence message endpoints

**Files:**

- Modify: `docs/manual/assets/logging/async-channel-sequence.svg`
- Regenerate: `docs/manual/assets/logging/async-channel-sequence.png`

- [ ] Normalize seven numbered message rows and the post-close return to lifeline/activation edges.
- [ ] Run XML, CairoSVG, common connector audits, sequence-style audit, targeted message endpoint assertions, and explicit `16×16` per-color marker assertions.
- [ ] Inspect the 3600×2240 PNG at full size; repeat if any message floats, overshoots, crosses a label, or uses an inconsistent arrowhead.

### Task 4: Publish and verify the derived site snapshot

**Files:**

- Regenerate: `public/manual-assets/bluetape4k-projects/logging/*`
- Regenerate: `src/data/manual/bluetape4k-projects.snapshot.json`
- Regenerate: synchronized manual metadata under `src/content/docs/**/manual/bluetape4k-projects/`

- [ ] Commit the canonical Projects diagrams after all checklist rows pass.
- [ ] Synchronize the Site snapshot from the final Projects commit and run snapshot tests, Astro diagnostics/build, browser rendering, and console checks.
- [ ] Verify both worktrees are clean and retain the local preview server.
