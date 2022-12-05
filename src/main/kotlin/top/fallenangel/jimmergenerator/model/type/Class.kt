package top.fallenangel.jimmergenerator.model.type

data class Class(
    override val name: String,
    override val `package`: String,
    // For KT: String/String? Int/Int?
    val nullableMarker: Boolean = false
) : Type
