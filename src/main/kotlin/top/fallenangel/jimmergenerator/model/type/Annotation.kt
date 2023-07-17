package top.fallenangel.jimmergenerator.model.type

data class Annotation(
    override val name: String,
    override val `package`: String,
    val parameters: List<Parameter>
) : Type {
    override fun toString(): String {
        return if (parameters.isNotEmpty()) {
            "@$name(${parameters.joinToString()})"
        } else {
            "@$name"
        }
    }
}
