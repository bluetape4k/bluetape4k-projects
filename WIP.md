# WIP - bluetape4k-projects

Snapshot: 2026-05-11 KST
Scope: open GitHub issues assigned to `debop`, created on or after 2026-01-01. Open count: 11 issues.

## Current Direction

This repo is now the core/shared library baseline after several domain groups were split into independent repositories. Active work should keep the split tracker current, prepare the next extraction steps, and promote small shared utilities that remove duplicate downstream code.

The new Spring annotation utility follow-up `#364` is a good small foundation item because `bluetape4k-leader` already implemented local reified helpers during #79 and `bluetape4k-aws` has similar Java-style Spring annotation calls.

Historical completed items from the old monorepo TODO are intentionally omitted from this active WIP. Use closed issues and `CHANGELOG.md` for completed work.

## Priority Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P0 | [#257](https://github.com/bluetape4k/bluetape4k-projects/issues/257) monorepo split epic | XL | Program tracker. Keep phase state updated as repo splits close. |
| P2 | [#364](https://github.com/bluetape4k/bluetape4k-projects/issues/364) Spring annotation reified utilities | S | Add shared Kotlin idioms for Spring `AnnotatedElementUtils` / `AnnotationUtils`; unblocks leader/AWS cleanup. |
| P2 | [#149](https://github.com/bluetape4k/bluetape4k-projects/issues/149) utils/vector | M | Foundation for AI utilities. |
| P2 | [#151](https://github.com/bluetape4k/bluetape4k-projects/issues/151) LLM/vector Testcontainers | L | Test foundation for AI/vector work. |
| P2 | [#148](https://github.com/bluetape4k/bluetape4k-projects/issues/148) utils/ai epic | XL | Split after `#149/#151` scope is clear. |
| P3 | [#108](https://github.com/bluetape4k/bluetape4k-projects/issues/108) QueryDSL demo completion | S | Low cross-project leverage; good small closure. |
| P3 | [#323](https://github.com/bluetape4k/bluetape4k-projects/issues/323) io README Mermaid | S | Pair with `#324/#325`. |
| P3 | [#324](https://github.com/bluetape4k/bluetape4k-projects/issues/324) AbstractCompressor KDoc throws | S | Pair with `#323/#325`. |
| P3 | [#325](https://github.com/bluetape4k/bluetape4k-projects/issues/325) compressor breaking-change changelog | S | Pair with `#323/#324`. |
| P4 | [#262](https://github.com/bluetape4k/bluetape4k-projects/issues/262) bluetape4k-data split | XL | Explicitly deferred until dependency direction is re-evaluated after exposed split. |
| P4 | [#251](https://github.com/bluetape4k/bluetape4k-projects/issues/251) states module analysis | S | Question-style issue; convert to ADR/research note only when needed. |

## Dependency Map

```text
#280 Boot 4-only policy decision (documented)
  -> dependencies repo #8 first official Spring Boot aliases (closed)
  -> #263 Spring Boot 3 removal + spring-boot4 -> spring-boot rename (closed)
      -> exposed repo #3 Spring Boot 3 removal + spring-boot4 -> spring-boot rename (closed)

#257 monorepo split tracker
  -> #333 cache in-repo folder reorganization (closed)
  -> #262 data extraction (deferred)

#364 Spring annotation reified utilities
  -> leader #79 cleanup: remove local helper once shared API is released
  -> aws cleanup: replace repeated `AnnotatedElementUtils.findMergedAnnotation(..., A::class.java)` style calls

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
| Policy / breaking cleanup |     1 | Keep `#257` updated as split phases close. |
| Extraction planning       |     1 | Keep `#257` current; `#262` is deferred.   |
| Shared Spring utilities   |     1 | `#364` as a small API PR before downstream cleanup. |
| AI/vector                 |     1 | `#149` or `#151`, not `#148` first.        |
| Docs polish               |  1 PR | `#323/#324/#325` together.                 |

## Cleanup Actions

| Candidate        | Action                                                                                        |
|------------------|-----------------------------------------------------------------------------------------------|
| `#280` vs `#263` | Resolved direction documented in PR #348; both are removed from the active queue.             |
| `#110`           | Inventory documented in `docs/infra-deprecated-inventory.md`; close after this PR merges.     |
| `#333`           | Closed; keep completion in issue/PR history, not active WIP.                                  |
| `#251`           | Close as not planned or convert into an ADR when a concrete state-machine decision is active. |
| `#262`           | Keep deferred; do not start until dependency direction is intentionally re-evaluated.         |
