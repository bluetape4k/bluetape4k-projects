configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.junit_bom))
    
    api(project(":bluetape4k-logging"))

    api(Libs.kotlin_test_junit5)

    api(Libs.junit_jupiter)
    api(Libs.junit_jupiter_engine)
    api(Libs.junit_jupiter_params)
    api(Libs.junit_platform_launcher)
    compileOnly(Libs.junit_jupiter_migrationsupport)

    api(Libs.kluent)
    api(Libs.mockk)
    api(Libs.awaitility_kotlin)

    api(Libs.datafaker)
    api(Libs.java_uuid_generator)
    api(Libs.random_beans)

    api(Libs.commons_lang3)
    implementation(Libs.logback)

    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_debug)
    implementation(Libs.kotlinx_coroutines_test)

    implementation(Libs.eclipse_collections)
    testImplementation(Libs.eclipse_collections_testutils)
}
