package top.fallenangel.jimmergenerator.model

import com.intellij.openapi.project.Project
import top.fallenangel.jimmergenerator.enums.DBType

object Context {
    lateinit var project: Project
    lateinit var dbType: DBType
}
