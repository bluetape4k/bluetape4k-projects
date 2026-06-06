# Issue #699 Copilot instructions policy alignment review

## Scope

- Updated `.github/copilot-instructions.md` to match current bluetape4k-projects policy.
- Kept the change to agent-facing guidance only; no production code or workflow behavior changed.

## Findings

- P0=0
- P1=0
- P2=0

## Evidence

- `gno query "Copilot instructions current bluetape4k project policy" -c bluetape4k-github --fast --no-rerank`: issue #699 evidence found.
- `gno query "Copilot instructions current bluetape4k project policy" -c bluetape4k-docs --fast --no-rerank`: weak docs match; repo-local `AGENTS.md` and workspace `AGENTS.md` used as current policy sources.
- `rg -n "Spring Boot 3|3\\.4\\.0|Kluent|한국어로|KDoc.*한국어|Korean KDoc|commit messages.*Korean|커밋 메시지는|Kluent-first" .github/copilot-instructions.md`: no matches.
- `git diff --check`: PASS.

## Follow-up

- `.github/git-commit-instructions.md` still contains older Korean commit-message guidance, but #699 scope and acceptance criteria target `.github/copilot-instructions.md` only.
- Follow-up issue created: #718.

## Residual Risk

- No rendered docs or Gradle tests were run because this is a single agent-guidance documentation change.
