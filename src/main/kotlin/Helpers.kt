package einstein.tweakerpacks

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun JsonObject.getStringOrThrow(name: String): String = getOrThrow(name).asString

fun JsonObject.getOrThrow(name: String): JsonElement =
    if (has(name)) get(name) else throw JsonSyntaxException("'$name' is missing from pack json")

fun parseJson(file: File): JsonObject =
    gson.fromJson(file.readText(), JsonObject::class.java) ?: throw JsonParseException("Json was null or empty")

// (ender) my zip file helpers
fun createZipFile(outputPath: File, fn: (ZipOutputStream) -> Unit): Boolean =
    try {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputPath))).use(fn)
        true
    } catch (e: Exception) {
        println("ERROR: Failed to create zip file - $outputPath | $e")
        false
    }

fun ZipOutputStream.putFile(path: String, function: (ZipOutputStream) -> Unit) =
    try {
        putNextEntry(ZipEntry(path))
        function(this)
        closeEntry()
    } catch (e: Exception) {
        println("ERROR: Failed to add file to zip - $path | ${e.message}")
    }

fun ZipOutputStream.putMcMeta(pack: PackData) =
    putFile("pack.mcmeta") {
        ByteArrayOutputStream().use { stream ->
            OutputStreamWriter(stream, StandardCharsets.UTF_8).use { it.write(gson.toJson(pack)) }
            write(stream.toByteArray())
        }
    }
