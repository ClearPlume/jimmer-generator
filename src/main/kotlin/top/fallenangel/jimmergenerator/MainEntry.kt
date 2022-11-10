package top.fallenangel.jimmergenerator

import com.intellij.database.psi.DbElement
import com.intellij.database.psi.DbTable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import top.fallenangel.jimmergenerator.ui.DialogConstructor
import top.fallenangel.jimmergenerator.ui.FrameData
import top.fallenangel.jimmergenerator.ui.MainFrame
import top.fallenangel.jimmergenerator.ui.frame
import top.fallenangel.jimmergenerator.util.Constant

class MainEntry : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val modules = ModuleManager.getInstance(project).modules.toMutableList().apply { add(0, Constant.dummyModule) }
        val tables = event.getData(LangDataKeys.PSI_ELEMENT_ARRAY)?.map { it as DbTable } ?: return

        val data = FrameData()
        DialogConstructor(project).apply {
            title = Constant.uiBundle.getString("dialog_title")

            centerPanel(frame(project, data, modules))

            okText(Constant.uiBundle.getString("button_ok"))
            ok {
                Messages.showInfoMessage("Clicked ok button", "Message")
            }

            cancelText(Constant.uiBundle.getString("button_cancel"))
            cancel {
                Messages.showInfoMessage("Clicked cancel button", "Message")
            }

            exhibit()
        }

        Messages.showInfoMessage(data.toString(), "Info After Dialog")

        DialogBuilder(project).apply {
            MainFrame(this, project, modules, tables)
            removeAllActions()
            show()
        }
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
