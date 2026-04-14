# Module bluetape4k-exposed-jasypt

English | [한국어](./README.ko.md)

A module for encrypting and decrypting Exposed column values using Jasypt.

## Overview

`bluetape4k-exposed-jasypt` provides transparent encryption of JetBrains Exposed column values using the [Jasypt](http://www.jasypt.org/) library. It uses deterministic encryption, so the same plaintext always produces the same ciphertext.

### Key Features

- **Deterministic encrypted column types**: Same input always produces the same ciphertext
- **String and binary encryption**: Supports `VARCHAR` and `BINARY` columns
- **Searchable / indexable**: Encrypted columns can be used in WHERE clauses and have indexes

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jasypt:${version}")
    implementation("io.github.bluetape4k:bluetape4k-crypto:${version}")
}
```

## Basic Usage

### 1. Defining Encrypted Columns

```kotlin
import io.bluetape4k.exposed.core.jasypt.jasyptVarChar
import io.bluetape4k.exposed.core.jasypt.jasyptBinary
import io.bluetape4k.crypto.encrypt.Encryptors
import org.jetbrains.exposed.v1.core.dao.id.IdTable

object Users: IdTable<Long>("users") {
    val name = varchar("name", 100)

    // Jasypt-encrypted VARCHAR column
    val ssn = jasyptVarChar(
        name = "ssn",
        colLength = 512,
        encryptor = Encryptors.Jasypt
    )

    // Jasypt-encrypted BINARY column
    val privateKey = jasyptBinary(
        name = "private_key",
        colLength = 1024,
        encryptor = Encryptors.Jasypt
    )
}
```

### 2. Using Encrypted Columns

```kotlin
// Automatically encrypted on insert
Users.insert {
    it[name] = "John Doe"
    it[ssn] = "123-45-6789"  // encrypted automatically before storage
}

// Automatically decrypted on read
val user = Users.selectAll().where { Users.id eq 1L }.single()
val ssn = user[Users.ssn]  // decrypted automatically

// Searchable (because deterministic encryption is used)
val userBySsn = Users.selectAll()
    .where { Users.ssn eq "123-45-6789" }
    .single()
```

## Deterministic Encryption Trade-offs

| Advantage                   | Disadvantage                                                 |
|-----------------------------|--------------------------------------------------------------|
| Searchable via WHERE clause | Same plaintext → same ciphertext (pattern analysis possible) |
| Supports indexes            | May not meet high-security requirements                      |
| Supports sorting            |                                                              |

## Architecture Diagram

### Column Type Structure (Summary)

```mermaid
classDiagram
    direction LR
    class JasyptEncryptedColumnType~T~ {
        <<ColumnType>>
        -encryptor: StringEncryptor
        +valueFromDB(value): T
        +valueToDB(value): String
    }
    class TableExtensions {
        <<extensionFunctions>>
        +Table.encryptedVarchar(name): Column~String~
        +Table.encryptedText(name): Column~String~
    }
    TableExtensions --> JasyptEncryptedColumnType : creates

    style JasyptEncryptedColumnType fill:#00897B,stroke:#00695C,color:#FFFFFF
    style TableExtensions fill:#E65100,stroke:#BF360C,color:#FFFFFF
