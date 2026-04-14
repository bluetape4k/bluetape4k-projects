# Module bluetape4k-exposed-dao

English | [한국어](./README.ko.md)

Provides entity extensions, String-based entities, and IdTable implementations with various client-side ID strategies for the JetBrains Exposed DAO layer.

## Overview

`bluetape4k-exposed-dao` provides:

- **DAO extension functions**: Helpers for common Entity implementations such as `idEquals`, `idHashCode`, and
  `entityToStringBuilder`
- **StringEntity**: DAO entities with a `String` primary key
- **Custom IdTables**: Various ID strategies including KSUID, ULID, Snowflake, Timebased UUID, and Soft Delete
- Built on `bluetape4k-exposed-core`, isolating features needed only in the DAO layer

## Adding Dependencies

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-dao:${version}")
}
```

## Basic Usage

### 1. Common DAO Entity implementation

```kotlin
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.entityToStringBuilder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object UserTable: LongIdTable("users") {
    val name = varchar("name", 100)
    val email = varchar("email", 200)
}

class UserEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<UserEntity>(UserTable)

    var name by UserTable.name
    var email by UserTable.email

    // ID-based equals/hashCode via idEquals/idHashCode
    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()

    // Convenient toString via entityToStringBuilder
    override fun toString(): String = entityToStringBuilder()
        .add("name", name)
        .add("email", email)
        .toString()
}
```

### 2. StringEntity (String primary key)

```kotlin
import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable

object TagTable: IdTable<String>("tags") {
    override val id = varchar("id", 64).entityId()
    val description = text("description").nullable()
    override val primaryKey = PrimaryKey(id)
}

class TagEntity(id: EntityID<String>): StringEntity(id) {
    companion object: StringEntityClass<TagEntity>(TagTable)

    var description by TagTable.description
}

// Usage: create an entity with an explicit ID
val tag = TagEntity.new("kotlin") {
    description = "Kotlin-related tag"
}
```

### 3. KSUID-based DAO entity

```kotlin
import io.bluetape4k.exposed.core.dao.id.KsuidTable
import io.bluetape4k.exposed.dao.id.KsuidEntity
import io.bluetape4k.exposed.dao.id.KsuidEntityClass
import io.bluetape4k.exposed.dao.id.KsuidEntityID

// Table with a KSUID primary key (auto-generated client-side, time-sortable)
object OrderTable: KsuidTable("orders") {
    val amount = decimal("amount", 10, 2)
    val status = varchar("status", 20)
}

class OrderEntity(id: KsuidEntityID): KsuidEntity(id) {
    companion object: KsuidEntityClass<OrderEntity>(OrderTable)

    var amount by OrderTable.amount
    var status by OrderTable.status
}

// KSUID is auto-generated on insert
val order = OrderEntity.new {
    amount = 15000.toBigDecimal()
    status = "PENDING"
}
println(order.id.value) // e.g. "2Dgh3kZ..." (27-character KSUID)
```

### 4. Snowflake ID-based DAO entity

```kotlin
import io.bluetape4k.exposed.core.dao.id.SnowflakeIdTable
import io.bluetape4k.exposed.dao.id.SnowflakeIdEntity
import io.bluetape4k.exposed.dao.id.SnowflakeIdEntityClass
import io.bluetape4k.exposed.dao.id.SnowflakeIdEntityID

// Table with a Snowflake ID (Long) primary key
object EventTable: SnowflakeIdTable("events") {
    val type = varchar("type", 50)
    val payload = text("payload")
}

class EventEntity(id: SnowflakeIdEntityID): SnowflakeIdEntity(id) {
    companion object: SnowflakeIdEntityClass<EventEntity>(EventTable)

    var type by EventTable.type
    var payload by EventTable.payload
}
```

### 5. ULID-based DAO entity

```kotlin
import io.bluetape4k.exposed.core.dao.id.UlidTable
import io.bluetape4k.exposed.dao.id.UlidEntity
import io.bluetape4k.exposed.dao.id.UlidEntityClass
import io.bluetape4k.exposed.dao.id.UlidEntityID

object SessionTable: UlidTable("sessions") {
    val userId = long("user_id")
    val status = varchar("status", 20)
}

class SessionEntity(id: UlidEntityID): UlidEntity(id) {
    companion object: UlidEntityClass<SessionEntity>(SessionTable)

