configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(project(":bluetape4k-junit5"))

    implementation(libs.kotlin.reflect)

    api(bt4k.slf4j.api)
    implementation(libs.jcl.over.slf4j)
    implementation(bt4k.logback)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.slf4j)
    testImplementation(libs.kotlinx.coroutines.test)
}
