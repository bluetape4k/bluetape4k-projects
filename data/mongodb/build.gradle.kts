// NOTE: implementation 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-jackson2"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.mongodb)

    // MongoDB Kotlin Coroutine Driver (네이티브 suspend/Flow 지원)
    api(libs.mongodb.driver.kotlin.coroutine)
    // MongoDB Kotlin Extensions (KProperty 기반 Filters/Sorts/Updates/Projections DSL)
    api(libs.mongodb.driver.kotlin.extensions)
    // BSON Kotlin 지원
    api(libs.mongo.bson.kotlin)
    // kotlinx.serialization BSON 코덱 (선택적)
    compileOnly(libs.mongo.bson.kotlinx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
