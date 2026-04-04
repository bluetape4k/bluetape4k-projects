configurations {
    testImplementation.get().extendsFrom(compileOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Spring BOM (SpEL 버전 관리)
    implementation(platform(Libs.spring_boot3_dependencies))
    compileOnly("org.springframework:spring-expression")

    // MVEL2
    compileOnly(Libs.mvel2)

    // Kotlin Script (jvm-host)
    compileOnly(Libs.kotlin_scripting_common)
    compileOnly(Libs.kotlin_scripting_jvm)
    compileOnly(Libs.kotlin_scripting_jvm_host)

    // Rule Reader
    compileOnly(Libs.jackson_dataformat_yaml)
    compileOnly(Libs.jackson_module_kotlin)
    compileOnly(Libs.typesafe_config)

    // Test
    testImplementation(Libs.mvel2)
    testImplementation(Libs.kotlin_scripting_jvm_host)
    testImplementation("org.springframework:spring-context")
    testImplementation(Libs.jackson_dataformat_yaml)
    testImplementation(Libs.jackson_module_kotlin)
    testImplementation(Libs.typesafe_config)
}
