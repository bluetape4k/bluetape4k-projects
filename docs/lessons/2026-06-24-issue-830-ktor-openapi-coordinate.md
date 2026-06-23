# Lessons Learned - Issue #830 Ktor OpenAPI Maven Coordinate (2026-06-24)

Related issue: #830
Module: `:bluetape4k-ktor-openapi`

## L1: README dependency snippets must follow `projectGroup`

### Problem

`ktor/openapi` README snippets used `io.bluetape4k` even though the repository
publishes under `projectGroup=io.github.bluetape4k`.

Users copying the snippet would request an artifact coordinate that does not
match the published Maven group.

### Lesson

When changing module dependency snippets, verify the group id against
`gradle.properties` instead of copying old README examples. For localized
README sets, update `README.md` and `README.ko.md` together and grep for stale
module coordinates before closing the issue.

## Evidence

- Source of truth: `gradle.properties` has
  `projectGroup=io.github.bluetape4k`.
- Fixed snippets:
  `io.github.bluetape4k:bluetape4k-ktor-openapi:$bluetape4kVersion`.
- Validation:
  `rg "io\\.bluetape4k:bluetape4k-ktor-openapi" ktor/openapi/README.md ktor/openapi/README.ko.md`
  returned no matches after the change.
