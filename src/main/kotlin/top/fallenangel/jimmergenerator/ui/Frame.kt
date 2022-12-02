package top.fallenangel.jimmergenerator.ui

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.database.psi.DbTable
import com.intellij.database.util.DasUtil
import com.intellij.ide.util.PackageChooserDialog
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dualView.TreeTableView
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.buttonGroup
import com.intellij.ui.layout.panel
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.model.Table
import top.fallenangel.jimmergenerator.model.dummy.DummyVirtualFile
import top.fallenangel.jimmergenerator.ui.table.MappingData
import top.fallenangel.jimmergenerator.ui.table.TableReference
import top.fallenangel.jimmergenerator.ui.table.column.PropertyColumnInfo
import top.fallenangel.jimmergenerator.ui.table.column.SelectedColumnInfo
import top.fallenangel.jimmergenerator.ui.table.column.TypeColumnInfo
import top.fallenangel.jimmergenerator.util.*
import java.awt.Color
import java.awt.event.ItemEvent
import java.io.InputStreamReader
import java.io.StringWriter
import javax.swing.tree.TreeCellRenderer

class Frame(dialog: DialogConstructor, private val data: FrameData, private val project: Project, private val modules: List<Module>, private val tables: List<DbTable>) {
    private val tableRef = TableReference()
    private val root = CheckedTreeNode()
    private val panel: DialogPanel = centerPanel()

    init {
        dialog.apply {
            title = Constant.uiBundle.getString("dialog_title")
            centerPanel(panel)

            okText(Constant.uiBundle.getString("button_ok"))
            ok {
                panel.apply()
                if (data.language == Language.UNKNOWN) {
                    Messages.showWarningDialog(project, Constant.messageBundle.getString("language_not_select_warning"), Constant.uiBundle.getString("warning"))
                    return@ok false
                }
                if (data.module == Constant.dummyModule) {
                    Messages.showWarningDialog(project, Constant.messageBundle.getString("module_not_select_warning"), Constant.uiBundle.getString("warning"))
                    return@ok false
                }
                if (data.sourceRoot == Constant.dummyFile) {
                    Messages.showWarningDialog(project, Constant.messageBundle.getString("source_root_not_select_warning"), Constant.uiBundle.getString("warning"))
                    return@ok false
                }
                generateCode()
                return@ok true
            }

            cancelText(Constant.uiBundle.getString("button_cancel"))

            exhibit()
        }
    }

    private fun centerPanel() = panel {
        titledRow(Constant.uiBundle.getString("label_split_general_setting")) {
            val sourceRoots = mutableListOf(Constant.dummyFile)

            row(Constant.uiBundle.getString("label_language")) {
                cell {
                    buttonGroup(data::language) {
                        radioButton(Constant.uiBundle.getString("radio_java"), Language.JAVA)
                        radioButton(Constant.uiBundle.getString("radio_kotlin"), Language.KOTLIN)
                    }
                }
            }

            row(Constant.uiBundle.getString("label_module")) {
                val renderer = SimpleListCellRenderer.create<Module?> { label, value, _ -> label.text = value?.name }
                comboBox(CollectionComboBoxModel(modules), data::module, renderer)
                        .constraints(CCFlags.growX)
                        .component.addItemListener {
                            if (it.stateChange == ItemEvent.SELECTED) {
                                sourceRoots.clear()
                                sourceRoots.add(Constant.dummyFile)
                                ModuleRootManager.getInstance(it.item as Module).getSourceRoots(false).forEach { root -> sourceRoots.add(root) }
                            }
                        }
            }

            row(Constant.uiBundle.getString("label_module_source")) {
                val renderer = SimpleListCellRenderer.create<VirtualFile> { label, value, _ ->
                    label.text = if (value is DummyVirtualFile) {
                        value.name
                    } else {
                        (value as VirtualFile).path
                    }
                }
                comboBox(MutableCollectionComboBoxModel(sourceRoots), data::sourceRoot, renderer).constraints(CCFlags.growX)
            }

            row(Constant.uiBundle.getString("label_package")) {
                cell(isFullWidth = true) {
                    val packageText = textField(data::`package`, 50).constraints(CCFlags.growX).component
                    button(Constant.uiBundle.getString("button_choose")) {
                        panel.apply()
                        if (data.module == Constant.dummyModule) {
                            Messages.showWarningDialog(project, Constant.messageBundle.getString("module_not_select_warning"), Constant.uiBundle.getString("warning"))
                            return@button
                        }
                        if (data.sourceRoot == Constant.dummyFile) {
                            Messages.showWarningDialog(project, Constant.messageBundle.getString("source_root_not_select_warning"), Constant.uiBundle.getString("warning"))
                            return@button
                        }
                        val chooser = PackageChooserDialog(Constant.uiBundle.getString("dialog_title_package"), data.module)
                        if (chooser.showAndGet()) {
                            packageText.text = chooser.selectedPackage.qualifiedName
                            data.`package` = chooser.selectedPackage.qualifiedName
                        }
                    }
                }
            }
        }

        titledRow(Constant.uiBundle.getString("label_split_tables")) {
            row {
                val columns = arrayOf(SelectedColumnInfo(tableRef, ""), TreeColumnInfo("Database Object"), PropertyColumnInfo("Entity Name"), TypeColumnInfo("Entity Type"))
                tables.forEach {
                    val tableColumns = mutableListOf<MappingData>()
                    val tableNode = MappingData(true, it.name, NameUtil.sneak2camel(it.name), "String", tableColumns)
                    DasUtil.getColumns(it).forEach { column ->
                        val tableColumn = MappingData(true, column.name, NameUtil.sneak2camel(column.name).uncapitalize(), "String", mutableListOf())
                        tableColumns.add(tableColumn)
                        tableNode.add(tableColumn)
                    }
                    root.add(tableNode)
                }
                val tableModel = ListTreeTableModelOnColumns(root, columns)
                tableRef.table = TreeTableView(tableModel).apply {
                    rowHeight = 26
                    setTreeCellRenderer(
                        TreeCellRenderer { _, value, selected, _, _, _, _ ->
                            return@TreeCellRenderer if (value is MappingData) {
                                JBLabel(value.obj).apply {
                                    foreground = if (selected) Color.WHITE else Color.BLACK
                                }
                            } else {
                                JBLabel("")
                            }
                        }
                    )
                }
                // 设置表格最小尺寸
                val tableSize = tableRef.table.preferredScrollableViewportSize
                tableSize.height = tableSize.height + 200
                tableRef.table.minimumSize = tableSize
                scrollPane(tableRef.table).component.minimumSize = tableSize
            }
        }
    }

    private fun generateCode() {
        val selectedPackage = data.`package`
        val selectedPath = "${data.sourceRoot.path}/${selectedPackage.replace('.', '/')}"
        val language = data.language
        val fileExt = data.language.fileExt

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
        Messages.showInfoMessage(project, Constant.messageBundle.getString("entities_generate_success_info"), Constant.uiBundle.getString("tips"))
    }
}

data class FrameData(
    var language: Language = Language.UNKNOWN,
    var module: Module = Constant.dummyModule,
    var sourceRoot: VirtualFile = Constant.dummyFile,
    var `package`: String = ""
)
