# Issue 860 검토 - gitleaks installer

## Scope

- `.github/workflows/ci.yml`
- `.github/workflows/security.yml`

## Review Notes

- The secret scan commands were not weakened or skipped.
- The installer no longer calls the unauthenticated `releases/latest` API.
- Both workflows now use the same pinned `GITLEAKS_VERSION`.
- The pinned release asset name was verified against the upstream GitHub
  release metadata before editing.

## Verification Plan

- `actionlint .github/workflows/ci.yml .github/workflows/security.yml`
- `rg -n "\\\\'" .github/workflows`
- `git diff --check`
- Local download/extract smoke test for the pinned Linux x64 asset
- PR `CI` run confirms `Secret Scan (gitleaks)` reaches `Run gitleaks`
