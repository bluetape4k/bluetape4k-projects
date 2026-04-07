# Module bluetape4k-exposed-measured

English | [한국어](./README.ko.md)

A custom ColumnType module for storing and retrieving `bluetape4k-measured` types (`Measure<T>`, `Temperature`,
`TemperatureDelta`) as `DOUBLE` columns in Exposed.

## Supported Columns

- `measure(name, baseUnit)`
- `length(name)`, `mass(name)`, `area(name)`, `volume(name)`
- `angle(name)`, `pressure(name)`, `storage(name)`, `frequency(name)`
- `energy(name)`, `power(name)`
- `temperature(name)`, `temperatureDelta(name)`

## Example

```kotlin
object ProductTable: Table("products") {
    val width = length("width")
    val weight = mass("weight")
    val storage = storage("storage")
    val temp = temperature("temp")
}
```

## Class Diagram

```mermaid
classDiagram
    class ColumnType~T~ {
        <<Exposed>>
        +sqlType(): String
        +valueFromDB(value: Any): T?
        +notNullValueToDB(value: T): Any
    }

    class MeasureColumnType~T~ {
        -baseUnit: T
        -fromBaseValue: (Double) -> Measure~T~
        +sqlType(): String
        +valueFromDB(value: Any): Measure~T~?
        +notNullValueToDB(value: Measure~T~): Any
    }
    class TemperatureColumnType {
        +sqlType(): String
        +valueFromDB(value: Any): Temperature?
        +notNullValueToDB(value: Temperature): Any
    }
    class TemperatureDeltaColumnType {
        +sqlType(): String
        +valueFromDB(value: Any): TemperatureDelta?
        +notNullValueToDB(value: TemperatureDelta): Any
    }

    class Measure~T~ {
        <<bluetape4k_measured>>
        +value: Double
        +unit: T
        +in(unit: T): Double
    }
    class Units {
        <<bluetape4k_measured>>
    }
    class Temperature {
        <<bluetape4k_measured>>
        +inKelvin(): Double
        +fromKelvin(k: Double): Temperature
    }

    ColumnType <|-- MeasureColumnType
    ColumnType <|-- TemperatureColumnType
    ColumnType <|-- TemperatureDeltaColumnType
    MeasureColumnType ..> Measure : stores as DOUBLE
    MeasureColumnType ..> Units : baseUnit
    TemperatureColumnType ..> Temperature : stores as Kelvin DOUBLE
```

## Query Execution Flow

```mermaid
flowchart LR
    A[SQL query executed] --> B{MeasuredTransaction}
    B --> C[Start Micrometer Timer]
    C --> D[Actual DB query]
    D --> E[Stop Timer + record tags]
    E --> F[Return result]

    style A fill:#607D8B
    style B fill:#9C27B0
    style C fill:#F44336
    style D fill:#607D8B
    style E fill:#F44336
    style F fill:#4CAF50
```

## Storage / Retrieval Sequence Diagram

```mermaid
sequenceDiagram
    participant App as Application
    participant Col as MeasureColumnType~Length~
    participant DB as Database

    Note over App,DB: Store — converts to base unit (meters) and saves as DOUBLE
    App->>Col: insert { it[width] = 1500.millimeters() }
    Col->>Col: notNullValueToDB(value in Length.meters)
    Note over Col: Measure(1500mm) → 1.5 (in meters)
    Col->>DB: INSERT ... VALUES (1.5)

    Note over App,DB: Retrieve — restores DOUBLE back to Measure type
    App->>DB: SELECT width FROM products WHERE id = 1
    DB-->>Col: 1.5 (Double)
    Col->>Col: fromBaseValue(1.5) → Measure(1.5, meters)
    Col-->>App: Measure(1.5, Length.meters)
    Note over App: 1.5.meters().inMillimeters() == 1500.0
```
