package io.bluetape4k.junit5.observability

import java.io.Serializable

private const val REDACTED_MARKER_VALUE = "values redacted"

/**
 * context propagation proof가 다루는 framework boundary를 식별합니다.
 *
 * future value는 additive입니다. 호출자는 exhaustive `when`으로 간주하지 말고 `else` branch를 포함해야 합니다
 * durable. Examples use only test-owned synthetic data; production request IDs, user data, and external trace IDs
 * must never be supplied.
 *
 * Example: `val boundary = ContextPropagationBoundary.COROUTINE`
 */
enum class ContextPropagationBoundary {
    COROUTINE,
    REACTOR,
    TASK_EXECUTOR,
    SPRING_OBSERVATION,
    KTOR_REQUEST,
}

/**
 * context propagation proof가 실행하는 lifecycle scenario를 식별합니다.
 *
 * future value는 additive입니다. 호출자는 exhaustive `when`으로 간주하지 말고 `else` branch를 포함해야 합니다
 * durable. Examples use only test-owned synthetic data; production request IDs, user data, and external trace IDs
 * must never be supplied.
 *
 * Example: `val scenario = ContextPropagationScenario.SUCCESS`
 */
enum class ContextPropagationScenario {
    SUCCESS,
    FAILURE,
    CANCELLATION,
    DEADLINE,
    ISOLATION,
}

/**
 * context boundary를 지난 뒤 관찰된 terminal outcome을 식별합니다.
 *
 * future value는 additive입니다. 호출자는 exhaustive `when`으로 간주하지 말고 `else` branch를 포함해야 합니다
 * durable. Examples use only test-owned synthetic data; production request IDs, user data, and external trace IDs
 * must never be supplied.
 *
 * Example: `val terminal = ContextPropagationTerminal.SUCCESS`
 */
enum class ContextPropagationTerminal {
    SUCCESS,
    FAILURE,
    CANCELLATION,
    DEADLINE_EXCEEDED,
}

/**
 * boundary 완료 후 cleanup을 probe한 위치를 식별합니다.
 *
 * future value는 additive입니다. 호출자는 exhaustive `when`으로 간주하지 말고 `else` branch를 포함해야 합니다
 * durable. Examples use only test-owned synthetic data; production request IDs, user data, and external trace IDs
 * must never be supplied.
 *
 * Example: `val location = ContextProbeLocation.CALLER`
 */
enum class ContextProbeLocation {
    CALLER,
    WORKER,
    REQUEST,
}

/**
 * Supplies a stable test-owned alias for one request or probe.
 *
 * future value는 additive입니다. 호출자는 exhaustive `when`으로 간주하지 말고 `else` branch를 포함해야 합니다
 * durable. Examples use only test-owned synthetic data; production request IDs, user data, and external trace IDs
 * must never be supplied.
 *
 * Example: `val alias = ContextRequestAlias.SINGLE`
 */
enum class ContextRequestAlias {
    SINGLE,
    REQUEST_A,
    REQUEST_B,
    PROBE,
}

/**
 * Identifies a stable observation point inside a propagation lifecycle.
 *
 * future value는 additive입니다. 호출자는 exhaustive `when`으로 간주하지 말고 `else` branch를 포함해야 합니다
 * durable. Examples use only test-owned synthetic data; production request IDs, user data, and external trace IDs
 * must never be supplied.
 *
 * Example: `val point = ContextObservationPoint.BOUNDARY_ENTER`
 */
enum class ContextObservationPoint {
    BOUNDARY_ENTER,
    AFTER_SUSPENSION,
    BEFORE_TERMINAL,
}

/**
 * Defines the relation applied to an isolation sample.
 *
 * future value는 additive입니다. 호출자는 exhaustive `when`으로 간주하지 말고 `else` branch를 포함해야 합니다
 * durable. Examples use only test-owned synthetic data; production request IDs, user data, and external trace IDs
 * must never be supplied.
 *
 * Example: `val mode = ContextMarkerExpectationMode.EXACT`
 */
enum class ContextMarkerExpectationMode {
    EXACT,
    ABSENT,
    NOT_IN,
}

