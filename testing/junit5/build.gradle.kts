configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(platform(Libs.junit_bom))
    
    api(project(":bluetape4k-logging"))

    api(Libs.kotlin_test_junit5)

    api(Libs.junit_jupiter)
    api(Libs.junit_jupiter_engine)
    api(Libs.junit_jupiter_params)
    compileOnly(Libs.junit_jupiter_migrationsupport)
    api(Libs.junit_platform_launcher)
    runtimeOnly(Libs.junit_platform_runner)

    api(Libs.kluent)
    api(Libs.mockk)
    api(Libs.awaitility_kotlin)

    api(Libs.datafaker)
    api(Libs.java_uuid_generator)
    api(Libs.random_beans)

    api(Libs.commons_lang3)
    compileOnly(Libs.logback)

    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_debug)
    compileOnly(Libs.kotlinx_coroutines_test)
}
