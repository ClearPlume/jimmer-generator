package top.fallenangel.jimmergenerator.ui

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.database.psi.DbTable
import com.intellij.ide.util.PackageChooserDialog
import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.model.Table
import top.fallenangel.jimmergenerator.util.*
import java.awt.Component
import java.awt.event.ItemEvent
import java.io.InputStreamReader
import java.io.StringWriter
import java.util.*
import java.util.function.Consumer
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxRenderer

class MainFrame(private val dialog: DialogBuilder, private val project: Project, modules: List<Module>, private val tables: List<DbTable>) {
    private lateinit var panel: JPanel
    private lateinit var javaRadio: JRadioButton
    private lateinit var kotlinRadio: JRadioButton
    private lateinit var moduleCombo: JComboBox<Module>
    private lateinit var sourceRootCombo: JComboBox<VirtualFile>
    private lateinit var packageText: JTextField
    private lateinit var choosePackageButton: JButton
    private lateinit var okButton: JButton
    private lateinit var cancelButton: JButton

    private val messageBundle: ResourceBundle
    private val uiBundle: ResourceBundle

    // private var locale = Locale.SIMPLIFIED_CHINESE
    private var locale = Locale.ENGLISH
    private var language = Language.UNKNOWN
    private var selectedModule = Constant.dummyModule
    private var selectedSourceRoot = Constant.dummyFile

    init {
        messageBundle = ResourceBundle.getBundle("message", locale)
        uiBundle = ResourceBundle.getBundle("ui", locale)

        dialog.setCenterPanel(panel)
        ButtonGroup().apply {
            add(javaRadio)
            add(kotlinRadio)
        }

        moduleCombo.renderer = ModuleRender()
        // 默认空模块选项
        moduleCombo.addItem(Constant.dummyModule)
        modules.forEach(Consumer { item: Module -> moduleCombo.addItem(item) })

        sourceRootCombo.renderer = VirtualFileRender()
        // 默认空源路径选项
        sourceRootCombo.addItem(Constant.dummyFile)

        event()
    }

