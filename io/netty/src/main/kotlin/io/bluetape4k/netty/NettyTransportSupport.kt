package io.bluetape4k.netty

import io.bluetape4k.support.classIsPresent


/**
 * Netty Transport 시 Unix 시스템에서 사용할 수 있는 netty-transport-native-epoll 라이브러리를 참조하는가?
 *
 * `io.netty.channel.epoll.EpollEventLoopGroup` class가 존재하고, Unix 시스템이라면 EpollEventLoopGroup을 사용하는 것이 성능에 유리합니다.
 * @return Boolean
 */
fun isPresentNettyTransportNativeEpoll(): Boolean =
    classIsPresent("io.netty.channel.epoll.EpollEventLoopGroup")

/**
 * Netty Transport 시 Mac 시스템에서 사용할 수 있는 netty-transport-native-kqueue 라이브러리를 참조하는가?
 *
 * `io.netty.channel.kqueue.KQueueEventLoopGroup` class가 존재하고, Mac 시스템이라면 KQueueEventLoopGroup 사용하는 것이 성능에 유리합니다.
 * @return Boolean
 */
fun isPresentNettyTransportNativeKQueue(): Boolean =
    classIsPresent("io.netty.channel.kqueue.KQueueEventLoopGroup")
