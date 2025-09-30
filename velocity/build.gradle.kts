plugins {
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":common"))

    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()

        relocate("kotlin", "thirdparty.io.d2a.ara.velocity.kotlin")

        minimize()
    }

    build {
        dependsOn(shadowJar)
    }

    jar {
        enabled = false
    }
}
