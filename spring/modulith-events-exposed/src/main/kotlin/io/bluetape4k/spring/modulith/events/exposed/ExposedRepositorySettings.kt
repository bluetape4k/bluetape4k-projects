package io.bluetape4k.spring.modulith.events.exposed

import org.springframework.modulith.events.support.CompletionMode

data class ExposedRepositorySettings(
    val schema: String? = null,
    val completionMode: CompletionMode = CompletionMode.UPDATE,
) {

    val isDeleteCompletion: Boolean get() = completionMode == CompletionMode.DELETE
    val isArchiveCompletion: Boolean get() = completionMode == CompletionMode.ARCHIVE
    val isUpdateCompletion: Boolean get() = completionMode == CompletionMode.UPDATE
}
