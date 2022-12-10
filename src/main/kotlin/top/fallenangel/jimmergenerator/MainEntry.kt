package top.fallenangel.jimmergenerator

import com.intellij.database.psi.DbElement
import com.intellij.database.psi.DbTable
import com.intellij.database.util.DasUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.module.ModuleManager
import com.intellij.psi.PsiElement
import top.fallenangel.jimmergenerator.component.SettingStorageComponent
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.model.DBType
import top.fallenangel.jimmergenerator.model.DbObj
import top.fallenangel.jimmergenerator.model.type.Annotation
import top.fallenangel.jimmergenerator.model.type.Class
import top.fallenangel.jimmergenerator.model.type.Parameter
import top.fallenangel.jimmergenerator.ui.Frame
import top.fallenangel.jimmergenerator.util.*

class MainEntry : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val modules = ModuleManager.getInstance(project).modules.toMutableList().apply { add(0, Constant.dummyModule) }
        val dbTables = event.getData(LangDataKeys.PSI_ELEMENT_ARRAY)?.map { it as DbTable } ?: return
        val dbType = DBType.valueOf(dbTables[0].dataSource.dbms)

        val tables = dbTables.map {
            val tableAnnotation = Annotation(
                "Table", Constant.jimmerPackage, listOf(
                    Parameter("name", it.name, Class("String"))
                )
            )
            DbObj(
                null, true, it.name,
                it.name.field2property(), false,
                Class(it.name.field2property()),
                (SettingStorageComponent.tableDefaultAnnotations + tableAnnotation).toMutableList(),
                it.comment
            ).also { table ->
                DasUtil.getColumns(it)
                        .toList()
                        .map { column ->
                            DbObj(
                                column, true, column.name,
                                column.name.field2property(uncapitalize = true), column.isBusinessKey,
                                column.captureType(Language.JAVA),
                                column.captureAnnotations(Language.JAVA),
                                column.comment
                            ).apply { table.add(this) }
                        }
            }
        }
        Frame(project, modules, tables, dbType)
    }

    /**
     * 检测是否应该显示菜单项
     */
    override fun update(event: AnActionEvent) {
        val selected = event.getData(LangDataKeys.PSI_ELEMENT_ARRAY)
        event.presentation.isVisible = selected.shouldShowMainEntry()
    }

    private fun Array<PsiElement>?.shouldShowMainEntry(): Boolean {
        if (this == null) {
            return false
        }
        return all {
            if (it !is DbElement) {
                return@all false
            }
            it.typeName == "table" || it.typeName == "view"
        }
    }
}
