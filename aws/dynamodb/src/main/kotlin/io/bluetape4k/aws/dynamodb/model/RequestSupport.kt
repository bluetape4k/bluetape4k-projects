package io.bluetape4k.aws.dynamodb.model

import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.BatchExecuteStatementRequest
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest
import software.amazon.awssdk.services.dynamodb.model.BatchStatementRequest
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest
import software.amazon.awssdk.services.dynamodb.model.Capacity
import software.amazon.awssdk.services.dynamodb.model.Condition
import software.amazon.awssdk.services.dynamodb.model.ConditionCheck
import software.amazon.awssdk.services.dynamodb.model.CreateBackupRequest
import software.amazon.awssdk.services.dynamodb.model.CreateGlobalTableRequest
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.Delete
import software.amazon.awssdk.services.dynamodb.model.DeleteBackupRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementRequest
import software.amazon.awssdk.services.dynamodb.model.ExecuteTransactionRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetRecordsRequest
import software.amazon.awssdk.services.dynamodb.model.GetShardIteratorRequest
import software.amazon.awssdk.services.dynamodb.model.ListGlobalTablesRequest
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest
import software.amazon.awssdk.services.dynamodb.model.ListTagsOfResourceRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.Record
import software.amazon.awssdk.services.dynamodb.model.ReplicaSettingsUpdate
import software.amazon.awssdk.services.dynamodb.model.ReplicaUpdate
import software.amazon.awssdk.services.dynamodb.model.RestoreTableFromBackupRequest
import software.amazon.awssdk.services.dynamodb.model.RestoreTableToPointInTimeRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.dynamodb.model.SequenceNumberRange
import software.amazon.awssdk.services.dynamodb.model.Stream
import software.amazon.awssdk.services.dynamodb.model.StreamRecord
import software.amazon.awssdk.services.dynamodb.model.StreamSpecification
import software.amazon.awssdk.services.dynamodb.model.TagResourceRequest
import software.amazon.awssdk.services.dynamodb.model.TransactGetItem
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsRequest
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest
import software.amazon.awssdk.services.dynamodb.model.UntagResourceRequest
import software.amazon.awssdk.services.dynamodb.model.Update
import software.amazon.awssdk.services.dynamodb.model.UpdateContinuousBackupsRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateContributorInsightsRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateGlobalTableRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateGlobalTableSettingsRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateTableRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest
import software.amazon.awssdk.services.dynamodb.model.WriteRequest

inline fun BatchExecuteStatementRequest(
    initializer: BatchExecuteStatementRequest.Builder.() -> Unit,
): BatchExecuteStatementRequest =
    BatchExecuteStatementRequest.builder().apply(initializer).build()

inline fun BatchGetItemRequest(
    initializer: BatchGetItemRequest.Builder.() -> Unit,
): BatchGetItemRequest =
    BatchGetItemRequest.builder().apply(initializer).build()

inline fun BatchStatementRequest(
    initializer: BatchStatementRequest.Builder.() -> Unit,
): BatchStatementRequest =
    BatchStatementRequest.builder().apply(initializer).build()

inline fun BatchWriteItemRequest(
    initializer: BatchWriteItemRequest.Builder.() -> Unit,
): BatchWriteItemRequest =
    BatchWriteItemRequest.builder().apply(initializer).build()

inline fun Capacity(
    initializer: Capacity.Builder.() -> Unit,
): Capacity =
    Capacity.builder().apply(initializer).build()

inline fun ConditionCheck(
    initializer: ConditionCheck.Builder.() -> Unit,
): ConditionCheck =
    ConditionCheck.builder().apply(initializer).build()

inline fun Condition(
    initializer: Condition.Builder.() -> Unit,
): Condition =
    Condition.builder().apply(initializer).build()

inline fun CreateBackupRequest(
    initializer: CreateBackupRequest.Builder.() -> Unit,
): CreateBackupRequest =
    CreateBackupRequest.builder().apply(initializer).build()

inline fun CreateGlobalTableRequest(
    initializer: CreateGlobalTableRequest.Builder.() -> Unit,
): CreateGlobalTableRequest =
    CreateGlobalTableRequest.builder().apply(initializer).build()

inline fun CreateTableRequest(
    initializer: CreateTableRequest.Builder.() -> Unit,
): CreateTableRequest =
    CreateTableRequest.builder().apply(initializer).build()

inline fun DeleteBackupRequest(
    initializer: DeleteBackupRequest.Builder.() -> Unit,
): DeleteBackupRequest =
    DeleteBackupRequest.builder().apply(initializer).build()

inline fun Delete(
    initializer: Delete.Builder.() -> Unit,
): Delete =
    Delete.builder().apply(initializer).build()

