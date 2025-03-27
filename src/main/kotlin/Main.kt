package einstein.tweakerpacks

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path

// Gson <3
val gson: Gson = GsonBuilder().setPrettyPrinting().create()

// Settings
const val PATH = "packs/src/"
val BUILD_FOLDER = File("packs/build")
val versionObj = parseJson(File("packs/version-info.json"))
val MINECRAFT_VERSION: String = versionObj.getStringOrThrow("minecraft_version")
val PACK_VERSION: Int = versionObj.getOrThrow("pack_version").asInt
const val DELETE_BUILD = false

// Defaults
val COMMON = PackModule("common", "")

// Code
fun main() {
    if (!BUILD_FOLDER.exists()) BUILD_FOLDER.mkdirs()
    else if (DELETE_BUILD) {
        BUILD_FOLDER.deleteRecursively()
        BUILD_FOLDER.mkdirs()
    }

    File(PATH).listFiles()?.forEach(::buildPack)
}

fun buildPack(file: File) {
    val json = File(file.path + "/pack-info.json")
    require(json.exists()) { "ERROR: invalid pack '${json.path}'" }
    val packInfo = gson.fromJson(json.readText(), PackInfo::class.java)

    val commonModule = packInfo.getCommonModuleFile()
    require(commonModule.exists()) { "ERROR: Couldn't find module 'common' for pack ${packInfo.id}" }
    if (packInfo.hasModules && packInfo.modules.isNotEmpty()) {
        packInfo.modules.forEach {
            val moduleFile = packInfo.getModuleFile(it.id)
            require(moduleFile.exists()) { "ERROR: Couldn't find module '${it.id}' for pack ${packInfo.id}" }

            createModuleZip(packInfo, it, moduleFile, commonModule)
        }
    } else {
        createModuleZip(packInfo, COMMON, commonModule)
    }
}

fun createModuleZip(
    packInfo: PackInfo, module: PackModule, moduleFile: File, commonFile: File? = null
) = try {
    createZipFile(
        listOfNotNull(moduleFile, commonFile),
        packInfo.getPackName() + if (module.name.isEmpty()) "" else "-${module.name}",
        packInfo.description
    )
} catch (e: IOException) {
    error("ERROR: Failed to create zip for module ${module.id} from pack ${packInfo.name} \n$e")
}

fun createZipFile(rootFiles: List<File>, path: String, description: String) {
    val output = File("$path.zip")
    if (output.exists()) {
        println("Zip already exists $path")
        return
    }

    createZipFile(output) { zipStream ->
        zipStream.putMcMeta(PackData(PACK_VERSION, description))
        rootFiles.forEach { rootFile ->
            rootFile.walkTopDown().forEach { file ->
                val filePath = file.absolutePath
                    .removePrefix(rootFile.absolutePath)
                    // (ender) this is really not need tbh scince it works fine with / or \
                    .removePrefix("/")
                    .removePrefix("\\")

                if (file.isFile) {
                    zipStream.writeFile(filePath, file, path)
                }
            }
        }
    }
}

fun ZipOutputStream.writeFile(filePath: String, file: File, fullPath: String) = this.putFile(filePath) {
    BufferedInputStream(file.inputStream()).use { fileStream ->
        val bytesCopied = fileStream.copyTo(it)
        if (bytesCopied == 0L && file.readText().isNotEmpty()) {
            // (ender) umm does this `Files.exists` even do what It's supposed to?
            if (Files.exists(Path(filePath))) println("ERROR: File already exists $filePath | $fullPath")
            else println("WARN: File not copied to zip. File had 0 bytes or an error occurred $filePath | $fullPath")
        } else println("SUCCESS: Copied file to zip $filePath | $fullPath")
    }
}
