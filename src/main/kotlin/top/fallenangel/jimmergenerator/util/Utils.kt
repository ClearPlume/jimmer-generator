package top.fallenangel.jimmergenerator.util

import com.intellij.database.model.DasColumn
import com.intellij.database.psi.DbTable
import com.intellij.database.util.DasUtil
import top.fallenangel.jimmergenerator.component.SettingStorageComponent
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.model.Field
import top.fallenangel.jimmergenerator.model.Table
import top.fallenangel.jimmergenerator.model.type.Annotation
import top.fallenangel.jimmergenerator.model.type.Class
import top.fallenangel.jimmergenerator.model.type.Parameter
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
                Field(NameUtil.sneak2camel(it.name).uncapitalize(), it.captureType(language), mutableListOf(), it.comment, false)
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

/**
 * 获取列类型
 *
 * @param language 语言, Java OR Kotlin
 */
fun DasColumn.captureType(language: Language): Class {
    val typeMappings = SettingStorageComponent.storage.state.typeMappings
    for (typeMapping in typeMappings) {
        if (Regex(typeMapping.column, RegexOption.IGNORE_CASE).matches(dataType.typeName)) {
            return when (language) {
                Language.JAVA -> {
                    if (DasUtil.isPrimary(this) && typeMapping.javaPrimitives != null) {
                        Class(typeMapping.javaPrimitives, "")
                    } else {
                        val lastDotIndex = typeMapping.java.lastIndexOf('.')
                        if (lastDotIndex != -1) {
                            Class(typeMapping.java.substring(lastDotIndex + 1), typeMapping.java.take(lastDotIndex))
                        } else {
                            Class(typeMapping.java, "")
                        }
                    }
                }

                Language.KOTLIN -> {
                    val lastDotIndex = typeMapping.kotlin.lastIndexOf('.')
                    if (lastDotIndex != -1) {
                        Class(typeMapping.kotlin.substring(lastDotIndex + 1), typeMapping.kotlin.take(lastDotIndex), !isNotNull)
                    } else {
                        Class(typeMapping.kotlin, "", !isNotNull)
                    }
                }
            }
        }
    }

    return when (language) {
        Language.JAVA -> Class("Object", "")
        Language.KOTLIN -> Class("Any", "", true)
    }
}

/**
 * 获取列在指定语言中的对应注解
 *
 * @param language 语言, Java OR Kotlin
 */
fun DasColumn.captureAnnotations(language: Language): MutableList<Annotation> {
    val annotations = mutableListOf<Annotation>()
    val primary = DasUtil.isPrimary(this)

    if (primary) {
        annotations.add(Annotation("Id", "org.babyfish.jimmer.sql", emptyList()))
        annotations.add(
            Annotation(
                "GeneratedValue", "org.babyfish.jimmer.sql",
                listOf(Parameter("strategy", "GenerationType.IDENTITY", Class("GenerationType", "org.babyfish.jimmer.sql")))
            )
        )
    } else if (language == Language.JAVA && !isNotNull) {
        annotations.add(Annotation("Null", "javax.validation.constraints", emptyList()))
    }
    return annotations
}

fun Table.captureImportList(): List<String> {
    val classes = mutableListOf("org.babyfish.jimmer.sql.Entity").apply {
        // 实体类中用到的所有全限定类名
        addAll(
            fields.map { "" }
        )
        // 实体类中用到的所有注解
        addAll(
            fields.map { "" }
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
        field.annotations.forEachIndexed { index, annotation ->
        }
    }
    return this
}
