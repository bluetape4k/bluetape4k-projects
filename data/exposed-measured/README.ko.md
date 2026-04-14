# Module bluetape4k-exposed-measured

[English](./README.md) | 한국어

Exposed에서 `bluetape4k-measured` 타입(`Measure<T>`, `Temperature`, `TemperatureDelta`)을
`DOUBLE` 컬럼으로 저장/조회하기 위한 Custom ColumnType 모듈입니다.

## 지원 컬럼

- `measure(name, baseUnit)`
- `length(name)`, `mass(name)`, `area(name)`, `volume(name)`
- `angle(name)`, `pressure(name)`, `storage(name)`, `frequency(name)`
- `energy(name)`, `power(name)`
- `temperature(name)`, `temperatureDelta(name)`

## 예제

```kotlin
object ProductTable: Table("products") {
    val width = length("width")
    val weight = mass("weight")
    val storage = storage("storage")
    val temp = temperature("temp")
}
```

## 클래스 다이어그램

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

    style ColumnType fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style MeasureColumnType fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style TemperatureColumnType fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style TemperatureDeltaColumnType fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style Measure fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style Units fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style Temperature fill:#FFFDE7,stroke:#FFF176,color:#F57F17
```

## 쿼리 실행 흐름

```mermaid
flowchart LR
    A[SQL 쿼리 실행] --> B{MeasuredTransaction}
    B --> C[Micrometer Timer 시작]
    C --> D[실제 DB 쿼리]
    D --> E[Timer 종료 + 태그 기록]
    E --> F[결과 반환]

    classDef queryStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef txStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef timerStyle fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    classDef resultStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32

    class A queryStyle
    class D queryStyle
    class B txStyle
    class C timerStyle
    class E timerStyle
    class F resultStyle
```

## 저장/조회 시퀀스 다이어그램

```mermaid
sequenceDiagram
        participant App as 애플리케이션
        participant Col as MeasureColumnType~Length~
        participant DB as Database

    Note over App,DB: 저장 — 기준 단위(meter)로 변환하여 DOUBLE 저장
    App->>Col: insert { it[width] = 1500.millimeters() }
    Col->>Col: notNullValueToDB(value in Length.meters)
    Note over Col: Measure(1500mm) → 1.5 (meter 기준)
    Col->>DB: INSERT ... VALUES (1.5)

    Note over App,DB: 조회 — DOUBLE을 Measure 타입으로 복원
    App->>DB: SELECT width FROM products WHERE id = 1
    DB-->>Col: 1.5 (Double)
    Col->>Col: fromBaseValue(1.5) → Measure(1.5, meters)
    Col-->>App: Measure(1.5, Length.meters)
    Note over App: 1.5.meters().inMillimeters() == 1500.0
```
