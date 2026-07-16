# Issue #754 Release Evidence And Tag Ownership

## Context

The 1.12.0 serializer stack needs a committed evidence manifest before the
final candidate commit exists. Its release workflow must also survive API
response loss without treating a concurrently created tag as owned, while
leaving releases for other versions available.

## Decision

- Record an `evidenceProducerSha` and deterministic tested-code digest instead
  of embedding a self-referential final candidate SHA.
- Permit only evidence paths between the producer and final candidate, and
  recompute the tested-code digest at both commits.
- Point the production ref at an annotated tag object that binds the request ID
  and candidate; only that object SHA proves ownership after response loss.
- Recover a lost closeout artifact only for the same request-bound annotated
  tag under an already no-bypass ruleset.
- Keep non-1.12.0 publication in a separate workflow that explicitly excludes
  and rejects 1.12.0 while retaining the generic Maven environment.

## Outcome

Evidence can be regenerated from a clean committed producer without a commit
identity cycle. Tag creation and rollback distinguish current-request
ownership from concurrent or pre-existing refs, and generic releases remain
available without bypassing the 1.12.0 hold.

## Verification

- Release-hold and GitHub-settings unit tests: 64 passing
- Workflow audit: PASS
- `actionlint` and `shellcheck`: PASS
- Serializer contract modules: BUILD SUCCESSFUL, including 1,037 io tests
- Jackson 2, Jackson 3, and Fastjson2 downstream compilation: BUILD SUCCESSFUL

## Future Guidance

Do not use a commit field that must name the commit containing itself. Bind
response-loss recovery to an independently verifiable request-owned object,
and scope temporary release holds to the target version rather than replacing
unrelated release paths.
