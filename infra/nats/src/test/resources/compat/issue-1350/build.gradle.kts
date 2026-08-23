plugins {
    kotlin("jvm") version "2.4.0"
    application
}

group = "io.bluetape4k.compat"
version = "1.0.0"

kotlin {
    jvmToolchain(25)
}

application {
    mainClass.set("NatsFlowPublishedConsumerKt")
}

dependencies {
    implementation("io.github.bluetape4k:bluetape4k-nats:2.0.0")
}
