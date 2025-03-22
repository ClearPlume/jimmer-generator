package top.fallenangel.jimmergenerator.ui

import com.intellij.ide.util.PackageChooserDialog
import com.intellij.openapi.module.Module
import com.intellij.openapi.observable.util.whenItemSelected
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dualView.TreeTableView
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.UIUtil
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.logic.CodeGenerator
import top.fallenangel.jimmergenerator.model.Context.Companion.project
import top.fallenangel.jimmergenerator.model.DbObj
import top.fallenangel.jimmergenerator.model.FrameData
import top.fallenangel.jimmergenerator.model.type.Class
import top.fallenangel.jimmergenerator.ui.table.*
import top.fallenangel.jimmergenerator.util.*
import javax.swing.SwingConstants
import javax.swing.tree.TreePath

class Frame(private val modules: List<Module>, private val tables: List<DbObj>) {
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
        // 基本设置块
        group(ui("split_basic_setting")) {
            val sourceRoots = mutableListOf(Constant.dummyFile)
            sourceRoots.addAll(ModuleRootManager.getInstance(modules[0]).getSourceRoots(false))
            val sourceRootModel = CollectionComboBoxModel(sourceRoots)

            row {
                // 语言选择单选框
                panel {
                    buttonsGroup {
                        row(ui("label_language")) {
                            radioButton(ui("radio_java"), Language.JAVA)
                            radioButton(ui("radio_kotlin"), Language.KOTLIN)
                        }
                    }
                            .bind(data::language)
                }

                // 模块选择下拉框
                panel {
                    row(ui("label_module")) {
                        comboBox(modules, SimpleListCellRenderer.create { label, value, _ -> label.text = value.name })
                                .bindItem(data::module.toNullableProperty())
                                .component
                                .whenItemSelected { module ->
                                    sourceRoots.clear()
                                    sourceRoots.add(Constant.dummyFile)
                                    sourceRoots.addAll(ModuleRootManager.getInstance(module).getSourceRoots(false))

                                    data.sourceRoot = Constant.dummyFile
                                    sourceRootModel.selectedItem = Constant.dummyFile
                                }
                    }
                }
            }

            row {
                panel {
                    // Source选择下拉框
                    row(ui("label_module_source")) {
                        comboBox(
                            sourceRootModel,
                            SimpleListCellRenderer.create { label, value, _ ->
                                label.text = value.name
                                label.toolTipText = value.path
                            },
                        )
                                .bindItem(data::sourceRoot.toNullableProperty())
                    }
                }

                panel {
                    // 包选择弹窗
                    row(ui("label_package")) {
                        val packageText = textField().bindText(data::`package`).component
                        button(ui("button_choose")) {
                            panel.apply()
                            if (data.sourceRoot == Constant.dummyFile) {
                                Messages.showWarningDialog(project, message("source_root_not_select_warning"), ui("warning"))
                                return@button
                            }
                            val chooser = PackageChooserDialog(ui("dialog_title_package"), data.module)
                            if (chooser.showAndGet()) {
                                packageText.text = chooser.selectedPackage.qualifiedName
                            }
                        }
                    }
                }
            }
        }

        // 命名设置块
        group(ui("split_naming_setting")) {
            row {
                textField().bindText(data::tablePrefix)
                        .label(ui("label_remove_table_prefix"))
                        .comment(ui("comment_split_naming"), -1)

                textField().bindText(data::tableSuffix)
                        .label(ui("label_remove_table_suffix"))
            }
            row {
                textField().bindText(data::entityPrefix)
                        .label(ui("label_add_entity_prefix"))

                textField().bindText(data::entitySuffix)
                        .label(ui("label_add_entity_suffix"))
            }
            row {
                textField().bindText(data::fieldPrefix)
                        .label(ui("label_remove_field_prefix"))
                        .comment(ui("comment_split_naming"), -1)

                textField().bindText(data::fieldSuffix)
                        .label(ui("label_remove_field_suffix"))
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
                }
                        .align(Align.FILL)
                        .comment(ui("comment_apply_naming_setting_button"), -1)
            }
        }

        // 实体信息设置表格
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
                setTreeCellRenderer { _, value, selected, _, _, _, _ ->
                    if (value is DbObj) {
                        JBLabel(value.objName, value.icon, SwingConstants.LEADING).apply {
                            foreground = if (selected) JBColor.WHITE else JBColor.BLACK
                        }
                    } else {
                        JBLabel("")
                    }
                }
                preferredScrollableViewportSize = JBDimension(-1, 300)
            }
            scrollCell(tableRef.value).apply {
                align(Align.FILL)
                comment(ui("comment_split_table"), 120)
            }
        }
    }
}
