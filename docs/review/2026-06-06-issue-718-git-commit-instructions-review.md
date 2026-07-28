# Issue #718 Git commit instructions policy alignment review

## Scope

- Updated `.github/git-commit-instructions.md` to match the current English contributor-facing artifact policy.
- Kept the change to commit guidance only; no production code, workflow, or Copilot instruction behavior changed.

## 발견 사항

- P0=0
- P1=0
- P2=0

## 증거

- `gh issue view 718 --json body`: issue scope requires English pushed commit messages and conventional prefixes.
- `gno query "git commit instructions English contributor policy" -c bluetape4k-github --fast --no-rerank`: #699/#718 context found.
- `.github/copilot-instructions.md`: current policy says contributor-facing artifacts, including commit messages, are English.
- `rg -n "한국어|Korean|커밋 메시지는 한국어|커밋 메시지는|머릿말" .github/git-commit-instructions.md`: no matches.
- `git diff --check`: PASS.

## Residual Risk

- No Gradle tests or rendered documentation checks were run because this is a single agent-guidance documentation change.
