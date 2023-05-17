package top.fallenangel.jimmergenerator.util

/**
 * 下划线转驼峰
 *
 * @param uncapitalize 是否针对转换结果做首字母小写操作
 */
fun String.sneak2camel(uncapitalize: Boolean = false): String {
    val camel = lowercase()
            .split(Regex("[^a-zA-Z\\d]"))
            .filter { it.isNotBlank() }
            .joinToString("") { it.capitalize() }
    return if (uncapitalize) {
        camel.uncapitalize()
    } else {
        camel
    }
}

/**
 * 首字母大写
 */
fun String.capitalize(): String {
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
