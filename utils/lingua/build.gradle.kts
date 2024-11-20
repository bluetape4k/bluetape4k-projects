configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // https://mvnrepository.com/artifact/com.github.pemistahl/lingua
    api("com.github.pemistahl:lingua:1.2.2")

    // bluetape4k
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))
}
