package top.fallenangel.jimmergenerator

import com.intellij.database.psi.DbElement
import com.intellij.database.psi.DbTable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.module.ModuleManager
import com.intellij.psi.PsiElement
import top.fallenangel.jimmergenerator.ui.DialogConstructor
import top.fallenangel.jimmergenerator.ui.Frame
import top.fallenangel.jimmergenerator.ui.FrameData
import top.fallenangel.jimmergenerator.util.Constant

class MainEntry : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val modules = ModuleManager.getInstance(project).modules.toMutableList().apply { add(0, Constant.dummyModule) }
        val tables = event.getData(LangDataKeys.PSI_ELEMENT_ARRAY)?.map { it as DbTable } ?: return

        Frame(DialogConstructor(project), FrameData(), project, modules, tables)
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
