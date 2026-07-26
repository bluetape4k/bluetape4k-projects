# bluetape4k-core review, tests, KDoc, README plan

Spec: `docs/superpowers/specs/2026-05-11-core-review-tests-docs-design.md`
Module: `bluetape4k-core`

## Tasks

1. Review and fix bounded collection input contracts.
    - complexity: medium
    - files: `RingBuffer.kt`, `BoundedStack.kt`
    - patterns: validation helpers, Korean KDoc, narrow behavior-preserving fixes except invalid inputs
    - verification: targeted collection tests

2. Add edge tests for bounded collection contracts.
    - complexity: medium
    - files: `RingBufferTest.kt`, `BoundedStackTest.kt`
    - patterns: `assertFailsWith`, `shouldBeEqualTo`, Given/When/Then style names already used in module
    - verification: `./gradlew :bluetape4k-core:test --tests "*RingBufferTest" --tests "*BoundedStackTest"`

3. Validate pagination construction and add edge tests.
    - complexity: medium
    - files: `PaginatedList.kt`, `PaginatedListTest.kt`
    - patterns: `requireZeroOrPositiveNumber`, `requirePositiveNumber`, Korean KDoc
    - verification: `./gradlew :bluetape4k-core:test --tests "*PaginatedListTest"`

4. KDoc/public examples pass for touched APIs.
    - complexity: low
    - files: `RingBuffer.kt`, `BoundedStack.kt`, `PaginatedList.kt`
    - patterns: Korean KDoc with `kotlin` code fences and realistic examples
    - verification: source review plus compile

5. README sync.
    - complexity: low
    - files: `bluetape4k/core/README.md`, `bluetape4k/core/README.ko.md`
    - patterns: keep English/Korean content aligned
    - verification: diff review

6. Verification and 6-Tier review gate.
    - complexity: medium
    - commands:
        - `./gradlew :bluetape4k-core:compileKotlin :bluetape4k-core:compileTestKotlin`
        - `./gradlew :bluetape4k-core:test`
        - `git diff --check`
    - gate: run security, Ops/SRE, structural, Kotlin/code quality, tests/types/silent failure, and performance/stability review. Iterate until P0/P1 are zero.

## Step 3-R local review

| Perspective   | Result                                                                           |
|---------------|----------------------------------------------------------------------------------|
| Implementer   | P0=0, P1=0. Tasks are ordered so behavior fixes precede tests/docs verification. |
| Test engineer | P0=0, P1=0. Edge tests map to each P1 finding.                                   |
| Architect     | P0=0, P1=0. No module boundary or dependency changes.                            |
| Delivery      | P0=0, P1=0. Worktree exists and verification is module-scoped.                   |

Claude advisor: not run for plan gate; this task explicitly targets the Step 6-R 6-Tier review gate, and implementation is local/module-scoped.
