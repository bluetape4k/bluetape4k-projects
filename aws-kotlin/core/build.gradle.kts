@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(Libs.aws_kotlin_aws_core)
    api(Libs.aws_kotlin_aws_config)
    api(Libs.aws_kotlin_aws_endpoint)

    testImplementation(project(":bluetape4k-aws-kotlin-tests"))

    // bluetape4k
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
