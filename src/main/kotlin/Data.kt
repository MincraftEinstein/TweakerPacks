package einstein.tweakerpacks

import com.google.gson.annotations.SerializedName
import java.io.File


data class PackInfo(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    @SerializedName("has_modules")
    val hasModules: Boolean,
    val modules: List<PackModule>,
) {
    fun getCommonModuleFile() = getModuleFile("common")
    fun getModuleFile(moduleId: String) = File("$PATH/$id/src/$moduleId")
    fun getPackName() = "$BUILD_FOLDER/$name-$MINECRAFT_VERSION-$version"
}

data class PackModule(
    val id: String,
    val name: String,
)


data class PackData(
    val pack: InternalPackInfo,
) {
    constructor(packFormat: Int, description: String) : this(InternalPackInfo(packFormat, description))
}

data class InternalPackInfo(
    @SerializedName("pack_format")
    val packFormat: Int,
    val description: String,
)
