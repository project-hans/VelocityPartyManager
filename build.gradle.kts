import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar

plugins {
    kotlin("jvm") version "2.0.20-Beta1"
    kotlin("kapt") version "2.0.20-Beta1"
    id("com.gradleup.shadow") version "9.0.0-beta8"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.8"
    kotlin("plugin.serialization") version "2.1.10"

}

group = "ar.caes"
version = "1.0.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")


    implementation("io.javalin:javalin:6.4.0")

    // Add this so test sources can resolve Velocity classes:
    testImplementation("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")

    // Add OkHttp for HTTP calls in tests:
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Add a simple JSON library:
    testImplementation("org.json:json:20231013")
    // Test dependencies:
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("io.mockk:mockk:1.13.2")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0")
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(17)

    }
    compileKotlin {
        kotlinOptions.jvmTarget = "17"

    }
}
kotlin {
    jvmToolchain(17)
}
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}


//completely useless, but will complain if missing
val templateSource = file("src/main/templates")
val templateDest = layout.buildDirectory.dir("generated/sources/templates")

val generateTemplates by tasks.registering(Copy::class) {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    from(templateSource)
    into(templateDest)
    expand(props)
}

sourceSets["main"].java.srcDir(generateTemplates.map { it.destinationDir })
