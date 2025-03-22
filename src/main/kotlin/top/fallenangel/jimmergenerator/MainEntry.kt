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
import top.fallenangel.jimmergenerator.enums.DBType
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.model.Context
import top.fallenangel.jimmergenerator.model.DbObj
import top.fallenangel.jimmergenerator.model.type.Annotation
import top.fallenangel.jimmergenerator.model.type.Class
import top.fallenangel.jimmergenerator.model.type.Parameter
import top.fallenangel.jimmergenerator.ui.Frame
import top.fallenangel.jimmergenerator.util.*

class MainEntry : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val modules = ModuleManager.getInstance(project).modules.toList()
        val dbTables = event.getData(LangDataKeys.PSI_ELEMENT_ARRAY)?.map { it as DbTable } ?: return
        val dbType = DBType.valueOf(dbTables[0].dataSource.dbms)

        Context.setProject(project)
        Context.setDbType(dbType)

        val tables = dbTables.map {
            val tableAnnotation = Annotation(
                "Table", Constant.JIMMER_PACKAGE, listOf(
                    Parameter("name", it.name, Class("String"))
                )
            )
            DbObj(
                null, true, it.name,
                it.field2property(), false,
                Class(it.field2property()),
                (SettingStorageComponent.tableDefaultAnnotations + tableAnnotation).toMutableList(),
                it.comment
            ).also { table ->
                DasUtil.getColumns(it)
                        .toList()
                        .map { column ->
                            table.add(
                                DbObj(
                                    column, true, column.name,
                                    column.field2property(uncapitalize = true), column.isBusinessKey,
                                    column.captureType(Language.JAVA),
                                    column.captureAnnotations(Language.JAVA),
                                    column.comment
                                )
                            )
                        }
            }
        }
        Frame(modules, tables)
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
            it.typeName in arrayOf("table", "view", "表", "视图")
        }
    }
}
