package io.bluetape4k.spring.modulith.events.exposed.schema

import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestampLiteral
import org.jetbrains.exposed.sql.notExists
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

fun ExposedEventPublicationTable.queryByListenerIdAndEvent(
    listenerId: String,
    serializedEvent: String,
): Query {
    val table = this
    return table.selectAll()
        .andWhere { table.listenerId eq listenerId }
        .andWhere { table.serializedEvent eq serializedEvent }
        .andWhere { table.completionDate.isNull() }
        .orderBy(table.publicationDate, SortOrder.ASC)
}

fun ExposedEventPublicationTable.queryByCompleteIsNotNull(): Query {
    val table = this
    return table.selectAll()
        .andWhere { table.completionDate.isNotNull() }
        .orderBy(table.publicationDate, SortOrder.ASC)
}

fun ExposedEventPublicationTable.queryByIncomplete(): Query {
    val table = this
    return table.selectAll()
        .andWhere { table.completionDate.isNull() }
        .orderBy(table.publicationDate, SortOrder.ASC)
}

fun ExposedEventPublicationTable.queryIncompleteBefore(publicationDate: Instant): Query {
    val self = this
    return selectAll()
        .andWhere { self.completionDate.isNull() }
        .andWhere { self.publicationDate less publicationDate }
        .orderBy(self.publicationDate, SortOrder.ASC)
}

fun ExposedEventPublicationTable.markCompletedByListenerIdAndEvent(
    listenerId: String,
    serializedEvent: String,
    completionDate: Instant,
): Int {
    val self = this
    val where = (self.listenerId eq listenerId) and
            (self.serializedEvent eq serializedEvent) and
            (self.completionDate.isNull())

    return update(where = { where }) {
        it[self.completionDate] = completionDate
    }
}

fun ExposedEventPublicationTable.markCompletedById(id: UUID, completionDate: Instant): Int {
    val table = this
    return update({ table.id eq id }) {
        it[table.completionDate] = completionDate
    }
}

fun ExposedEventPublicationTable.deleteById(id: UUID): Int {
    val table = this
    return table.deleteWhere { table.id eq id }
}

fun ExposedEventPublicationTable.deleteByEventAndListenerId(
    listenerId: String,
    serializedEvent: String,
): Int {
    val table = this
    return table.deleteWhere {
        (table.listenerId eq listenerId) and (table.serializedEvent eq serializedEvent)
    }
}

fun ExposedEventPublicationTable.deleteByIds(identifiers: List<UUID>): Int {
    val table = this
    return table.deleteWhere { table.id inList identifiers }
}

fun ExposedEventPublicationTable.deleteCompleted(): Int {
    val table = this
    return table.deleteWhere {
        table.completionDate.isNotNull()
    }
}

fun ExposedEventPublicationTable.deleteCompletedBefore(completionDate: Instant): Int {
    val table = this
    return table.deleteWhere {
        table.publicationDate less publicationDate
    }
}

fun ExposedEventPublicationTable.copyToArchiveById(
    archiveTable: ExposedEventPublicationTable,
    id: UUID,
    completionDate: Instant,
): Int? {
    val sourceTable = this

    val query = sourceTable
        .select(
            sourceTable.id,
            sourceTable.listenerId,
            sourceTable.eventType,
            sourceTable.serializedEvent,
            sourceTable.publicationDate,
            timestampLiteral(completionDate)
        )
        .andWhere { sourceTable.id eq id }
        .andWhere { notExists(archiveTable.selectAll().where { archiveTable.id eq sourceTable.id }) }

    return archiveTable.insert(selectQuery = query)
}

fun ExposedEventPublicationTable.copyToArchiveByEventAndListenerId(
    archiveTable: ExposedEventPublicationTable,
    listenerId: String,
    serializedEvent: String,
    completionDate: Instant,
): Int? {
    val sourceTable = this

    val query = sourceTable
        .select(
            sourceTable.id,
            sourceTable.listenerId,
            sourceTable.eventType,
            sourceTable.serializedEvent,
            sourceTable.publicationDate,
            timestampLiteral(completionDate)
        )
        .andWhere { sourceTable.listenerId eq listenerId }
        .andWhere { sourceTable.serializedEvent eq serializedEvent }
        .andWhere { notExists(archiveTable.selectAll().where { archiveTable.id eq sourceTable.id }) }

    return archiveTable.insert(selectQuery = query)
}
