package top.fallenangel.jimmergenerator.enums

import com.intellij.database.Dbms

enum class DBType(val l: String, val r: String) {
    ORACLE("""\"""", """\""""), MEMSQL("", ""), MYSQL("`", "`"), POSTGRES("""\"""", """\""""),
    MSSQL("[", "]"), DB2("",""), SQLITE("",""), H2("",""), MONGO("","");

    companion object {
        fun valueOf(db: Dbms) = DBType.valueOf(db.name)
    }
}
