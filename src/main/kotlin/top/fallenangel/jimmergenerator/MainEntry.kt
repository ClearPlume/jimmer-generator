package top.fallenangel.jimmergenerator

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.database.psi.DbElement
import com.intellij.database.psi.DbTable
import com.intellij.ide.util.PackageChooserDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ui.configuration.ChooseModulesDialog
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.model.Table
import java.io.InputStreamReader
import java.io.StringWriter

class MainEntry : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        // 选择生成哪种语言的实体类
        val language = if (Messages.OK == Messages.showOkCancelDialog("Select which language entity classes to generate", "Choose Language", "Java", "Kotlin", null)) {
            Language.JAVA
        } else {
            Language.KOTLIN
        }

        val project = event.project ?: return

        // 选择模块
        val modules = ModuleManager.getInstance(project).modules.asList()
        val selectedModule = with(ChooseModulesDialog(project, modules, "Module Chooser", "Select the module that needs to generate the Jimmer entity classes")) {
            setSingleSelectionMode()
            val selectedModules = showAndGetResult()
            if (selectedModules.isEmpty()) {
                Messages.showErrorDialog("A module must be selected!", "错误")
                return
            }
            selectedModules[0]
        }
        val selectedModulePath = ModuleRootManager.getInstance(selectedModule)
                .getSourceRoots(false)
                .find { it.name.matches(Regex("^kotlin$|^java$")) }!!
                .path

        // 选择保存包
        val packageChooser = PackageChooserDialog("Package Chooser", selectedModule).apply { show() }
        val selectedPackage = packageChooser.selectedPackage?.qualifiedName ?: ""
        val selectedPath = "$selectedModulePath/${selectedPackage.replace('.', '/')}"

        // 获取选中的表
        val tables = event.getData(LangDataKeys.PSI_ELEMENT_ARRAY)?.map { it as DbTable } ?: return
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

            val template = InputStreamReader(ResourceUtil.getResourceAsStream("/templates/${language.fileExt}.vm"))
            velocityEngine.evaluate(velocityContext, writer, "Velocity Code Generate", template)

            val psiManager = PsiManager.getInstance(project)
            val classFile = WriteCommandAction.runWriteCommandAction(project, Computable {
                val selectVirtualPackage = LocalFileSystem.getInstance().findFileByPath(selectedPath)!!
                val selectedDirectory = psiManager.findDirectory(selectVirtualPackage)!!
                selectedDirectory.findFile("$tableEntityName.${language.fileExt}")?.delete()

                selectVirtualPackage.createChildData(project, "$tableEntityName.${language.fileExt}").apply {
                    setBinaryContent(writer.toString().toByteArray())
                }
            })
            psiManager.findFile(classFile)?.apply {
                ReformatCodeProcessor(project, this, null, false).run()
            }

            val selectVirtualPackage = LocalFileSystem.getInstance().findFileByPath(selectedPath)!!
            val baseTableEntityFileName = "${tableEntityName}Base.${language.fileExt}"
            selectVirtualPackage.findChild(baseTableEntityFileName) ?: run {
                val baseWriter = StringWriter()
                val baseVelocityContext = VelocityContext().apply {
                    put("package", selectedPackage)
                    put("table", table)
                }
                val baseTemplate = InputStreamReader(ResourceUtil.getResourceAsStream("/templates/${language.fileExt}-base.vm"))
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
