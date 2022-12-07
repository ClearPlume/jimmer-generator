package top.fallenangel.jimmergenerator.model.type

data class Class(
    override val name: String,
    override val `package`: String = "",
    val nullable: Boolean = false
) : Type
