plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.andy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("org.andy.musicque")
    mainClass.set("org.andy.musicque.Launcher")
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing", "javafx.media")
}

dependencies {
    // Thêm thư viện Kotlin Standard Library
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    implementation("org.controlsfx:controlsfx:11.2.1")
    implementation("com.dlsc.formsfx:formsfx-core:11.6.0") {
        exclude(group = "org.openjfx")
    }
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.bootstrapfx:bootstrapfx-core:0.4.0")
    implementation("eu.hansolo:tilesfx:21.0.9") {
        exclude(group = "org.openjfx")
    }
    implementation("com.github.almasb:fxgl:17.3") {
        exclude(group = "org.openjfx")
        // Dòng exclude("org.jetbrains.kotlin") đã bị xóa
    }
    implementation("org.openjfx:javafx-media:21.0.6")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("com.google.guava:guava:33.0.0-jre")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    implementation("org.openjfx:javafx-controls:21.0.6")
    implementation("org.openjfx:javafx-fxml:21.0.6")
    implementation("org.openjfx:javafx-web:21.0.6")
    implementation("org.openjfx:javafx-media:21.0.6")
    implementation("org.openjfx:javafx-swing:21.0.6")
}

tasks.withType<Test> {
    useJUnitPlatform()
}