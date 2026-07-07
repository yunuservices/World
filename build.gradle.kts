import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

group = "io.yunuservices"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://maven.canvasmc.io/releases") {
        name = "canvas-releases"
    }
    maven("https://maven.canvasmc.io/snapshots") {
        name = "canvas-snapshots"
    }
}

dependencies {
    compileOnly("io.canvasmc.canvas:canvas-api:26.1.2.build.836-stable")
    compileOnly("net.kyori:adventure-text-minimessage:5.2.0")
    implementation("org.incendo:cloud-paper:2.0.0-beta.17")
    implementation("org.tomlj:tomlj:1.1.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    options.release.set(25)
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    filteringCharset = Charsets.UTF_8.name()
    filesMatching("paper-plugin.yml") {
        expand(
            "version" to project.version,
        )
    }
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.withType<ShadowJar>().configureEach {
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
