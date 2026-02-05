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
import aws.sdk.kotlin.services.dynamodb.paginators.scanPaginated
import aws.sdk.kotlin.services.dynamodb.putItem
import aws.smithy.kotlin.runtime.ServiceException
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.aws.kotlin.dynamodb.model.toAttributeValue
import io.bluetape4k.aws.kotlin.http.defaultCrtHttpEngineOf
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val log by lazy { KotlinLogging.logger { } }

/**
 * [DynamoDbClient]를 생성합니다.
 *
 * @param endpointUrl S3 엔드포인트 URL
 * @param region AWS 리전
 * @param credentialsProvider AWS 자격 증명 제공자
 * @param httpClientEngine [HttpClientEngine] 엔진 (기본적으로 [aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine] 를 사용합니다.)
 * @param builder [DynamoDbClient.Config.Builder] 를 통해 [DynamoDbClient.Config] 를 설정합니다.
 *
 * @return [DynamoDbClient] 인스턴스
 */
fun dynamoDbClientOf(
    endpointUrl: String? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClientEngine: HttpClientEngine = defaultCrtHttpEngineOf(),
    @BuilderInference builder: DynamoDbClient.Config.Builder.() -> Unit = {},
): DynamoDbClient {
    endpointUrl.requireNotBlank("endpoint")

    return DynamoDbClient {
        endpointUrl?.let { this.endpointUrl = Url.parse(it) }
        region?.let { this.region = it }
        credentialsProvider?.let { this.credentialsProvider = it }
        httpClient = httpClientEngine

        builder()
    }
}

/**
 * DynamoDB 테이블을 생성합니다.
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
 * 테이븖 명[name]을 가진 DynamoDB 테이블이 존재하는지 확인합니다.
 */
suspend fun DynamoDbClient.existsTable(name: String): Boolean {
    name.requireNotBlank("name")
    return this.listTables { }.tableNames?.contains(name) ?: false
}

/**
 * 테이블 명[name]을 가진 DynamoDB 테이블이 존재한다면 삭제합니다.
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
 * 테이블 명[name]을 가진 DynamoDB 테이블의 상태를 확인합니다.
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
 * 테이블 명[name]을 가진 DynamoDB 테이블이 준비될 때까지 [timeout] 시간만큼 대기합니다.
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


suspend fun DynamoDbClient.putItem(
    tableName: String,
    item: Map<String, Any?>,
    @BuilderInference builder: PutItemRequest.Builder.() -> Unit = {},
): PutItemResponse {
    tableName.requireNotBlank("tableName")

    return putItem {
        this.tableName = tableName
        this.item = item.mapValues { it.value.toAttributeValue() }

        builder()
    }
}

fun DynamoDbClient.scanPaginated(
    tableName: String,
    exclusiveStartKey: Map<String, Any?>,
    limit: Int = 1,
    @BuilderInference builder: ScanRequest.Builder.() -> Unit = {},
): Flow<ScanResponse> {
    tableName.requireNotBlank("tableName")

    return scanPaginated {
        this.tableName = tableName
        this.exclusiveStartKey = exclusiveStartKey.mapValues { it.value.toAttributeValue() }
        this.limit = limit

        builder()
    }
}
