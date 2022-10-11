package top.fallenangel.jimmergenerator.model

data class Table(
    val name: String,
    val fields: List<Field>,
    val remark: String?,
)
