# Issue 838 - Cassandra demo coroutine README example

## Context

`spring-boot/cassandra-demo` README files documented a coroutine repository
method returning `Flow<Person>` as `suspend fun`. The tested repository uses a
regular function for `Flow<Person>` queries and reserves `suspend fun` for
single-result nullable lookups.

## Decision

Mirror the tested repository shape in both README locales:

- `CoroutineCrudRepository<Person, String>`;
- `fun findByLastname(lastname: String): Flow<Person>`;
- `suspend fun findByFirstnameAndLastname(...): Person?`.

Add a short note that `Flow<T>` repository queries are regular functions while
single-result coroutine lookups use `suspend fun`.

## Follow-up Guard

`ReadmeCoroutineRepositoryContractTest` reads both README locales and blocks the
stale `UUID` / `findByLastName` / `suspend Flow` example from returning.
