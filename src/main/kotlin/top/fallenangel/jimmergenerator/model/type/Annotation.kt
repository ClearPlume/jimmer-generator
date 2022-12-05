package top.fallenangel.jimmergenerator.model.type

data class Annotation(
    override val name: String,
    override val `package`: String,
    val parameters: List<Parameter>
) : Type
