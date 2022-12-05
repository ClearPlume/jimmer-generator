package top.fallenangel.jimmergenerator.model

import top.fallenangel.jimmergenerator.model.type.Annotation
import top.fallenangel.jimmergenerator.model.type.Class

data class Field(
    val name: String,
    val type: Class,
    val annotations: MutableList<Annotation>,
    val remark: String?,
    val nullable: Boolean
)
