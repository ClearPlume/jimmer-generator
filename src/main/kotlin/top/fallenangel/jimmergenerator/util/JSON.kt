package top.fallenangel.jimmergenerator.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ser.FilterProvider
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

val jsonMapper by lazy {
    jsonMapper {
        addModule(kotlinModule {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            serializationInclusion(JsonInclude.Include.USE_DEFAULTS)
        })
    }
}

fun JsonNode.toJSONString() = toString()

object JSON {
    fun isValidArray(text: String): Boolean {
        return try {
            jsonMapper.readTree(text).isArray
        } catch (_: Exception) {
            false
        }
    }

    fun isValid(text: String): Boolean {
        return try {
            jsonMapper.readTree(text).isObject
        } catch (_: Exception) {
            false
        }
    }

    fun parseMap(text: String): MutableMap<String, Any> = jsonMapper.readValue(text, jacksonTypeRef())

    fun parseObject(text: String): JsonNode = parseObject(text, JsonNode::class.java)

    fun <T> parseObject(text: String, type: Class<T>): T = jsonMapper.readValue(text, type)

    fun <T> parseObject(text: String, type: TypeReference<T>): T = jsonMapper.readValue(text, type)

    fun parseArray(text: String): MutableList<MutableMap<String, Any>> {
        return jsonMapper.readValue(text, jacksonTypeRef())
    }

    fun parseArray(bytes: ByteArray): MutableList<MutableMap<String, Any>> {
        return jsonMapper.readValue(bytes, jacksonTypeRef())
    }

    fun <T> parseArray(text: String, type: Class<T>): MutableList<T> {
        return jsonMapper.readValue(text, jsonMapper.typeFactory.constructCollectionType(MutableList::class.java, type))
    }

    fun <T> parseArray(text: String, type: TypeReference<T>): MutableList<T> {
        return jsonMapper.readValue(text, jsonMapper.typeFactory.constructCollectionType(MutableList::class.java, jsonMapper.constructType(type.type)))
    }

    fun toJSONString(`object`: Any): String = jsonMapper.writeValueAsString(`object`)

    fun toJSONString(`object`: Any, filters: FilterProvider): String = jsonMapper.writer(filters).writeValueAsString(`object`)
}
