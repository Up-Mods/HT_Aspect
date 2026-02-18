import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.text.SimpleDateFormat
import java.util.*

plugins {
    `maven-publish`
    alias(libs.plugins.hytale)
    alias(libs.plugins.shadow)
}

val javaVersion = 25
val buildTime = Date()
val buildNumber: Int = providers.environmentVariable("BUILD_NUMBER").map { it.toInt() }.orElse(0).get()
val runningOnCI = providers.environmentVariable("CI").orNull.toBoolean()

group = "dev.upcraft.ht"
version = SimpleDateFormat("YY.M.d").format(buildTime)

hytale {

}

repositories {
    mavenCentral()
    maven("https://maven.hytale-mods.dev/releases")
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.jspecify)

    shadow(libs.guava)
    implementation(libs.guava)

    // this mod is optional, but is included so you can preview your mod icon
    // in the in-game mod list via the /modlist command
    runtimeOnly(libs.bettermodlist)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }

    withSourcesJar()
}

tasks.named<ProcessResources>("processResources") {
    var replaceProperties = mapOf(
        "plugin_group" to findProperty("plugin_group"),
        "plugin_maven_group" to project.group,
        "plugin_name" to project.name,
        "plugin_version" to "${project.version}+${buildNumber}",
        "server_version" to findProperty("server_version"),

        "plugin_description" to findProperty("plugin_description"),
        "plugin_website" to findProperty("plugin_website"),

        "plugin_main_entrypoint" to findProperty("plugin_main_entrypoint")
    )

    filesMatching("manifest.json") {
        expand(replaceProperties)
    }

    inputs.properties(replaceProperties)
}

tasks.withType<Jar> {
    manifest {
        attributes["Specification-Title"] = rootProject.name
        attributes["Specification-Version"] = version
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] =
            providers.environmentVariable("COMMIT_SHA_SHORT")
                .map { "${version}-${it}" }
                .getOrElse(version.toString())
    }
}

tasks.jar {
    archiveClassifier = "slim"
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier = null
    configurations = project.configurations.shadow.map { listOf(it) }
}

tasks.assemble.configure {
    dependsOn(shadowJar)
}

publishing {
    repositories {
        maven("https://maven.hytale-mods.dev/releases") {
            credentials {
                username = providers.environmentVariable("HM_RELEASES_MAVEN_USER").orNull
                password = providers.environmentVariable("HM_RELEASES_MAVEN_TOKEN").orNull
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            suppressPomMetadataWarningsFor("runtimeElements")
        }
    }
}

// IDEA no longer automatically downloads sources/javadoc jars for dependencies, so we need to explicitly enable the behavior.
idea {
    module {
        isDownloadSources = !runningOnCI
        isDownloadJavadoc = !runningOnCI
    }
}
