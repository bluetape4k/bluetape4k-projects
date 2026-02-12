package io.bluetape4k.io.serializer

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.CompletableFuture

/**
 * Kryo 를 이용한 작업을 함수로 표현
 * Kryo 가 thread-safe 하지 않기 때문에 이 함수를 사용해야 합니다.
 *
 * ```
 * val result = withKryo {
 *    val kryo = this
 *    kryo.register(MyClass::class.java)
 *    kryo.readObject(input, MyClass::class.java)
 *    // do something
 * }
 */
inline fun <T> withKryo(@BuilderInference func: Kryo.() -> T): T {
    val kryo = KryoProvider.obtainKryo()
    return try {
        func(kryo)
    } finally {
        KryoProvider.releaseKryo(kryo)
    }
}

/**
 * Kryo Ouptut을 Pool 에서 받아서 작업 후 반환합니다.
 */
inline fun <T> withKryoOutput(@BuilderInference func: (output: Output) -> T): T {
    val output = KryoProvider.obtainOutput()
    return try {
        func(output)
    } finally {
        KryoProvider.releaseOutput(output)
    }
}

/**
 * Kryo Input을 Pool 에서 받아서 작업 후 반환합니다.
 */
inline fun <T> withKryoInput(
    @BuilderInference func: (input: Input) -> T,
): T {
    val input = KryoProvider.obtainInput()
    return try {
        func(input)
    } finally {
        KryoProvider.releaseInput(input)
    }
}

/**
 * Kryo 를 이용한 비동기 작업을 함수로 표현
 * Kryo 가 thread-safe 하지 않기 때문에 이 함수를 사용해야 합니다.
 */
inline fun <T: Any> withKryoAsync(
    @BuilderInference crossinline func: Kryo.() -> T?,
): CompletableFuture<T?> {
    val kryo = KryoProvider.obtainKryo()
    return CompletableFuture.supplyAsync { func(kryo) }
        .whenCompleteAsync { _, _ ->
            KryoProvider.releaseKryo(kryo)
        }
}

/**
 * Coroutines 환경 하에서 Kryo 작업을 수행합니다.
 *
 * @param T 결과 수형
 * @param func [Kryo] 로 작업하는 함수
 * @return 작업 결과
 */
suspend inline fun <T: Any> withKryoSuspending(
    @BuilderInference crossinline func: suspend Kryo.() -> T?,
): T? = coroutineScope {
    val kryo = KryoProvider.obtainKryo()

    try {
        func(kryo)
    } finally {
        KryoProvider.releaseKryo(kryo)
    }
}
