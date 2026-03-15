# GEMINI.md

This file provides guidance to Gemini CLI when working with code in this repository.

## Core Mandates

- **CLAUDE.md & AGENTS.md**: Always adhere to the rules and patterns defined in `CLAUDE.md` and `AGENTS.md`.
- **bluetape4k-patterns**: Always use the `bluetape4k-patterns` skill for Kotlin code implementation.
- **Token Efficiency**: Use the specialized bin tools for summaries to save context.
    - `git status` -> `./bin/repo-status`
    - `git diff` -> `./bin/repo-diff`
    - `gradle` -> `./bin/repo-test-summary -- ./gradlew <task>`
- **Language**: Use **Korean
  ** for KDoc, commit messages, and summaries/descriptions as much as possible, as requested in `CLAUDE.md`.

## Workflow Adaptation (OMX Integration)

Although Gemini CLI is a single-agent interface, it should adopt the roles defined in
`AGENTS.md` during the development lifecycle:

1. **Research (Explore/Analyst)**: Use `grep_search`, `glob`, and `./bin/repo-status` to map the codebase.
2. **Strategy (Planner/Architect)**: Formulate a grounded plan. Use `enter_plan_mode` for complex tasks.
3. **Execution (Executor/Debugger)**: Apply surgical changes according to `bluetape4k-patterns`.
4. **Validation (Verifier/QA)**: Run tests using `./bin/repo-test-summary`.

## Technology Stack & Standards

- **Kotlin**: 2.3+ (Language & API)
- **JVM**: Java 21 (Toolchain)
- **Spring Boot**: 3.4.0+
- **Exposed**: 1.0.0+
- **Testing**: JUnit 5, MockK, Kluent.
- **Logging**: `KLogging()` or `KLoggingChannel()`.
- **Validation**: `bluetape4k-core`'s `require*()` and `check*()` extensions.

## Tooling Preference

- `fd` for file searching.
- `rg` for text searching.
- `ast-grep` for structural changes.
- `gh` for GitHub interactions.
- `ruff` for Python (minimal use).

## Important Files

- `CLAUDE.md`: General project guidelines.
- `AGENTS.md`: Multi-agent orchestration rules (adopt these mindsets).
- `bluetape4k-patterns`: Kotlin implementation patterns.
- `GEMINI.md`: This file.
