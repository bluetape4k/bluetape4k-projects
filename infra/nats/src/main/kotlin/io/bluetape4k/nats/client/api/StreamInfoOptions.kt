package io.bluetape4k.nats.client.api

import io.nats.client.api.StreamInfoOptions

inline fun streamInfoOptions(
    initializer: StreamInfoOptions.Builder.() -> Unit,
): StreamInfoOptions =
    StreamInfoOptions.builder().apply(initializer).build()

fun streamInfoOptionsOfFilterSubject(subjectsFilter: String): StreamInfoOptions =
    StreamInfoOptions.filterSubjects(subjectsFilter)

fun streamInfoOptionsOfAllSubjects(): StreamInfoOptions =
    StreamInfoOptions.allSubjects()
