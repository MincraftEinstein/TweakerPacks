package einstein.tweakerpacks

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path

const val PATH = "packs/src/"
val gson: Gson = GsonBuilder().create()
val buildFolder = File("packs/build")
val versionObj = parseJson(File("packs/version-info.json"))
val minecraftVersion: String = versionObj.getStringOrThrow("minecraft_version")
val packVersion: Int = versionObj.getOrThrow("pack_version").asInt

fun main() {
    if (!buildFolder.exists()) {
        buildFolder.mkdirs()
    }

    File(PATH).listFiles()?.forEach(::buildPack)
}

fun buildPack(file: File) {
    var json = File(file.path + "/pack-info.json")
    val jsonPath = json.path
    if (!json.exists()) {
        error("ERROR: invalid pack '$jsonPath'")
        return
    }

    var jsonObj = parseJson(json)
    var packId = jsonObj.getStringOrThrow("id")
    var packName = jsonObj.getStringOrThrow("name")
    var description = jsonObj.getStringOrThrow("description")
    var modules = jsonObj.tryGet("modules")?.asJsonArray
    var version = jsonObj.getStringOrThrow("version")

    val commonModule = File("$PATH/$packId/src/common")
    if (!commonModule.exists()) {
        error("ERROR: Couldn't find module 'common' for pack $packId")
        return
    }

    if (modules != null && !modules.isEmpty) {
        modules.forEach {
            val obj = it.asJsonObject
            var moduleId = obj.getStringOrThrow("id")
            var moduleName = obj.getStringOrThrow("name")
            var moduleFile = File("$PATH/$packId/src/$moduleId")

            if (!moduleFile.exists()) {
                error("ERROR: Couldn't find module '$moduleId' for pack $packId")
                return
            }

            createModuleZip(packName, version, description, moduleId, moduleName, moduleFile, commonModule)
        }
        return
    }
    createModuleZip(packName, version, description, "common", "common", commonModule, null)
}

fun createModuleZip(
    packName: String,
    version: String,
    description: String,
    moduleId: String,
    moduleName: String,
    moduleFile: File,
    commonFile: File?
) {
    val files = mutableListOf(moduleFile)
    commonFile?.let { files + commonFile }

    try {
        createZipFile(
            files,
            "$buildFolder/$packName-$minecraftVersion-$version" + if (moduleName.isEmpty()) "" else "-$moduleName",
            description
        )
    } catch (e: IOException) {
        error("ERROR: Failed to create zip for module $moduleId from pack $packName \n$e")
    }
}

fun JsonObject.getStringOrThrow(name: String): String {
    return getOrThrow(name).asString
}

fun JsonObject.getOrThrow(name: String): JsonElement {
    val element = tryGet(name)
    if (element != null) {
        return element
    }
    throw JsonSyntaxException("'$name' is missing from pack json")
}

fun JsonObject.tryGet(name: String): JsonElement? {
    if (has(name)) {
        return get(name)
    }
    return null
}

fun parseJson(json: File): JsonObject {
    try {
        var reader =
            JsonReader(BufferedReader(InputStreamReader(json.toURI().toURL().openStream(), StandardCharsets.UTF_8)))
        reader.isLenient = false
        val obj = gson.getAdapter(JsonObject::class.java).read(reader)
        if (obj != null) return obj
        throw JsonParseException("Json was null or empty")
    } catch (e: IOException) {
        throw JsonParseException(e)
    }
}

fun createZipFile(rootFiles: List<File>, path: String, description: String) {
    var output = File("$path.zip")
    if (output.exists()) {
        println("Zip already exists $path")
        return
    }

    var visitedPaths = mutableListOf<String>()

    ZipOutputStream(BufferedOutputStream(output.outputStream())).use { zipStream ->
        rootFiles.forEach { rootFile ->
            rootFile.walkTopDown().forEach { file ->
                var filePath = file.absolutePath
                    .removePrefix(rootFile.absolutePath)
                    .removePrefix("/")
                    .removePrefix("\\")

                if (file.isDirectory) {
                    filePath += "/"
                }

                if (!visitedPaths.contains(filePath)) {
                    zipStream.putNextEntry(ZipEntry(filePath))
                    visitedPaths.add(filePath)
                }

                if (file.isFile) {
                    BufferedInputStream(file.inputStream()).use { fileStream ->
                        var bytesCopied = fileStream.copyTo(zipStream)
                        if (bytesCopied == 0L)
                            if (Files.exists(Path(filePath))) println("ERROR: File already exists $filePath | $path")
                            else println("WARN: File not copied to zip. File had 0 bytes or an error occurred $filePath | $path")
                        else println("SUCCESS: Copied file to zip $filePath | $path")
                    }
                }
            }
        }

        createMcMetaFile(zipStream, description)
    }
}

fun createMcMetaFile(zipStream: ZipOutputStream, description: String) {
    try {
        ByteArrayOutputStream().use { stream ->
            OutputStreamWriter(stream, StandardCharsets.UTF_8).use { streamWriter ->
                var writer = JsonWriter(streamWriter)
                writer.serializeNulls = false
                writer.setIndent("  ")

                writer.beginObject()

                writer.name("pack")
                writer.beginObject()

                writer.name("pack_format")
                writer.value(packVersion)

                writer.name("description")
                writer.value(description)

                writer.endObject()

                writer.endObject()
            }

            zipStream.putNextEntry(ZipEntry("pack.mcmeta"))
            zipStream.write(stream.toByteArray())
        }
    } catch (_: IOException) {
        error("ERROR: Failed to create mcmeta file")
    }
}