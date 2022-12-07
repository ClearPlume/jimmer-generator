package top.fallenangel.jimmergenerator.ui

import com.intellij.codeInsight.actions.ReformatCodeProcessor
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
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dualView.TreeTableView
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.buttonGroup
import com.intellij.ui.layout.panel
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo
import com.intellij.util.ui.UIUtil
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.model.DBType
import top.fallenangel.jimmergenerator.model.DbObj
import top.fallenangel.jimmergenerator.model.dummy.DummyVirtualFile
import top.fallenangel.jimmergenerator.model.type.Class
import top.fallenangel.jimmergenerator.ui.table.TableReference
import top.fallenangel.jimmergenerator.ui.table.column.BusinessKeyColumnInfo
import top.fallenangel.jimmergenerator.ui.table.column.PropertyColumnInfo
import top.fallenangel.jimmergenerator.ui.table.column.SelectedColumnInfo
import top.fallenangel.jimmergenerator.ui.table.column.TypeColumnInfo
import top.fallenangel.jimmergenerator.util.Constant
import top.fallenangel.jimmergenerator.util.NameUtil
import top.fallenangel.jimmergenerator.util.ResourceUtil
import top.fallenangel.jimmergenerator.util.field2property
import java.awt.event.ItemEvent
import java.io.InputStreamReader
import java.io.StringWriter
import javax.swing.SwingConstants
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreePath

class Frame(private val project: Project, private val modules: List<Module>, private val tables: List<DbObj>, dbType: DBType) {
    private val data = FrameData()
    private val uiBundle = Constant.uiBundle
    private val messageBundle = Constant.messageBundle

    private val tableRef = TableReference()
    private val root = CheckedTreeNode()
    private val panel: DialogPanel = centerPanel()

    init {
        DialogConstructor(project).apply {
            title = uiBundle.getString("dialog_title")
            centerPanel(panel)

            okText(uiBundle.getString("button_ok"))
            ok {
                panel.apply()
                val languageConfirmed = Messages.showYesNoDialog(
                    project,
                    messageBundle.getString("confirm_current_language") + NameUtil.sneak2camel(data.language.name),
                    uiBundle.getString("tips"),
                    UIUtil.getQuestionIcon()
                )
                if (languageConfirmed == Messages.NO) {
                    return@ok false
                }
                if (data.module == Constant.dummyModule) {
                    Messages.showWarningDialog(project, messageBundle.getString("module_not_select_warning"), uiBundle.getString("warning"))
                    return@ok false
                }
                if (data.sourceRoot == Constant.dummyFile) {
                    Messages.showWarningDialog(project, messageBundle.getString("source_root_not_select_warning"), uiBundle.getString("warning"))
                    return@ok false
                }
                generateCode()
                return@ok true
            }
            cancelText(uiBundle.getString("button_cancel"))
            exhibit()
        }
    }

