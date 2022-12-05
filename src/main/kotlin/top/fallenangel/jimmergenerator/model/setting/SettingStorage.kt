package top.fallenangel.jimmergenerator.model.setting

import top.fallenangel.jimmergenerator.model.type.Annotation

data class SettingStorage(
    val typeMappings: List<TypeMapping>,
    val tableDefaultAnnotations: List<Annotation>
)
