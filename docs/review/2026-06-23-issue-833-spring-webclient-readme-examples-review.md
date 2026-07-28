# Issue 833 검토 - Spring WebClient README examples

## Scope

- `spring-boot/core/README.md`
- `spring-boot/core/README.ko.md`
- `spring-boot/core/src/test/kotlin/io/bluetape4k/spring/tests/WebClientReadmeExamplesTest.kt`

## Review Notes

- `httpGet` and `httpPost` examples no longer call `.retrieve()` after the
  project helpers.
- English and Korean README snippets remain source-equivalent.
- The new README validation test blocks the exact double-retrieve drift pattern
  that made the snippets non-compilable.
- The compile smoke test uses the real `WebClient` extension functions with
  `bodyToFlux` and `bodyToMono`; it does not perform network I/O.

## Verification

- RED: `WebClientReadmeExamplesTest` failed on
  `README.md should not chain .retrieve() after httpGet/httpPost`.
- GREEN: `WebClientReadmeExamplesTest` passed after README updates.
- Code review: native `code-reviewer` reported 0 critical, 0 high, 0 medium,
  and 0 low findings. Residual risk is limited to future README drift outside
  the checked double-`retrieve()` pattern.
- `./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.tests.WebClientReadmeExamplesTest' --no-build-cache`
- `./gradlew :bluetape4k-spring-boot-core:compileKotlin :bluetape4k-spring-boot-core:compileTestKotlin :bluetape4k-spring-boot-core:test --no-build-cache`
- `rg -n "http(Get|Post)\\([^)]*\\)\\s*\\.\\s*retrieve\\(\\)" spring-boot/core/README.md spring-boot/core/README.ko.md`
- `git diff --check`
