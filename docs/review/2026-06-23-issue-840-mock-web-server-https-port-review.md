# Issue 840 검토 - mock-web-server HTTPS port

## Scope

- `testing/mock-web-server/README.md`
- `testing/mock-web-server/README.ko.md`
- `testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/ReadmeHttpsPortContractTest.kt`

## Review Notes

- README English and Korean now document HTTPS port `8443`, matching
  `bluetape4k.https.port`.
- Docker examples now map `8443:8443`, matching the Jib container port list.
- The regression test reads runtime config, build metadata, and both README
  files without starting Spring.
- The test blocks standalone stale `443` documentation while allowing the
  correct `8443` value.

## Verification

- RED: `ReadmeHttpsPortContractTest` failed while README files documented `443`.
- GREEN: `ReadmeHttpsPortContractTest` passed after README updates.
- `./gradlew :bluetape4k-mock-web-server:compileKotlin :bluetape4k-mock-web-server:compileTestKotlin :bluetape4k-mock-web-server:test --no-build-cache`
- `rg -n '\\*\\*443\\*\\*|(^|[^0-9])443\\s*\\(HTTPS\\)|\`443\`|-p 443:443' testing/mock-web-server/README.md testing/mock-web-server/README.ko.md`
- `git diff --check`
