package top.fallenangel.jimmergenerator.util

import com.intellij.database.model.DasColumn
import com.intellij.database.util.DasUtil
import top.fallenangel.jimmergenerator.component.SettingStorageComponent
import top.fallenangel.jimmergenerator.enums.Language
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
                        Class(typeMapping.javaPrimitives, nullable = !isNotNull)
                    } else {
                        val lastDotIndex = typeMapping.java.lastIndexOf('.')
                        if (lastDotIndex != -1) {
                            Class(typeMapping.java.substring(lastDotIndex + 1), typeMapping.java.take(lastDotIndex), !isNotNull)
                        } else {
                            Class(typeMapping.java, nullable = !isNotNull)
                        }
                    }
                }

                Language.KOTLIN -> {
                    val lastDotIndex = typeMapping.kotlin.lastIndexOf('.')
                    if (lastDotIndex != -1) {
                        Class(typeMapping.kotlin.substring(lastDotIndex + 1), typeMapping.kotlin.take(lastDotIndex), !isNotNull)
                    } else {
                        Class(typeMapping.kotlin, nullable = !isNotNull)
                    }
                }
            }
        }
    }

    return when (language) {
        Language.JAVA -> Class("Object", nullable = true)
        Language.KOTLIN -> Class("Any", nullable = true)
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

/**
 * 去除列名前后缀
 *
 * @param prefix 前缀
 * @param suffix 后缀
 * @param uncapitalize 是否对结果进行首字母小写操作
 */
fun String.field2property(prefix: String = "", suffix: String = "", uncapitalize: Boolean = false): String {
    var property = this.lowercase()
    property = property.replaceFirst(Regex("^$prefix", RegexOption.IGNORE_CASE), "")
    property = property.replaceFirst(Regex("$suffix$", RegexOption.IGNORE_CASE), "")
    return if (uncapitalize) {
        NameUtil.sneak2camel(property).uncapitalize()
    } else {
        NameUtil.sneak2camel(property)
    }
}

/**
 * 判断列是否为应该默认为业务主键
 */
val DasColumn.isBusinessKey: Boolean
    get() {
        if (DasUtil.isPrimary(this)) return false
        return DasUtil.isIndexColumn(this) && isNotNull
    }
