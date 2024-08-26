import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val buildPath = File("C:/Files/Minecraft/Servers/Default/plugins")
//val buildPath = File("./run/plugins")
val mcVersion = "1.21.1"
val kotlinVersion = kotlin.coreLibrariesVersion

repositories {
    mavenCentral()
    maven("https://repo.blugon.kr/repository/maven-public/")
    maven("https://maven.enginehub.org/repo")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(kotlin("reflect"))
    paperweight.paperDevBundle("${mcVersion}-R0.1-SNAPSHOT")
    implementation("kr.blugon:plugin-utils:latest.release")
    implementation("kr.blugon:mini-color:latest.release")
    implementation("kr.blugon:kotlin-brigadier:latest.release")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.4")
}

extra.apply {
    set("ProjectName", project.name)
    set("ProjectVersion", project.version)
    set("KotlinVersion", kotlinVersion)
    set("MinecraftVersion", mcVersion.split(".").subList(0, 2).joinToString("."))
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    processResources {
        filesMatching("*.yml") {
            expand(project.properties)
            expand(extra.properties)
        }
    }

    create<Jar>("buildPaper") {
        this.build()
    }

    shadowJar {
        this.build()
    }

    runServer {
        minecraftVersion(mcVersion)
    }
}

fun Jar.build() {
    val file = File("./build/libs/${project.name}.jar")
    if(file.exists()) file.deleteOnExit()
    archiveBaseName.set(project.name) //Project Name
    archiveFileName.set("${project.name}.jar") //Build File Name
    archiveVersion.set(project.version.toString()) //Version
    from(sourceSets["main"].output)

    doLast {
        copy {
            from(archiveFile) //Copy from
            into(buildPath) //Copy to
        }
    }
}