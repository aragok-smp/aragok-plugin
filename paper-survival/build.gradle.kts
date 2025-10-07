import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":common"))
    compileOnly(project(":paper-base", configuration = "shadow"))

    compileOnly("io.papermc.paper:paper-api:1.21.9-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }

    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()

        // relocate("kotlin", "thirdparty.io.d2a.ara.paper.survival.kotlin")
        exclude("kotlin/**")

        minimize()
    }

    build {
        dependsOn(shadowJar)
    }

    jar {
        enabled = false
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
