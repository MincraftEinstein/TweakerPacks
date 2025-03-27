package einstein.tweakerpacks

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


fun JsonObject.getStringOrThrow(name: String): String = getOrThrow(name).asString
fun JsonObject.getOrThrow(name: String): JsonElement =
    if (has(name)) get(name) else throw JsonSyntaxException("'$name' is missing from pack json")

fun parseJson(file: File): JsonObject =
    gson.fromJson(file.readText(), JsonObject::class.java) ?: throw JsonParseException("Json was null or empty")


// (ender) my zip file helpers
fun createZipFile(outputPath: File, fn: (ZipOutputStream) -> Unit): Boolean = try {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputPath))).use(fn)
    true
} catch (e: Exception) {
    println("Error creating ZIP file: $e")
    false
}


fun ZipOutputStream.putFile(path: String, fn: (ZipOutputStream) -> Unit) = try {
    this.putNextEntry(ZipEntry(path))
    fn(this)
    this.closeEntry()
} catch (e: Exception) {
    println("Error adding file [$path] to zip.\n${e.message}")
}

fun ZipOutputStream.putMcMeta(pack: PackData) = this.putFile("pack.mcmeta") {
    ByteArrayOutputStream().use { stream ->
        OutputStreamWriter(stream, StandardCharsets.UTF_8).use { it.write(gson.toJson(pack)) }
        this.write(stream.toByteArray())
    }
}


