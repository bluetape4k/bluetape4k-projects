# Module bluetape4k-measured

English | [한국어](./README.ko.md)

`bluetape4k-measured` represents compound units such as `m/s` and
`kg*m/s^2` in a type-safe way, based on composable unit types (`Units`) and measured values (`Measure`).

## Core Concepts

- `Units`: a unit definition, including suffix and ratio relative to the base unit
- `Measure<T: Units>`: value + unit
- `UnitsProduct`, `UnitsRatio`, `InverseUnits`: representations of compound units

## Provided Units

- Length: `Length`
- Time: `Time`
- Mass: `Mass`
- Volume: `Volume`
- Temperature: `Temperature` / `TemperatureDelta`
- Angle: `Angle`
- Area: `Area`
- Storage capacity: `Storage`
- Digital size: `BinarySize`
- Frequency: `Frequency`
- Energy / Power: `Energy`, `Power`
- Motion-unit utilities: `MotionUnits`, `Velocity`, `Acceleration`
- Graphics length: `GraphicsLength`
- Pressure: `Pressure`

## Quick Example

```kotlin
import io.bluetape4k.measured.*
import io.bluetape4k.measured.Length.Companion.meters
import io.bluetape4k.measured.Time.Companion.seconds

val speed = 10 * meters / seconds
val duration = 5 * seconds
val distance = speed * duration

println(distance `as` meters) // 50.0 m
println(distance.toHuman())    // 50.0 m
```

## Test

```bash
./gradlew :bluetape4k-measured:test
```

## Class Diagram

```mermaid
classDiagram
    class Units {
        <<interface>>
        +suffix: String
        +ratio: Double
    }

    class Measure~T : Units~ {
        +amount: Double
        +units: T
        +as(target: T) Double
        +toHuman() String
        +times(other) UnitsProduct
        +div(other) UnitsRatio
    }

    class UnitsProduct~A_Units_B_Units~ {
        +left: Measure~A~
        +right: Measure~B~
    }

    class UnitsRatio~N_Units_D_Units~ {
        +numerator: Measure~N~
        +denominator: Measure~D~
    }

    class Length {
        +meters
        +kilometers
        +centimeters
    }

    class Time {
        +seconds
        +minutes
        +hours
    }

    class Mass {
        +kilograms
        +grams
        +pounds
    }

    class Velocity
    class Acceleration

    Units <|-- Length
    Units <|-- Time
    Units <|-- Mass
    Measure --> Units
    UnitsRatio --|> Measure : compound unit
    UnitsProduct --|> Measure : compound unit
    Velocity --> UnitsRatio : "Length / Time (m/s)"
    Acceleration --> UnitsRatio : "Velocity / Time (m/s²)"


```

## Unit Composition Flow

```mermaid
flowchart LR
    L["10 * meters<br/>(Measure&lt;Length&gt;)"]
    T["seconds<br/>(Time)"]
    V["speed<br/>(UnitsRatio&lt;Length, Time&gt;)<br/>= 10 m/s"]
    D["5 * seconds<br/>(Measure&lt;Time&gt;)"]
    R["distance<br/>(Measure&lt;Length&gt;)<br/>= 50 m"]

    L -->|"/ seconds"| V
    V -->|"* duration"| R
    D --> R
```

## Compatibility Adapter for `units`

Compatibility extension functions are available so you can migrate gradually from `bluetape4k-units` to
`bluetape4k-measured`.

```kotlin
import io.bluetape4k.measured.*

val legacyLength = io.bluetape4k.units.Length(1500.0, io.bluetape4k.units.LengthUnit.METER)
val measuredLength = legacyLength.toMeasuredLength()
val roundTrip = measuredLength.toLegacyLength()

println(measuredLength.toHuman())   // 1.5 km
println(roundTrip.inMeter())        // 1500.0
```
