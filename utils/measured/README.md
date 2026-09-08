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
- Electrical quantities: `Current`, `Charge`, `Voltage`, `Resistance`
- Data transfer rate: `DataRate`, `DataRateFormat`
- Mechanics: `Force`, `Torque`
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

Electrical quantities and mechanical measurements retain their semantic types:

```kotlin
val voltage = 2.kiloWatts() / 500.milliAmps()
val force = 1.kilograms() * 9.8.metersPerSecondSquared()
val torque = force.torqueAt(2.meters())
val rate = 10.megabytesPerSecond()

println(voltage.toHuman()) // 4.0 kV
println(torque.toHuman())  // 19.6 N·m
println(rate.toHuman())    // 10.0 MB/s
println(rate.toHuman(DataRateFormat.DECIMAL_BITS)) // 80.0 Mbit/s
```

`DataRate.toHuman()` uses decimal bytes by default. Use
`DataRateFormat.DECIMAL_BITS` or `DataRateFormat.BINARY_BYTES` when the display
policy must be explicit. A force becomes torque only through `torqueAt`, which
requires a perpendicular moment arm; a generic force/length product remains a
composite unit.

## Test

```bash
./gradlew :bluetape4k-measured:test
```

## Class Diagram

![measured Class Structure diagram](../../docs/images/readme-diagrams/utils-measured-diagram-01.png)

## Unit Composition Flow

![Unit Composition Flow diagram](../../docs/images/readme-diagrams/utils-measured-diagram-02.png)

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
