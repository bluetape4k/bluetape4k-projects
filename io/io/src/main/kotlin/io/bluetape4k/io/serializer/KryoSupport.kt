package io.bluetape4k.io.serializer

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.CompletableFuture

/**
 * Kryo 를 이용한 작업을 함수로 표현합니다.
 * Kryo 가 thread-safe 하지 않기 때문에 풀(Pool)에서 인스턴스를 대여한 후 작업하고 반환합니다.
 *
 * ```kotlin
 * val bytes: ByteArray = withKryo {
 *     val output = Output(1024)
 *     writeClassAndObject(output, "Hello, Kryo!")
 *     output.toBytes()
 * }
 * ```
 *
 * @param T 반환 수형
 * @param func [Kryo] 인스턴스를 수신자로 실행할 작업 블록
 * @return 작업 결과
 */
inline fun <T> withKryo(func: Kryo.() -> T): T {
    val kryo = KryoProvider.obtainKryo()
    return try {
        func(kryo)
    } finally {
        KryoProvider.releaseKryo(kryo)
    }
}

/**
 * Kryo [Output]을 풀(Pool)에서 대여하여 작업한 후 반환합니다.
 * 작업 완료 또는 예외 발생 시 [Output]은 자동으로 풀에 반환됩니다.
 *
 * ```kotlin
 * val bytes: ByteArray = withKryoOutput { output ->
 *     output.reset()
 *     withKryo { writeObject(output, "Hello, Kryo!") }
 *     output.toBytes()
 * }
 * ```
 *
 * @param T 반환 수형
 * @param func [Output]을 인자로 받아 실행할 작업 블록
 * @return 작업 결과
 */
inline fun <T> withKryoOutput(func: (output: Output) -> T): T {
    val output = KryoProvider.obtainOutput()
    return try {
        func(output)
    } finally {
        KryoProvider.releaseOutput(output)
    }
}

/**
 * Kryo [Input]을 풀(Pool)에서 대여하여 작업한 후 반환합니다.
 * 작업 완료 또는 예외 발생 시 [Input]은 자동으로 풀에 반환됩니다.
 *
 * ```kotlin
 * val bytes: ByteArray = withKryoOutput { output ->
 *     output.reset()
 *     withKryo { writeObject(output, "Hello, Kryo!") }
 *     output.toBytes()
 * }
 * val text: String = withKryoInput { input ->
 *     input.setBuffer(bytes)
 *     withKryo { readObject(input, String::class.java) }
 * }  // text="Hello, Kryo!"
 * ```
 *
 * @param T 반환 수형
 * @param func [Input]을 인자로 받아 실행할 작업 블록
 * @return 작업 결과
 */
inline fun <T> withKryoInput(
    func: (input: Input) -> T,
): T {
    val input = KryoProvider.obtainInput()
    return try {
        func(input)
    } finally {
        KryoProvider.releaseInput(input)
    }
}

/**
 * Kryo 를 이용한 비동기 작업을 [CompletableFuture]로 반환합니다.
 * Kryo 가 thread-safe 하지 않으므로 풀에서 인스턴스를 대여하고, 작업 완료 후 자동 반환합니다.
 *
 * ```kotlin
 * val future: CompletableFuture<ByteArray?> = withKryoAsync {
 *     val output = Output(1024)
 *     writeClassAndObject(output, "Hello, Async Kryo!")
 *     output.toBytes()
 * }
 * val bytes = future.get()  // 비동기 결과 획득
 * ```
 *
 * @param T 반환 수형
 * @param func [Kryo] 인스턴스를 수신자로 비동기 실행할 작업 블록
 * @return 작업 결과를 담은 [CompletableFuture]
 */
inline fun <T: Any> withKryoAsync(
    crossinline func: Kryo.() -> T?,
): CompletableFuture<T?> {
    val kryo = KryoProvider.obtainKryo()
    return CompletableFuture.supplyAsync { func(kryo) }
        .whenCompleteAsync { _, _ ->
            KryoProvider.releaseKryo(kryo)
        }
}

/**
 * Coroutines 환경에서 Kryo 작업을 수행합니다.
 * Kryo 가 thread-safe 하지 않으므로 풀에서 인스턴스를 대여하고, 작업 완료 후 자동 반환합니다.
 *
 * ```kotlin
 * val bytes: ByteArray? = withKryoSuspending {
 *     val output = Output(1024)
 *     writeClassAndObject(output, "Hello, Suspend Kryo!")
 *     output.toBytes()
 * }
 * val text: String? = withKryoSuspending {
 *     val input = Input(bytes!!)
 *     readClassAndObject(input) as String
 * }  // text="Hello, Suspend Kryo!"
 * ```
 *
 * @param T 결과 수형
 * @param func [Kryo] 인스턴스를 수신자로 실행할 suspend 작업 블록
 * @return 작업 결과, 또는 null
 */
suspend inline fun <T: Any> withKryoSuspending(
    crossinline func: suspend Kryo.() -> T?,
): T? = coroutineScope {
    val kryo = KryoProvider.obtainKryo()

    try {
        func(kryo)
    } finally {
        KryoProvider.releaseKryo(kryo)
    }
}
