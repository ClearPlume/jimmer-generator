package top.fallenangel.jimmergenerator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper

@Suppress("unused")
class DialogConstructor(project: Project) : DialogWrapper(project) {
    private lateinit var panel: DialogPanel
    private lateinit var okEvent: () -> Boolean
    private lateinit var cancelEvent: () -> Boolean

    fun centerPanel(panel: DialogPanel) {
        this.panel = panel
    }

    fun okText(text: String) = setOKButtonText(text)

    fun ok(event: () -> Boolean) {
        okEvent = event
    }

    fun cancelText(text: String) = setCancelButtonText(text)

    fun cancel(event: () -> Boolean) {
        cancelEvent = event
    }

    fun exhibit() {
        init()
        show()
    }

    override fun createCenterPanel() = panel

    override fun doOKAction() {
        if (::okEvent.isInitialized) {
            if (okEvent()) {
                super.doOKAction()
            }
        } else {
            super.doOKAction()
        }
    }

    override fun doCancelAction() {
        if (::cancelEvent.isInitialized) {
            if (cancelEvent()) {
                super.doCancelAction()
            }
        } else {
            super.doCancelAction()
        }
    }
}
