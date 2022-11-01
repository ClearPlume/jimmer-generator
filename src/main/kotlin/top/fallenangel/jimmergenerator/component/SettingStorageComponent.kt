package top.fallenangel.jimmergenerator.component

import com.alibaba.fastjson2.to
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.ProjectManager
import top.fallenangel.jimmergenerator.util.ResourceUtil
import top.fallenangel.jimmergenerator.model.setting.SettingStorage

@State(name = "JimmerGeneratorSetting", storages = [Storage("jimmer-generator-setting.xml")])
class SettingStorageComponent : PersistentStateComponent<SettingStorage> {
    private var settingStorage: SettingStorage

    init {
        settingStorage = ResourceUtil.getResourceAsString("/setting.json").to<SettingStorage>()
    }

    override fun getState(): SettingStorage {
        return settingStorage
    }

    override fun loadState(state: SettingStorage) {
        settingStorage = state
    }

    companion object {
        val storage: SettingStorageComponent = ProjectManager.getInstance().defaultProject.getService(SettingStorageComponent::class.java)
    }
}
