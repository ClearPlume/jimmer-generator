package top.fallenangel.jimmergenerator.model

import com.intellij.database.Dbms

enum class DBType(l: String, r: String) {
    ORACLE("\"", "\""), MEMSQL("", ""), MYSQL("`", "`"), POSTGRES("\"", "\""),
    MSSQL("[", "]"), DB2("",""), SQLITE("",""), H2("",""), MONGO("","");

    companion object {
        fun valueOf(db: Dbms) = DBType.valueOf(db.name)
    }
}
