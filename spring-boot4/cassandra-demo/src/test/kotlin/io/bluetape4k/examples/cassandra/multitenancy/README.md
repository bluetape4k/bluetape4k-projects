# Multi tenancy in Scylla

## 참고 자료

[Multi-Tenant Cassandra Clusters with Spring Data Cassandra](https://dzone.com/articles/multi-tenant-cassandra-cluster-with-spring-data-ca)

## Table Level Multi-Tenant

Thread Local 을 이용하여 `tenantId` 를 관리하고, 이를 활용하여 Partition Key 로 사용되는 tenantId 컬럼을 매칭해서 사용하도록 합니다.

## Keyspace level Multi-Tenant

기본적으로는 Thread Local 을 이용하여 `tenantId` 를 관리하고, Session의 `keyspace` 를 활용하거나 Query 에 `keyspace` 를 지정해서 사용하는 방법이 있다.

`Repository` 를 사용하는 경우에는 `SimpleCassandraRepository` 를 상속받고, TenantId 를 `keyspace`로
지정하는 `KeyspaceAwareCassandraRepository` 를 구현하고 사용한다

Keyspace로 multi-tenant를 구현한 경우 Repository 구현체를 변경하는 법

```kotlin
@SpringBootApplication
@EnableCassandraReposities(repositoryBaseClass = KeyspaceAwareCassandraRepository::class)
class MultitenantConfiguraiton {
// ...
}
```

## 주의할 점

ThreadLocal 을 이용하여 tenantId 를 사용할 경우, Reactive 나 Coroutines 에서 문제가 발생할 수 있습니다.
Coroutines 환경에서는 `ThreadLocal` 을 하나의 `CoroutineContext` 로 취급할 수 있으므로 다음과 같이 사용하면 됩니다.

ThreadLocal 을 Coroutine ContextElement로 사용

```kotlin
TenantIdProvider.tenantId.set("fox")
val job1 = launch(Dispatchers.IO + TenantIdProvider.tenantId.asContextElement()) {
    repeat(10) {
        val loaded = repository.findAllByName("homer").toList()

        loaded.size shouldBeEqualTo 1
        loaded.first() shouldBeEqualTo Employee("fox", "homer")
    }
}

// 다른 정보를 지정하더라도, 위의 thread local 값과는 다른 것을 사용한다
TenantIdProvider.tenantId.set("apple")
val job2 = launch(Dispatchers.IO + TenantIdProvider.tenantId.asContextElement()) {
    repeat(10) {
        val loaded = repository.findAllByName("Steve").toList()
        loaded.shouldBeEmpty()
    }
}
```
