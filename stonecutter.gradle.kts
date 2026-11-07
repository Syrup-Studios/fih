plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.17.21" apply false
    id("net.neoforged.moddev") version "2.0.147" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.0" apply false
}

stonecutter active null

stonecutter.parameters {
    val loader = current.project.substringAfterLast('-')
    var modPackage = ""
    properties {
        tags(current.version, loader)
        val modGroup: String = this["mod.group"]
        val modId: String = this["mod.id"]
        modPackage = "$modGroup.$modId"
    }
    constants.match(loader, "fabric", "neoforge")
    replacements.string(true) { replace("net.syrupstudios.fih", modPackage) }
}
