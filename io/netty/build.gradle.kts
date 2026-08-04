configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    api(libs.netty.buffer)
    api(libs.netty.all)
    compileOnly(bt4k.jctools.core)

    // Coroutines
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // NOTE: linux-x86_64 를 따로 추가해줘야 제대로 classifier가 지정된다.
    compileOnly(libs.netty.transport.classes.epoll)
    compileOnly(libs.netty.transport.classes.kqueue)

    // Netty 를 Mac M1 에서 사용하기 위한 설정
    compileOnly(libs.netty.resolver.dns.classes.macos)
}
