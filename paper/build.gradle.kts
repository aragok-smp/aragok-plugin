plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":common"))

    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

tasks.shadowJar {
    archiveBaseName.set("aragok-paper")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}