/**
 * Captures the marker observed at one propagation point.
 *
 * Constructor changes are compatibility-sensitive. Callers must not persist or destructure this value or use it as
 * a wire contract. Serializable snapshots are not persistence or wire formats. Markers must be test-owned synthetic
 * values, never production request IDs, user data, or external trace IDs. Root context is represented by `null`.
 * [toString] preserves only bounded coordinates and redacts marker-bearing fields.
 *
 * Example:
 * ```kotlin
 * val observation = ContextMarkerObservation(
 *     point = ContextObservationPoint.BOUNDARY_ENTER,
 *     observedMarker = "synthetic-parent",
 * )
 * ```
 */
data class ContextMarkerObservation(
    val point: ContextObservationPoint,
    val observedMarker: String?,
): Serializable {
    override fun toString(): String =
        "ContextMarkerObservation(point=$point, observedMarker=$REDACTED_MARKER_VALUE)"

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Describes the synthetic marker expected at one propagation point.
 *
 * Constructor changes are compatibility-sensitive. Callers must not persist or destructure this value or use it as
 * a wire contract. Serializable snapshots are not persistence or wire formats. Markers must be test-owned synthetic
 * values, never production request IDs, user data, or external trace IDs. Root context is represented by `null`,
 * so the literal marker `root` is invalid. [toString] preserves only bounded coordinates and redacts marker-bearing
 * fields.
 *
 * Example:
 * ```kotlin
 * val expectation = ContextMarkerExpectation(
 *     point = ContextObservationPoint.BOUNDARY_ENTER,
 *     expectedMarker = "synthetic-parent",
 * )
 * ```
 */
data class ContextMarkerExpectation(
    val point: ContextObservationPoint,
    val expectedMarker: String,
): Serializable {
    override fun toString(): String =
        "ContextMarkerExpectation(point=$point, expectedMarker=$REDACTED_MARKER_VALUE)"

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Captures the marker remaining at one cleanup location.
 *
 * Constructor changes are compatibility-sensitive. Callers must not persist or destructure this value or use it as
 * a wire contract. Serializable snapshots are not persistence or wire formats. Markers must be test-owned synthetic
 * values, never production request IDs, user data, or external trace IDs. Root context is represented by `null`.
 * [toString] preserves only bounded coordinates and redacts marker-bearing fields.
 *
 * Example:
 * ```kotlin
 * val probe = ContextCleanupProbe(
 *     location = ContextProbeLocation.CALLER,
 *     observedMarker = null,
 * )
 * ```
 */
data class ContextCleanupProbe(
    val location: ContextProbeLocation,
    val observedMarker: String?,
): Serializable {
    override fun toString(): String =
        "ContextCleanupProbe(location=$location, observedMarker=$REDACTED_MARKER_VALUE)"

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Describes the marker expected at one cleanup location.
 *
 * Constructor changes are compatibility-sensitive. Callers must not persist or destructure this value or use it as
 * a wire contract. Serializable snapshots are not persistence or wire formats. Markers must be test-owned synthetic
 * values, never production request IDs, user data, or external trace IDs. Root context is represented by `null`.
 * [toString] preserves only bounded coordinates and redacts marker-bearing fields.
 *
 * Example:
 * ```kotlin
 * val expectation = ContextCleanupExpectation(
 *     location = ContextProbeLocation.CALLER,
 *     expectedMarker = null,
 * )
 * ```
 */
data class ContextCleanupExpectation(
    val location: ContextProbeLocation,
    val expectedMarker: String?,
): Serializable {
    override fun toString(): String =
        "ContextCleanupExpectation(location=$location, expectedMarker=$REDACTED_MARKER_VALUE)"

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Captures framework-neutral evidence for one context propagation lifecycle.
 *
 * Constructor changes are compatibility-sensitive. Callers must not persist or destructure this value or use it as
 * a wire contract. Serializable snapshots are not persistence or wire formats. Markers must be test-owned synthetic
 * values, never production request IDs, user data, or external trace IDs.
 *
 * Example:
 * ```kotlin
 * val marker = "synthetic-parent"
 * val observation = ContextPropagationObservation(
 *     boundary = ContextPropagationBoundary.COROUTINE,
 *     scenario = ContextPropagationScenario.SUCCESS,
 *     requestAlias = ContextRequestAlias.SINGLE,
 *     markerObservations = listOf(
 *         ContextMarkerObservation(ContextObservationPoint.BOUNDARY_ENTER, marker),
 *         ContextMarkerObservation(ContextObservationPoint.AFTER_SUSPENSION, marker),
 *         ContextMarkerObservation(ContextObservationPoint.BEFORE_TERMINAL, marker),
 *     ),
 *     cleanupProbes = listOf(
 *         ContextCleanupProbe(ContextProbeLocation.CALLER, null),
 *     ),
 *     terminal = ContextPropagationTerminal.SUCCESS,
 * )
 * ```
 */
data class ContextPropagationObservation(
    val boundary: ContextPropagationBoundary,
    val scenario: ContextPropagationScenario,
    val requestAlias: ContextRequestAlias,
    val markerObservations: List<ContextMarkerObservation>,
    val cleanupProbes: List<ContextCleanupProbe>,
    val terminal: ContextPropagationTerminal,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Describes the expected framework-neutral propagation lifecycle.
 *
 * Constructor changes are compatibility-sensitive. Callers must not persist or destructure this value or use it as
 * a wire contract. Serializable snapshots are not persistence or wire formats. Markers must be test-owned synthetic
 * values, never production request IDs, user data, or external trace IDs.
 *
 * Example:
 * ```kotlin
 * val marker = "synthetic-parent"
 * val expectation = ContextPropagationExpectation(
 *     boundary = ContextPropagationBoundary.COROUTINE,
 *     scenario = ContextPropagationScenario.SUCCESS,
 *     requestAlias = ContextRequestAlias.SINGLE,
 *     markerExpectations = listOf(
 *         ContextMarkerExpectation(ContextObservationPoint.BOUNDARY_ENTER, marker),
 *         ContextMarkerExpectation(ContextObservationPoint.AFTER_SUSPENSION, marker),
 *         ContextMarkerExpectation(ContextObservationPoint.BEFORE_TERMINAL, marker),
 *     ),
 *     cleanupExpectations = listOf(
 *         ContextCleanupExpectation(ContextProbeLocation.CALLER, null),
 *     ),
 *     expectedTerminal = ContextPropagationTerminal.SUCCESS,
 * )
 * ```
 */
data class ContextPropagationExpectation(
    val boundary: ContextPropagationBoundary,
    val scenario: ContextPropagationScenario,
    val requestAlias: ContextRequestAlias,
    val markerExpectations: List<ContextMarkerExpectation>,
    val cleanupExpectations: List<ContextCleanupExpectation>,
    val expectedTerminal: ContextPropagationTerminal,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Captures repeated marker observations for one isolation alias.
 *
 * Constructor changes are compatibility-sensitive. Callers must not persist or destructure this value or use it as
 * a wire contract. Serializable snapshots are not persistence or wire formats. Markers must be test-owned synthetic
 * values, never production request IDs, user data, or external trace IDs. Root context is represented by `null`.
 * [toString] preserves only bounded coordinates and redacts marker-bearing fields.
 *
 * Example:
 * ```kotlin
 * val sample = ContextIsolationSample(
 *     requestAlias = ContextRequestAlias.REQUEST_A,
 *     observedMarkers = listOf("synthetic-parent-A", "synthetic-parent-A"),
 * )
 * ```
 */
data class ContextIsolationSample(
    val requestAlias: ContextRequestAlias,
    val observedMarkers: List<String?>,
): Serializable {
    override fun toString(): String =
        "ContextIsolationSample(requestAlias=$requestAlias, observedMarkers=$REDACTED_MARKER_VALUE)"

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Describes the relation expected for one isolation alias.
 *
 * Constructor changes are compatibility-sensitive. Callers must not persist or destructure this value or use it as
 * a wire contract. Serializable snapshots are not persistence or wire formats. Markers must be test-owned synthetic
 * values, never production request IDs, user data, or external trace IDs. Root context is represented by `null`.
 * [toString] preserves only bounded coordinates and redacts marker-bearing fields.
 *
 * Example:
 * ```kotlin
 * val expectation = ContextIsolationSampleExpectation(
 *     requestAlias = ContextRequestAlias.REQUEST_A,
 *     mode = ContextMarkerExpectationMode.EXACT,
 *     expectedMarker = "synthetic-parent-A",
 *     minimumObservationCount = 2,
 * )
 * ```
 */
data class ContextIsolationSampleExpectation(
    val requestAlias: ContextRequestAlias,
    val mode: ContextMarkerExpectationMode,
    val expectedMarker: String? = null,
    val forbiddenMarkers: List<String> = emptyList(),
    val minimumObservationCount: Int = 1,
): Serializable {
    override fun toString(): String =
        "ContextIsolationSampleExpectation(" +
                "requestAlias=$requestAlias, " +
                "mode=$mode, " +
                "expectedMarker=$REDACTED_MARKER_VALUE, " +
                "forbiddenMarkers=$REDACTED_MARKER_VALUE, " +
                "minimumObservationCount=$minimumObservationCount)"

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Captures framework-neutral evidence for an isolation scenario.
 *
 * Constructor changes are compatibility-sensitive. Callers must not persist or destructure this value or use it as
 * a wire contract. Serializable snapshots are not persistence or wire formats. Markers must be test-owned synthetic
 * values, never production request IDs, user data, or external trace IDs.
 *
 * Example:
 * ```kotlin
 * val observation = ContextIsolationObservation(
 *     boundary = ContextPropagationBoundary.KTOR_REQUEST,
 *     samples = listOf(
 *         ContextIsolationSample(
 *             ContextRequestAlias.REQUEST_A,
 *             listOf("synthetic-parent-A", "synthetic-parent-A"),
 *         ),
 *         ContextIsolationSample(
 *             ContextRequestAlias.REQUEST_B,
 *             listOf("synthetic-parent-B", "synthetic-parent-B"),
 *         ),
 *         ContextIsolationSample(
 *             ContextRequestAlias.PROBE,
 *             listOf("synthetic-probe"),
 *         ),
 *     ),
 *     cleanupProbes = listOf(
 *         ContextCleanupProbe(ContextProbeLocation.REQUEST, null),
 *     ),
 * )
 * ```
 */
data class ContextIsolationObservation(
    val boundary: ContextPropagationBoundary,
    val samples: List<ContextIsolationSample>,
    val cleanupProbes: List<ContextCleanupProbe>,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Describes the expected framework-neutral isolation evidence.
 *
 * Constructor changes are compatibility-sensitive. Callers must not persist or destructure this value or use it as
 * a wire contract. Serializable snapshots are not persistence or wire formats. Markers must be test-owned synthetic
 * values, never production request IDs, user data, or external trace IDs.
 *
 * Example:
 * ```kotlin
 * val expectation = ContextIsolationExpectation(
 *     boundary = ContextPropagationBoundary.KTOR_REQUEST,
 *     samples = listOf(
 *         ContextIsolationSampleExpectation(
 *             requestAlias = ContextRequestAlias.REQUEST_A,
 *             mode = ContextMarkerExpectationMode.EXACT,
 *             expectedMarker = "synthetic-parent-A",
 *             minimumObservationCount = 2,
 *         ),
 *         ContextIsolationSampleExpectation(
 *             requestAlias = ContextRequestAlias.REQUEST_B,
 *             mode = ContextMarkerExpectationMode.EXACT,
 *             expectedMarker = "synthetic-parent-B",
 *             minimumObservationCount = 2,
 *         ),
 *         ContextIsolationSampleExpectation(
 *             requestAlias = ContextRequestAlias.PROBE,
 *             mode = ContextMarkerExpectationMode.NOT_IN,
 *             forbiddenMarkers = listOf(
 *                 "synthetic-parent-A",
 *                 "synthetic-parent-B",
 *             ),
 *         ),
 *     ),
 *     cleanupExpectations = listOf(
 *         ContextCleanupExpectation(ContextProbeLocation.REQUEST, null),
 *     ),
 * )
 * ```
 */
data class ContextIsolationExpectation(
    val boundary: ContextPropagationBoundary,
    val samples: List<ContextIsolationSampleExpectation>,
    val cleanupExpectations: List<ContextCleanupExpectation>,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Verifies one framework-neutral propagation snapshot against its expectation.
 *
 * The assertion emits no log or console output and never includes marker values in failures. Supply only test-owned
 * synthetic markers; never supply production request IDs, user data, or external trace IDs. Serializable snapshots
 * are test exchange values, not persistence or wire formats.
 *
 * Example:
 * ```kotlin
 * val marker = "synthetic-parent"
 * val observation = ContextPropagationObservation(
 *     boundary = ContextPropagationBoundary.COROUTINE,
 *     scenario = ContextPropagationScenario.SUCCESS,
 *     requestAlias = ContextRequestAlias.SINGLE,
 *     markerObservations = listOf(
 *         ContextMarkerObservation(ContextObservationPoint.BOUNDARY_ENTER, marker),
 *         ContextMarkerObservation(ContextObservationPoint.AFTER_SUSPENSION, marker),
 *         ContextMarkerObservation(ContextObservationPoint.BEFORE_TERMINAL, marker),
 *     ),
 *     cleanupProbes = listOf(
 *         ContextCleanupProbe(ContextProbeLocation.CALLER, null),
 *     ),
 *     terminal = ContextPropagationTerminal.SUCCESS,
 * )
 * val expectation = ContextPropagationExpectation(
 *     boundary = ContextPropagationBoundary.COROUTINE,
 *     scenario = ContextPropagationScenario.SUCCESS,
 *     requestAlias = ContextRequestAlias.SINGLE,
 *     markerExpectations = listOf(
 *         ContextMarkerExpectation(ContextObservationPoint.BOUNDARY_ENTER, marker),
 *         ContextMarkerExpectation(ContextObservationPoint.AFTER_SUSPENSION, marker),
 *         ContextMarkerExpectation(ContextObservationPoint.BEFORE_TERMINAL, marker),
 *     ),
 *     cleanupExpectations = listOf(
 *         ContextCleanupExpectation(ContextProbeLocation.CALLER, null),
 *     ),
 *     expectedTerminal = ContextPropagationTerminal.SUCCESS,
 * )
 *
 * assertContextPropagationConformance(observation, expectation)
 * ```
 */
fun assertContextPropagationConformance(
    observation: ContextPropagationObservation,
    expectation: ContextPropagationExpectation,
) {
    validatePropagationObservation(observation)
    validatePropagationExpectation(expectation)

    assertContext(
        observation.boundary == expectation.boundary,
        coordinates(observation.boundary, observation.scenario, observation.requestAlias, field = "boundary"),
    )
    assertContext(
        observation.scenario == expectation.scenario,
        coordinates(observation.boundary, observation.scenario, observation.requestAlias, field = "scenario"),
    )
    assertContext(
        observation.requestAlias == expectation.requestAlias,
        coordinates(observation.boundary, observation.scenario, observation.requestAlias, field = "requestAlias"),
    )
    assertContext(
        observation.terminal == expectation.expectedTerminal,
        coordinates(
            observation.boundary,
            observation.scenario,
            observation.requestAlias,
            field = "terminal",
            terminal = observation.terminal,
        ),
    )

    val observationsByPoint = observation.markerObservations.associateBy(ContextMarkerObservation::point)
    val expectationsByPoint = expectation.markerExpectations.associateBy(ContextMarkerExpectation::point)
    assertContext(
        observationsByPoint.keys == expectationsByPoint.keys,
        coordinates(
            observation.boundary,
            observation.scenario,
            observation.requestAlias,
            field = "markerObservations",
        ),
    )
    expectationsByPoint.forEach { (point, markerExpectation) ->
        assertContext(
            observationsByPoint.getValue(point).observedMarker == markerExpectation.expectedMarker,
            coordinates(
                observation.boundary,
                observation.scenario,
                observation.requestAlias,
                field = "marker",
                point = point,
            ),
        )
    }

    assertCleanup(
        observation.cleanupProbes,
        expectation.cleanupExpectations,
        observation.boundary,
        observation.scenario,
        observation.requestAlias,
    )
}

/**
 * Verifies framework-neutral isolation samples against their expectation modes.
 *
 * The assertion emits no log or console output and never includes marker values in failures. Supply only test-owned
 * synthetic markers; never supply production request IDs, user data, or external trace IDs. Serializable snapshots
 * are test exchange values, not persistence or wire formats.
 *
 * Example:
 * ```kotlin
 * val observation = ContextIsolationObservation(
 *     boundary = ContextPropagationBoundary.KTOR_REQUEST,
 *     samples = listOf(
 *         ContextIsolationSample(
 *             ContextRequestAlias.REQUEST_A,
 *             listOf("synthetic-parent-A", "synthetic-parent-A"),
 *         ),
 *         ContextIsolationSample(
 *             ContextRequestAlias.REQUEST_B,
 *             listOf("synthetic-parent-B", "synthetic-parent-B"),
 *         ),
 *         ContextIsolationSample(
 *             ContextRequestAlias.PROBE,
 *             listOf("synthetic-probe"),
 *         ),
 *     ),
 *     cleanupProbes = listOf(
 *         ContextCleanupProbe(ContextProbeLocation.REQUEST, null),
 *     ),
 * )
 * val expectation = ContextIsolationExpectation(
 *     boundary = ContextPropagationBoundary.KTOR_REQUEST,
 *     samples = listOf(
 *         ContextIsolationSampleExpectation(
 *             requestAlias = ContextRequestAlias.REQUEST_A,
 *             mode = ContextMarkerExpectationMode.EXACT,
 *             expectedMarker = "synthetic-parent-A",
 *             minimumObservationCount = 2,
 *         ),
 *         ContextIsolationSampleExpectation(
 *             requestAlias = ContextRequestAlias.REQUEST_B,
 *             mode = ContextMarkerExpectationMode.EXACT,
 *             expectedMarker = "synthetic-parent-B",
 *             minimumObservationCount = 2,
 *         ),
 *         ContextIsolationSampleExpectation(
 *             requestAlias = ContextRequestAlias.PROBE,
 *             mode = ContextMarkerExpectationMode.NOT_IN,
 *             forbiddenMarkers = listOf(
 *                 "synthetic-parent-A",
 *                 "synthetic-parent-B",
 *             ),
 *         ),
 *     ),
 *     cleanupExpectations = listOf(
 *         ContextCleanupExpectation(ContextProbeLocation.REQUEST, null),
 *     ),
 * )
 *
 * assertContextIsolation(observation, expectation)
 * ```
 */
fun assertContextIsolation(
    observation: ContextIsolationObservation,
    expectation: ContextIsolationExpectation,
) {
    validateIsolationObservation(observation)
    validateIsolationExpectation(expectation)

    assertContext(
        observation.boundary == expectation.boundary,
        coordinates(observation.boundary, field = "boundary"),
    )

    val observationsByAlias = observation.samples.associateBy(ContextIsolationSample::requestAlias)
    val expectationsByAlias = expectation.samples.associateBy(ContextIsolationSampleExpectation::requestAlias)
    assertContext(
        observationsByAlias.keys == expectationsByAlias.keys,
        coordinates(observation.boundary, field = "samples"),
    )

    expectationsByAlias.forEach { (alias, sampleExpectation) ->
        val sample = observationsByAlias.getValue(alias)
        val sampleCoordinates = coordinates(
            boundary = observation.boundary,
            requestAlias = alias,
            field = "observedMarkers",
            mode = sampleExpectation.mode,
        )
        assertContext(
            sample.observedMarkers.size >= sampleExpectation.minimumObservationCount,
            sampleCoordinates,
        )
        val relationMatches = when (sampleExpectation.mode) {
            ContextMarkerExpectationMode.EXACT ->
                sample.observedMarkers.all { it == sampleExpectation.expectedMarker }

            ContextMarkerExpectationMode.ABSENT ->
                sample.observedMarkers.all { it == null }

            ContextMarkerExpectationMode.NOT_IN ->
                sample.observedMarkers.all { it != null && it !in sampleExpectation.forbiddenMarkers }
        }
        assertContext(relationMatches, sampleCoordinates)
    }

    assertCleanup(
        observation.cleanupProbes,
        expectation.cleanupExpectations,
        observation.boundary,
    )
}

private fun validatePropagationObservation(observation: ContextPropagationObservation) {
    validateUnique(
        observation.markerObservations.map(ContextMarkerObservation::point),
        coordinates(
            observation.boundary,
            observation.scenario,
            observation.requestAlias,
            field = "markerObservations",
        ),
    )
    observation.markerObservations.forEach { markerObservation ->
        validateMarker(
            markerObservation.observedMarker,
            coordinates(
                observation.boundary,
                observation.scenario,
                observation.requestAlias,
                field = "observedMarker",
                point = markerObservation.point,
            ),
        )
    }
    validateCleanupProbes(
        observation.cleanupProbes,
        observation.boundary,
        observation.scenario,
        observation.requestAlias,
    )
}

private fun validatePropagationExpectation(expectation: ContextPropagationExpectation) {
    validateUnique(
        expectation.markerExpectations.map(ContextMarkerExpectation::point),
        coordinates(
            expectation.boundary,
            expectation.scenario,
            expectation.requestAlias,
            field = "markerExpectations",
        ),
    )
    expectation.markerExpectations.forEach { markerExpectation ->
        validateMarker(
            markerExpectation.expectedMarker,
            coordinates(
                expectation.boundary,
                expectation.scenario,
                expectation.requestAlias,
                field = "expectedMarker",
                point = markerExpectation.point,
            ),
        )
    }
    validateCleanupExpectations(
        expectation.cleanupExpectations,
        expectation.boundary,
        expectation.scenario,
        expectation.requestAlias,
    )
}

private fun validateIsolationObservation(observation: ContextIsolationObservation) {
    validateUnique(
        observation.samples.map(ContextIsolationSample::requestAlias),
        coordinates(observation.boundary, field = "samples"),
    )
    observation.samples.forEach { sample ->
        sample.observedMarkers.forEach { marker ->
            validateMarker(
                marker,
                coordinates(observation.boundary, requestAlias = sample.requestAlias, field = "observedMarkers"),
            )
        }
    }
    validateCleanupProbes(observation.cleanupProbes, observation.boundary)
}

private fun validateIsolationExpectation(expectation: ContextIsolationExpectation) {
    validateUnique(
        expectation.samples.map(ContextIsolationSampleExpectation::requestAlias),
        coordinates(expectation.boundary, field = "samples"),
    )

    expectation.samples.forEach { sample ->
        val baseCoordinates = coordinates(
            boundary = expectation.boundary,
            requestAlias = sample.requestAlias,
            field = "minimumObservationCount",
            mode = sample.mode,
        )
        assertContext(sample.minimumObservationCount >= 1, baseCoordinates)

        validateMarker(
            sample.expectedMarker,
            coordinates(
                expectation.boundary,
                requestAlias = sample.requestAlias,
                field = "expectedMarker",
                mode = sample.mode,
            ),
        )
        sample.forbiddenMarkers.forEach { marker ->
            validateMarker(
                marker,
                coordinates(
                    expectation.boundary,
                    requestAlias = sample.requestAlias,
                    field = "forbiddenMarkers",
                    mode = sample.mode,
                ),
            )
        }

        when (sample.mode) {
            ContextMarkerExpectationMode.EXACT -> {
                assertContext(
                    sample.expectedMarker != null,
                    coordinates(
                        expectation.boundary,
                        requestAlias = sample.requestAlias,
                        field = "expectedMarker",
                        mode = sample.mode,
                    ),
                )
                assertContext(
                    sample.forbiddenMarkers.isEmpty(),
                    coordinates(
                        expectation.boundary,
                        requestAlias = sample.requestAlias,
                        field = "forbiddenMarkers",
                        mode = sample.mode,
                    ),
                )
            }

            ContextMarkerExpectationMode.ABSENT -> {
                assertContext(
                    sample.expectedMarker == null,
                    coordinates(
                        expectation.boundary,
                        requestAlias = sample.requestAlias,
                        field = "expectedMarker",
                        mode = sample.mode,
                    ),
                )
                assertContext(
                    sample.forbiddenMarkers.isEmpty(),
                    coordinates(
                        expectation.boundary,
                        requestAlias = sample.requestAlias,
                        field = "forbiddenMarkers",
                        mode = sample.mode,
                    ),
                )
            }

            ContextMarkerExpectationMode.NOT_IN -> {
                assertContext(
                    sample.expectedMarker == null,
                    coordinates(
                        expectation.boundary,
                        requestAlias = sample.requestAlias,
                        field = "expectedMarker",
                        mode = sample.mode,
                    ),
                )
                assertContext(
                    sample.forbiddenMarkers.isNotEmpty(),
                    coordinates(
                        expectation.boundary,
                        requestAlias = sample.requestAlias,
                        field = "forbiddenMarkers",
                        mode = sample.mode,
                    ),
                )
                validateUnique(
                    sample.forbiddenMarkers,
                    coordinates(
                        expectation.boundary,
                        requestAlias = sample.requestAlias,
                        field = "forbiddenMarkers",
                        mode = sample.mode,
                    ),
                )
            }
        }
    }

    val exactMarkers = expectation.samples
        .filter { it.mode == ContextMarkerExpectationMode.EXACT }
        .mapNotNull(ContextIsolationSampleExpectation::expectedMarker)
    validateUnique(
        exactMarkers,
        coordinates(expectation.boundary, field = "expectedMarker", mode = ContextMarkerExpectationMode.EXACT),
    )
    validateCleanupExpectations(expectation.cleanupExpectations, expectation.boundary)
}

private fun validateCleanupProbes(
    probes: List<ContextCleanupProbe>,
    boundary: ContextPropagationBoundary,
    scenario: ContextPropagationScenario? = null,
    requestAlias: ContextRequestAlias? = null,
) {
    validateUnique(
        probes.map(ContextCleanupProbe::location),
        coordinates(boundary, scenario, requestAlias, field = "cleanupProbes"),
    )
    probes.forEach { probe ->
        validateMarker(
            probe.observedMarker,
            coordinates(
                boundary,
                scenario,
                requestAlias,
                field = "observedMarker",
                location = probe.location,
            ),
        )
    }
}

private fun validateCleanupExpectations(
    expectations: List<ContextCleanupExpectation>,
    boundary: ContextPropagationBoundary,
    scenario: ContextPropagationScenario? = null,
    requestAlias: ContextRequestAlias? = null,
) {
    validateUnique(
        expectations.map(ContextCleanupExpectation::location),
        coordinates(boundary, scenario, requestAlias, field = "cleanupExpectations"),
    )
    expectations.forEach { expectation ->
        validateMarker(
            expectation.expectedMarker,
            coordinates(
                boundary,
                scenario,
                requestAlias,
                field = "expectedMarker",
                location = expectation.location,
            ),
        )
    }
}

private fun assertCleanup(
    probes: List<ContextCleanupProbe>,
    expectations: List<ContextCleanupExpectation>,
    boundary: ContextPropagationBoundary,
    scenario: ContextPropagationScenario? = null,
    requestAlias: ContextRequestAlias? = null,
) {
    val probesByLocation = probes.associateBy(ContextCleanupProbe::location)
    val expectationsByLocation = expectations.associateBy(ContextCleanupExpectation::location)
    assertContext(
        probesByLocation.keys == expectationsByLocation.keys,
        coordinates(boundary, scenario, requestAlias, field = "cleanupProbes"),
    )
    expectationsByLocation.forEach { (location, expectation) ->
        assertContext(
            probesByLocation.getValue(location).observedMarker == expectation.expectedMarker,
            coordinates(
                boundary,
                scenario,
                requestAlias,
                field = "cleanup",
                location = location,
            ),
        )
    }
}

private fun validateMarker(marker: String?, coordinates: ContextFailureCoordinates) {
    assertContext(marker != "root", coordinates)
}

private fun <T> validateUnique(values: List<T>, coordinates: ContextFailureCoordinates) {
    assertContext(values.size == values.toSet().size, coordinates)
}

private fun assertContext(condition: Boolean, coordinates: ContextFailureCoordinates) {
    if (!condition) {
        throw AssertionError(coordinates.message())
    }
}

private class ContextFailureCoordinates(
    val boundary: ContextPropagationBoundary,
    val scenario: ContextPropagationScenario?,
    val requestAlias: ContextRequestAlias?,
    val field: String,
    val mode: ContextMarkerExpectationMode?,
    val point: ContextObservationPoint?,
    val location: ContextProbeLocation?,
    val terminal: ContextPropagationTerminal?,
) {
    fun message(): String =
        buildList {
            add("Context conformance failed")
            add("boundary=$boundary")
            scenario?.let { add("scenario=$it") }
            requestAlias?.let { add("alias=$it") }
            add("field=$field")
            mode?.let { add("mode=$it") }
            point?.let { add("point=$it") }
            location?.let { add("location=$it") }
            terminal?.let { add("terminal=$it") }
            add("relation=mismatch")
            add("values redacted")
        }.joinToString(separator = "; ")
}

private fun coordinates(
    boundary: ContextPropagationBoundary,
    scenario: ContextPropagationScenario? = null,
    requestAlias: ContextRequestAlias? = null,
    field: String,
    mode: ContextMarkerExpectationMode? = null,
    point: ContextObservationPoint? = null,
    location: ContextProbeLocation? = null,
    terminal: ContextPropagationTerminal? = null,
): ContextFailureCoordinates =
    ContextFailureCoordinates(
        boundary = boundary,
        scenario = scenario,
        requestAlias = requestAlias,
        field = field,
        mode = mode,
        point = point,
        location = location,
        terminal = terminal,
    )
