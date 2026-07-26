# Issue #497 Disabled Test Release Gate Plan

1. Add a `buildSrc` scanner and Gradle task for disabled-test reporting.
2. Register root `checkDisabledTests` and wire it into `check`.
3. Add unit tests for known-bug violations, issue references, unsupported capability categorization, and conditional disabled annotations.
4. Add release documentation pointing maintainers to the generated report and gate rule.
5. Verify with `:buildSrc:test`, `checkDisabledTests`, and a root task listing or dry run that proves the task is registered.
6. Add a lessons note for future disabled-test triage.
