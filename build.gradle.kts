plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    `maven-publish`
}

group = "com.ratger.arrowtracker"
version = "1.0.0"
val pluginName = "ArrowTracker"
val artifactBaseName = pluginName
val publicationGroupId = providers.gradleProperty("publicationGroupId").orElse("com.github.FrustratedQuim")
val publicationArtifactId = providers.gradleProperty("publicationArtifactId").orElse(pluginName)

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/") {
        content {
            includeGroup("com.github.retrooper")
        }
    }
    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.Tofaa2.EntityLib")
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.entitylib.spigot)

    compileOnly(libs.paper.api)
    compileOnly(libs.packetevents.spigot)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
}

val apiJar by tasks.registering(org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set(publicationArtifactId)
    archiveClassifier.set("")
    destinationDirectory.set(layout.buildDirectory.dir("api-libs"))
    from(sourceSets.main.get().output) {
        include("com/ratger/arrowtracker/api/**")
        exclude("com/ratger/arrowtracker/api/internal/**")
    }
}

val apiSourcesJar by tasks.registering(org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set(publicationArtifactId)
    archiveClassifier.set("sources")
    destinationDirectory.set(layout.buildDirectory.dir("api-libs"))
    from(sourceSets.main.get().allSource) {
        include("com/ratger/arrowtracker/api/**")
        exclude("com/ratger/arrowtracker/api/internal/**")
    }
}

publishing {
    publications {
        create<MavenPublication>("api") {
            groupId = publicationGroupId.get()
            artifactId = publicationArtifactId.get()
            artifact(apiJar)
            artifact(apiSourcesJar)
            pom {
                name.set(publicationArtifactId)
                description.set("Public API for the ArrowTracker Paper plugin")
            }
        }
    }
}

tasks {
    compileKotlin {
        exclude("**/*Test.kt")
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(libs.versions.java.get()))
            freeCompilerArgs.add("-Xno-param-assertions")
            freeCompilerArgs.add("-Xno-call-assertions")
            freeCompilerArgs.add("-Xno-receiver-assertions")
        }
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(libs.versions.java.get().toInt())
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "pluginName" to pluginName,
            "version" to project.version
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveBaseName.set(artifactBaseName)
        archiveClassifier.set("")
        relocate("kotlin", "${project.group}.lib.kotlin")
        relocate("me.tofaa.entitylib", "${project.group}.lib.entitylib")
    }

    jar {
        enabled = false
    }

    assemble {
        dependsOn(shadowJar)
    }

    build {
        dependsOn(shadowJar)
    }
}
