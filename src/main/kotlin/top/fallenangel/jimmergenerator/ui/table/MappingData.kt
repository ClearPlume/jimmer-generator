package top.fallenangel.jimmergenerator.ui.table

import com.intellij.ui.CheckedTreeNode

data class MappingData(
    var selected: Boolean,
    val obj: String,
    var property: String,
    var type: String,
    val children: MutableList<MappingData>
) : CheckedTreeNode()
