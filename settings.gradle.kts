pluginManagement {
    repositories {
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.springframework.boot" ->
                    useModule("org.springframework.boot:spring-boot-gradle-plugin:${requested.version}")
                "io.spring.dependency-management" ->
                    useModule("io.spring.gradle:dependency-management-plugin:${requested.version}")
                "com.diffplug.spotless" ->
                    useModule("com.diffplug.spotless:spotless-plugin-gradle:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "mycyclecoach"