    private fun centerPanel() = panel {
        titledRow(uiBundle.getString("split_basic_setting")) {
            val sourceRoots = mutableListOf(Constant.dummyFile)

            row(uiBundle.getString("label_language")) {
                cell {
                    buttonGroup(data::language) {
                        radioButton(uiBundle.getString("radio_java"), Language.JAVA)
                        radioButton(uiBundle.getString("radio_kotlin"), Language.KOTLIN)
                    }
                }
            }

            row(uiBundle.getString("label_module")) {
                val renderer = SimpleListCellRenderer.create<Module> { label, value, _ -> label.text = value.name }
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

            row(uiBundle.getString("label_module_source")) {
                val renderer = SimpleListCellRenderer.create<VirtualFile> { label, value, _ ->
                    label.text = if (value is DummyVirtualFile) {
                        value.name
                    } else {
                        (value as VirtualFile).path
                    }
                }
                comboBox(MutableCollectionComboBoxModel(sourceRoots), data::sourceRoot, renderer).constraints(CCFlags.growX)
            }

            row(uiBundle.getString("label_package")) {
                cell(isFullWidth = true) {
                    val packageText = textField(data::`package`, 50).constraints(CCFlags.growX).component
                    button(uiBundle.getString("button_choose")) {
                        panel.apply()
                        if (data.module == Constant.dummyModule) {
                            Messages.showWarningDialog(project, messageBundle.getString("module_not_select_warning"), uiBundle.getString("warning"))
                            return@button
                        }
                        if (data.sourceRoot == Constant.dummyFile) {
                            Messages.showWarningDialog(project, messageBundle.getString("source_root_not_select_warning"), uiBundle.getString("warning"))
                            return@button
                        }
                        val chooser = PackageChooserDialog(uiBundle.getString("dialog_title_package"), data.module)
                        if (chooser.showAndGet()) {
                            packageText.text = chooser.selectedPackage.qualifiedName
                            data.`package` = chooser.selectedPackage.qualifiedName
                        }
                    }
                }
            }
        }

        titledRow(uiBundle.getString("split_naming_setting")) {
            row {
                label(uiBundle.getString("label_remove_table_prefix"))
                        .comment(uiBundle.getString("comment_split_naming"))
                textField(data::tablePrefix).growPolicy(GrowPolicy.MEDIUM_TEXT)

                label(uiBundle.getString("label_remove_table_suffix")).withLargeLeftGap()
                textField(data::tableSuffix).growPolicy(GrowPolicy.MEDIUM_TEXT)
            }
            row {
                label(uiBundle.getString("label_add_entity_prefix"))
                textField(data::entityPrefix).growPolicy(GrowPolicy.MEDIUM_TEXT)

                label(uiBundle.getString("label_add_entity_suffix")).withLargeLeftGap()
                textField(data::entitySuffix).growPolicy(GrowPolicy.MEDIUM_TEXT)
            }
            row {
                label(uiBundle.getString("label_remove_field_prefix"))
                        .comment(uiBundle.getString("comment_split_naming"))
                textField(data::fieldPrefix).growPolicy(GrowPolicy.MEDIUM_TEXT)

                label(uiBundle.getString("label_remove_field_suffix")).withLargeLeftGap()
                textField(data::fieldSuffix).growPolicy(GrowPolicy.MEDIUM_TEXT)
            }
            row {
                button(uiBundle.getString("button_apply_naming_setting")) {
                    panel.apply()
                    val tables = root.children().toList().map { it as DbObj }
                    tables.forEach { table ->
                        val entityName = table.name.field2property(data.tablePrefix, data.tableSuffix)
                        table.property = "${data.entityPrefix}$entityName${data.entitySuffix}"
                        table.type = Class("${data.entityPrefix}$entityName${data.entitySuffix}")
                        table.children.forEach {
                            it.property = it.name.field2property(data.fieldPrefix, data.fieldSuffix, true)
                        }
                    }
                    tableRef.table.tableModel.valueForPathChanged(TreePath(root.path), null)
                }.constraints(CCFlags.growX)
            }
        }

        titledRow(uiBundle.getString("split_table_mapping")) {
            row {
                val columns = arrayOf(
                    SelectedColumnInfo(tableRef, ""),
                    TreeColumnInfo(uiBundle.getString("column_obj_name")),
                    PropertyColumnInfo(uiBundle.getString("column_property_name")),
                    TypeColumnInfo(uiBundle.getString("column_property_type")),
                    BusinessKeyColumnInfo(uiBundle.getString("column_business_key"))
                )
                tables.forEach { root.add(it) }
                val tableModel = ListTreeTableModelOnColumns(root, columns)
                tableRef.table = TreeTableView(tableModel).apply {
                    rowHeight = 26
                    setTreeCellRenderer(
                        TreeCellRenderer { _, value, selected, _, _, _, _ ->
                            if (value !is DbObj) return@TreeCellRenderer JBLabel("")
                            return@TreeCellRenderer JBLabel(value.name, value.icon, SwingConstants.LEADING).apply {
                                foreground = if (selected) JBColor.WHITE else JBColor.BLACK
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
            val table = DbObj(null, true, tableEntityName, "", false, Class(""), mutableListOf(), it.remark)
            val velocityContext = VelocityContext().apply {
                put("package", selectedPackage)
                put("importList", emptyList<String>())
                put("table", table)
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
        Messages.showInfoMessage(project, messageBundle.getString("entities_generate_success_info"), uiBundle.getString("tips"))
    }
}

data class FrameData(
    var language: Language = Language.JAVA,
    var module: Module = Constant.dummyModule,
    var sourceRoot: VirtualFile = Constant.dummyFile,
    var `package`: String = "",
    var tablePrefix: String = "",
    var tableSuffix: String = "",
    var fieldPrefix: String = "",
    var fieldSuffix: String = "",
    var entityPrefix: String = "",
    var entitySuffix: String = ""
)
