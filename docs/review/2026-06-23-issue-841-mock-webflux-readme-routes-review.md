# Issue 841 검토 - mock-webflux README routes

## Scope

- `testing/mock-webflux-server/README.md`
- `testing/mock-webflux-server/README.ko.md`
- `testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/ReadmeRouteContractTest.kt`

## Review Notes

- README endpoint tables now describe only implemented mock-webflux routes.
- English and Korean README tables were changed equivalently.
- `/httpbin/json` was also removed after checking controller annotations; it
  was README-only even though the issue body did not list it explicitly.
- The new test is file-based and does not start a Spring context, so it gives a
  fast regression signal for stale README endpoint rows.

## Verification

- RED: `ReadmeRouteContractTest` failed while README files still listed stale routes.
- GREEN: `ReadmeRouteContractTest` passed after README cleanup.
- `./gradlew :bluetape4k-mock-webflux-server:compileKotlin :bluetape4k-mock-webflux-server:compileTestKotlin :bluetape4k-mock-webflux-server:test --no-build-cache`
- `rg -n '/admin/info|/httpbin/(stream-bytes/\\{n\\}|drip|sse|brotli|html|xml|json|robots\\.txt|deny)' testing/mock-webflux-server/README.md testing/mock-webflux-server/README.ko.md`
- `git diff --check`
