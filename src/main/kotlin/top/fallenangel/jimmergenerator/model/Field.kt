package top.fallenangel.jimmergenerator.model

data class Field(
    val name: String,
    var type: String,
    val annotations: MutableList<String>,
    val remark: String?,
)
