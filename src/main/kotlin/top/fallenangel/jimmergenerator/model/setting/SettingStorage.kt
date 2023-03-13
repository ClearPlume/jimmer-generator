package top.fallenangel.jimmergenerator.model.setting

import kotlinx.serialization.Serializable
import top.fallenangel.jimmergenerator.model.type.Annotation

@Serializable
data class SettingStorage(
    val typeMappings: List<TypeMapping>,
    val tableDefaultAnnotations: List<Annotation>
)
