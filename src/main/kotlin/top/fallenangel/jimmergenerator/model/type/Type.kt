package top.fallenangel.jimmergenerator.model.type

interface Type {
    val name: String
    val `package`: String
    val import: String
        get() = if (`package`.isNotBlank()) {
            "$`package`.$name"
        } else {
            name
        }
}
