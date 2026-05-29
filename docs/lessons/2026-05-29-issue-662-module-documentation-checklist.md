# Issue 662: Module Documentation Drift Checklist

## Context

Module lifecycle changes can drift across Gradle registration, README locale
pairs, CI filters, Nightly/examples workflows, BOM/catalog publication, and
agent references.

## Decision

Create a contributor-facing checklist under `docs/process/` and link it from the
module group reference. Keep detailed process guidance out of transient agent
instructions only.

## Outcome

- Added `docs/process/module-documentation-checklist.md`.
- Linked the checklist from `.codex/references/module-groups.md`.
- Updated the module group reference to the current Spring Boot 4.x-only
  `spring-boot/` group and marked old Spring Boot paths as historical.

## Verification

- Checked Markdown whitespace with `git diff --check`.
- Verified the checklist link target exists.
- Verified current Spring Boot group wording no longer lists active
  `spring-boot3/*` or `spring-boot4/*` groups.

## Future Guidance

Every module add, rename, move, removal, split, or repository promotion should
include checklist evidence in the PR body.
