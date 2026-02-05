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
    @BuilderInference builder: BatchExecuteStatementRequest.Builder.() -> Unit,
): BatchExecuteStatementRequest =
    BatchExecuteStatementRequest.builder().apply(builder).build()

inline fun BatchGetItemRequest(
    @BuilderInference builder: BatchGetItemRequest.Builder.() -> Unit,
): BatchGetItemRequest =
    BatchGetItemRequest.builder().apply(builder).build()

inline fun BatchStatementRequest(
    @BuilderInference builder: BatchStatementRequest.Builder.() -> Unit,
): BatchStatementRequest =
    BatchStatementRequest.builder().apply(builder).build()

inline fun BatchWriteItemRequest(
    @BuilderInference builder: BatchWriteItemRequest.Builder.() -> Unit,
): BatchWriteItemRequest =
    BatchWriteItemRequest.builder().apply(builder).build()

inline fun Capacity(
    @BuilderInference builder: Capacity.Builder.() -> Unit,
): Capacity =
    Capacity.builder().apply(builder).build()

inline fun ConditionCheck(
    @BuilderInference builder: ConditionCheck.Builder.() -> Unit,
): ConditionCheck =
    ConditionCheck.builder().apply(builder).build()

inline fun Condition(
    @BuilderInference builder: Condition.Builder.() -> Unit,
): Condition =
    Condition.builder().apply(builder).build()

inline fun CreateBackupRequest(
    @BuilderInference builder: CreateBackupRequest.Builder.() -> Unit,
): CreateBackupRequest =
    CreateBackupRequest.builder().apply(builder).build()

inline fun CreateGlobalTableRequest(
    @BuilderInference builder: CreateGlobalTableRequest.Builder.() -> Unit,
): CreateGlobalTableRequest =
    CreateGlobalTableRequest.builder().apply(builder).build()

inline fun CreateTableRequest(
    @BuilderInference builder: CreateTableRequest.Builder.() -> Unit,
): CreateTableRequest =
    CreateTableRequest.builder().apply(builder).build()

inline fun DeleteBackupRequest(
    @BuilderInference builder: DeleteBackupRequest.Builder.() -> Unit,
): DeleteBackupRequest =
    DeleteBackupRequest.builder().apply(builder).build()

inline fun Delete(
    @BuilderInference builder: Delete.Builder.() -> Unit,
): Delete =
    Delete.builder().apply(builder).build()

inline fun DeleteRequest(
    @BuilderInference builder: DeleteRequest.Builder.() -> Unit,
): DeleteRequest =
    DeleteRequest.builder().apply(builder).build()

inline fun DeleteTableRequest(
    @BuilderInference builder: DeleteTableRequest.Builder.() -> Unit,
): DeleteTableRequest =
    DeleteTableRequest.builder().apply(builder).build()

inline fun ExecuteStatementRequest(
    @BuilderInference builder: ExecuteStatementRequest.Builder.() -> Unit,
): ExecuteStatementRequest =
    ExecuteStatementRequest.builder().apply(builder).build()

inline fun ExecuteTransactionRequest(
    @BuilderInference builder: ExecuteTransactionRequest.Builder.() -> Unit,
): ExecuteTransactionRequest =
    ExecuteTransactionRequest.builder().apply(builder).build()

inline fun GetItemRequest(
    @BuilderInference builder: GetItemRequest.Builder.() -> Unit,
): GetItemRequest =
    GetItemRequest.builder().apply(builder).build()

inline fun GetRecordsRequest(
    @BuilderInference builder: GetRecordsRequest.Builder.() -> Unit,
): GetRecordsRequest =
    GetRecordsRequest.builder().apply(builder).build()

inline fun GetShardIteratorRequest(
    @BuilderInference builder: GetShardIteratorRequest.Builder.() -> Unit,
): GetShardIteratorRequest =
    GetShardIteratorRequest.builder().apply(builder).build()

inline fun ListGlobalTablesRequest(
    @BuilderInference builder: ListGlobalTablesRequest.Builder.() -> Unit,
): ListGlobalTablesRequest =
    ListGlobalTablesRequest.builder().apply(builder).build()

inline fun ListTablesRequest(
    @BuilderInference builder: ListTablesRequest.Builder.() -> Unit,
): ListTablesRequest =
    ListTablesRequest.builder().apply(builder).build()

inline fun ListTagsOfResourceRequest(
    @BuilderInference builder: ListTagsOfResourceRequest.Builder.() -> Unit,
): ListTagsOfResourceRequest =
    ListTagsOfResourceRequest.builder().apply(builder).build()

inline fun PutItemRequest(
    @BuilderInference builder: PutItemRequest.Builder.() -> Unit,
): PutItemRequest =
    PutItemRequest.builder().apply(builder).build()

inline fun PutRequest(
    @BuilderInference builder: PutRequest.Builder.() -> Unit,
): PutRequest =
    PutRequest.builder().apply(builder).build()

fun putRequestOf(items: Map<String, AttributeValue>): PutRequest =
    PutRequest {
        item(items)
    }

inline fun QueryRequest(
    @BuilderInference builder: QueryRequest.Builder.() -> Unit,
): QueryRequest =
    QueryRequest.builder().apply(builder).build()

inline fun Record(
    @BuilderInference builder: Record.Builder.() -> Unit,
): Record =
    Record.builder().apply(builder).build()