    var userId by SessionTable.userId
    var status by SessionTable.status
}
```

### 6. Timebased UUID-based DAO entity

```kotlin
import io.bluetape4k.exposed.core.dao.id.TimebasedUUIDTable
import io.bluetape4k.exposed.core.dao.id.TimebasedUUIDBase62Table
import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntity
import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntityClass
import io.bluetape4k.exposed.dao.id.TimebasedUUIDBase62Entity
import io.bluetape4k.exposed.dao.id.TimebasedUUIDBase62EntityClass

// UUID v7 (time-based) primary key
object SessionTable: TimebasedUUIDTable("sessions") {
    val userId = long("user_id")
    val expiresAt = long("expires_at")
}

class SessionEntity(id: TimebasedUUIDEntityID): TimebasedUUIDEntity(id) {
    companion object: TimebasedUUIDEntityClass<SessionEntity>(SessionTable)

    var userId by SessionTable.userId
}

// Base62-encoded UUID primary key (URL-safe)
object TokenTable: TimebasedUUIDBase62Table("tokens") {
    val userId = long("user_id")
    val scope = varchar("scope", 100)
}

class TokenEntity(id: TimebasedUUIDBase62EntityID): TimebasedUUIDBase62Entity(id) {
    companion object: TimebasedUUIDBase62EntityClass<TokenEntity>(TokenTable)

    var userId by TokenTable.userId
}
```

### 7. Soft Delete IdTable

```kotlin
import io.bluetape4k.exposed.core.dao.id.SoftDeletedIdTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID

// Table with an automatically-added isDeleted column
object PostTable: SoftDeletedIdTable<Long>("posts") {
    override val id: Column<EntityID<Long>> = long("id").autoIncrement().entityId()
    val title = varchar("title", 255)
    val content = text("content")
    override val primaryKey = PrimaryKey(id)
}

// Soft delete
transaction {
    PostTable.update({ PostTable.id eq postId }) {
        it[isDeleted] = true
    }
}

// Query only active records
transaction {
    PostTable.selectAll()
        .where { PostTable.isDeleted eq false }
        .map { it[PostTable.title] }
}
```

## Diagrams

### Core AuditableEntity Structure

Illustrates the relationships among `AuditableLongEntity`, `AuditableLongEntityClass`, and the custom IdTable hierarchy.

```mermaid
classDiagram
    direction TB
    class AuditableLongEntity {
        <<abstractEntity>>
        +createdBy: String
        +createdAt: Instant
        +updatedBy: String
        +updatedAt: Instant
        +flush()
    }
    class AuditableLongEntityClass~E~ {
        <<abstractEntityClass>>
        +new(init): E
    }
    class KsuidTable {
        <<IdTable_String>>
        +id: Column~String~
    }
    class SnowflakeIdTable {
        <<IdTable_Long>>
        +id: Column~Long~
    }
    AuditableLongEntityClass --> AuditableLongEntity : manages

    style AuditableLongEntity fill:#1976D2,stroke:#1565C0,color:#FFFFFF
    style AuditableLongEntityClass fill:#1976D2,stroke:#1565C0,color:#FFFFFF
    style KsuidTable fill:#F57F17,stroke:#E65100,color:#FFFFFF
    style SnowflakeIdTable fill:#F57F17,stroke:#E65100,color:#FFFFFF
```

### Custom IdTable Hierarchy

The full hierarchy of IdTable implementations used together with DAO entities.

```mermaid
classDiagram
    class IdTable {
<<ExposedCore>>
+id: Column~EntityID~
+primaryKey: PrimaryKey
}
class SoftDeletedIdTable {
<<abstract_exposed_core>>
+isDeleted: Column~Boolean~
}
class KsuidTable {
+id: Column~EntityID~String~~
}
class KsuidMillisTable {
+id: Column~EntityID~String~~
}
class UlidTable {
+id: Column~EntityID~String~~
}
class SnowflakeIdTable {
+id: Column~EntityID~Long~~
}
class TimebasedUUIDTable {
+id: Column~EntityID~UUID~~
}
class TimebasedUUIDBase62Table {
+id: Column~EntityID~String~~
}

IdTable <|-- SoftDeletedIdTable
IdTable <|-- KsuidTable
IdTable <|-- KsuidMillisTable
IdTable <|-- UlidTable
IdTable <|-- SnowflakeIdTable
IdTable <|-- TimebasedUUIDTable
IdTable <|-- TimebasedUUIDBase62Table

    style IdTable fill:#37474F,stroke:#263238,color:#FFFFFF
    style SoftDeletedIdTable fill:#1976D2,stroke:#1565C0,color:#FFFFFF
    style KsuidTable fill:#F57F17,stroke:#E65100,color:#FFFFFF
    style KsuidMillisTable fill:#F57F17,stroke:#E65100,color:#FFFFFF
    style UlidTable fill:#F57F17,stroke:#E65100,color:#FFFFFF
    style SnowflakeIdTable fill:#F57F17,stroke:#E65100,color:#FFFFFF
    style TimebasedUUIDTable fill:#F57F17,stroke:#E65100,color:#FFFFFF
    style TimebasedUUIDBase62Table fill:#F57F17,stroke:#E65100,color:#FFFFFF
