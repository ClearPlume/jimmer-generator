package top.fallenangel.jimmergenerator.model

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.util.Constant

data class FrameData(
    var language: Language = Language.JAVA,
    var sourceRoot: VirtualFile = Constant.dummyFile,
    var `package`: String = "",
    var tablePrefix: String = "",
    var tableSuffix: String = "",
    var fieldPrefix: String = "",
    var fieldSuffix: String = "",
    var entityPrefix: String = "",
    var entitySuffix: String = "",
) {
    lateinit var module: Module

    override fun toString(): String {
        return buildString {
            append("FrameData(language=")
            append(language)
            append(", sourceRoot=")
            append(sourceRoot)
            append(", `package`='")
            append(`package`)
            append("', tablePrefix='")
            append(tablePrefix)
            append("', tableSuffix='")
            append(tableSuffix)
            append("', fieldPrefix='")
            append(fieldPrefix)
            append("', fieldSuffix='")
            append(fieldSuffix)
            append("', entityPrefix='")
            append(entityPrefix)
            append("', entitySuffix='")
            append(entitySuffix)
            append("', module=")
            append(module)
            append(")")
        }
    }
}
