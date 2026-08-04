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
    implementation(platform(bt4k.spring.boot4.dependencies))
    compileOnly("org.springframework:spring-expression")

    // MVEL2
    compileOnly(bt4k.mvel2)

    // Janino (runtime Java expression/script compiler)
    compileOnly(bt4k.janino)
    compileOnly(bt4k.janino.commons.compiler)

    // Groovy (runtime script engine)
    compileOnly(bt4k.groovy)

    // Kotlin Script (jvm-host)
    compileOnly(libs.kotlin.scripting.common)
    compileOnly(libs.kotlin.scripting.jvm)
    compileOnly(libs.kotlin.scripting.jvm.host)

    // Rule Reader
    compileOnly(libs.jackson.dataformat.yaml)
    compileOnly(libs.jackson.module.kotlin)
    compileOnly(bt4k.typesafe.config)

    // Test
    testImplementation(bt4k.mvel2)
    testImplementation(bt4k.janino)
    testImplementation(bt4k.janino.commons.compiler)
    testImplementation(bt4k.groovy)
    testImplementation(libs.kotlin.scripting.jvm.host)
    testImplementation("org.springframework:spring-context")
    testImplementation(libs.jackson.dataformat.yaml)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(bt4k.typesafe.config)
}