```

### Entity Extension Hierarchy

DAO Entity and EntityClass hierarchies corresponding to each IdTable.

```mermaid
classDiagram
    class Entity {
<<ExposedCore>>
+id: EntityID~ID~
}
class StringEntity {
<<abstract>>
}
class KsuidEntity
class KsuidMillisEntity
class UlidEntity
class SnowflakeIdEntity
class TimebasedUUIDEntity
class TimebasedUUIDBase62Entity

class EntityClass {
<<ExposedCore>>
+new(init) E
+findById(id) E?
}
class StringEntityClass {
<<abstract>>
}
class KsuidEntityClass
class SnowflakeIdEntityClass
class TimebasedUUIDEntityClass

Entity <|-- StringEntity
StringEntity <|-- KsuidEntity
StringEntity <|-- KsuidMillisEntity
StringEntity <|-- UlidEntity
StringEntity <|-- TimebasedUUIDBase62Entity
Entity <|-- SnowflakeIdEntity
Entity <|-- TimebasedUUIDEntity

EntityClass <|-- StringEntityClass
StringEntityClass <|-- KsuidEntityClass
StringEntityClass <|-- TimebasedUUIDBase62EntityClass
EntityClass <|-- SnowflakeIdEntityClass
EntityClass <|-- TimebasedUUIDEntityClass

KsuidEntityClass --> KsuidEntity: manages
SnowflakeIdEntityClass --> SnowflakeIdEntity: manages
TimebasedUUIDEntityClass --> TimebasedUUIDEntity: manages

    style Entity fill:#37474F,stroke:#263238,color:#FFFFFF
    style EntityClass fill:#37474F,stroke:#263238,color:#FFFFFF
    style StringEntity fill:#1976D2,stroke:#1565C0,color:#FFFFFF
    style StringEntityClass fill:#1976D2,stroke:#1565C0,color:#FFFFFF
    style KsuidEntity fill:#00897B,stroke:#00695C,color:#FFFFFF
    style KsuidMillisEntity fill:#00897B,stroke:#00695C,color:#FFFFFF
    style UlidEntity fill:#00897B,stroke:#00695C,color:#FFFFFF
    style SnowflakeIdEntity fill:#00897B,stroke:#00695C,color:#FFFFFF
    style TimebasedUUIDEntity fill:#00897B,stroke:#00695C,color:#FFFFFF
    style TimebasedUUIDBase62Entity fill:#00897B,stroke:#00695C,color:#FFFFFF
    style KsuidEntityClass fill:#00897B,stroke:#00695C,color:#FFFFFF
    style SnowflakeIdEntityClass fill:#00897B,stroke:#00695C,color:#FFFFFF
    style TimebasedUUIDEntityClass fill:#00897B,stroke:#00695C,color:#FFFFFF
