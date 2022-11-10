package top.fallenangel.jimmergenerator.ui

import com.intellij.ide.util.PackageChooserDialog
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.buttonGroup
import com.intellij.ui.layout.panel
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.model.dummy.DummyVirtualFile
import top.fallenangel.jimmergenerator.util.Constant

fun frame(project: Project, data: FrameData, modules: List<Module>) = panel {
    val sourceRoots = listOf(Constant.dummyFile)

    lateinit var sourceRootSelect: ComboBox<VirtualFile>

    row(Constant.uiBundle.getString("label_language")) {
        cell {
            radioGroup(data::language) {
                radio("Java", Language.JAVA, data::language)
                radio("Kotlin", Language.KOTLIN, data::language)
            }
            buttonGroup(data::`package`) {
                radioButton("", "")
            }
        }
    }

    row(Constant.uiBundle.getString("label_module")) {
        select(modules, SimpleListCellRenderer.create { label, value, _ -> label.text = value.name }, data::module) {
            sourceRootSelect.removeAllItems()
            sourceRootSelect.addItem(Constant.dummyFile)
            ModuleRootManager.getInstance(it).getSourceRoots(false).forEach { root -> sourceRootSelect.addItem(root) }
        }
    }

    row(Constant.uiBundle.getString("label_module_source")) {
        select(MutableCollectionComboBoxModel(sourceRoots.toMutableList()), SimpleListCellRenderer.create { label, value, _ ->
            label.text = if (value is DummyVirtualFile) {
                value.name
            } else {
                (value as VirtualFile).path
            }
        }, data::sourceRoot, { sourceRootSelect = it })
    }

    row(Constant.uiBundle.getString("label_package")) {
        cell(isFullWidth = true) {
            textField(data::`package`, 50).constraints(CCFlags.growX)
            button(Constant.uiBundle.getString("button_choose")) {
                if (data.module == Constant.dummyModule) {
                    Messages.showWarningDialog(project, Constant.messageBundle.getString("module_not_select_warning"), Constant.uiBundle.getString("warning"))
                    return@button
                }
                val chooser = PackageChooserDialog(Constant.uiBundle.getString("dialog_title_package"), data.module)
                if (chooser.showAndGet()) {
                    data.`package` = chooser.selectedPackage.qualifiedName
                }
            }
        }
    }

    row("Data binding)Language: ") {
        cell {
            buttonGroup(data::language) {
                radioButton("Java", Language.JAVA)
                radioButton("Kotlin", Language.KOTLIN)
            }
        }
    }

    row {
        text(property = data::text)
    }

    row {
        button("data") {
            Messages.showInfoMessage(data.toString(), "Info")
        }
    }
}

data class FrameData(
    var language: Language = Language.UNKNOWN,
    var module: Module = Constant.dummyModule,
    var sourceRoot: VirtualFile = Constant.dummyFile,
    var `package`: String = "",
    var text: String = ""
)
