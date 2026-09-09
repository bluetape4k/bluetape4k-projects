package io.bluetape4k.measured

fun baseBinarySizeRate(): Measure<UnitsRatio<BinarySize, Time>> =
    1.megabytes10() / 1.seconds()

fun baseMassAccelerationForce(): Measure<Force> =
    1.kilograms() * 1.metersPerSecondSquared()
