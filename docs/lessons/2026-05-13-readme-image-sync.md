# README image sync

## Context

The project README image needed to match the bluetape4k organization profile image.

## Decision

Copy the profile image into the repository and keep the README image path stable.

Superseded on 2026-05-14: the obsolete top-level `doc/` directory was removed. The current README image lives under `docs/assets/projects-workbench.png`.

## Outcome

Both English and Korean README files now describe the copied workbench image with locale-appropriate alt text.

## Verification

Checked that both README files reference the same local image asset.

## Future guidance

Prefer replacing the local README asset under `docs/assets/` when updating the top-level project image, so GitHub rendering does not depend on cross-repository relative paths.
