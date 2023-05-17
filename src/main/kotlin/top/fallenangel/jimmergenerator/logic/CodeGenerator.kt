package top.fallenangel.jimmergenerator.logic

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import top.fallenangel.jimmergenerator.model.DbObj
import top.fallenangel.jimmergenerator.model.Context.dbType
import top.fallenangel.jimmergenerator.model.Context.project
import top.fallenangel.jimmergenerator.model.FrameData
import top.fallenangel.jimmergenerator.util.ResourceUtil
import top.fallenangel.jimmergenerator.util.message
import top.fallenangel.jimmergenerator.util.ui
import java.io.InputStreamReader
import java.io.StringWriter

class CodeGenerator(private val data: FrameData, private val tables: List<DbObj>) {
    fun generate(): Boolean {
        val selectedPackage = data.`package`
        val selectedPath = "${data.sourceRoot.path}/${selectedPackage.replace('.', '/')}"
        val fileExt = data.language.fileExt

        // 保存选中的表
        val velocityEngine = VelocityEngine()
        val selectedTables = tables.filter { it.selected }
        selectedTables.forEach {
            val selectedColumns = it.children.filter { column -> column.selected }
            // 计算实体和属性的注解列表
            it.captureAnnotations(data.language, dbType)
            selectedColumns.forEach { column -> column.captureAnnotations(data.language, dbType) }

            // 计算每张表的注解导包列表
            val tableAnnotations = it.annotations + selectedColumns.map { column -> column.annotations }.flatten()
            val annotationImports = tableAnnotations.map { annotation -> annotation.import }
            val parameterImports = tableAnnotations.map { annotation -> annotation.parameters }
                    .flatten()
                    .filter { param -> param.type.`package`.isNotBlank() }
                    .map { param -> param.type.import }

            // 计算每张表的类型导包列表
            val fieldImports = selectedColumns
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
                Messages.showWarningDialog(project, message("save_path_not_exists"), ui("warning"))
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
        Messages.showInfoMessage(project, message("entities_generate_success_info"), ui("tips"))
        return true
    }
}
