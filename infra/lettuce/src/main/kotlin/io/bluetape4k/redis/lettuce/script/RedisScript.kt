package io.bluetape4k.redis.lettuce.script

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.awaitSuspending
import io.lettuce.core.RedisNoScriptException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * 재사용되는 Redis Lua 스크립트를 표현합니다.
 *
 * 스크립트 원문과 로컬에서 계산된 SHA1 해시를 함께 보관하여, 런타임에 매번 원문을 전송하는 대신
 * `EVALSHA` 로 실행하고 [RedisNoScriptException] 이 발생하면 자동으로 원문 전송(`EVAL`) 으로
 * 재시도할 수 있게 합니다.
 *
 * Redis 는 서버에 저장하는 스크립트의 키를 `SHA1(source_bytes)` 로 계산하므로 클라이언트에서 미리
 * 계산한 해시를 그대로 사용할 수 있습니다.
 *
 * @property source Lua 스크립트 원문
 */
class RedisScript(val source: String) {
    /** `source` 의 SHA1 16진 문자열 (Redis 의 SCRIPT LOAD 결과와 동일) */
    val sha1: String = sha1Hex(source)

    companion object: KLogging() {
        private fun sha1Hex(text: String): String {
            val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray(Charsets.UTF_8))
            val sb = StringBuilder(digest.size * 2)
            for (b in digest) {
                val v = b.toInt() and 0xff
                sb.append(Character.forDigit(v ushr 4, 16))
                sb.append(Character.forDigit(v and 0x0f, 16))
            }
            return sb.toString()
        }
    }
}

/**
 * 동기/비동기/코루틴 모두에서 재사용 가능한 Lua 스크립트 실행 도우미입니다.
 *
 * 개선: 기존 구현들은 매 호출마다 스크립트 원문을 전송했습니다. 같은 스크립트를 수십~수천 번
 * 실행할 때 네트워크 대역폭·파싱 비용·Redis 스크립트 캐시 lookup 비용이 모두 낭비됩니다.
 * `EVALSHA` 는 20 바이트 SHA1 만 전송하므로 핫 패스에서 훨씬 효율적입니다.
 */
object RedisScriptRunner: KLogging() {

    /** 동기: `EVALSHA` 우선, NOSCRIPT 시 원문 전송으로 fallback. */
    fun <T> run(
        commands: RedisCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        vararg args: String,
    ): T = runScripting(commands, script, outputType, keys, *args)

    /** Runs the script through a cluster-compatible synchronous scripting interface. */
    fun <T> run(
        commands: RedisScriptingCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        vararg args: String,
    ): T = runScripting(commands, script, outputType, keys, *args)

    private fun <T> runScripting(
        commands: RedisScriptingCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        vararg args: String,
    ): T {
        return try {
            commands.evalsha<T>(script.sha1, outputType, keys, *args)
        } catch (_: RedisNoScriptException) {
            log.debug { "NOSCRIPT → 원문 전송 fallback (sha1=${script.sha1})" }
            commands.eval<T>(script.source, outputType, keys, *args)
        }
    }

    /** 비동기: `EVALSHA` 우선, NOSCRIPT 시 원문 전송 fallback 한 [CompletableFuture]. */
    fun <T> runAsync(
        commands: RedisAsyncCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        vararg args: String,
    ): CompletableFuture<T> = runAsyncScripting(commands, script, outputType, keys, *args)

    /** Runs the script through a cluster-compatible asynchronous scripting interface. */
    fun <T> runAsync(
        commands: RedisScriptingAsyncCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        vararg args: String,
    ): CompletableFuture<T> = runAsyncScripting(commands, script, outputType, keys, *args)

    private fun <T> runAsyncScripting(
        commands: RedisScriptingAsyncCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        vararg args: String,
    ): CompletableFuture<T> {
        val future = commands.evalsha<T>(script.sha1, outputType, keys, *args).toCompletableFuture()
        return future.exceptionallyCompose { error ->
            val cause = if (error is CompletionException) error.cause ?: error else error
            if (cause is RedisNoScriptException) {
                log.debug { "NOSCRIPT(async) → 원문 전송 fallback (sha1=${script.sha1})" }
                commands.eval<T>(script.source, outputType, keys, *args).toCompletableFuture()
            } else {
                CompletableFuture.failedFuture(cause)
            }
        }
    }

    /** 코루틴: `EVALSHA` 우선, NOSCRIPT 시 원문 전송 fallback. */
    suspend fun <T> runSuspending(
        commands: RedisAsyncCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        vararg args: String,
    ): T = runSuspendingScripting(commands, script, outputType, keys, *args)

    /** Runs the script suspending through a cluster-compatible asynchronous scripting interface. */
    suspend fun <T> runSuspending(
        commands: RedisScriptingAsyncCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        vararg args: String,
    ): T = runSuspendingScripting(commands, script, outputType, keys, *args)

    private suspend fun <T> runSuspendingScripting(
        commands: RedisScriptingAsyncCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        vararg args: String,
    ): T {
        return try {
            commands.evalsha<T>(script.sha1, outputType, keys, *args).awaitSuspending()
        } catch (_: RedisNoScriptException) {
            log.debug { "NOSCRIPT(suspend) → 원문 전송 fallback (sha1=${script.sha1})" }
            commands.eval<T>(script.source, outputType, keys, *args).awaitSuspending()
        }
    }
}
