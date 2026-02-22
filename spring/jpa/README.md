# Module bluetape4k-spring-jpa

Spring Data JPA를 위한 확장 기능을 제공합니다.

## 주요 기능

- **StatelessSession 지원**: 대량 데이터 처리를 위한 Hibernate StatelessSession 통합
- **트랜잭션 통합**: Spring 트랜잭션과 자동 연동

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-spring-jpa:${version}")
}
```

## 주요 기능 상세

### 1. StatelessSession 지원

Hibernate의 `StatelessSession`은 일반 `Session`과 달리 1차 캐시, 더티 체킹, 지연 로딩 등을 제공하지 않습니다. 대신 메모리 사용량이 적고 대량 데이터 처리에 적합합니다.

#### StatelessSession 이란?

| 특징      | Session | StatelessSession |
|---------|---------|------------------|
| 1차 캐시   | O       | X                |
| 더티 체킹   | O       | X                |
| 지연 로딩   | O       | X                |
| 메모리 사용  | 높음      | 낮음               |
| 대량 처리   | 느림      | 빠름               |
| JPA 이벤트 | 발생      | 발생하지 않음          |

#### 설정 방법

```kotlin
import io.bluetape4k.spring.jpa.stateless.StatelessSessionFactoryBean
import org.hibernate.SessionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JpaConfig {

    @Bean
    fun statelessSession(sessionFactory: SessionFactory): StatelessSessionFactoryBean {
        return StatelessSessionFactoryBean(sessionFactory)
    }
}
```

#### 사용 예시

```kotlin
import org.hibernate.StatelessSession
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BulkDataService(
    private val statelessSession: StatelessSession
) {

    @Transactional
    fun bulkInsertOrders(orders: List<Order>) {
        // 대량 INSERT - 메모리 효율적
        orders.forEach { order ->
            statelessSession.insert(order)
        }
    }

    @Transactional
    fun bulkUpdatePrices(productIds: List<Long>, newPrice: BigDecimal) {
        // 대량 UPDATE
        productIds.forEach { productId ->
            val product = statelessSession.get(Product::class.java, productId)
            product?.let {
                it.price = newPrice
                statelessSession.update(it)
            }
        }
    }

    @Transactional
    fun bulkDeleteOldRecords(ids: List<Long>) {
        // 대량 DELETE
        ids.forEach { id ->
            val record = statelessSession.get(OldRecord::class.java, id)
            record?.let { statelessSession.delete(it) }
        }
    }
}
```

#### EntityManager와 함께 사용

```kotlin
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MixedDataService(
    private val entityManager: EntityManager,
    private val statelessSession: StatelessSession
) {

    @Transactional
    fun processLargeDataset() {
        // 일반 JPA 작업
        val config = entityManager.find(Config::class.java, 1L)

        // 대량 처리는 StatelessSession 사용
        repeat(100_000) {
            val log = AuditLog(
                action = "PROCESS",
                timestamp = Instant.now()
            )
            statelessSession.insert(log)

            // 주기적으로 flush
            if (it % 1000 == 0) {
                statelessSession.flush()
            }
        }
    }
}
```

#### EntityManager 확장으로 사용

```kotlin
import io.bluetape4k.hibernate.asSessionImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DataService(
    private val entityManager: EntityManager
) {

    @Transactional
    fun bulkInsertWithEntityManager(entities: List<Entity>) {
        // EntityManager에서 StatelessSession 획득
        val session = entityManager.asSessionImpl()
        val statelessSession = session.sessionFactory.openStatelessSession(
            session.jdbcCoordinator.logicalConnection.physicalConnection
        )

        try {
            entities.forEach { entity ->
                statelessSession.insert(entity)
            }
        } finally {
            statelessSession.close()
        }
    }
}
```

---

### 2. 주의사항

#### 트랜잭션 필수

StatelessSession은 반드시 활성화된 트랜잭션 내에서 사용해야 합니다.

```kotlin
@Transactional  // 필수!
fun processData() {
    statelessSession.insert(entity)  // 트랜잭션 없으면 예외 발생
}
```

#### 더티 체킹 미지원

StatelessSession은 더티 체킹을 하지 않으므로, 변경 사항을 명시적으로 `update()` 해야 합니다.

```kotlin
@Transactional
fun updateEntity(id: Long, newName: String) {
    val entity = statelessSession.get(Entity::class.java, id)
    entity.name = newName

    // 명시적 update 필요
    statelessSession.update(entity)
}
```

#### 연관관계 처리

연관관계가 있는 엔티티의 경우, 외래 키 값을 직접 설정해야 합니다.

```kotlin
@Transactional
fun insertOrderWithItems(order: Order, items: List<OrderItem>) {
    // 먼저 Order 저장
    val orderId = statelessSession.insert(order) as Long

    // OrderItem에 외래 키 설정
    items.forEach { item ->
        item.orderId = orderId
        statelessSession.insert(item)
    }
}
```

---

### 3. 성능 비교

```kotlin
@Service
class PerformanceTestService(
    private val orderRepository: OrderRepository,
    private val statelessSession: StatelessSession
) {

    // 일반 JPA Repository 사용 - 느림
    @Transactional
    fun insertWithJpa(count: Int) {
        repeat(count) {
            orderRepository.save(Order(/* ... */))
        }
        // count가 크면 OutOfMemoryError 발생 가능
    }

    // StatelessSession 사용 - 빠름
    @Transactional
    fun insertWithStateless(count: Int) {
        repeat(count) {
            statelessSession.insert(Order(/* ... */))

            if (it % 1000 == 0) {
                statelessSession.flush()
            }
        }
    }
}
```

---

## 테스트

```bash
./gradlew :spring:jpa:test
```

## 참고

- [Hibernate StatelessSession](https://docs.jboss.org/hibernate/orm/6.6/javadocs/org/hibernate/StatelessSession.html)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/reference/)
