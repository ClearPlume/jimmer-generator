package top.fallenangel.jimmergenerator.model.type

interface Type {
    val name: String
    val `package`: String
    val import: String
        get() = "$`package`.$name"
}
