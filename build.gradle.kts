import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
    java
    jacoco
}

group = "com.mycyclecoach"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val jacocoExclusions = listOf(
    "com/mycyclecoach/Main.class",
    "com/mycyclecoach/config/**",
    "com/mycyclecoach/infrastructure/**",
)

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(jacocoExclusions)
        },
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val coverageVerification = tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn(tasks.jacocoTestReport)
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(jacocoExclusions)
        },
    )
    executionData.setFrom(tasks.jacocoTestReport.map { it.executionData })
    violationRules {
        rule {
            limit {
                setCounter("LINE")
                setValue("COVEREDRATIO")
                setMinimum("0.80".toBigDecimal())
            }
        }
    }
}

tasks.named("check") {
    dependsOn(coverageVerification)
}

spotless {
    java {
        palantirJavaFormat("2.44.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
