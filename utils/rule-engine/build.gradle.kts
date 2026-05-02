configurations {
    testImplementation.get().extendsFrom(compileOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Spring BOM (SpEL 버전 관리)
    implementation(platform(libs.spring.boot3.dependencies))
    compileOnly("org.springframework:spring-expression")

    // MVEL2
    compileOnly(libs.mvel2)

    // Janino (runtime Java expression/script compiler)
    compileOnly(libs.janino)
    compileOnly(libs.janino.commons.compiler)

    // Groovy (runtime script engine)
    compileOnly(libs.groovy)

    // Kotlin Script (jvm-host)
    compileOnly(libs.kotlin.scripting.common)
    compileOnly(libs.kotlin.scripting.jvm)
    compileOnly(libs.kotlin.scripting.jvm.host)

    // Rule Reader
    compileOnly(libs.jackson.dataformat.yaml)
    compileOnly(libs.jackson.module.kotlin)
    compileOnly(libs.typesafe.config)

    // Test
    testImplementation(libs.mvel2)
    testImplementation(libs.janino)
    testImplementation(libs.janino.commons.compiler)
    testImplementation(libs.groovy)
    testImplementation(libs.kotlin.scripting.jvm.host)
    testImplementation("org.springframework:spring-context")
    testImplementation(libs.jackson.dataformat.yaml)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.typesafe.config)
}
