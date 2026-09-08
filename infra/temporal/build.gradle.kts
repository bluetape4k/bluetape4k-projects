dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)
    api(bt4k.temporal.sdk)
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-assertions"))
    testImplementation(libs.kotlinx.coroutines.test)
    api(platform(bt4k.temporal.bom))
    api(bt4k.temporal.kotlin)
    testImplementation(bt4k.temporal.testing)
}
