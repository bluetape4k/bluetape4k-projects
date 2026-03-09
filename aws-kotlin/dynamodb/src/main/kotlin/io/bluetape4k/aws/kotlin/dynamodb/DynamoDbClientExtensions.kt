package io.bluetape4k.aws.kotlin.dynamodb

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.createTable
import aws.sdk.kotlin.services.dynamodb.deleteTable
import aws.sdk.kotlin.services.dynamodb.listTables
import aws.sdk.kotlin.services.dynamodb.model.AttributeDefinition
import aws.sdk.kotlin.services.dynamodb.model.CreateTableRequest
import aws.sdk.kotlin.services.dynamodb.model.CreateTableResponse
import aws.sdk.kotlin.services.dynamodb.model.DeleteTableResponse
import aws.sdk.kotlin.services.dynamodb.model.DescribeTableRequest
import aws.sdk.kotlin.services.dynamodb.model.KeySchemaElement
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import aws.sdk.kotlin.services.dynamodb.model.PutItemResponse
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import aws.sdk.kotlin.services.dynamodb.model.ScanResponse
import aws.sdk.kotlin.services.dynamodb.model.TableStatus
import aws.sdk.kotlin.services.dynamodb.paginators.listTablesPaginated
import aws.sdk.kotlin.services.dynamodb.paginators.scanPaginated
import aws.sdk.kotlin.services.dynamodb.paginators.tableNames
import aws.sdk.kotlin.services.dynamodb.putItem
import aws.smithy.kotlin.runtime.ServiceException
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.aws.kotlin.dynamodb.model.toAttributeValue
import io.bluetape4k.aws.kotlin.dynamodb.model.toAttributeValueMap
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.utils.ShutdownQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.any
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val log by lazy { KotlinLogging.logger { } }

/**
 * [DynamoDbClient]를 생성합니다.
 *
 * @param endpointUrl DynamoDB 엔드포인트 URL
 * @param region AWS 리전
 * @param credentialsProvider AWS 자격 증명 제공자
 * @param httpClient [HttpClientEngine] 엔진 (기본적으로 [aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine] 를 사용합니다.)
 * @param builder [DynamoDbClient.Config.Builder] 를 통해 [DynamoDbClient.Config] 를 설정합니다.
 *
 * @return [DynamoDbClient] 인스턴스
 */
inline fun dynamoDbClientOf(
    endpointUrl: Url? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClient: HttpClientEngine = HttpClientEngineProvider.defaultHttpEngine,
    @BuilderInference crossinline builder: DynamoDbClient.Config.Builder.() -> Unit = {},
): DynamoDbClient {
    region.requireNotBlank("region")

    return DynamoDbClient {
        endpointUrl?.let { this.endpointUrl = it }
        region?.let { this.region = it }
        credentialsProvider?.let { this.credentialsProvider = it }
        this.httpClient = httpClient

        builder()
    }.apply {
        ShutdownQueue.register(this)
    }
}

/**
 * DSL 블록으로 DynamoDB 테이블을 생성합니다.
 *
 * ## 동작/계약
 * - [tableName]은 blank이면 `IllegalArgumentException`을 던진다.
 * - [readCapacityUnits] 또는 [writeCapacityUnits]가 null이 아닌 경우에만 `provisionedThroughput`을 설정한다.
 * - 추가 설정은 [builder] 블록으로 확장할 수 있다.
 *
 * ```kotlin
 * val response = client.createTable("orders") {
 *     keySchema = listOf(keySchemaElementOf("id", KeyType.Hash))
 *     attributeDefinitions = listOf(attributeDefinitionOf("id", ScalarAttributeType.S))
 * }
 * ```
 *
 * @param tableName 생성할 테이블 이름
 * @param readCapacityUnits 읽기 용량 단위 (null이면 생략)
 * @param writeCapacityUnits 쓰기 용량 단위 (null이면 생략)
 * @throws IllegalArgumentException [tableName]이 blank인 경우
 */
suspend fun DynamoDbClient.createTable(
    tableName: String,
    keySchema: List<KeySchemaElement>? = null,
    attributeDefinitions: List<AttributeDefinition>? = null,
    readCapacityUnits: Long? = null,
    writeCapacityUnits: Long? = null,
    @BuilderInference builder: CreateTableRequest.Builder.() -> Unit = {},
): CreateTableResponse {
    tableName.requireNotBlank("tableName")

    return createTable {
        this.tableName = tableName
        keySchema?.let { this.keySchema = it }
        attributeDefinitions?.let { this.attributeDefinitions = it }
        if (readCapacityUnits != null || writeCapacityUnits != null) {
            provisionedThroughput {
                readCapacityUnits?.let { this.readCapacityUnits = it }
                writeCapacityUnits?.let { this.writeCapacityUnits = it }
            }
        }

        builder()
    }
}

/**
 * [name] 이름의 DynamoDB 테이블이 존재하는지 확인합니다.
 *
 * ## 동작/계약
 * - `listTablesPaginated`를 통해 모든 페이지를 순회하며 [name] 포함 여부를 반환한다.
 * - [name]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val exists = client.existsTable("orders")
 * // exists == true or false
 * ```
 *
 * @throws IllegalArgumentException [name]이 blank인 경우
 */
suspend fun DynamoDbClient.existsTable(name: String): Boolean {
    name.requireNotBlank("name")
    return listTablesPaginated()
        .tableNames()
        .any { it == name }
}

