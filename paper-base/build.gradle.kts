plugins {
    alias(libs.plugins.shadow)
}

repositories {
    maven("https://repo.lucko.me/") {
        name = "lucko-repo"
    }
}

dependencies {
    implementation(project(":common"))

    compileOnly("io.papermc.paper:paper-api:1.21.9-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.5-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()

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
