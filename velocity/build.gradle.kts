plugins {
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":common"))

    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

tasks.shadowJar {
    archiveBaseName.set("aragok-velocity")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}