```

## Class Diagram

```mermaid
classDiagram
    class ColumnWithTransform~Exposed_Entity~ {
        <<Exposed>>
    }
    class ColumnTransformer~Exposed_Entity~ {
        <<interface>>
        +unwrap(value: Entity): Exposed
        +wrap(value: Exposed): Entity
    }

    class JasyptVarCharColumnType {
        -encryptor: Encryptor
        +delegate: VarCharColumnType
    }
    class JasyptBinaryColumnType {
        -encryptor: Encryptor
        +delegate: BinaryColumnType
    }
    class JasyptBlobColumnType {
        -encryptor: Encryptor
        +delegate: BlobColumnType
    }

    class StringJasyptEncryptionTransformer {
        +unwrap(value: String): String
        +wrap(value: String): String
    }
    class ByteArrayJasyptEncryptionTransformer {
        +unwrap(value: ByteArray): ByteArray
        +wrap(value: ByteArray): ByteArray
    }
    class JasyptBlobTransformer {
        +unwrap(value: ByteArray): ExposedBlob
        +wrap(value: ExposedBlob): ByteArray
    }

    class Encryptor {
        <<bluetape4k_crypto>>
        +encrypt(value: String): String
        +decrypt(value: String): String
        +encrypt(value: ByteArray): ByteArray
        +decrypt(value: ByteArray): ByteArray
    }

    ColumnWithTransform <|-- JasyptVarCharColumnType
    ColumnWithTransform <|-- JasyptBinaryColumnType
    ColumnWithTransform <|-- JasyptBlobColumnType

    ColumnTransformer <|.. StringJasyptEncryptionTransformer
    ColumnTransformer <|.. ByteArrayJasyptEncryptionTransformer
    ColumnTransformer <|.. JasyptBlobTransformer

    JasyptVarCharColumnType --> StringJasyptEncryptionTransformer
    JasyptBinaryColumnType --> ByteArrayJasyptEncryptionTransformer
    JasyptBlobColumnType --> JasyptBlobTransformer

    StringJasyptEncryptionTransformer --> Encryptor
    ByteArrayJasyptEncryptionTransformer --> Encryptor
    JasyptBlobTransformer --> Encryptor

    style ColumnWithTransform fill:#37474F,stroke:#263238,color:#FFFFFF
    style ColumnTransformer fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style JasyptVarCharColumnType fill:#00897B,stroke:#00695C,color:#FFFFFF
    style JasyptBinaryColumnType fill:#00897B,stroke:#00695C,color:#FFFFFF
    style JasyptBlobColumnType fill:#00897B,stroke:#00695C,color:#FFFFFF
    style StringJasyptEncryptionTransformer fill:#AD1457,stroke:#880E4F,color:#FFFFFF
    style ByteArrayJasyptEncryptionTransformer fill:#AD1457,stroke:#880E4F,color:#FFFFFF
    style JasyptBlobTransformer fill:#AD1457,stroke:#880E4F,color:#FFFFFF
    style Encryptor fill:#E65100,stroke:#BF360C,color:#FFFFFF
```

## Encryption / Decryption Sequence Diagrams

### Automatic Encryption on DB Insert

```mermaid
sequenceDiagram
    box rgb(227, 242, 253) Application
        participant App as Application
    end
    box rgb(232, 245, 233) Column / Encryption
        participant Col as JasyptVarCharColumnType
        participant Tx as StringJasyptEncryptionTransformer
        participant Enc as Encryptor (Jasypt)
    end
    box rgb(255, 243, 224) Database
        participant DB as Database
    end

    App->>Col: insert { it[ssn] = "123-45-6789" }
    Col->>Tx: unwrap("123-45-6789")
    Tx->>Enc: encrypt("123-45-6789")
    Note over Enc: Deterministic encryption<br/>same input → same ciphertext
    Enc-->>Tx: "ENC(xyz...)" (ciphertext)
    Tx-->>Col: "ENC(xyz...)"
    Col->>DB: INSERT ... VALUES ('ENC(xyz...)')
```

### DB Read and Conditional Search

```mermaid
sequenceDiagram
    box rgb(227, 242, 253) Application
        participant App as Application
    end
    box rgb(232, 245, 233) Column / Encryption
        participant Col as JasyptVarCharColumnType
        participant Tx as StringJasyptEncryptionTransformer
        participant Enc as Encryptor (Jasypt)
    end
    box rgb(255, 243, 224) Database
        participant DB as Database
    end

    Note over App,DB: Conditional search (possible because of deterministic encryption)
    App->>Col: where { ssn eq "123-45-6789" }
    Col->>Tx: unwrap("123-45-6789")
    Tx->>Enc: encrypt("123-45-6789")
    Enc-->>Col: "ENC(xyz...)" (always the same)
    Col->>DB: WHERE ssn = 'ENC(xyz...)'

    Note over App,DB: Decrypting query results
    DB-->>Col: "ENC(xyz...)"
    Col->>Tx: wrap("ENC(xyz...)")
    Tx->>Enc: decrypt("ENC(xyz...)")
    Enc-->>Tx: "123-45-6789"
    Tx-->>Col: "123-45-6789"
    Col-->>App: row[Users.ssn] == "123-45-6789"
```

## Key Files / Classes

| File                         | Description                   |
|------------------------------|-------------------------------|
| `JasyptVarCharColumnType.kt` | Encrypted VARCHAR column type |
| `JasyptBinaryColumnType.kt`  | Encrypted BINARY column type  |
| `Tables.kt`                  | Table extension functions     |

## Notes

1. **Security considerations
   **: Deterministic encryption is advantageous for indexing and searching, but since the same plaintext always maps to the same ciphertext, it may not be appropriate for high-security requirements.

2. **Column length**: Encrypted values are longer than the original plaintext, so allocate sufficient column length.

3. **Key management**: Encryption keys must be managed securely.

## Testing

```bash
./gradlew :bluetape4k-exposed-jasypt:test
```

## References

- [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- [Jasypt](http://www.jasypt.org/)
- bluetape4k-crypto
