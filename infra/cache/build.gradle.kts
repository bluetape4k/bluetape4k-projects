configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-cache-core"))
    implementation(project(":bluetape4k-cache-hazelcast"))
    implementation(project(":bluetape4k-cache-ignite2"))
    implementation(project(":bluetape4k-cache-lettuce"))
    implementation(project(":bluetape4k-cache-redisson"))
}