inline fun DeleteRequest(
    initializer: DeleteRequest.Builder.() -> Unit,
): DeleteRequest =
    DeleteRequest.builder().apply(initializer).build()

inline fun DeleteTableRequest(
    initializer: DeleteTableRequest.Builder.() -> Unit,
): DeleteTableRequest =
    DeleteTableRequest.builder().apply(initializer).build()

inline fun ExecuteStatementRequest(
    initializer: ExecuteStatementRequest.Builder.() -> Unit,
): ExecuteStatementRequest =
    ExecuteStatementRequest.builder().apply(initializer).build()

inline fun ExecuteTransactionRequest(
    initializer: ExecuteTransactionRequest.Builder.() -> Unit,
): ExecuteTransactionRequest =
    ExecuteTransactionRequest.builder().apply(initializer).build()

inline fun GetItemRequest(
    initializer: GetItemRequest.Builder.() -> Unit,
): GetItemRequest =
    GetItemRequest.builder().apply(initializer).build()

inline fun GetRecordsRequest(
    initializer: GetRecordsRequest.Builder.() -> Unit,
): GetRecordsRequest =
    GetRecordsRequest.builder().apply(initializer).build()

inline fun GetShardIteratorRequest(
    initializer: GetShardIteratorRequest.Builder.() -> Unit,
): GetShardIteratorRequest =
    GetShardIteratorRequest.builder().apply(initializer).build()

inline fun ListGlobalTablesRequest(
    initializer: ListGlobalTablesRequest.Builder.() -> Unit,
): ListGlobalTablesRequest =
    ListGlobalTablesRequest.builder().apply(initializer).build()

inline fun ListTablesRequest(
    initializer: ListTablesRequest.Builder.() -> Unit,
): ListTablesRequest =
    ListTablesRequest.builder().apply(initializer).build()

inline fun ListTagsOfResourceRequest(
    initializer: ListTagsOfResourceRequest.Builder.() -> Unit,
): ListTagsOfResourceRequest =
    ListTagsOfResourceRequest.builder().apply(initializer).build()

inline fun PutItemRequest(
    initializer: PutItemRequest.Builder.() -> Unit,
): PutItemRequest =
    PutItemRequest.builder().apply(initializer).build()

inline fun PutRequest(
    initializer: PutRequest.Builder.() -> Unit,
): PutRequest =
    PutRequest.builder().apply(initializer).build()

fun putRequestOf(items: Map<String, AttributeValue>): PutRequest = PutRequest {
    item(items)
}

inline fun QueryRequest(
    initializer: QueryRequest.Builder.() -> Unit,
): QueryRequest =
    QueryRequest.builder().apply(initializer).build()

inline fun record(
    initializer: Record.Builder.() -> Unit,
): Record =
    Record.builder().apply(initializer).build()

inline fun ReplicaSettingsUpdate(
    initializer: ReplicaSettingsUpdate.Builder.() -> Unit,
): ReplicaSettingsUpdate =
    ReplicaSettingsUpdate.builder().apply(initializer).build()

inline fun ReplicaUpdate(
    initializer: ReplicaUpdate.Builder.() -> Unit,
): ReplicaUpdate =
    ReplicaUpdate.builder().apply(initializer).build()

inline fun RestoreTableFromBackupRequest(
    initializer: RestoreTableFromBackupRequest.Builder.() -> Unit,
): RestoreTableFromBackupRequest =
    RestoreTableFromBackupRequest.builder().apply(initializer).build()

inline fun RestoreTableToPointInTimeRequest(
    initializer: RestoreTableToPointInTimeRequest.Builder.() -> Unit,
): RestoreTableToPointInTimeRequest =
    RestoreTableToPointInTimeRequest.builder().apply(initializer).build()

inline fun ScanRequest(
    initializer: ScanRequest.Builder.() -> Unit,
): ScanRequest =
    ScanRequest.builder().apply(initializer).build()

inline fun SequenceNumberRange(
    initializer: SequenceNumberRange.Builder.() -> Unit,
): SequenceNumberRange =
    SequenceNumberRange.builder().apply(initializer).build()

inline fun Stream(
    initializer: Stream.Builder.() -> Unit,
): Stream =
    Stream.builder().apply(initializer).build()

inline fun StreamRecord(
    initializer: StreamRecord.Builder.() -> Unit,
): StreamRecord =
    StreamRecord.builder().apply(initializer).build()

inline fun StreamSpecification(
    initializer: StreamSpecification.Builder.() -> Unit,
): StreamSpecification =
    StreamSpecification.builder().apply(initializer).build()

inline fun TagResourceRequest(initializer: TagResourceRequest.Builder.() -> Unit): TagResourceRequest =
    TagResourceRequest.builder().apply(initializer).build()

