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

inline fun queryOptions(@BuilderInference builder: QueryOptions.QueryOptionsBuilder.() -> Unit): QueryOptions =
    QueryOptions.builder().apply(builder).build()

inline fun insertOptions(@BuilderInference builder: InsertOptions.InsertOptionsBuilder.() -> Unit): InsertOptions =
    InsertOptions.builder().apply(builder).build()

inline fun updateOptions(@BuilderInference builder: UpdateOptions.UpdateOptionsBuilder.() -> Unit): UpdateOptions =
    UpdateOptions.builder().apply(builder).build()

inline fun writeOptions(@BuilderInference builder: WriteOptions.WriteOptionsBuilder.() -> Unit): WriteOptions =
    WriteOptions.builder().apply(builder).build()

inline fun deleteOptions(@BuilderInference builder: DeleteOptions.DeleteOptionsBuilder.() -> Unit): DeleteOptions =
    DeleteOptions.builder().apply(builder).build()


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

fun Delete.addWriteOptions(writeOptions: WriteOptions): Delete {
    var applied = this

    if (applied is DeleteSelection && writeOptions.timestamp != null) {
        applied = applied.usingTimestamp(writeOptions.timestamp!!) as Delete
    }
    return applied
}

val WriteOptions.isPositiveTtl: Boolean get() = ttl != null && !ttl!!.isNegative