```

## Key Files and Classes

| File                                 | Description                                                       |
|--------------------------------------|-------------------------------------------------------------------|
| `EntityExtensions.kt`                | Entity helpers: `idEquals`, `idHashCode`, `entityToStringBuilder` |
| `StringEntity.kt`                    | Entity/EntityClass with a String primary key                      |
| `dao/id/KsuidTable.kt`               | KSUID PK IdTable                                                  |
| `dao/id/KsuidMillisTable.kt`         | KSUID Millis PK IdTable                                           |
| `dao/id/UlidTable.kt`                | ULID PK IdTable                                                   |
| `dao/id/SnowflakeIdTable.kt`         | Snowflake Long PK IdTable                                         |
| `dao/id/TimebasedUUIDTable.kt`       | Timebased UUID PK IdTable                                         |
| `dao/id/TimebasedUUIDBase62Table.kt` | Timebased UUID Base62-encoded PK IdTable                          |
| `dao/id/SoftDeletedIdTable.kt`       | Soft Delete IdTable with an `isDeleted` column                    |

## ID Strategy Comparison

| IdTable                    | PK type  | Length   | Characteristics                           |
|----------------------------|----------|----------|-------------------------------------------|
| `KsuidTable`               | `String` | 27 chars | Time-sortable, URL-safe                   |
| `KsuidMillisTable`         | `String` | 27 chars | Millisecond-precision KSUID               |
| `UlidTable`                | `String` | 26 chars | StatefulMonotonic ULID                    |
| `SnowflakeIdTable`         | `Long`   | —        | Distributed environments, high throughput |
| `TimebasedUUIDTable`       | `UUID`   | 36 chars | Time-sortable UUID v7                     |
| `TimebasedUUIDBase62Table` | `String` | up to 24 | UUID v7 encoded as Base62                 |
| `SoftDeletedIdTable`       | Generic  | —        | Includes an `isDeleted` column            |

## AuditableEntity (Audit Tracking for DAO)

`AuditableEntity` and `AuditableEntityClass` add automatic audit tracking to DAO entities.

### How AuditableEntity Works

Overrides `flush()` to automatically set `createdBy` and `updatedBy`.

#### Automatic field assignment

| Situation              | Auto-set field | Notes                                                                      |
|------------------------|----------------|----------------------------------------------------------------------------|
| New entity INSERT      | `createdBy`    | `createdAt` is set by the table's DB `defaultExpression(CurrentTimestamp)` |
| Existing entity UPDATE | `updatedBy`    | `updatedAt` is set when `auditedUpdateById()` is called on the Repository  |

```mermaid
sequenceDiagram
    box rgb(227, 242, 253) Application
        participant Caller
        participant UserContext
    end
    box rgb(232, 245, 233) Domain
        participant Article
        participant AuditableEntity
    end
    box rgb(255, 243, 224) Database
        participant DB
    end

    Caller->>UserContext: withUser("alice") { ... }
    Caller->>Article: new / flush
    Article->>AuditableEntity: flush()
    AuditableEntity->>UserContext: getCurrentUser()
    AuditableEntity->>DB: INSERT / UPDATE
    DB-->>Article: Save createdBy or updatedBy
```

#### Important notes

- Calling `flush()` alone does not automatically set `updatedAt`.
- Automatic `updatedAt` assignment is only guaranteed when using `AuditableJdbcRepository.auditedUpdateById()`.

### Table definition (exposed-core)

```kotlin
import io.bluetape4k.exposed.core.auditable.AuditableLongIdTable

object ArticleTable : AuditableLongIdTable("articles") {
    val title = varchar("title", 255)
    val content = text("content")
    // createdBy, createdAt, updatedBy, updatedAt are added automatically
}
```

### Entity definition

```kotlin
import io.bluetape4k.exposed.dao.auditable.AuditableLongEntity
import io.bluetape4k.exposed.dao.auditable.AuditableLongEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.time.Instant

class Article(id: EntityID<Long>) : AuditableLongEntity(id) {
    companion object : AuditableLongEntityClass<Article>(ArticleTable)

    var title by ArticleTable.title
    var content by ArticleTable.content

    override var createdBy by ArticleTable.createdBy
    override var createdAt by ArticleTable.createdAt
    override var updatedBy by ArticleTable.updatedBy
    override var updatedAt by ArticleTable.updatedAt
}
```

### Using the entity

```kotlin
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import io.bluetape4k.exposed.core.auditable.UserContext

transaction {
    UserContext.withUser("alice@example.com") {
        // INSERT: createdBy is automatically set to "alice@example.com" when flush() is called
        val article = Article.new {
            title = "Exposed DAO Auditing"
            content = "Auto tracking of changes"
        }
        println("Creator: ${article.createdBy}")  // "alice@example.com"
    }

    UserContext.withUser("bob@example.com") {
        // UPDATE: updatedBy is automatically set to "bob@example.com" when flush() is called
        article.title = "Updated Title"
        article.flush()
        println("Modifier: ${article.updatedBy}")  // "bob@example.com"
    }
}
```

`UserContext.withUser(...)` restores the outer user context after a nested call exits, so the audit scope remains consistent.

### Concrete entity and EntityClass types

| Primary key | Entity                | EntityClass                |
|-------------|-----------------------|----------------------------|
| `Int`       | `AuditableIntEntity`  | `AuditableIntEntityClass`  |
| `Long`      | `AuditableLongEntity` | `AuditableLongEntityClass` |
| `UUID`      | `AuditableUUIDEntity` | `AuditableUUIDEntityClass` |

## Testing

```bash
./gradlew :bluetape4k-exposed-dao:test
```

## References

- [JetBrains Exposed DAO](https://github.com/JetBrains/Exposed/wiki/DAO)
- [bluetape4k-exposed-core](../exposed-core)
- [bluetape4k-exposed-jdbc (AuditableJdbcRepository)](../exposed-jdbc)
- [bluetape4k-idgenerators](../../../utils/idgenerators)
