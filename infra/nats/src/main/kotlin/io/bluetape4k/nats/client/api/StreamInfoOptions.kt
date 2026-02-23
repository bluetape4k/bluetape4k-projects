package io.bluetape4k.nats.client.api

import io.bluetape4k.support.requireNotBlank
import io.nats.client.api.StreamInfoOptions

inline fun streamInfoOptions(
    @BuilderInference builder: StreamInfoOptions.Builder.() -> Unit,
): StreamInfoOptions =
    StreamInfoOptions.builder().apply(builder).build()

fun streamInfoOptionsOfFilterSubject(subjectsFilter: String): StreamInfoOptions {
    subjectsFilter.requireNotBlank("subjectsFilter")

    return StreamInfoOptions.filterSubjects(subjectsFilter)
}

fun streamInfoOptionsOfAllSubjects(): StreamInfoOptions =
    StreamInfoOptions.allSubjects()