inline fun TransactGetItem(
    initializer: TransactGetItem.Builder.() -> Unit,
): TransactGetItem =
    TransactGetItem.builder().apply(initializer).build()

inline fun TransactGetItemsRequest(
    initializer: TransactGetItemsRequest.Builder.() -> Unit,
): TransactGetItemsRequest =
    TransactGetItemsRequest.builder().apply(initializer).build()

inline fun TransactWriteItem(
    initializer: TransactWriteItem.Builder.() -> Unit,
): TransactWriteItem =
    TransactWriteItem.builder().apply(initializer).build()

inline fun TransactWriteItemsRequest(
    initializer: TransactWriteItemsRequest.Builder.() -> Unit,
): TransactWriteItemsRequest =
    TransactWriteItemsRequest.builder().apply(initializer).build()

inline fun UntagResourceRequest(
    initializer: UntagResourceRequest.Builder.() -> Unit,
): UntagResourceRequest =
    UntagResourceRequest.builder().apply(initializer).build()

inline fun UpdateContinuousBackupsRequest(
    initializer: UpdateContinuousBackupsRequest.Builder.() -> Unit,
): UpdateContinuousBackupsRequest =
    UpdateContinuousBackupsRequest.builder().apply(initializer).build()

inline fun UpdateContributorInsightsRequest(
    initializer: UpdateContributorInsightsRequest.Builder.() -> Unit,
): UpdateContributorInsightsRequest =
    UpdateContributorInsightsRequest.builder().apply(initializer).build()

inline fun Update(initializer: Update.Builder.() -> Unit): Update =
    Update.builder().apply(initializer).build()

inline fun UpdateGlobalTableRequest(
    initializer: UpdateGlobalTableRequest.Builder.() -> Unit,
): UpdateGlobalTableRequest =
    UpdateGlobalTableRequest.builder().apply(initializer).build()

inline fun UpdateGlobalTableSettingsRequest(
    initializer: UpdateGlobalTableSettingsRequest.Builder.() -> Unit,
): UpdateGlobalTableSettingsRequest =
    UpdateGlobalTableSettingsRequest.builder().apply(initializer).build()

inline fun UpdateItemRequest(
    initializer: UpdateItemRequest.Builder.() -> Unit,
): UpdateItemRequest =
    UpdateItemRequest.builder().apply(initializer).build()

inline fun UpdateTableRequest(
    initializer: UpdateTableRequest.Builder.() -> Unit,
): UpdateTableRequest =
    UpdateTableRequest.builder().apply(initializer).build()

inline fun UpdateTimeToLiveRequest(
    initializer: UpdateTimeToLiveRequest.Builder.() -> Unit,
): UpdateTimeToLiveRequest =
    UpdateTimeToLiveRequest.builder().apply(initializer).build()

inline fun <reified T: Any> WriteBatch(
    table: MappedTableResource<T>,
    initializer: WriteBatch.Builder<T>.() -> Unit,
): WriteBatch {
    return WriteBatch.builder(T::class.java)
        .mappedTableResource(table)
        .apply(initializer)
        .build()
}

inline fun <reified T: Any> writeBatchOf(
    table: MappedTableResource<T>,
    items: Collection<T>,
): WriteBatch = WriteBatch<T>(table) {
    items.forEach { addPutItem(it) }
}

fun <T: Any> writeBatchOf(
    table: MappedTableResource<T>,
    items: Collection<T>,
    itemClass: Class<T>,
): WriteBatch {
    return WriteBatch.builder(itemClass)
        .mappedTableResource(table)
        .apply {
            items.forEach { addPutItem(it) }
        }
        .build()
}

inline fun WriteRequest(
    initializer: WriteRequest.Builder.() -> Unit,
): WriteRequest {
    return WriteRequest.builder().apply(initializer).build()
}

fun writeRequestOf(items: Map<String, AttributeValue>): WriteRequest = WriteRequest {
    this.putRequest(PutRequest { item(items) })
}

/**
 * [QueryRequest]의 정보를 문자열로 표현한다
 *
 * @return [QueryRequest] 정보를 나타내는 문자열
 */
fun QueryRequest.describe(): String = buildString {
    appendLine()
    append("keyConditions: ").appendLine(keyConditions())
    append("keyConditionExpression: ").appendLine(keyConditionExpression())
    append("filters Expressin: ").appendLine(this@describe.filterExpression())

    append("expression names: ").appendLine(this@describe.expressionAttributeNames())
    append("expression values: ").appendLine(this@describe.expressionAttributeValues())
    append("attributesToGet: ").appendLine(this@describe.attributesToGet())
}
