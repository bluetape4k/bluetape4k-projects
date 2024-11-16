package io.bluetape4k.concurrent.virtualthread

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Kotlin Coroutines 에서 Virtual Thread를 사용하기 위한 Dispatcher
 * Virtual thread를 사용하는 [ExecutorService]를 사용하여 반환
 */
val Dispatchers.VT: CoroutineDispatcher
    get() = VirtualThreadExecutor.asCoroutineDispatcher()

/**
 * Kotlin Coroutines 에서 Virtual Thread를 사용하기 위한 Dispatcher
 * Virtual thread를 사용하는 새로운 [ExecutorService] 를 생성하여 반환
 */
val Dispatchers.newVT: CoroutineDispatcher
    get() = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
