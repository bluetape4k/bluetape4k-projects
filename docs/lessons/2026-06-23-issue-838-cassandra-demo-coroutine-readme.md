# 이슈 838 - Cassandra demo coroutine README example

## 배경

`spring-boot/cassandra-demo` README file은 `Flow<Person>`을 반환하는 coroutine
repository method를 `suspend fun`으로 문서화했다. test된 repository는 `Flow<Person>`
query에 일반 function을 사용하고, single-result nullable lookup에만 `suspend fun`을
사용한다.

## 결정

양쪽 README locale에서 test된 repository shape를 그대로 반영한다.

- `CoroutineCrudRepository<Person, String>`
- `fun findByLastname(lastname: String): Flow<Person>`
- `suspend fun findByFirstnameAndLastname(...): Person?`

`Flow<T>` repository query는 일반 function이고, single-result coroutine lookup은
`suspend fun`을 사용한다는 짧은 note도 추가한다.

## 후속 가드

`ReadmeCoroutineRepositoryContractTest`는 양쪽 README locale을 읽고 stale `UUID`,
`findByLastName`, `suspend Flow` example이 돌아오는 것을 막는다.
