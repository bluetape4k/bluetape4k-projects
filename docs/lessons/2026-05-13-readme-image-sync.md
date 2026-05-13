# README image sync

## Context

The project README image needed to match the bluetape4k organization profile image.

## Decision

Copy the profile image into `doc/bluetape4k.png` and keep the existing README image path stable.

## Outcome

Both English and Korean README files now describe the copied workbench image with locale-appropriate alt text.

## Verification

Checked that `doc/bluetape4k.png` is a PNG image and that both README files reference it.

## Future guidance

Prefer replacing the local README asset in `doc/` when updating the top-level project image, so GitHub rendering does not depend on cross-repository relative paths.
