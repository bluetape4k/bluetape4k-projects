# Module bluetape4k-aws-dynamodb

AWS SDK V2 DynamoDB 사용 시 기본 기능 외에 추가적인 기능을 제공합니다.

## 주요 기능

* DynamoDbClient/DynamoDbAsyncClient 생성 지원
* DynamoDbEnhancedClient 확장 메서드
* Query DSL 지원 (기본 및 Enhanced)
* Coroutines 기반 Repository 패턴
* Batch Write Executor

## DynamoDbClient 생성

DynamoDbClient 및 DynamoDbAsyncClient를 쉽게 생성할 수 있는 함수를 제공합니다.

```kotlin
// 동기 클라이언트 생성
val dynamoDbClient = dynamoDbClientOf(
    endpoint = endpoint,
    region = region,
    credentialsProvider = credentialsProvider,
) {
    httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)
}

// 비동기 클라이언트 생성
val dynamoDbAsyncClient = dynamoDbAsyncClientOf(
    endpoint = endpoint,
    region = region,
    credentialsProvider = credentialsProvider,
) {
    httpClient(SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient)
}
```

## DynamoDbEnhancedClient 확장 메서드

DynamoDbEnhancedClient를 사용하여 타입 안전하게 DynamoDB를 사용할 수 있습니다.

```kotlin
/**
 * 제네릭 타입으로 DynamoDbTable을 생성합니다.
 *
 * @param T entity type
 * @param tableName table name
 * @return [DynamoDbTable] instance
 */
inline fun <reified T: Any> DynamoDbEnhancedClient.table(tableName: String): DynamoDbTable<T> {
    return table(tableName, TableSchema.fromBean(T::class.java))
}
```

### 테이블 생성 및 사용

```kotlin
// Enhanced Client 생성
val enhancedClient = DynamoDbEnhancedClient.builder()
    .dynamoDbClient(dynamoDbClient)
    .build()

// 테이블 접근
val table = enhancedClient.table<FoodDocument>("food")

// 아이템 저장
val food = FoodDocument(
    id = UUID.randomUUID().toString(),
    name = "Kimchi",
    price = 10000
)
table.putItem(food)

// 아이템 조회
val key = Key.builder().partitionValue(food.id).build()
val loaded = table.getItem(key)
```

### Batch Write 지원

```kotlin
/**
 * 대량의 Item 을 저장할 때, [DynamoDb.MAX_BATCH_ITEM_SIZE] 만큼의 크기로 나누어 저장한다.
 *
 * @param T entity type
 * @param table [MappedTableResource] instance
 * @param items 저장할 item 컬렉션
 * @param chunkSize [DynamoDb.MAX_BATCH_ITEM_SIZE] 보다 작은 값을 사용해야 한다 (1~25)
 * @return [BatchWriteResult] 컬렉션
 */
inline fun <reified T: Any> DynamoDbEnhancedClient.batchWriteItems(
    table: MappedTableResource<T>,
    items: Collection<T>,
    chunkSize: Int = MAX_BATCH_ITEM_SIZE,
): List<BatchWriteResult>
```

사용 예제:

```kotlin
val foods = List(100) { index ->
    FoodDocument(
        id = UUID.randomUUID().toString(),
        name = "Food-$index",
        price = index * 1000
    )
}

// Batch write 실행
val results = enhancedClient.batchWriteItems(table, foods)
```

## Query DSL

DynamoDB 쿼리를 DSL 형태로 작성할 수 있습니다.

### 기본 Query DSL

```kotlin
val request = queryRequest {
    tableName = "local-table"

    primaryKey("myPrimaryKey") {
        eq(2)
    }
    sortKey("mySortKey") {
        between(2 to 3)
    }

    filtering {
        attribute("a") {
            lt(1)
        } and attribute("b") {
            gt(2)
        } or {
            attribute("c") {
                eq(3)
            } and attributeExists("d")
        }
    }
}

val response = dynamoDbClient.query(request)
```

### Enhanced Query DSL

Enhanced Client용 Query DSL도 제공합니다.

