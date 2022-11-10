package top.fallenangel.jimmergenerator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper

class DialogConstructor(project: Project) : DialogWrapper(project) {
    private lateinit var panel: DialogPanel
    private lateinit var okEvent: () -> Unit
    private lateinit var cancelEvent: () -> Unit

    fun centerPanel(panel: DialogPanel) {
        this.panel = panel
    }

    fun okText(text: String) = setOKButtonText(text)

    fun ok(event: () -> Unit) {
        okEvent = event
    }

    fun cancelText(text: String) = setCancelButtonText(text)

    fun cancel(event: () -> Unit) {
        cancelEvent = event
    }

    fun exhibit() {
        init()
        show()
    }

    override fun createCenterPanel() = panel

    override fun doOKAction() {
        okEvent()
        super.doOKAction()
    }

    override fun doCancelAction() {
        cancelEvent()
        super.doCancelAction()
    }
}
