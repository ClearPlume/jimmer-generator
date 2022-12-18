package top.fallenangel.jimmergenerator.enums

enum class DbObjTrait {
    TABLE, PRIMARY, INDEX, FOREIGN, NOTNULL, COLUMN;

    infix fun or(trait: Int) = (1 shl ordinal) or trait
}
