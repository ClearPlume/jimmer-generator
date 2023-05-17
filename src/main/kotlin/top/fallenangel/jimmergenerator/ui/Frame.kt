package top.fallenangel.jimmergenerator.ui

import com.intellij.ide.util.PackageChooserDialog
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.JBColor
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dualView.TreeTableView
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.panel
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo
import com.intellij.util.ui.UIUtil
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.logic.CodeGenerator
import top.fallenangel.jimmergenerator.model.Context.Companion.project
import top.fallenangel.jimmergenerator.model.DbObj
import top.fallenangel.jimmergenerator.model.FrameData
import top.fallenangel.jimmergenerator.model.type.Class
import top.fallenangel.jimmergenerator.ui.table.*
import top.fallenangel.jimmergenerator.util.*
import java.awt.event.ItemEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.SwingConstants
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreePath

class Frame(private val modules: Array<Module>, private val tables: List<DbObj>) {
    private val data = FrameData().apply { module = modules[0] }

    private val languageRef = Reference(Language.JAVA)
    private val root = CheckedTreeNode()
    private val tableRef = Reference<TreeTableView>()
    private val panel: DialogPanel = centerPanel()

    init {
        DialogConstructor(project).apply {
            title = ui("dialog_title")
            centerPanel(panel)

            okText(ui("button_ok"))
            ok {
                panel.apply()
                if (tables.count { it.selected } == 0) {
                    Messages.showWarningDialog(project, message("table_not_select_warning"), ui("warning"))
                    return@ok false
                }
                val languageConfirmed = Messages.showYesNoDialog(
                    project,
                    message("confirm_current_language") + data.language.name.sneak2camel(),
                    ui("tips"),
                    UIUtil.getQuestionIcon()
                )
                if (languageConfirmed == Messages.NO) {
                    return@ok false
                }
                if (data.sourceRoot == Constant.dummyFile) {
                    Messages.showWarningDialog(project, message("source_root_not_select_warning"), ui("warning"))
                    return@ok false
                }
                return@ok CodeGenerator(data, tables).generate()
            }
            cancelText(ui("button_cancel"))
            exhibit()
        }
    }

