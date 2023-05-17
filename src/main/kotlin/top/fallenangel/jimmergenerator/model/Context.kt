package top.fallenangel.jimmergenerator.model

import com.intellij.openapi.project.Project
import top.fallenangel.jimmergenerator.enums.DBType

class Context {
    private lateinit var _project: Project
    private lateinit var _dbType: DBType

    companion object {
        private val context = Context()

        val project by lazy { context._project }
        val dbType by lazy { context._dbType }

        fun setProject(project: Project) {
            context._project = project
        }

        fun setDbType(dbType: DBType) {
            context._dbType = dbType
        }
    }
}
