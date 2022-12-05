package top.fallenangel.jimmergenerator.model

import top.fallenangel.jimmergenerator.model.type.Annotation

data class Table(
    val name: String,
    val fields: List<Field>,
    val remark: String?,
    val annotations: List<Annotation>
)
