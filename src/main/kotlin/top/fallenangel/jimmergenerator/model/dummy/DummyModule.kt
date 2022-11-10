package top.fallenangel.jimmergenerator.model.dummy

import com.intellij.mock.MockComponentManager
import com.intellij.openapi.command.impl.DummyProject
import com.intellij.openapi.module.Module
import com.intellij.psi.search.NonClasspathDirectoriesScope
import top.fallenangel.jimmergenerator.util.Constant
import java.nio.file.Path

@Suppress("UnstableApiUsage", "OVERRIDE_DEPRECATION")
class DummyModule(private val moduleName: String) : MockComponentManager(null, Constant.disposable), Module {
    override fun getName() = moduleName

    override fun toString() = "Module: $moduleName"

    override fun getModuleFile() = null

    override fun getModuleNioFile(): Path = Path.of(".")

    override fun getProject() = DummyProject.getInstance()

    override fun isLoaded() = true

    override fun setOption(key: String, value: String?) {}

    override fun getOptionValue(key: String) = null

    override fun getModuleScope() = NonClasspathDirectoriesScope(emptyList())

    override fun getModuleScope(includeTests: Boolean) = NonClasspathDirectoriesScope(emptyList())

    override fun getModuleWithLibrariesScope() = NonClasspathDirectoriesScope(emptyList())

    override fun getModuleWithDependenciesScope() = NonClasspathDirectoriesScope(emptyList())

    override fun getModuleContentScope() = NonClasspathDirectoriesScope(emptyList())

    override fun getModuleContentWithDependenciesScope() = NonClasspathDirectoriesScope(emptyList())

    override fun getModuleWithDependenciesAndLibrariesScope(includeTests: Boolean) = NonClasspathDirectoriesScope(emptyList())

    override fun getModuleWithDependentsScope() = NonClasspathDirectoriesScope(emptyList())

    override fun getModuleTestsWithDependentsScope() = NonClasspathDirectoriesScope(emptyList())

    override fun getModuleRuntimeScope(includeTests: Boolean) = NonClasspathDirectoriesScope(emptyList())
}
