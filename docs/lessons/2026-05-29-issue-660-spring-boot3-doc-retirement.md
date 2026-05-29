# Issue 660: Spring Boot 3 Documentation Retirement

## Context

The repository now presents Spring Boot 4.x as the only current support line.
Searches still surfaced Spring Boot 3 references in historical plans, specs,
security reviews, and changelog entries.

## Decision

Keep historical evidence intact, but make the active support boundary explicit
in current-facing README files and archive entrypoints.

## Outcome

- Root `README.md` and `README.ko.md` now state Spring Boot 4.x only.
- `docs/superpowers/README.md` marks specs, plans, and research notes as
  historical internal artifacts.
- `docs/security-review/README.md` marks security review files as point-in-time
  evidence.

## Verification

- Confirmed root README files already used Spring Boot 4.x and added the
  retired-line clarification in both languages.
- Confirmed repo-local `AGENTS.md` already states Spring Boot 4.x modules and
  no active `spring-boot3/*` line.

## Future Guidance

Do not rewrite historical Spring Boot 3 references unless the file is being
republished as current-facing documentation. Prefer a local archive note or
directory-level context over broad regex replacement.
