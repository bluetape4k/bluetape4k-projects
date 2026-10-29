package io.bluetape4k.assertions;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AssertionJavaCompatibilityTest {

    @Test
    void legacyDateTimeAndDurationOwnersRemainCallableFromJava() {
        Instant now = Instant.now();
        DateTimesInstantZonedKt.shouldBeAfter(now, now.minusSeconds(1));
        DurationAssertionsKt.shouldBePositive(Duration.ofSeconds(1));
    }
}
