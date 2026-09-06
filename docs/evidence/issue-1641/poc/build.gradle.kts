plugins {
    kotlin("jvm") version "2.4.10"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.fory:fory-json-kotlin:1.7.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.2")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.65")
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass = "Issue1641EvaluationKt"
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--add-opens=java.base/java.lang.invoke=ALL-UNNAMED")
}
