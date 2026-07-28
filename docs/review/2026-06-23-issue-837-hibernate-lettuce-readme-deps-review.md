# Issue 837 검토 - hibernate-lettuce README dependencies

## Scope

- `spring-boot/hibernate-lettuce/README.md`
- `spring-boot/hibernate-lettuce/README.ko.md`
- `spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/ReadmeDependencyContractTest.kt`

## Review Notes

- README dependency snippets now use copyable consumer Gradle coordinates
  instead of bluetape4k-internal version catalog helpers.
- English and Korean dependency sections were updated equivalently.
- The Spring Boot 4 migration note now points to the public BOM coordinate and
  the real `org.springframework.boot:spring-boot-hibernate` coordinate.
- The new file-based contract test uses `bluetape4k-assertions` and does not
  start a Spring context.

## Verification

- RED: `ReadmeDependencyContractTest` failed while README files still contained
  internal `Libs.*` helpers.
- GREEN: `ReadmeDependencyContractTest` passed after README cleanup.
- `./gradlew :bluetape4k-spring-boot-hibernate-lettuce:compileKotlin :bluetape4k-spring-boot-hibernate-lettuce:compileTestKotlin :bluetape4k-spring-boot-hibernate-lettuce:test --no-build-cache`
  passed with the existing LettuceNearCache tests plus the README dependency
  contract.
- Stale-helper `rg` guard passed with no matches in the README files.
- `git diff --check` passed.
