package top.fallenangel.jimmergenerator

import com.intellij.database.model.DasColumn
import com.intellij.database.psi.DbTable
import com.intellij.database.util.DasUtil
import top.fallenangel.jimmergenerator.component.SettingStorageComponent
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.model.Field
import top.fallenangel.jimmergenerator.model.Table
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object ResourceUtil {
    fun getResourceAsStream(resource: String): InputStream {
        return ResourceUtil::class.java.getResourceAsStream(resource)!!
    }

    fun getResourceAsString(resource: String): String {
        val reader = BufferedReader(InputStreamReader(getResourceAsStream(resource)))
        return reader.readText()
    }
}

object NameUtil {
    fun sneak2camel(sneak: String): String {
        return sneak.lowercase()
                .split("_")
                .map { it.capitalize() }
                .reduce { curr, next -> "$curr$next" }
    }
}

fun DbTable.fields(language: Language): List<Field> {
    return DasUtil.getColumns(this)
            .map {
                // 获取列基本信息
                val primary = DasUtil.isPrimary(it)
                val nullable = !it.isNotNull

                // 提取列注解
                val annotations = mutableListOf<String>()
                if (primary) {
                    annotations.add("org.babyfish.jimmer.sql.Id")
                    annotations.add("org.babyfish.jimmer.sql.GeneratedValue")
                }
                if (language == Language.JAVA && nullable) {
                    annotations.add("javax.validation.constraints.Null")
                }
                Field(NameUtil.sneak2camel(it.name).uncapitalize(), it.captureType(language), annotations, it.comment)
            }
            .toList()
}

private fun String.capitalize(): String {
    val chars = toCharArray()
    chars[0] = chars[0].uppercaseChar()
    return chars.concatToString()
}

private fun String.uncapitalize(): String {
    val chars = toCharArray()
    chars[0] = chars[0].lowercaseChar()
    return chars.concatToString()
}

private fun DasColumn.captureType(language: Language): String {
    val typeMappings = SettingStorageComponent.storage.state.typeMappings
    for (typeMapping in typeMappings) {
        if (Regex(typeMapping.column, RegexOption.IGNORE_CASE).matches(dataType.specification)) {
            return when (language) {
                Language.JAVA -> if (DasUtil.isPrimary(this)) typeMapping.javaPrimitives!! else typeMapping.java
                Language.KOTLIN -> if (isNotNull) typeMapping.kotlin else "${typeMapping.kotlin}?"
            }
        }
    }

    return when (language) {
        Language.JAVA -> "java.lang.Object"
        Language.KOTLIN -> if (isNotNull) "kotlin.Any" else "kotlin.Any?"
    }
}

fun Table.captureImportList(): List<String> {
    val types = fields.map { it.type }
            .distinct()
            .filter { it.contains('.') }
            .toMutableList().apply { add("org.babyfish.jimmer.sql.Entity") }
    val annotations = fields.map { it.annotations }
            .flatten()
            .distinct()
            .filter { it.contains('.') }
    return (types + annotations)
            .asSequence()
            .map {
                if (it.endsWith('?')) {
                    return@map it.take(it.length - 1)
                }
                it
            }
            .map {
                val lastDotIndex = it.lastIndexOf('.')
                it.substring(0, lastDotIndex) to it.substring(lastDotIndex + 1)
            }
            .groupBy { it.first }
            .map {
                if (it.value.distinct().size == 1) {
                    "${it.key}.${it.value[0].second}"
                } else {
                    "${it.key}.*"
                }
            }
            .distinct()
}

fun Table.removeClassPackage(): Table {
    for (field in fields) {
        field.type = field.type.substringAfterLast('.')
        field.annotations.forEachIndexed { index, annotation ->
            field.annotations[index] = annotation.substringAfterLast('.')
        }
    }
    return this
}
