package top.fallenangel.jimmergenerator.component

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.ProjectManager
import top.fallenangel.jimmergenerator.model.setting.SettingStorage
import top.fallenangel.jimmergenerator.util.JSON
import top.fallenangel.jimmergenerator.util.ResourceUtil

@State(name = "JimmerGeneratorSetting", storages = [Storage("jimmer-generator-setting.xml")])
class SettingStorageComponent : PersistentStateComponent<SettingStorage> {
    private var settingStorage: SettingStorage

    init {
        settingStorage = JSON.parseObject(ResourceUtil.getResourceAsString("/setting.json"), SettingStorage::class.java)
    }

    override fun getState(): SettingStorage {
        return settingStorage
    }

    override fun loadState(state: SettingStorage) {
        settingStorage = state
    }

    companion object {
        private val storage: SettingStorageComponent = ProjectManager.getInstance().defaultProject.getService(SettingStorageComponent::class.java)

        val tableDefaultAnnotations = storage.state.tableDefaultAnnotations
        val typeMappings = storage.state.typeMappings

        val javaTypes = (typeMappings.map { it.java } + typeMappings.map { it.javaPrimitives })
                .filterNotNull()
                .filter { it.isNotBlank() }
                .sortedBy { it.lowercase() }
        val kotlinTypes = typeMappings.map { it.kotlin }.sortedBy { it.lowercase() }
    }
}
