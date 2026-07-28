# Issue 843 검토 - BluetapeHttpServer property keys

## Scope

- `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/http/BluetapeHttpServer.kt`
- `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/PropertyExportingServerContractTest.kt`
- `testing/testcontainers/README.md`
- `testing/testcontainers/README.ko.md`

## Review Notes

- `BluetapeHttpServer.propertyKeys()` now matches the canonical mock HTTP key
  set used by `BluetapeWebfluxServer`.
- `BluetapeHttpServer.properties()` keeps old camelCase keys through
  `withCompatKeys`, while the documented and contract-tested keys are
  kebab-case.
- English and Korean README property tables and Spring Boot placeholders now
  use `testcontainers.bluetape-http.httpbin-url`,
  `testcontainers.bluetape-http.jsonplaceholder-url`, and
  `testcontainers.bluetape-http.web-url`.
- The regression test constructs the server wrappers without calling `start()`,
  so it verifies the key contract without depending on Docker image builds.

## Verification

- RED: `PropertyExportingServerContractTest.mock HTTP server propertyKeys expose canonical kebab-case keys` failed on the old camelCase keys.
- GREEN: `PropertyExportingServerContractTest` passed after the key change.
- `./gradlew :bluetape4k-testcontainers:compileKotlin :bluetape4k-testcontainers:compileTestKotlin :bluetape4k-testcontainers:test --tests "io.bluetape4k.testcontainers.PropertyExportingServerContractTest" --no-build-cache`
- `rg -n 'testcontainers\\.bluetape-http\\.(httpbinUrl|jsonplaceholderUrl|webUrl)|\`httpbinUrl\`, \`jsonplaceholderUrl\`, \`webUrl\`' testing/testcontainers/README.md testing/testcontainers/README.ko.md`
- `git diff --check`
