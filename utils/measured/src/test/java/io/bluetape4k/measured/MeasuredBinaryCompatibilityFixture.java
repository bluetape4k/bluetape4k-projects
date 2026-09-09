package io.bluetape4k.measured;

public final class MeasuredBinaryCompatibilityFixture {

    private MeasuredBinaryCompatibilityFixture() {
    }

    public static Measure<DataRate> invokeLegacyBinaryMethod(
            Measure<BinarySize> size,
            Measure<Time> duration
    ) {
        return DataRateOperationsKt.binarySizeDivTimeToDataRate(size, duration);
    }
}
