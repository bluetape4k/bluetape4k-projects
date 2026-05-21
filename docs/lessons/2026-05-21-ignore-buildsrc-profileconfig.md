# Ignore buildSrc profile config

## Context

`buildSrc/.profileconfig.json` appeared as an untracked local profile artifact after README diagram work.

## Decision

Ignore the file at the exact `buildSrc` path instead of broadening the pattern to every nested `.profileconfig.json`.

## Outcome

The local profile file no longer appears in normal repository status while root `/.profileconfig.json` remains ignored as before.

## Verification

- `git check-ignore -v buildSrc/.profileconfig.json`
- `git status -sb`

## Future note

Keep local tool profile ignores path-specific unless the same generated file is confirmed across multiple module directories.