/**
 * [name] 이름의 DynamoDB 테이블이 존재하는 경우 삭제합니다.
 *
 * ## 동작/계약
 * - 테이블이 존재하면 삭제 후 [DeleteTableResponse]를 반환한다.
 * - 테이블이 없으면 null을 반환한다.
 *
 * ```kotlin
 * val response = client.deleteTableIfExists("orders")
 * // response != null → 삭제됨, null → 테이블 없음
 * ```
 */
suspend fun DynamoDbClient.deleteTableIfExists(name: String): DeleteTableResponse? {
    return if (existsTable(name)) {
        log.debug { "DynamoDB 테이블[$name]을 삭제합니다." }
        deleteTable { this.tableName = name }
    } else {
        null
    }
}

/**
 * [name] 이름의 DynamoDB 테이블 상태를 반환합니다.
 *
 * ## 동작/계약
 * - `DescribeTable`을 호출해 [TableStatus]를 반환한다.
 * - 재시도 가능한(`isRetryable`) [ServiceException] 발생 시 null을 반환한다.
 * - 재시도 불가능한 예외는 그대로 던진다.
 *
 * ```kotlin
 * val status = client.getTableStatus("orders")
 * // status == TableStatus.Active or null
 * ```
 */
suspend fun DynamoDbClient.getTableStatus(name: String): TableStatus? {
    return try {
        val req = DescribeTableRequest { tableName = name }
        describeTable(req).table?.tableStatus
    } catch (ex: ServiceException) {
        if (!ex.sdkErrorMetadata.isRetryable) throw ex
        null
    }
}

/**
 * [name] 이름의 DynamoDB 테이블이 `CREATING` 상태를 벗어날 때까지 대기합니다.
 *
 * ## 동작/계약
 * - 10ms 간격으로 테이블 상태를 폴링하며 `CREATING`이 아닐 때 반환한다.
 * - [timeout] 내에 준비되지 않으면 `TimeoutCancellationException`을 던진다.
 *
 * ```kotlin
 * client.createTable("orders") { ... }
 * client.waitForTableReady("orders", 30.seconds)
 * ```
 *
 * @param timeout 최대 대기 시간 (기본: 60초)
 * @throws kotlinx.coroutines.TimeoutCancellationException 타임아웃 초과 시
 */
suspend fun DynamoDbClient.waitForTableReady(name: String, timeout: Duration = 60.seconds) {
    log.debug { "DynamoDb 테이블[$name]이 준비될 때까지 [timeout] 만큼 대기합니다 ... " }

    withTimeout(timeout) {
        while (true) {
            if (getTableStatus(name) != TableStatus.Creating) {
                log.debug { "DynamoDb 테이블[$name]이 준비되었습니다." }
                break
            }
            delay(10)
        }
    }
}


/**
 * `Map<String, Any?>` 형태의 [item]을 [tableName] 테이블에 저장합니다.
 *
 * ## 동작/계약
 * - [item] 값들을 [toAttributeValueMap]으로 `AttributeValue`로 변환한 뒤 PutItem을 호출한다.
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - 추가 설정은 [builder] 블록으로 확장할 수 있다.
 *
 * ```kotlin
 * client.putItem("users", mapOf("id" to "u1", "name" to "Alice"))
 * ```
 *
 * @param tableName 저장할 DynamoDB 테이블 이름
 * @param item 저장할 아이템 (값은 `AttributeValue`로 자동 변환)
 * @throws IllegalArgumentException [tableName]이 blank인 경우
 */
suspend inline fun DynamoDbClient.putItem(
    tableName: String,
    item: Map<String, Any?>,
    @BuilderInference crossinline builder: PutItemRequest.Builder.() -> Unit = {},
): PutItemResponse {
    tableName.requireNotBlank("tableName")

    return putItem {
        this.tableName = tableName
        this.item = item.toAttributeValueMap()

        builder()
    }
}

/**
 * [tableName] 테이블을 [exclusiveStartKey]부터 페이지네이션으로 스캔합니다.
 *
 * ## 동작/계약
 * - [exclusiveStartKey] 값들을 [toAttributeValue]로 변환해 시작 키로 설정한다.
 * - [limit]으로 각 페이지당 반환할 최대 아이템 수를 제한한다.
 * - 결과는 `Flow<ScanResponse>` 스트림으로 반환되며 코루틴에서 collect할 수 있다.
 *
 * ```kotlin
 * val pages: Flow<ScanResponse> = client.scanPaginated("orders", emptyMap(), limit = 100)
 * pages.collect { page -> page.items?.forEach { process(it) } }
 * ```
 *
 * @param tableName 스캔할 DynamoDB 테이블 이름
 * @param exclusiveStartKey 페이지 시작 키 (첫 페이지는 빈 맵)
 * @param limit 페이지당 최대 아이템 수 (기본: 1)
 */
inline fun DynamoDbClient.scanPaginated(
    tableName: String,
    exclusiveStartKey: Map<String, Any?>,
    limit: Int = 1,
    @BuilderInference crossinline builder: ScanRequest.Builder.() -> Unit = {},
): Flow<ScanResponse> {
    tableName.requireNotBlank("tableName")

    return scanPaginated {
        this.tableName = tableName
        this.exclusiveStartKey = exclusiveStartKey.mapValues { it.value.toAttributeValue() }
        this.limit = limit

        builder()
    }
}
