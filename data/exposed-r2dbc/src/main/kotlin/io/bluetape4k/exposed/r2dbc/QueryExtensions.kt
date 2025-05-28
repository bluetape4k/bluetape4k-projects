package io.bluetape4k.exposed.r2dbc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.r2dbc.Query

suspend fun Query.forEach(block: (ResultRow) -> Unit) {
    this.collect { row ->
        block(row)
    }
}

suspend fun Query.forEachIndexed(block: (Int, ResultRow) -> Unit) {
    this.collectIndexed { index, row ->
        block(index, row)
    }
}

suspend fun <T> Flow<T>.any(): Boolean {
    return this.firstOrNull() != null
}

suspend fun <T: Comparable<T>> Flow<T>.sorted(): List<T> = toList().sorted()

suspend fun <T> Flow<T>.distinct(): List<T> = distinctUntilChanged().toList()
