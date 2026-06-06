# bluetape4k-projects Copilot Instructions

This repository contains Kotlin/JVM backend libraries for the bluetape4k ecosystem.
Follow the workspace and repo-local `AGENTS.md` files when they provide stricter or newer guidance.

## Language And Public Artifacts

- Write public API KDoc in English for new or changed public classes, interfaces, objects, and extension functions.
- Keep contributor-facing artifacts in English, including commit messages, PR titles/bodies, changelog entries, release notes, and GitHub issues.
- Keep `README.md` in English and update existing localized README files such as `README.ko.md` together when user-facing documentation changes.
- Keep internal planning, review, and lesson documents concise; Korean is acceptable for internal human-readable docs.

## Kotlin And Module Policy

- Use Kotlin 2.3+ and the repository's Java 21 toolchain.
- Use Spring Boot 4.x only for this repository's Spring Boot modules.
- Prefer Kotlin extensions, DSLs, immutable values, and existing bluetape4k helpers before adding new utilities.
- Reuse established module patterns from the current source tree and `settings.gradle.kts`.
- Keep examples concise, runnable, and realistic enough for production-style usage.

## Testing

- Use JUnit 5, MockK, and `io.bluetape4k.assertions` helpers.
- Prefer bluetape4k assertion comparison matchers such as `shouldBeEqualTo` over boolean assertions.
- Use `assertFailsWith<T> { }` for exception checks unless an existing bluetape4k assertion pattern is clearly more appropriate.

## Spring And Data

- For Spring code, follow Spring Boot 4.x conventions and the current `spring-boot/*` module layout.
- For Exposed code, follow current Exposed 1.2+ import and receiver-shadowing rules from repo guidance.
- Prefer existing database/testcontainer launchers and project test infrastructure over ad hoc setup.

## Git And Validation

- Commit messages that will be pushed to GitHub must be in English and use the repository's conventional prefixes when helpful, such as `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `build:`, or `chore:`.
- Keep changes small, reviewable, and scoped to the requested issue.
- Run the smallest relevant compile/test/documentation checks and `git diff --check` before claiming completion.
