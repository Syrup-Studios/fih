pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.fabricmc.net/")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.3"
}

rootProject.name = stonecutter.properties["mod.id"]

stonecutter {
    create(rootProject) {
        val targets = stonecutter.properties.raw("targets").asList()
        val projectNames = mutableSetOf<String>()
        targets.forEach { target ->
            val values = target.asMap()
            val minecraft = values.getValue("minecraft").asPrimitive().content.toString()
            val loaders = values.getValue("loaders").asList()
            require(loaders.isNotEmpty()) { "Target $minecraft must define at least one loader" }
            loaders.forEach { loader ->
                val name = loader.asPrimitive().content.toString()
                require(name in setOf("fabric", "neoforge")) { "Unsupported loader '$name' for Minecraft $minecraft" }
                val projectName = "$minecraft-$name"
                require(projectNames.add(projectName)) { "Duplicate target '$projectName'" }
                version(projectName, minecraft).buildscript("build.$name.gradle.kts")
            }
        }
        require(projectNames.isNotEmpty()) { "At least one Minecraft target is required" }
        vcsVersion = projectNames.first()
    }
}