    private fun event() {
        // 选择语言事件
        javaRadio.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                language = Language.JAVA
            }
        }
        kotlinRadio.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                language = Language.KOTLIN
            }
        }

        // 选择模块事件
        moduleCombo.addItemListener { event ->
            val module = event.item as Module
            selectedModule = if (module == Constant.dummyModule) {
                Constant.dummyModule
            } else {
                sourceRootCombo.removeAllItems()
                sourceRootCombo.addItem(Constant.dummyFile)
                ModuleRootManager.getInstance(module).getSourceRoots(false).forEach { sourceRootCombo.addItem(it) }
                module
            }
        }

        // 选择资源路径事件
        sourceRootCombo.addItemListener { event ->
            val root = event.item as VirtualFile
            selectedSourceRoot = if (root == Constant.dummyFile) {
                Constant.dummyFile
            } else {
                root
            }
        }

        // 选择保存包事件
        choosePackageButton.addActionListener {
            if (selectedModule == Constant.dummyModule) {
                Messages.showWarningDialog(project, messageBundle.getString("module_not_select_warning"), uiBundle.getString("warning"))
                return@addActionListener
            }
            val chooser = PackageChooserDialog(uiBundle.getString("dialog_title_package"), selectedModule)
            if (chooser.showAndGet()) {
                packageText.text = chooser.selectedPackage.qualifiedName
            }
        }

        // 确定按钮事件
        okButton.addActionListener {
            if (language == Language.UNKNOWN) {
                Messages.showWarningDialog(project, messageBundle.getString("language_not_select_warning"), uiBundle.getString("warning"))
                return@addActionListener
            }
            if (selectedModule == Constant.dummyModule) {
                Messages.showWarningDialog(project, messageBundle.getString("module_not_select_warning"), uiBundle.getString("warning"))
                return@addActionListener
            }
            if (selectedSourceRoot == Constant.dummyFile) {
                Messages.showWarningDialog(project, messageBundle.getString("source_root_not_select_warning"), uiBundle.getString("warning"))
                return@addActionListener
            }
            generateCode()
            Messages.showInfoMessage(project, messageBundle.getString("entities_generate_success_info"), uiBundle.getString("tips"))
            closeFrame()
        }

        // 取消按钮事件
        cancelButton.addActionListener { closeFrame() }
    }

    private fun closeFrame() {
        dialog.dialogWrapper.close(DialogWrapper.CANCEL_EXIT_CODE)
    }

    private fun generateCode() {
        val selectedPackage = packageText.text
        val selectedPath = "${selectedModule.path()}/${selectedPackage.replace('.', '/')}"
        val fileExt = language.fileExt

        // 保存选中的表
        val velocityEngine = VelocityEngine()
        tables.forEach {
            val writer = StringWriter()
            val tableEntityName = NameUtil.sneak2camel(it.name)
            val table = Table(tableEntityName, it.fields(language), it.comment)
            val velocityContext = VelocityContext().apply {
                put("package", selectedPackage)
                put("importList", table.captureImportList())
                put("table", table.removeClassPackage())
            }

            val template = InputStreamReader(ResourceUtil.getResourceAsStream("/templates/$fileExt.vm"))
            velocityEngine.evaluate(velocityContext, writer, "Velocity Code Generate", template)

            val psiManager = PsiManager.getInstance(project)
            val classFile = WriteCommandAction.runWriteCommandAction(project, Computable {
                val selectVirtualPackage = LocalFileSystem.getInstance().findFileByPath(selectedPath)!!
                val selectedDirectory = psiManager.findDirectory(selectVirtualPackage)!!
                selectedDirectory.findFile("$tableEntityName.$fileExt")?.delete()

                selectVirtualPackage.createChildData(project, "$tableEntityName.$fileExt").apply {
                    setBinaryContent(writer.toString().toByteArray())
                }
            })
            psiManager.findFile(classFile)?.apply {
                ReformatCodeProcessor(project, this, null, false).run()
            }

            val selectVirtualPackage = LocalFileSystem.getInstance().findFileByPath(selectedPath)!!
            val baseTableEntityFileName = "${tableEntityName}Base.$fileExt"
            selectVirtualPackage.findChild(baseTableEntityFileName) ?: run {
                val baseWriter = StringWriter()
                val baseVelocityContext = VelocityContext().apply {
                    put("package", selectedPackage)
                    put("table", table)
                }
                val baseTemplate = InputStreamReader(ResourceUtil.getResourceAsStream("/templates/$fileExt-base.vm"))
                velocityEngine.evaluate(baseVelocityContext, baseWriter, "Velocity Code Generate Base", baseTemplate)
                val baseClassFile = WriteCommandAction.runWriteCommandAction(project, Computable {
                    selectVirtualPackage.createChildData(project, baseTableEntityFileName).apply {
                        setBinaryContent(baseWriter.toString().toByteArray())
                    }
                })
                psiManager.findFile(baseClassFile)?.apply {
                    ReformatCodeProcessor(project, this, null, false).run()
                }

                baseWriter.close()
                baseTemplate.close()
            }

            writer.close()
            template.close()
        }
    }

    private fun Module.path(): String {
        return ModuleRootManager.getInstance(this)
                .getSourceRoots(false)
                .find { source -> source.name.matches(Regex("^kotlin$|^java$")) && !source.path.contains("ksp") }!!
                .path
    }

    private class ModuleRender : BasicComboBoxRenderer() {
        override fun getListCellRendererComponent(list: JList<*>?, value: Any, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            text = (value as Module).name
            return this
        }
    }

    private class VirtualFileRender : BasicComboBoxRenderer() {
        override fun getListCellRendererComponent(list: JList<*>?, value: Any, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            text = if (value is MockVirtualFile) {
                value.name
            } else {
                (value as VirtualFile).path
            }
            return this
        }
    }
}
