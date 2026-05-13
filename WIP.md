# WIP - bluetape4k-projects

Snapshot: 2026-05-13 KST
Scope: open GitHub issues assigned to `debop`, created on or after 2026-01-01. Open count: 10 issues.

## Recently Completed

- `#416` Spring Boot idgenerator example closed by PR #422.
- `#419` Ktor idgenerator example closed by PR #421.
- `#420` Examples workflow split closed by PR #423.
- PR #424 aligned the `graphdb-memgraph` Nightly timeout/retry policy with the other graphdb jobs.
- PR #429 synchronized the top-level README image with the bluetape4k organization profile image.
- `#257` monorepo split epic and `#364` Spring annotation reified utility follow-up are now closed.
- PR #417 refreshed WIP after adding the idgenerator example lane; this refresh removes the now-completed example items from the active queue.

## Current Direction

This repo is now the core/shared library baseline after several domain groups were split into independent repositories. Active work should keep repository-facing docs aligned with the current module graph, promote small shared utilities that remove duplicate downstream code, and avoid reopening completed extraction lanes without a fresh issue.

The next high-leverage implementation item is `#418` because it extends the existing optional HTTP backend model without introducing a new common abstraction. The docs lane should first close the top-level README/module graph drift and then continue with `#354` cache README/KDoc cross-link checks plus the compressor docs trio `#323/#324/#325`.

Examples now have a dedicated `Examples` GitHub Actions workflow. Do not reopen the idgenerator example lane unless a new follow-up issue is created.

Historical completed items from the old monorepo TODO are intentionally omitted from this active WIP. Use closed issues and `CHANGELOG.md` for completed work.

## Priority Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P1 | [#418](https://github.com/bluetape4k/bluetape4k-projects/issues/418) optional Ktor client helpers for `io/http` | S | Thin optional backend; keep Ktor lifecycle explicit and do not introduce a common HTTP abstraction. |
| P2 | [#354](https://github.com/bluetape4k/bluetape4k-projects/issues/354) cache README/KDoc cross-link audit | S | Follow-up after cache folder reorganization; check English/Korean README import paths and KDoc links. |
| P2 | [#149](https://github.com/bluetape4k/bluetape4k-projects/issues/149) utils/vector | M | Foundation for AI utilities. |
| P2 | [#151](https://github.com/bluetape4k/bluetape4k-projects/issues/151) LLM/vector Testcontainers | L | Test foundation for AI/vector work. |
| P2 | [#148](https://github.com/bluetape4k/bluetape4k-projects/issues/148) utils/ai epic | XL | Split after `#149/#151` scope is clear. |
| P3 | [#108](https://github.com/bluetape4k/bluetape4k-projects/issues/108) QueryDSL demo completion | S | Low cross-project leverage; good small closure. |
| P3 | [#323](https://github.com/bluetape4k/bluetape4k-projects/issues/323) io README Mermaid | S | Pair with `#324/#325`. |
| P3 | [#324](https://github.com/bluetape4k/bluetape4k-projects/issues/324) AbstractCompressor KDoc throws | S | Pair with `#323/#325`. |
| P3 | [#325](https://github.com/bluetape4k/bluetape4k-projects/issues/325) compressor breaking-change changelog | S | Pair with `#323/#324`. |
| P4 | [#251](https://github.com/bluetape4k/bluetape4k-projects/issues/251) states module analysis | S | Question-style issue; convert to ADR/research note only when needed. |

## Dependency Map

```text
#280 Boot 4-only policy decision (documented)
  -> dependencies repo #8 first official Spring Boot aliases (closed)
  -> #263 Spring Boot 3 removal + spring-boot4 -> spring-boot rename (closed)
      -> exposed repo #3 Spring Boot 3 removal + spring-boot4 -> spring-boot rename (closed)

#257 monorepo split tracker (closed)
  -> #333 cache in-repo folder reorganization (closed)
  -> #354 cache README/KDoc cross-link audit

#418 Ktor client helpers
  -> io/http README pair documents Ktor CIO as an optional suspend-native backend
  -> compileOnly Ktor client dependencies plus targeted tests

#416 Spring Boot idgenerator example (closed by PR #422)
#419 Ktor idgenerator example (closed by PR #421)
  -> #420 Examples workflow split (closed by PR #423)

#110 infra deprecated inventory (documented in docs/infra-deprecated-inventory.md)
  -> follow-up deprecated cleanup PRs
  -> safer extraction/refactor planning

#149 vector utilities
#151 LLM/vector Testcontainers
  -> #148 utils/ai

#323 README
#324 KDoc
#325 CHANGELOG
  -> one small docs PR is preferred
```

## WIP Limits

| Lane                      | Limit | Current next                               |
|---------------------------|------:|--------------------------------------------|
| Optional HTTP backend     |     1 | `#418` Ktor client helpers.                |
| Cache/docs audit          |     1 | `#354` after current top-level docs sync.  |
| AI/vector                 |     1 | `#149` or `#151`, not `#148` first.        |
| Docs polish               |  1 PR | `#323/#324/#325` together.                 |

## Cleanup Actions

| Candidate        | Action                                                                                        |
|------------------|-----------------------------------------------------------------------------------------------|
| `#280` vs `#263` | Resolved direction documented in PR #348; both are removed from the active queue.             |
| `#110`           | Inventory documented in `docs/infra-deprecated-inventory.md`; close after this PR merges.     |
| `#333`           | Closed; keep completion in issue/PR history, not active WIP.                                  |
| `#257`           | Closed; keep split status in README/CHANGELOG and standalone repo history, not active WIP.    |
| `#364`           | Closed; remove from active queue and let downstream repos adopt the shared utility as needed. |
| `#251`           | Close as not planned or convert into an ADR when a concrete state-machine decision is active. |
