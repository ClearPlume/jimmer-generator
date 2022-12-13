package top.fallenangel.jimmergenerator.model.type

data class Parameter(
    val name: String = "",
    val value: Any,
    val type: Class
) {
    val anonymity: Boolean
        get() = name.isBlank() || name == "value"
}
