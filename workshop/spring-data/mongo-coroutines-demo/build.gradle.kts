plugins {
    kotlin("plugin.spring")
    kotlin("kapt")
    id(Plugins.spring_boot)
}

springBoot {
    mainClass.set("healingpaper.bluetape4k.workshop.mongo.MongoApplicationKt")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(Libs.springBootStarter("actuator"))
    implementation(Libs.springBootStarter("aop"))
    implementation(Libs.springBootStarter("webflux"))
    implementation(Libs.springBootStarter("validation"))

    implementation(Libs.springBootStarter("data-mongodb"))
    implementation(Libs.springBootStarter("data-mongodb-reactive"))

    runtimeOnly(Libs.springBoot("devtools"))
    kapt(Libs.springBoot("configuration-processor"))

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // Mongo Driver
    implementation(Libs.mongodb_driver_sync)
    implementation(Libs.mongodb_driver_reactivestreams)

    // MongoDB Testcontainers
    implementation(project(":bluetape4k-testcontainers"))
    implementation(Libs.testcontainers)
    implementation(Libs.testcontainers_mongodb)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)
    testImplementation(Libs.turbine)

    implementation(project(":bluetape4k-jackson"))
    implementation(project(":bluetape4k-idgenerators"))
    implementation(project(":bluetape4k-netty"))
    testImplementation(project(":bluetape4k-junit5"))
}
