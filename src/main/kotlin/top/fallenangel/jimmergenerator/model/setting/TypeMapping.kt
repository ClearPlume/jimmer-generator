package top.fallenangel.jimmergenerator.model.setting

data class TypeMapping(
    val column: String,
    val java: String,
    val javaPrimitives: String? = null,
    val kotlin: String
)
