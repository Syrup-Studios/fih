import groovy.json.JsonGenerator
import groovy.json.JsonGenerator.Options
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.Action
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.nio.file.Paths


plugins {
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
}

interface NeoForgeArtifactBuildService : BuildService<BuildServiceParameters.None>

val neoForgeArtifactService = gradle.sharedServices.registerIfAbsent("neoForgeArtifactBuildService", NeoForgeArtifactBuildService::class.java) {
    maxParallelUsages.set(1)
}
tasks.matching { it.name == "createMinecraftArtifacts" }.configureEach {
    usesService(neoForgeArtifactService)
}


class NeoforgeMetadataAction(private val values: Map<String, Any>) : Action<FileCopyDetails> {
    override fun execute(file: FileCopyDetails) { file.expand(values) }
}

class NeoforgeSourcePackageAction(private val oldPath: String, private val newPath: String, private val generatedRoot: String) : Action<FileCopyDetails> {
    override fun execute(file: FileCopyDetails) {
        val prefix = "$oldPath/"
        if (!file.file.toPath().startsWith(Paths.get(generatedRoot)) && file.path.startsWith(prefix)) {
            file.relativePath = RelativePath(true, *"$newPath/${file.path.removePrefix(prefix)}".split('/').toTypedArray())
        }
    }
}

val modId = project.property("mod.id") as String
require(Regex("^[a-z][a-z0-9_]{1,63}$").matches(modId)) { "Invalid mod.id '$modId'" }
val modPackage = "${project.property("mod.group")}.${modId}"
require(javax.lang.model.SourceVersion.isName(modPackage)) { "Invalid Java package derived from mod.group and mod.id: '$modPackage'" }
val modName = project.property("mod.name") as String
val stonecutterExtension = extensions.getByType(StonecutterBuildExtension::class.java)
val minecraftVersion = stonecutterExtension.current.version
val loader = "neoforge"
val modVersion = "${project.property("mod.version")}+$minecraftVersion-$loader"
val targetJava = (project.property("java.version") as String).toInt()
require(targetJava > 0) { "java.version must be positive" }
val generator: JsonGenerator = Options().disableUnicodeEscaping().build()
val escape = { value: String -> generator.toJson(value).let { it.substring(1, it.length - 1) } }
val generatedDir = layout.buildDirectory.dir("generated/sources/modIdentity/java")
val identityProperties = mapOf("modIdJava" to generator.toJson(modId), "modNameJava" to generator.toJson(modName), "modPackage" to modPackage)
val generateModIdentity = tasks.register<Sync>("generateModIdentity") {
    inputs.properties(identityProperties)
    from(rootProject.file("src/main/templates/ModIdentity.java.template")) {
        into(modPackage.replace('.', '/'))
        rename { "ModIdentity.java" }
        expand(identityProperties)
    }
    into(generatedDir)
}

extensions.getByType(SourceSetContainer::class.java).named("main") { java.srcDir(generatedDir) }
tasks.withType<JavaCompile>().configureEach { dependsOn(generateModIdentity) }

extensions.configure(JavaPluginExtension::class.java) {
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(targetJava)
    targetCompatibility = JavaVersion.toVersion(targetJava)
    toolchain.languageVersion = JavaLanguageVersion.of(targetJava)
}
tasks.named<Jar>("sourcesJar").configure { dependsOn(generateModIdentity) }
tasks.named<Jar>("sourcesJar").configure {
    eachFile(NeoforgeSourcePackageAction("net/syrupstudios/fih", modPackage.replace('.', '/'), generatedDir.get().asFile.absolutePath))
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJava)
}

tasks.withType<Jar>().configureEach { from(rootProject.file(project.property("mod.license_file") as String)) }

val authors = extensions.getByType(StonecutterBuildExtension::class.java).properties.raw("mod", "authors").asList().map { it.asPrimitive().content.toString() }
val metadata = mapOf(
    "version" to escape(modVersion),
    "modId" to escape(modId),
    "modIdKey" to modId,
    "modPackage" to escape(modPackage),
    "modName" to escape(modName),
    "modDescription" to escape(project.property("mod.description") as String),
    "authorsToml" to escape(authors.joinToString(", ")),
    "license" to escape(project.property("mod.license") as String),
    "homepage" to escape(project.property("mod.homepage") as String),
    "issues" to escape(project.property("mod.issues") as String),
    "sources" to escape(project.property("mod.sources") as String),
    "java" to targetJava.toString(),
    "mc" to escape(project.property("mod.neoforge_mc_range") as String),
    "neoforge" to project.property("deps.neoforge") as String
)