    private fun centerPanel() = panel {
        titledRow(ui("split_basic_setting")) {
            val sourceRoots = mutableListOf(Constant.dummyFile)
            ModuleRootManager.getInstance(modules[0]).getSourceRoots(false).forEach { root -> sourceRoots.add(root) }

            row(ui("label_language")) {
                cell {
                    val languageChanged: (Language) -> Unit = {
                        languageRef.value = it
                        val tables = root.children().toList().map { table -> table as DbObj }
                        tables.forEach { table ->
                            table.children.forEach { column ->
                                column.type = column.column!!.captureType(it)
                            }
                        }
                        tableRef.value.tableModel.valueForPathChanged(TreePath(root.path), null)
                    }
                    radioGroup(data::language, languageChanged) {
                        radio(ui("radio_java"), Language.JAVA)
                        radio(ui("radio_kotlin"), Language.KOTLIN)
                    }
                }
            }

            row(ui("label_module")) {
                val renderer = SimpleListCellRenderer.create<Module> { label, value, _ -> label.text = value.name }
                comboBox(DefaultComboBoxModel(modules), data::module, renderer)
                        .constraints(CCFlags.growX)
                        .component.addItemListener {
                            if (it.stateChange == ItemEvent.SELECTED) {
                                sourceRoots.clear()
                                sourceRoots.add(Constant.dummyFile)
                                ModuleRootManager.getInstance(it.item as Module).getSourceRoots(false).forEach { root -> sourceRoots.add(root) }
                            }
                        }
            }

            row(ui("label_module_source")) {
                val renderer = SimpleListCellRenderer.create<VirtualFile> { label, value, _ ->
                    label.text = value.path
                }
                comboBox(MutableCollectionComboBoxModel(sourceRoots), data::sourceRoot, renderer).constraints(CCFlags.growX)
            }

            row(ui("label_package")) {
                cell(isFullWidth = true) {
                    val packageText = textField(data::`package`, 50).constraints(CCFlags.growX).component
                    button(ui("button_choose")) {
                        panel.apply()
                        if (data.sourceRoot == Constant.dummyFile) {
                            Messages.showWarningDialog(project, message("source_root_not_select_warning"), ui("warning"))
                            return@button
                        }
                        val chooser = PackageChooserDialog(ui("dialog_title_package"), data.module)
                        if (chooser.showAndGet()) {
                            packageText.text = chooser.selectedPackage.qualifiedName
                            data.`package` = chooser.selectedPackage.qualifiedName
                        }
                    }
                }
            }
        }

        titledRow(ui("split_naming_setting")) {
            row {
                label(ui("label_remove_table_prefix"))
                        .comment(ui("comment_split_naming"), -1)
                textField(data::tablePrefix).growPolicy(GrowPolicy.MEDIUM_TEXT)

                label(ui("label_remove_table_suffix")).withLargeLeftGap()
                textField(data::tableSuffix).growPolicy(GrowPolicy.MEDIUM_TEXT)
            }
            row {
                label(ui("label_add_entity_prefix"))
                textField(data::entityPrefix).growPolicy(GrowPolicy.MEDIUM_TEXT)

                label(ui("label_add_entity_suffix")).withLargeLeftGap()
                textField(data::entitySuffix).growPolicy(GrowPolicy.MEDIUM_TEXT)
            }
            row {
                label(ui("label_remove_field_prefix"))
                        .comment(ui("comment_split_naming"), -1)
                textField(data::fieldPrefix).growPolicy(GrowPolicy.MEDIUM_TEXT)

                label(ui("label_remove_field_suffix")).withLargeLeftGap()
                textField(data::fieldSuffix).growPolicy(GrowPolicy.MEDIUM_TEXT)
            }
            row {
                button(ui("button_apply_naming_setting")) {
                    panel.apply()
                    val tables = root.children().toList().map { it as DbObj }
                    tables.forEach { table ->
                        val entityName = table.field2property(data.tablePrefix, data.tableSuffix)
                        table.property = "${data.entityPrefix}$entityName${data.entitySuffix}"
                        table.type = Class("${data.entityPrefix}$entityName${data.entitySuffix}")
                        table.children.forEach {
                            it.property = it.field2property(data.fieldPrefix, data.fieldSuffix, true)
                        }
                    }
                    tableRef.value.tableModel.valueForPathChanged(TreePath(root.path), null)
                }.constraints(CCFlags.growX).comment(ui("comment_apply_naming_setting_button"), -1)
            }
        }

        row {
            val columns = arrayOf(
                SelectedColumnInfo(tableRef, ""),
                TreeColumnInfo(ui("column_obj_name")),
                PropertyColumnInfo(tableRef, ui("column_property_name")),
                TypeColumnInfo(languageRef, ui("column_property_type")),
                BusinessKeyColumnInfo(ui("column_business_key")),
                NullableColumnInfo(ui("column_nullable"))
            )
            tables.forEach { root.add(it) }
            val tableModel = ListTreeTableModelOnColumns(root, columns)
            tableRef.value = TreeTableView(tableModel).apply {
                rowHeight = 26
                setTreeCellRenderer(
                    TreeCellRenderer { _, value, selected, _, _, _, _ ->
                        if (value !is DbObj) return@TreeCellRenderer JBLabel("")
                        return@TreeCellRenderer JBLabel(value.objName, value.icon, SwingConstants.LEADING).apply {
                            foreground = if (selected) JBColor.WHITE else JBColor.BLACK
                        }
                    }
                )
            }
            // 设置表格最小尺寸
            val tableSize = tableRef.value.preferredScrollableViewportSize
            tableSize.height = tableSize.height + 200
            tableRef.value.minimumSize = tableSize
            scrollPane(tableRef.value).apply {
                component.minimumSize = tableSize
                comment(ui("comment_split_table"), 120)
            }
        }
    }
}
