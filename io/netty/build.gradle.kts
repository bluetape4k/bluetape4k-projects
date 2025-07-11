configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    api(Libs.netty_buffer)
    api(Libs.netty_all)
    compileOnly(Libs.jctools_core)

    // Coroutines
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // NOTE: linux-x86_64 를 따로 추가해줘야 제대로 classifier가 지정된다.
    compileOnly(Libs.netty_transport_classes_epoll)
    compileOnly(Libs.netty_transport_classes_kqueue)

    // Netty 를 Mac M1 에서 사용하기 위한 설정
    compileOnly(Libs.netty_resolver_dns_classes_macos)
}
