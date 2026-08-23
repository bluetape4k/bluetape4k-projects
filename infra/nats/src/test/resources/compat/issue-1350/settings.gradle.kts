pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        val compatRepository = providers.gradleProperty("compatRepository")
        if (compatRepository.isPresent) {
            maven { url = uri(compatRepository.get()) }
        }
        mavenCentral()
    }
}

rootProject.name = "issue-1350-published-consumer"
