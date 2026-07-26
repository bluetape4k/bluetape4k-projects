package io.bluetape4k.redis.lettuce.coordination.internal

import io.bluetape4k.redis.lettuce.script.RedisScript
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import java.util.concurrent.CompletableFuture

internal class CoordinationScriptExecutor(
    private val observer: CoordinationObserver = CoordinationObserver(),
) {
    fun <T> run(
        commands: RedisScriptingCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        dimensions: CoordinationDimensions = CoordinationDimensions.EMPTY,
        vararg args: String,
    ): T = observer.observe(CoordinationObservationName.OPERATION_OUTCOME, dimensions) {
        RedisScriptRunner.run(commands, script, outputType, keys, *args)
    }

    fun <T> runAsync(
        commands: RedisScriptingAsyncCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        dimensions: CoordinationDimensions = CoordinationDimensions.EMPTY,
        vararg args: String,
    ): CompletableFuture<T> =
        observer.observeFuture(
            CoordinationObservationName.OPERATION_OUTCOME,
            dimensions,
            RedisScriptRunner.runAsync(commands, script, outputType, keys, *args),
        )

    suspend fun <T> runSuspending(
        commands: RedisScriptingAsyncCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        dimensions: CoordinationDimensions = CoordinationDimensions.EMPTY,
        vararg args: String,
    ): T = observer.observeSuspending(CoordinationObservationName.OPERATION_OUTCOME, dimensions) {
        RedisScriptRunner.runSuspending(commands, script, outputType, keys, *args)
    }
}