inline fun ReplicaSettingsUpdate(
    @BuilderInference builder: ReplicaSettingsUpdate.Builder.() -> Unit,
): ReplicaSettingsUpdate =
    ReplicaSettingsUpdate.builder().apply(builder).build()

inline fun ReplicaUpdate(
    @BuilderInference builder: ReplicaUpdate.Builder.() -> Unit,
): ReplicaUpdate =
    ReplicaUpdate.builder().apply(builder).build()

inline fun RestoreTableFromBackupRequest(
    @BuilderInference builder: RestoreTableFromBackupRequest.Builder.() -> Unit,
): RestoreTableFromBackupRequest =
    RestoreTableFromBackupRequest.builder().apply(builder).build()

inline fun RestoreTableToPointInTimeRequest(
    @BuilderInference builder: RestoreTableToPointInTimeRequest.Builder.() -> Unit,
): RestoreTableToPointInTimeRequest =
    RestoreTableToPointInTimeRequest.builder().apply(builder).build()

inline fun ScanRequest(
    @BuilderInference builder: ScanRequest.Builder.() -> Unit,
): ScanRequest =
    ScanRequest.builder().apply(builder).build()

inline fun SequenceNumberRange(
    @BuilderInference builder: SequenceNumberRange.Builder.() -> Unit,
): SequenceNumberRange =
    SequenceNumberRange.builder().apply(builder).build()

inline fun Stream(
    @BuilderInference builder: Stream.Builder.() -> Unit,
): Stream =
    Stream.builder().apply(builder).build()

inline fun StreamRecord(
    @BuilderInference builder: StreamRecord.Builder.() -> Unit,
): StreamRecord =
    StreamRecord.builder().apply(builder).build()

inline fun StreamSpecification(
    @BuilderInference builder: StreamSpecification.Builder.() -> Unit,
): StreamSpecification =
    StreamSpecification.builder().apply(builder).build()

inline fun TagResourceRequest(
    @BuilderInference builder: TagResourceRequest.Builder.() -> Unit,
): TagResourceRequest =
    TagResourceRequest.builder().apply(builder).build()

inline fun TransactGetItem(
    @BuilderInference builder: TransactGetItem.Builder.() -> Unit,
): TransactGetItem =
    TransactGetItem.builder().apply(builder).build()

inline fun TransactGetItemsRequest(
    @BuilderInference builder: TransactGetItemsRequest.Builder.() -> Unit,
): TransactGetItemsRequest =
    TransactGetItemsRequest.builder().apply(builder).build()

inline fun TransactWriteItem(
    @BuilderInference builder: TransactWriteItem.Builder.() -> Unit,
): TransactWriteItem =
    TransactWriteItem.builder().apply(builder).build()

inline fun TransactWriteItemsRequest(
    @BuilderInference builder: TransactWriteItemsRequest.Builder.() -> Unit,
): TransactWriteItemsRequest =
    TransactWriteItemsRequest.builder().apply(builder).build()

inline fun UntagResourceRequest(
    @BuilderInference builder: UntagResourceRequest.Builder.() -> Unit,
): UntagResourceRequest =
    UntagResourceRequest.builder().apply(builder).build()

inline fun UpdateContinuousBackupsRequest(
    @BuilderInference builder: UpdateContinuousBackupsRequest.Builder.() -> Unit,
): UpdateContinuousBackupsRequest =
    UpdateContinuousBackupsRequest.builder().apply(builder).build()

inline fun UpdateContributorInsightsRequest(
    @BuilderInference builder: UpdateContributorInsightsRequest.Builder.() -> Unit,
): UpdateContributorInsightsRequest =
    UpdateContributorInsightsRequest.builder().apply(builder).build()

inline fun Update(
    @BuilderInference builder: Update.Builder.() -> Unit,
): Update =
    Update.builder().apply(builder).build()

inline fun UpdateGlobalTableRequest(
    @BuilderInference builder: UpdateGlobalTableRequest.Builder.() -> Unit,
): UpdateGlobalTableRequest =
    UpdateGlobalTableRequest.builder().apply(builder).build()

inline fun UpdateGlobalTableSettingsRequest(
    @BuilderInference builder: UpdateGlobalTableSettingsRequest.Builder.() -> Unit,
): UpdateGlobalTableSettingsRequest =
    UpdateGlobalTableSettingsRequest.builder().apply(builder).build()

inline fun UpdateItemRequest(
    @BuilderInference builder: UpdateItemRequest.Builder.() -> Unit,
): UpdateItemRequest =
    UpdateItemRequest.builder().apply(builder).build()

inline fun UpdateTableRequest(
    @BuilderInference builder: UpdateTableRequest.Builder.() -> Unit,
): UpdateTableRequest =
    UpdateTableRequest.builder().apply(builder).build()

inline fun UpdateTimeToLiveRequest(
    @BuilderInference builder: UpdateTimeToLiveRequest.Builder.() -> Unit,
): UpdateTimeToLiveRequest =
    UpdateTimeToLiveRequest.builder().apply(builder).build()

inline fun <reified T: Any> WriteBatch(
    table: MappedTableResource<T>,
    @BuilderInference builder: WriteBatch.Builder<T>.() -> Unit,
): WriteBatch {
    return WriteBatch.builder(T::class.java)
        .mappedTableResource(table)
        .apply(builder)
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
    @BuilderInference builder: WriteRequest.Builder.() -> Unit,
): WriteRequest {
    return WriteRequest.builder().apply(builder).build()
}

fun writeRequestOf(items: Map<String, AttributeValue>): WriteRequest =
    WriteRequest {
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
