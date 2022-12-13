package top.fallenangel.jimmergenerator.util

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

fun message(key: String): String = Constant.messageBundle.getString(key)

fun ui(key: String): String = Constant.uiBundle.getString(key)