```kotlin
val queryRequest = queryEnhancedRequest<FoodDocument> {
    primaryKey("id") {
        equals("partition-key-value")
    }
    sortKey("timestamp") {
        greaterThan("2024-01-01")
        // 또는 between("2024-01-01", "2024-12-31")
        // 또는 beginsWith("prefix-")
    }
    filtering {
        and {
            eq("category", "Korean")
            gt("price", 5000)
            lt("price", 20000)
        }
    }
    scanIndexForward = false  // 내림차순 정렬
}

val results = table.query(queryRequest).items()
```

## Coroutine Repository

Coroutines 환경에서 사용할 수 있는 Repository 패턴을 제공합니다.

```kotlin
/**
 * Coroutines 환경에서 DynamoDB Repository를 사용하기 위한 인터페이스
 */
interface DynamoDbCoroutineRepository<T: DynamoDbEntity> {

    val client: DynamoDbEnhancedAsyncClient
    val table: DynamoDbAsyncTable<T>
    val itemClass: Class<T>

    suspend fun findByKey(key: Key): T?
    suspend fun findFirst(request: QueryEnhancedRequest): List<T>
    suspend fun findFirstByPartitionKey(partitionKey: String): List<T>
    suspend fun count(request: QueryEnhancedRequest): Long
    suspend fun save(item: T)
    fun saveAll(items: Collection<T>): Flow<BatchWriteResult>
    suspend fun update(item: T): T?
    suspend fun delete(item: T): T?
    suspend fun delete(key: Key): T?
    fun deleteAll(items: Iterable<T>): Flow<T>
    fun deleteAllByKeys(keys: Iterable<Key>): Flow<T>
}
```

### Repository 구현 예제

```kotlin
class FoodRepository(
    override val client: DynamoDbEnhancedAsyncClient,
): DynamoDbCoroutineRepository<FoodDocument> {

    override val table = client.table<FoodDocument>("food")
    override val itemClass = FoodDocument::class.java

    suspend fun findByCategory(category: String): List<FoodDocument> {
        val request = queryEnhancedRequest<FoodDocument> {
            filtering {
                eq("category", category)
            }
        }
        return findFirst(request)
    }
}
```

### Repository 사용 예제

```kotlin
@Test
fun `coroutine repository CRUD operations`() = runSuspendIO {
        val repository = FoodRepository(enhancedAsyncClient)

        // 저장
        val food = FoodDocument(
            id = UUID.randomUUID().toString(),
            name = "Bibimbap",
            price = 12000
        )
        repository.save(food)

        // 조회
        val loaded = repository.findByKey(food.key)
        loaded?.name shouldBeEqualTo "Bibimbap"

        // 업데이트
        val updated = food.copy(price = 15000)
        repository.update(updated)

        // 삭제
        repository.delete(food)
    }
```

## Batch Executor

대량의 아이템을 효율적으로 처리할 수 있는 Batch Executor를 제공합니다.

```kotlin
val batchExecutor = DynamoDbBatchExecutor(dynamoDbClient)

// 대량 아이템 저장
val items = List(1000) { createItem(it) }
batchExecutor.batchWriteItems("table-name", items)

// 대량 아이템 삭제
val keys = items.map { it.key }
batchExecutor.batchDeleteItems("table-name", keys)
```

## AttributeValue 지원

Kotlin 타입을 DynamoDB AttributeValue로 쉽게 변환할 수 있는 확장 함수를 제공합니다.

```kotlin
// String to AttributeValue
val attr = "value".toAttributeValue()

// Number to AttributeValue
val numAttr = 42.toAttributeValue()

// List to AttributeValue
val listAttr = listOf("a", "b", "c").toAttributeValue()

// Map to AttributeValue
val mapAttr = mapOf("key" to "value").toAttributeValue()
```

## Table Schema 지원

Kotlin 데이터 클래스를 위한 TableSchema 생성을 지원합니다.

```kotlin
@DynamoDbBean
data class FoodDocument(
    @get:DynamoDbPartitionKey
    var id: String = "",
    var name: String = "",
    var price: Int = 0
)

// TableSchema 자동 생성
val tableSchema = TableSchema.fromBean(FoodDocument::class.java)
```
