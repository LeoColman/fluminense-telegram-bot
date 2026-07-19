import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
    id("com.bmuschko.docker-remote-api") version "9.3.0"
}

group = "br.com.colman"
version = "1.9.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.0.7")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.code.gson:gson:2.9.0")

    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    archiveVersion.set("")
}

application {
    mainClass.set("br.com.colman.bot.FluminenseTelegramBotKt")
}

tasks.register<DockerBuildImage>("buildDockerImage") {
    inputDir.set(file("."))
    images.add("leocolman/fluminense-telegram-bot:${version}")
    images.add("leocolman/fluminense-telegram-bot:latest")
}

tasks.register<DockerPushImage>("pushDockerImage") {
    dependsOn("buildDockerImage")
    images.add("leocolman/fluminense-telegram-bot:${version}")
    images.add("leocolman/fluminense-telegram-bot:latest")

    registryCredentials {
        username.set(System.getenv("DOCKER_USERNAME"))
        password.set(System.getenv("DOCKER_PASSWORD"))
    }
}