import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    id("eclipse")
    id("idea")
    id("maven-publish")
    id("net.neoforged.gradle") version "[6.0.13, 6.2)"
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")  // MixinExtras, Fabric ASM
    maven("https://maven.jamieswhiteshirt.com/libs-release")  // Reach Entity Attributes
    maven("https://api.modrinth.com/maven")  // LazyDFU
    maven("https://maven.createmod.net")  // Create Forge, Flywheel, Ponder
    maven("https://maven.ithundxr.dev/mirror") // Registrate
    maven("https://maven.theillusivec4.top/")  // Curios
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://maven.blamejared.com/")  // JEI
    maven("https://squiddev.cc/maven/")  // CC: Tweaked
}


val mod_version: String by project
val minecraft_version: String by project
val maven_group: String by project
val archives_base_name: String by project
val mod_id: String by project

val forge_version: String by project
val forge_version_major: String by project
val forge_kotlin_version: String by rootProject
val create_version: String by rootProject
val create_version_short: String by project
val flywheel_version: String by rootProject
val registrate_version: String by rootProject
val ponder_version: String by rootProject

val ktor_version: String by rootProject
val kotlin_json_version: String by rootProject
val kotlin_css_version: String by rootProject
val kotlin_io_version: String by rootProject

version = mod_version
group = maven_group

val archives_version = "$mod_version+mc$minecraft_version-neoforge"

val shade: Configuration = configurations.create("shade")
configurations.getByName("implementation").extendsFrom(shade)
configurations.getByName("minecraftLibrary").extendsFrom(shade)

dependencies {
    minecraft("net.neoforged:forge:${minecraft_version}-${forge_version}")
    implementation("thedarkcolour:kotlinforforge:$forge_kotlin_version")

    shade("io.ktor:ktor-server-cio-jvm:$ktor_version")
    shade("io.ktor:ktor-server-core-jvm:$ktor_version")
    shade("io.ktor:ktor-server-cors-jvm:$ktor_version")
    shade("org.jetbrains.kotlinx:kotlinx-io-core-jvm:$kotlin_io_version")
    shade("org.jetbrains.kotlinx:kotlinx-io-bytestring-jvm:$kotlin_io_version")
    shade("org.jetbrains.kotlin-wrappers:kotlin-css-jvm:$kotlin_css_version") {
        isTransitive = false
    }

    // included in Kotlin for Forge
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlin_json_version")


    implementation(fg.deobf("com.simibubi.create:create-${minecraft_version}:${create_version}:slim"))
    implementation(fg.deobf("net.createmod.ponder:Ponder-Forge-${minecraft_version}:${ponder_version}"))
    compileOnly(fg.deobf("dev.engine-room.flywheel:flywheel-forge-api-${minecraft_version}:${flywheel_version}"))
    runtimeOnly(fg.deobf("dev.engine-room.flywheel:flywheel-forge-${minecraft_version}:${flywheel_version}"))
    implementation(fg.deobf("com.tterrag.registrate:Registrate:${registrate_version}"))

    // Janky but gradle has forced my hand.
    val annotations = annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1")
    if (annotations != null) {
        compileOnly(annotations)
    }
    implementation("io.github.llamalad7:mixinextras-forge:0.4.1")
}

minecraft {
    mappings("official", minecraft_version)

    runs {
        // applies to all the run configs below
        configureEach {
            // Recommended logging data for a userdev environment
            // The markers can be added/remove as needed separated by commas.
            // "SCAN": For mods scan.
            // "REGISTRIES": For firing of registry events.
            // "REGISTRYDUMP": For getting the contents of all registries.
            property("forge.logging.markers", "REGISTRIES")

            // Recommended logging level for the console
            // You can set various levels here.
            // Please read: https://stackoverflow.com/questions/2031163/when-to-use-the-different-log-levels
            property("forge.logging.console.level", "debug")

            mods {
                this.create(mod_id) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("client") {
            // Comma-separated list of namespaces to load gametests from. Empty = all namespaces.
            property("forge.enabledGameTestNamespaces", mod_id)
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
        }

        create("server") {
            property("forge.enabledGameTestNamespaces", mod_id)
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
            args("--nogui")
        }

        // This run config launches GameTestServer and runs all registered gametests, then exits.
        // By default, the server will crash when no gametests are provided.
        // The gametest system is also enabled by default for other run configs under the /test command.
        create("gameTestServer") {
            property("forge.enabledGameTestNamespaces", mod_id)
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
        }

        create("data") {
            // example of overriding the workingDirectory set in configureEach above, uncomment if you want to use it
            // workingDirectory project.file('run-data')
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")

            // Specify the modid for data generation, where to output the resulting resource, and where to look for existing resources.
            args("--mod", mod_id, "--all", "--output", file("src/generated/resources/").getAbsolutePath(), "--existing", file("src/main/resources/").getAbsolutePath())
        }
    }
}

val targetJavaVersion = 17
val preferredJvm = JvmTarget.JVM_17

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(preferredJvm)
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filteringCharset = "UTF-8"

        filesMatching("META-INF/mods.toml") {
            expand(
                "version" to version,
                "minecraft_version" to minecraft_version,
                "forge_version" to forge_version_major,
                "kff_version" to forge_kotlin_version,
                "create_version" to create_version_short
            )
        }
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(targetJavaVersion)
    }

    jar {
        archiveBaseName.set(archives_base_name)
        archiveVersion.set(archives_version)
        archiveClassifier.set("slim")
    }

    shadowJar {
        archiveBaseName.set(archives_base_name)
        archiveVersion.set(archives_version)
        archiveClassifier.set("")

        dependencies {
            exclude(dependency("org.jetbrains.kotlin:.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-serialization-.*"))
            exclude(dependency("org.slf4j:.*"))
        }
        configurations = listOf(shade)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    reobf {
        shadowJar {}
    }
}

afterEvaluate {
    val shadowJar = tasks.named<ShadowJar>("shadowJar").get()
    val reobfJar = tasks.named<RenameJarInPlace>("reobfJar").get()
    val build = tasks.named<DefaultTask>("build").get()

    reobfJar.dependsOn(shadowJar)
    reobfJar.input.set(shadowJar.archiveFile)
    build.dependsOn(reobfJar)
}