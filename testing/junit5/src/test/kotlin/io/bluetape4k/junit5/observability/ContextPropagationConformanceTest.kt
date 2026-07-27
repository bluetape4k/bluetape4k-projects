package io.bluetape4k.junit5.observability

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContextPropagationConformanceTest {

    @Test
    fun `matching propagation snapshots satisfy the conformance contract`() {
        assertContextPropagationConformance(propagationObservation(), propagationExpectation())
    }

    @Test
    fun `all terminal distinctions satisfy their matching scenarios`() {
        val cases = listOf(
            ContextPropagationScenario.SUCCESS to ContextPropagationTerminal.SUCCESS,
            ContextPropagationScenario.FAILURE to ContextPropagationTerminal.FAILURE,
            ContextPropagationScenario.CANCELLATION to ContextPropagationTerminal.CANCELLATION,
            ContextPropagationScenario.DEADLINE to ContextPropagationTerminal.DEADLINE_EXCEEDED,
        )

        cases.forEach { (scenario, terminal) ->
            assertContextPropagationConformance(
                propagationObservation().copy(scenario = scenario, terminal = terminal),
                propagationExpectation().copy(scenario = scenario, expectedTerminal = terminal),
            )
        }
    }

    @Test
    fun `propagation boundary scenario alias and terminal must match exactly`() {
        val mismatchedExpectations = listOf(
            propagationExpectation().copy(boundary = ContextPropagationBoundary.REACTOR),
            propagationExpectation().copy(scenario = ContextPropagationScenario.FAILURE),
            propagationExpectation().copy(requestAlias = ContextRequestAlias.PROBE),
            propagationExpectation().copy(expectedTerminal = ContextPropagationTerminal.FAILURE),
        )

        mismatchedExpectations.forEach { mismatchedExpectation ->
            assertFailsWith<AssertionError> {
                assertContextPropagationConformance(propagationObservation(), mismatchedExpectation)
            }
        }
    }

    @Test
    fun `propagation observation point sets must match exactly`() {
        val observation = propagationObservation().copy(
            markerObservations = propagationObservation().markerObservations.dropLast(1),
        )

        assertFailsWith<AssertionError> {
            assertContextPropagationConformance(observation, propagationExpectation())
        }
    }

    @Test
    fun `propagation observation points must be unique`() {
        val observation = propagationObservation().copy(
            markerObservations = propagationObservation().markerObservations +
                    ContextMarkerObservation(ContextObservationPoint.BOUNDARY_ENTER, PARENT_MARKER),
        )

        assertFailsWith<AssertionError> {
            assertContextPropagationConformance(observation, propagationExpectation())
        }
    }

    @Test
    fun `cleanup location sets must match exactly and remain unique`() {
        val missingLocation = propagationObservation().copy(
            cleanupProbes = propagationObservation().cleanupProbes.dropLast(1),
        )
        val duplicateLocation = propagationExpectation().copy(
            cleanupExpectations = propagationExpectation().cleanupExpectations +
                    ContextCleanupExpectation(ContextProbeLocation.CALLER, null),
        )

        assertFailsWith<AssertionError> {
            assertContextPropagationConformance(missingLocation, propagationExpectation())
        }
        assertFailsWith<AssertionError> {
            assertContextPropagationConformance(propagationObservation(), duplicateLocation)
        }
    }

    @Test
    fun `literal root is rejected because root context is represented by null`() {
        val cases = listOf<() -> Unit>(
            {
                assertContextPropagationConformance(
                    propagationObservation().copy(
                        markerObservations = propagationObservation().markerObservations.map {
                            if (it.point == ContextObservationPoint.AFTER_SUSPENSION) {
                                it.copy(observedMarker = "root")
                            } else {
                                it
                            }
                        },
                    ),
                    propagationExpectation(),
                )
            },
            {
                assertContextPropagationConformance(
                    propagationObservation(),
                    propagationExpectation().copy(
                        markerExpectations = propagationExpectation().markerExpectations.map {
                            if (it.point == ContextObservationPoint.AFTER_SUSPENSION) {
                                it.copy(expectedMarker = "root")
                            } else {
                                it
                            }
                        },
                    ),
                )
            },
            {
                assertContextIsolation(
                    isolationObservation().copy(
                        samples = listOf(ContextIsolationSample(ContextRequestAlias.REQUEST_A, listOf("root"))),
                    ),
                    isolationExpectation(
                        ContextIsolationSampleExpectation(
                            requestAlias = ContextRequestAlias.REQUEST_A,
                            mode = ContextMarkerExpectationMode.EXACT,
                            expectedMarker = REQUEST_A_MARKER,
                        ),
                    ),
                )
            },
        )

        cases.forEach { failingAssertion ->
            assertFailsWith<AssertionError>(block = failingAssertion)
        }
    }

    @Test
    fun `isolation aliases must match exactly and remain unique`() {
        val duplicateObservation = isolationObservation().copy(
            samples = isolationObservation().samples + isolationObservation().samples.single(),
        )
        val mismatchedExpectation = ContextIsolationExpectation(
            boundary = ContextPropagationBoundary.COROUTINE,
            samples = listOf(
                ContextIsolationSampleExpectation(
                    requestAlias = ContextRequestAlias.REQUEST_B,
                    mode = ContextMarkerExpectationMode.EXACT,
                    expectedMarker = REQUEST_B_MARKER,
                ),
            ),
            cleanupExpectations = cleanupExpectations(),
        )

        assertFailsWith<AssertionError> {
            assertContextIsolation(duplicateObservation, isolationExpectation(exactExpectation()))
        }
        assertFailsWith<AssertionError> {
            assertContextIsolation(isolationObservation(), mismatchedExpectation)
        }
    }

    @Test
    fun `EXACT isolation requires every observation to equal its marker`() {
        assertContextIsolation(isolationObservation(), isolationExpectation(exactExpectation()))

        val mismatch = isolationObservation().copy(
            samples = listOf(ContextIsolationSample(ContextRequestAlias.REQUEST_A, listOf(REQUEST_B_MARKER))),
        )
        assertFailsWith<AssertionError> {
            assertContextIsolation(mismatch, isolationExpectation(exactExpectation()))
        }
    }

    @Test
    fun `ABSENT isolation requires every observation to be null`() {
        val absentObservation = isolationObservation().copy(
            samples = listOf(ContextIsolationSample(ContextRequestAlias.PROBE, listOf(null, null))),
        )
        val absentExpectation = ContextIsolationSampleExpectation(
            requestAlias = ContextRequestAlias.PROBE,
            mode = ContextMarkerExpectationMode.ABSENT,
            minimumObservationCount = 2,
        )

        assertContextIsolation(absentObservation, isolationExpectation(absentExpectation))

        assertFailsWith<AssertionError> {
            assertContextIsolation(
                absentObservation.copy(
                    samples = listOf(ContextIsolationSample(ContextRequestAlias.PROBE, listOf(PARENT_MARKER))),
                ),
                isolationExpectation(absentExpectation),
            )
        }
    }

    @Test
    fun `NOT_IN isolation rejects every forbidden marker`() {
        val observation = isolationObservation().copy(
            samples = listOf(ContextIsolationSample(ContextRequestAlias.REQUEST_B, listOf(REQUEST_B_MARKER, null))),
        )
        val expectation = ContextIsolationSampleExpectation(
            requestAlias = ContextRequestAlias.REQUEST_B,
            mode = ContextMarkerExpectationMode.NOT_IN,
            forbiddenMarkers = listOf(PARENT_MARKER, REQUEST_A_MARKER),
            minimumObservationCount = 2,
        )

        assertContextIsolation(observation, isolationExpectation(expectation))

        assertFailsWith<AssertionError> {
            assertContextIsolation(
                observation.copy(
                    samples = listOf(ContextIsolationSample(ContextRequestAlias.REQUEST_B, listOf(PARENT_MARKER))),
                ),
                isolationExpectation(expectation),
            )
        }
    }

    @Test
    fun `isolation samples enforce their minimum observation count`() {
        val expectation = exactExpectation().copy(minimumObservationCount = 2)

        assertFailsWith<AssertionError> {
            assertContextIsolation(isolationObservation(), isolationExpectation(expectation))
        }
    }

    @Test
    fun `isolation expectation modes reject invalid field combinations`() {
        val invalidExpectations = listOf(
            exactExpectation().copy(expectedMarker = null),
            exactExpectation().copy(forbiddenMarkers = listOf(PARENT_MARKER)),
            exactExpectation().copy(mode = ContextMarkerExpectationMode.ABSENT),
            exactExpectation().copy(
                mode = ContextMarkerExpectationMode.NOT_IN,
                expectedMarker = null,
                forbiddenMarkers = emptyList(),
            ),
            exactExpectation().copy(
                mode = ContextMarkerExpectationMode.NOT_IN,
                expectedMarker = null,
                forbiddenMarkers = listOf(PARENT_MARKER, PARENT_MARKER),
            ),
            exactExpectation().copy(minimumObservationCount = 0),
        )

        invalidExpectations.forEach { invalidExpectation ->
            assertFailsWith<AssertionError> {
                assertContextIsolation(isolationObservation(), isolationExpectation(invalidExpectation))
            }
        }
    }

    @Test
    fun `EXACT isolation markers must be unique across aliases`() {
        val observation = ContextIsolationObservation(
            boundary = ContextPropagationBoundary.COROUTINE,
            samples = listOf(
                ContextIsolationSample(ContextRequestAlias.REQUEST_A, listOf(REQUEST_A_MARKER)),
                ContextIsolationSample(ContextRequestAlias.REQUEST_B, listOf(REQUEST_A_MARKER)),
            ),
            cleanupProbes = cleanupProbes(),
        )
        val expectation = ContextIsolationExpectation(
            boundary = ContextPropagationBoundary.COROUTINE,
            samples = listOf(
                exactExpectation(),
                exactExpectation().copy(requestAlias = ContextRequestAlias.REQUEST_B),
            ),
            cleanupExpectations = cleanupExpectations(),
        )

        assertFailsWith<AssertionError> {
            assertContextIsolation(observation, expectation)
        }
    }

    @Test
    fun `every failure family reports only safe redacted coordinates`() {
        val failureCases = listOf<Pair<String, () -> Unit>>(
            "observation-point set" to {
                assertContextPropagationConformance(
                    propagationObservation().copy(markerObservations = emptyList()),
                    propagationExpectation(),
                )
            },
            "propagation marker" to {
                assertContextPropagationConformance(
                    propagationObservation().copy(
                        markerObservations = propagationObservation().markerObservations.map {
                            if (it.point == ContextObservationPoint.BOUNDARY_ENTER) {
                                it.copy(observedMarker = CANARY)
                            } else {
                                it
                            }
                        },
                    ),
                    propagationExpectation(),
                )
            },
            "cleanup" to {
                assertContextPropagationConformance(
                    propagationObservation().copy(
                        cleanupProbes = propagationObservation().cleanupProbes.map {
                            if (it.location == ContextProbeLocation.CALLER) {
                                it.copy(observedMarker = CANARY)
                            } else {
                                it
                            }
                        },
                    ),
                    propagationExpectation(),
                )
            },
            "isolation EXACT" to {
                assertContextIsolation(
                    isolationObservation().copy(
                        samples = listOf(ContextIsolationSample(ContextRequestAlias.REQUEST_A, listOf(CANARY))),
                    ),
                    isolationExpectation(exactExpectation()),
                )
            },
            "isolation NOT_IN" to {
                assertContextIsolation(
                    isolationObservation().copy(
                        samples = listOf(ContextIsolationSample(ContextRequestAlias.REQUEST_A, listOf(CANARY))),
                    ),
                    isolationExpectation(
                        ContextIsolationSampleExpectation(
                            requestAlias = ContextRequestAlias.REQUEST_A,
                            mode = ContextMarkerExpectationMode.NOT_IN,
                            forbiddenMarkers = listOf(CANARY),
                        ),
                    ),
                )
            },
            "invalid expectation" to {
                assertContextIsolation(
                    isolationObservation(),
                    isolationExpectation(
                        exactExpectation().copy(forbiddenMarkers = listOf(CANARY)),
                    ),
                )
            },
        )

        failureCases.forEach { (family, failingAssertion) ->
            val failure = assertFailsWith<AssertionError>(block = failingAssertion)
            val message = failure.message.orEmpty()
            message shouldContain "values redacted"
            message shouldContain "relation=mismatch"
            message shouldNotContain "secret-parent"
            message shouldNotContain "forged-log"
            message shouldNotContain "\r"
            message shouldNotContain "\n"
            message shouldContain family.safeCoordinate()
        }
    }

    private fun propagationObservation(): ContextPropagationObservation =
        ContextPropagationObservation(
            boundary = ContextPropagationBoundary.COROUTINE,
            scenario = ContextPropagationScenario.SUCCESS,
            requestAlias = ContextRequestAlias.SINGLE,
            markerObservations = ContextObservationPoint.entries.map { point ->
                ContextMarkerObservation(point, PARENT_MARKER)
            },
            cleanupProbes = cleanupProbes(),
            terminal = ContextPropagationTerminal.SUCCESS,
        )

    private fun propagationExpectation(): ContextPropagationExpectation =
        ContextPropagationExpectation(
            boundary = ContextPropagationBoundary.COROUTINE,
            scenario = ContextPropagationScenario.SUCCESS,
            requestAlias = ContextRequestAlias.SINGLE,
            markerExpectations = ContextObservationPoint.entries.map { point ->
                ContextMarkerExpectation(point, PARENT_MARKER)
            },
            cleanupExpectations = cleanupExpectations(),
            expectedTerminal = ContextPropagationTerminal.SUCCESS,
        )

    private fun isolationObservation(): ContextIsolationObservation =
        ContextIsolationObservation(
            boundary = ContextPropagationBoundary.COROUTINE,
            samples = listOf(
                ContextIsolationSample(ContextRequestAlias.REQUEST_A, listOf(REQUEST_A_MARKER)),
            ),
            cleanupProbes = cleanupProbes(),
        )

    private fun isolationExpectation(
        sampleExpectation: ContextIsolationSampleExpectation,
    ): ContextIsolationExpectation =
        ContextIsolationExpectation(
            boundary = ContextPropagationBoundary.COROUTINE,
            samples = listOf(sampleExpectation),
            cleanupExpectations = cleanupExpectations(),
        )

    private fun exactExpectation(): ContextIsolationSampleExpectation =
        ContextIsolationSampleExpectation(
            requestAlias = ContextRequestAlias.REQUEST_A,
            mode = ContextMarkerExpectationMode.EXACT,
            expectedMarker = REQUEST_A_MARKER,
        )

    private fun cleanupProbes(): List<ContextCleanupProbe> =
        ContextProbeLocation.entries.map { location ->
            ContextCleanupProbe(location, null)
        }

    private fun cleanupExpectations(): List<ContextCleanupExpectation> =
        ContextProbeLocation.entries.map { location ->
            ContextCleanupExpectation(location, null)
        }

    private fun String.safeCoordinate(): String =
        when (this) {
            "observation-point set" -> "field=markerObservations"
            "propagation marker" -> "point=BOUNDARY_ENTER"
            "cleanup" -> "location=CALLER"
            "isolation EXACT" -> "mode=EXACT"
            "isolation NOT_IN" -> "mode=NOT_IN"
            "invalid expectation" -> "field=forbiddenMarkers"
            else -> error("Unknown failure family")
        }

    private companion object {
        const val PARENT_MARKER = "synthetic-parent"
        const val REQUEST_A_MARKER = "synthetic-request-a"
        const val REQUEST_B_MARKER = "synthetic-request-b"
        const val CANARY = "secret-parent\r\nforged-log"
    }
}