val metadataPath = "META-INF/neoforge.mods.toml"
tasks.named<ProcessResources>("processResources") {
    inputs.properties(metadata)
    exclude("fabric.mod.json")
    filesMatching(metadataPath, NeoforgeMetadataAction(metadata))
}

version = modVersion
group = project.property("mod.group") as String
base.archivesName = project.property("mod.id") as String

neoForge {
    version = project.property("deps.neoforge") as String
    val runDirectory = rootProject.file("run")
    runs {
        create("client") { client(); gameDirectory = runDirectory }
        create("server") { server(); gameDirectory = runDirectory }
    }
    mods.create(project.property("mod.id") as String) { sourceSet(sourceSets.main.get()) }
}

tasks.named<Jar>("sourcesJar") { exclude("fabric.mod.json") }

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.named<Jar>("jar"), tasks.named<Jar>("sourcesJar"))
    into(rootProject.layout.projectDirectory.dir("build/libs/${project.property("mod.version")}"))
    dependsOn("build")
}

val modrinthId = project.property("publish.modrinth").toString().trim()
val curseforgeId = project.property("publish.curseforge").toString().trim()
val modrinthToken = providers.gradleProperty("MODRINTH_TOKEN").filter { it.isNotBlank() }.orElse(providers.environmentVariable("MODRINTH_TOKEN").filter { it.isNotBlank() })
val curseforgeToken = providers.gradleProperty("CURSEFORGE_TOKEN").filter { it.isNotBlank() }.orElse(providers.environmentVariable("CURSEFORGE_TOKEN").filter { it.isNotBlank() })
val publishDryRun = providers.gradleProperty("publish.dry_run").orElse("false").map {
    when (it) {
        "true" -> true
        "false" -> false
        else -> throw GradleException("publish.dry_run must be true or false")
    }
}
val compatibleVersions = stonecutter.properties.rawOrNull("mod.mc_releases")
    ?.asList()?.map { it.asPrimitive().content.toString() } ?: listOf(minecraftVersion)
require((modrinthId.isBlank() && curseforgeId.isBlank()) || compatibleVersions.isNotEmpty()) {
    "Set mod.mc_releases to at least one Minecraft version before publishing"
}
val uploadVersion = modVersion
val releaseType = when (project.property("publish.release_type").toString().lowercase()) {
    "stable" -> me.modmuss50.mpp.ReleaseType.STABLE
    "beta" -> me.modmuss50.mpp.ReleaseType.BETA
    "alpha" -> me.modmuss50.mpp.ReleaseType.ALPHA
    else -> throw GradleException("publish.release_type must be stable, beta, or alpha")
}
val publishEnvironment = me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment.CLIENT_AND_SERVER
val modrinthRequires = stonecutter.properties.rawOrNull("publish.modrinth_requires")?.asList().orEmpty().map { it.asPrimitive().content.toString() }
val curseforgeRequires = stonecutter.properties.rawOrNull("publish.curseforge_requires")?.asList().orEmpty().map { it.asPrimitive().content.toString() }

publishMods {
    file = tasks.named<Jar>("jar").flatMap { it.archiveFile }
    dryRun.set(publishDryRun)
    version = uploadVersion
    displayName = "${project.property("mod.name")} ${project.property("mod.version")} - NeoForge ${minecraftVersion}"
    changelog = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOG.md")).asText
    type = releaseType
    modLoaders.add("neoforge")

    if (modrinthId.isNotBlank()) modrinth {
        projectId = modrinthId
        accessToken.set(modrinthToken)
        compatibleVersions.forEach { minecraftVersions.add(it) }
        environment.set(publishEnvironment)
        modrinthRequires.forEach { requires(it) }
        additionalFile(tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile }) { type = me.modmuss50.mpp.platforms.modrinth.ModrinthApi.AdditionalFileType.SOURCES_JAR }
    }
    if (curseforgeId.isNotBlank()) curseforge {
        projectId = curseforgeId
        accessToken.set(curseforgeToken)
        compatibleVersions.forEach { minecraftVersions.add(it) }
        client = publishEnvironment != me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment.SERVER_ONLY
        server = publishEnvironment != me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment.CLIENT_ONLY
        curseforgeRequires.forEach { requires(it) }
        additionalFile(tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile }) { name = "Sources" }
    }
}
