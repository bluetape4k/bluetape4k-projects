# Issue 838 Review - Cassandra demo coroutine README example

## Scope

- `spring-boot/cassandra-demo/README.md`
- `spring-boot/cassandra-demo/README.ko.md`
- `spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/ReadmeCoroutineRepositoryContractTest.kt`

## Review Notes

- README coroutine repository snippets now match the tested
  `CoroutinePersonRepository` shape.
- English and Korean README snippets were updated equivalently.
- The new file-based contract test checks the `Flow<T>` regular-function shape
  and the single-result `suspend fun` shape without starting a Spring context.

## Verification

- RED: `ReadmeCoroutineRepositoryContractTest` failed while README files still
  documented `CoroutineCrudRepository<Person, UUID>` and `suspend Flow`.
- GREEN: `ReadmeCoroutineRepositoryContractTest` passed after README cleanup.
- `./gradlew :bluetape4k-spring-boot-cassandra-demo:compileTestKotlin :bluetape4k-spring-boot-cassandra-demo:test --no-build-cache`
  passed, including `CoroutinePersonRepositoryTest` and the README contract.
- Stale-signature `rg` guard passed with no matches in the README files.
- `git diff --check` passed.
