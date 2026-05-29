# Issue 645 Ktor OpenAPI Plan

## Steps

1. Add Ktor OpenAPI, routing OpenAPI, and Swagger UI aliases using the governed
   Ktor version.
2. Add `ktor/openapi` with explicit wrappers over Ktor's official route helpers.
3. Test a static OpenAPI document covering health/readiness and one domain route.
4. Update module README files, root README locale set, CI, Nightly, and lesson.
5. Verify with module compile, module tests, Kover XML, actionlint, and diff
   hygiene.

## Acceptance Mapping

- Dependency choice: captured in the design note.
- Generated/served output: covered by `/openapi` and `/swagger` tests using a
  static spec.
- Explicit metadata: documented through Ktor runtime `.describe {}` guidance.
- README locale set: `README.md` and `README.ko.md` are updated.
