# Issue 839 검토 - hibernate-lettuce demo README dependencies

## Scope

- `spring-boot/hibernate-lettuce-demo/README.md`
- `spring-boot/hibernate-lettuce-demo/README.ko.md`
- `spring-boot/hibernate-lettuce-demo/src/test/kotlin/io/bluetape4k/examples/cache/lettuce/ReadmeDependencyContractTest.kt`

## Review Notes

- README dependency snippets now use copyable consumer Gradle coordinates
  instead of bluetape4k-internal version catalog helpers.
- English and Korean dependency sections were updated equivalently.
- The Spring Boot 4 note now points to the public BOM coordinate and clarifies
  that `spring-boot-starter-data-jpa` supplies the demo Hibernate runtime.
- The new test is file-based and does not start a Spring context, so it gives a
  fast regression signal for stale dependency snippets.

## Verification

- RED: `ReadmeDependencyContractTest` failed while README files still contained
  `Libs.` helpers.
- GREEN: `ReadmeDependencyContractTest` passed after README cleanup.
- `./gradlew :bluetape4k-spring-boot-hibernate-lettuce-demo:compileKotlin :bluetape4k-spring-boot-hibernate-lettuce-demo:compileTestKotlin :bluetape4k-spring-boot-hibernate-lettuce-demo:test --no-build-cache`
  passed with 6 demo integration tests plus the README dependency contract.
- Stale-helper `rg` guard passed with no matches in the README files.
- `git diff --check` passed.
