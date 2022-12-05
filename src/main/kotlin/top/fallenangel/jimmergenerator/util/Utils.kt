package top.fallenangel.jimmergenerator.util

import com.intellij.database.model.DasColumn
import com.intellij.database.psi.DbTable
import com.intellij.database.util.DasUtil
import top.fallenangel.jimmergenerator.component.SettingStorageComponent
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.exception.UnreachableArm
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
                .split(Regex("[^a-zA-Z\\d]"))
                .filter { it.isNotBlank() }
                .map { it.capitalize() }
                .joinToString("") { it }
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
                    annotations.add("org.babyfish.jimmer.sql.GeneratedValue(strategy = GenerationType.IDENTITY)")
                }
                if (language == Language.JAVA && nullable) {
                    annotations.add("javax.validation.constraints.Null")
                }
                Field(NameUtil.sneak2camel(it.name).uncapitalize(), it.captureType(language), annotations, it.comment)
            }
            .toList()
}

/**
 * 首字母大写
 */
private fun String.capitalize(): String {
    val chars = toCharArray()
    chars[0] = chars[0].uppercaseChar()
    return chars.concatToString()
}

/**
 * 首字母小写
 */
fun String.uncapitalize(): String {
    val chars = toCharArray()
    chars[0] = chars[0].lowercaseChar()
    return chars.concatToString()
}

private fun DasColumn.captureType(language: Language): String {
    val typeMappings = SettingStorageComponent.storage.state.typeMappings
    for (typeMapping in typeMappings) {
        if (Regex(typeMapping.column, RegexOption.IGNORE_CASE).matches(dataType.specification)) {
            return when (language) {
                Language.JAVA -> if (DasUtil.isPrimary(this) && typeMapping.javaPrimitives != null) typeMapping.javaPrimitives else typeMapping.java
                Language.KOTLIN -> if (isNotNull) typeMapping.kotlin else "${typeMapping.kotlin}?"
                Language.UNKNOWN -> throw UnreachableArm()
            }
        }
    }

    return when (language) {
        Language.JAVA -> "java.lang.Object"
        Language.KOTLIN -> if (isNotNull) "kotlin.Any" else "kotlin.Any?"
        Language.UNKNOWN -> throw UnreachableArm()
    }
}

fun Table.captureImportList(): List<String> {
    val classes = mutableListOf("org.babyfish.jimmer.sql.Entity").apply {
        // 实体类中用到的所有全限定类名
        addAll(
            fields.map { it.type }
                    .distinct()
                    .filter { it.contains('.') }
        )
        // 实体类中用到的所有注解
        addAll(
            fields.map { it.annotations }
                    .flatten()
                    .distinct()
                    .filter { it.contains('.') }
        )
    }
    return classes.asSequence()
            .map {
                // kotlin中的可空类型是以“?”结尾，需要去除
                if (it.endsWith('?')) {
                    return@map it.take(it.length - 1)
                }
                it
            }
            .map {
                val (clazz) = it.split("(")
                val lastDotIndex = clazz.lastIndexOf('.')
                clazz.substring(0, lastDotIndex) to clazz.substring(lastDotIndex + 1)
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
            val (clazz) = annotation.split("(")
            val lastDotIndex = clazz.lastIndexOf('.')
            field.annotations[index] = annotation.substring(lastDotIndex + 1)
        }
    }
    return this
}
