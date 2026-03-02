// NOTE: implementation 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-jackson"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_mongodb)

    // MongoDB Kotlin Coroutine Driver (네이티브 suspend/Flow 지원)
    api(Libs.mongodb_driver_kotlin_coroutine)
    // MongoDB Kotlin Extensions (KProperty 기반 Filters/Sorts/Updates/Projections DSL)
    api(Libs.mongodb_driver_kotlin_extensions)
    // BSON Kotlin 지원
    api(Libs.mongo_bson_kotlin)
    // kotlinx.serialization BSON 코덱 (선택적)
    compileOnly(Libs.mongo_bson_kotlinx)

    // Coroutines
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
