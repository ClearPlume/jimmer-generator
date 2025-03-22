package top.fallenangel.jimmergenerator.util

import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasNamed
import com.intellij.database.util.DasUtil
import top.fallenangel.jimmergenerator.component.SettingStorageComponent
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.model.type.Annotation
import top.fallenangel.jimmergenerator.model.type.Class
import top.fallenangel.jimmergenerator.model.type.Parameter

/**
 * 去除表列名前后缀
 *
 * @param prefix 前缀
 * @param suffix 后缀
 * @param uncapitalize 是否对结果进行首字母小写操作
 */
fun DasNamed.field2property(prefix: String = "", suffix: String = "", uncapitalize: Boolean = false): String {
    var property = name.lowercase()
    property = if (property == prefix) {
        property
    } else {
        property.replaceFirst(Regex("^${prefix}_?", RegexOption.IGNORE_CASE), "")
    }
    property = if (property == suffix) {
        property
    } else {
        property.replaceFirst(Regex("_?$suffix$", RegexOption.IGNORE_CASE), "")
    }
    return property.sneak2camel(uncapitalize)
}

/**
 * 判断列是否为应该默认为业务主键
 */
val DasColumn.isBusinessKey: Boolean
    get() {
        if (DasUtil.isPrimary(this)) return false
        return DasUtil.isIndexColumn(this) && isNotNull
    }

/**
 * 获取列类型
 *
 * @param language 语言, Java OR Kotlin
 */
fun DasColumn.captureType(language: Language): Class {
    val typeMappings = SettingStorageComponent.typeMappings
    for (typeMapping in typeMappings) {
        if (Regex(typeMapping.column, RegexOption.IGNORE_CASE).matches(dasType.specification)) {
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
        annotations.add(Annotation("Id", Constant.JIMMER_PACKAGE, emptyList()))
        annotations.add(
            Annotation(
                "GeneratedValue", Constant.JIMMER_PACKAGE,
                listOf(Parameter("strategy", "GenerationType.IDENTITY", Class("GenerationType", Constant.JIMMER_PACKAGE)))
            )
        )
    } else if (language == Language.JAVA && !isNotNull) {
        annotations.add(Annotation("Null", "javax.validation.constraints", emptyList()))
    }
    if (isBusinessKey) {
        annotations.add(Annotation("Key", Constant.JIMMER_PACKAGE, emptyList()))
    }
    return annotations
}
