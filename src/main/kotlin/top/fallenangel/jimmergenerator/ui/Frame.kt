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
import top.fallenangel.jimmergenerator.ui.table.*
import top.fallenangel.jimmergenerator.util.*
import java.awt.event.ItemEvent
import java.io.InputStreamReader
import java.io.StringWriter
import javax.swing.SwingConstants
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreePath

class Frame(private val project: Project, private val modules: List<Module>, private val tables: List<DbObj>, private val dbType: DBType) {
    private val data = FrameData()
    private val uiBundle = Constant.uiBundle
    private val messageBundle = Constant.messageBundle

    private val languageRef = Reference(Language.JAVA)
    private val root = CheckedTreeNode()
    private val tableRef = Reference<TreeTableView>()
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
                return@ok generateCode()
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
                                .component
                                .addItemListener {
                                    data.language = if (it.stateChange == ItemEvent.SELECTED) Language.KOTLIN else Language.JAVA
                                    languageRef.value = data.language
                                    val tables = root.children().toList().map { table -> table as DbObj }
                                    tables.forEach { table ->
                                        table.children.forEach { column ->
                                            column.type = column.column!!.captureType(data.language)
                                        }
                                    }
                                    tableRef.value.tableModel.valueForPathChanged(TreePath(root.path), null)
                                }
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
                        .comment(uiBundle.getString("comment_split_naming"), -1)
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
                        .comment(uiBundle.getString("comment_split_naming"), -1)
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
                    tableRef.value.tableModel.valueForPathChanged(TreePath(root.path), null)
                }.constraints(CCFlags.growX).comment(uiBundle.getString("comment_apply_naming_setting_button"), -1)
            }
        }

        row {
            val columns = arrayOf(
                SelectedColumnInfo(tableRef, ""),
                TreeColumnInfo(uiBundle.getString("column_obj_name")),
                PropertyColumnInfo(tableRef, uiBundle.getString("column_property_name")),
                TypeColumnInfo(languageRef, uiBundle.getString("column_property_type")),
                BusinessKeyColumnInfo(uiBundle.getString("column_business_key")),
                NullableColumnInfo(uiBundle.getString("column_nullable"))
            )
            tables.forEach { root.add(it) }
            val tableModel = ListTreeTableModelOnColumns(root, columns)
            tableRef.value = TreeTableView(tableModel).apply {
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
            val tableSize = tableRef.value.preferredScrollableViewportSize
            tableSize.height = tableSize.height + 200
            tableRef.value.minimumSize = tableSize
            scrollPane(tableRef.value).apply {
                component.minimumSize = tableSize
                comment(uiBundle.getString("comment_split_table"), 120)
            }
        }
    }

    private fun generateCode(): Boolean {
        val selectedPackage = data.`package`
        val selectedPath = "${data.sourceRoot.path}/${selectedPackage.replace('.', '/')}"
        val fileExt = data.language.fileExt

        // 保存选中的表
        val velocityEngine = VelocityEngine()
        tables.forEach {
            // 计算实体和属性的注解列表
            it.captureAnnotations(data.language, dbType)
            it.children.forEach { column -> column.captureAnnotations(data.language, dbType) }

            // 计算每张表的注解导包列表
            val tableAnnotations = it.annotations + it.children.map { column -> column.annotations }.flatten()
            val annotationImports = tableAnnotations.map { annotation -> annotation.import }
            val parameterImports = tableAnnotations.map { annotation -> annotation.parameters }
                    .flatten()
                    .filter { param -> param.type.`package`.isNotBlank() }
                    .map { param -> param.type.import }

            // 计算每张表的类型导包列表
            val fieldImports = it.children
                    .map { field -> field.type }
                    .filter { type -> type.`package`.isNotBlank() }
                    .map { type -> type.import }

            val writer = StringWriter()
            val tableEntityName = it.property
            val velocityContext = VelocityContext().apply {
                put("package", selectedPackage)
                put("importList", (annotationImports + parameterImports + fieldImports).toSet())
                put("table", it)
            }

            val template = InputStreamReader(ResourceUtil.getResourceAsStream("/templates/$fileExt.vm"))
            velocityEngine.evaluate(velocityContext, writer, "Velocity Code Generate", template)

            val psiManager = PsiManager.getInstance(project)
            val selectVirtualPackage = LocalFileSystem.getInstance().findFileByPath(selectedPath)
            if (selectVirtualPackage == null) {
                Messages.showWarningDialog(project, messageBundle.getString("save_path_not_exists"), uiBundle.getString("warning"))
                return false
            }
            val classFile = WriteCommandAction.runWriteCommandAction(project, Computable {
                val selectedDirectory = psiManager.findDirectory(selectVirtualPackage)!!
                selectedDirectory.findFile("$tableEntityName.$fileExt")?.delete()

                selectVirtualPackage.createChildData(project, "$tableEntityName.$fileExt").apply {
                    setBinaryContent(writer.toString().toByteArray())
                }
            })
            psiManager.findFile(classFile)?.apply {
                ReformatCodeProcessor(project, this, null, false).run()
            }

            val baseTableEntityFileName = "${tableEntityName}Base.$fileExt"
            selectVirtualPackage.findChild(baseTableEntityFileName) ?: run {
                val baseWriter = StringWriter()
                val baseVelocityContext = VelocityContext().apply {
                    put("package", selectedPackage)
                    put("table", it)
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
        return true
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
