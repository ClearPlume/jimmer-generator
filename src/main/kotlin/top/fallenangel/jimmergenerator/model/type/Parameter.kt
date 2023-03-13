package top.fallenangel.jimmergenerator.model.type

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class Parameter(
    val name: String = "",
    @Contextual
    val value: Any,
    val type: Class
) {
    val anonymity: Boolean
        get() = name.isBlank() || name == "value"
}
