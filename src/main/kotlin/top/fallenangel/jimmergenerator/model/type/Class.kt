package top.fallenangel.jimmergenerator.model.type

import kotlinx.serialization.Serializable

@Serializable
data class Class(
    override val name: String,
    override val `package`: String = "",
    val nullable: Boolean = false
) : Type
