# Source-Verified README Diagrams

## Context

README diagram assets can outlive the Mermaid blocks that generated them. A source audit found a stale root Spring Boot lane, stale JUnit and Hibernate labels, plus outdated TiDB wording.

## Decision

Treat current source symbols as authoritative over historical Mermaid. The root module diagram now shows the versionless `spring-boot/*` line, diagram labels were updated to `RandomExtension` and exact converter class names, and TiDB wording now says the support was removed rather than deprecated.

## Verification

Validate README diagrams with XML parsing, PNG rerendering, source-token grep, and README link checks before publishing.

## Future Guidance

When regenerating README images, recover Mermaid only as a starting point. Confirm each prominent class/API label against current source before committing assets.
