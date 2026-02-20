package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.querybuilder.delete.Delete
import com.datastax.oss.driver.api.querybuilder.delete.DeleteSelection
import com.datastax.oss.driver.api.querybuilder.insert.Insert
import com.datastax.oss.driver.api.querybuilder.update.Update
import com.datastax.oss.driver.api.querybuilder.update.UpdateStart
import org.springframework.data.cassandra.core.DeleteOptions
import org.springframework.data.cassandra.core.InsertOptions
import org.springframework.data.cassandra.core.UpdateOptions
import org.springframework.data.cassandra.core.cql.QueryOptions
import org.springframework.data.cassandra.core.cql.WriteOptions

/**
 * [QueryOptions]를 DSL 형태로 생성합니다.
 */
inline fun queryOptions(
    @BuilderInference builder: QueryOptions.QueryOptionsBuilder.() -> Unit,
): QueryOptions =
    QueryOptions.builder().apply(builder).build()

/**
 * [InsertOptions]를 DSL 형태로 생성합니다.
 */
inline fun insertOptions(
    @BuilderInference builder: InsertOptions.InsertOptionsBuilder.() -> Unit,
): InsertOptions =
    InsertOptions.builder().apply(builder).build()

/**
 * [UpdateOptions]를 DSL 형태로 생성합니다.
 */
inline fun updateOptions(
    @BuilderInference builder: UpdateOptions.UpdateOptionsBuilder.() -> Unit,
): UpdateOptions =
    UpdateOptions.builder().apply(builder).build()

/**
 * [WriteOptions]를 DSL 형태로 생성합니다.
 */
inline fun writeOptions(
    @BuilderInference builder: WriteOptions.WriteOptionsBuilder.() -> Unit,
): WriteOptions =
    WriteOptions.builder().apply(builder).build()

/**
 * [DeleteOptions]를 DSL 형태로 생성합니다.
 */
inline fun deleteOptions(
    @BuilderInference builder: DeleteOptions.DeleteOptionsBuilder.() -> Unit,
): DeleteOptions =
    DeleteOptions.builder().apply(builder).build()


/**
 * [Insert]에 [WriteOptions]를 적용합니다.
 */
fun Insert.addWriteOptions(writeOptions: WriteOptions): Insert {
    var applied = this

    if (writeOptions.isPositiveTtl) {
        applied = applied.usingTtl(writeOptions.ttl!!.seconds.toInt())
    }
    writeOptions.timestamp?.run {
        applied = applied.usingTimestamp(this)
    }
    return applied
}

/**
 * [Update]에 [WriteOptions]를 적용합니다.
 */
fun Update.addWriteOptions(writeOptions: WriteOptions): Update {
    var applied = this

    if (applied is UpdateStart) {
        if (writeOptions.isPositiveTtl) {
            applied = applied.usingTtl(writeOptions.ttl!!.seconds.toInt()) as Update
        }
        if (writeOptions.timestamp != null) {
            applied = (applied as UpdateStart).usingTimestamp(writeOptions.timestamp!!) as Update
        }
    }
    return applied
}

/**
 * [UpdateStart]에 [WriteOptions]를 적용합니다.
 */
fun UpdateStart.addWriteOptions(writeOptions: WriteOptions): UpdateStart {
    var applied: UpdateStart = this

    if (writeOptions.isPositiveTtl) {
        applied = applied.usingTtl(writeOptions.ttl!!.seconds.toInt())
    }
    if (writeOptions.timestamp != null) {
        applied = applied.usingTimestamp(writeOptions.timestamp!!)
    }
    return applied
}

/**
 * [Delete]에 [WriteOptions]를 적용합니다.
 */
fun Delete.addWriteOptions(writeOptions: WriteOptions): Delete {
    var applied = this

    if (applied is DeleteSelection && writeOptions.timestamp != null) {
        applied = applied.usingTimestamp(writeOptions.timestamp!!) as Delete
    }
    return applied
}

/**
 * TTL 이 0보다 크면 true
 */
val WriteOptions.isPositiveTtl: Boolean get() = ttl != null && !ttl!!.isNegative
