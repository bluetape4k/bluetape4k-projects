package io.bluetape4k.nats.client.api

import io.nats.client.api.StreamInfoOptions

inline fun streamInfoOptions(
    @BuilderInference builder: StreamInfoOptions.Builder.() -> Unit,
): StreamInfoOptions =
    StreamInfoOptions.builder().apply(builder).build()

fun streamInfoOptionsOfFilterSubject(subjectsFilter: String): StreamInfoOptions =
    StreamInfoOptions.filterSubjects(subjectsFilter)

fun streamInfoOptionsOfAllSubjects(): StreamInfoOptions =
    StreamInfoOptions.allSubjects()
