package top.fallenangel.jimmergenerator.model

import com.intellij.database.model.DasColumn
import com.intellij.database.util.DasUtil
import com.intellij.ui.CheckedTreeNode
import top.fallenangel.jimmergenerator.model.type.Annotation
import top.fallenangel.jimmergenerator.model.type.Class

data class DbObj(
    val column: DasColumn?,
    var selected: Boolean,
    val name: String,
    var property: String,
    var type: Class,
    val annotations: MutableList<Annotation>,
    val remark: String?
) : CheckedTreeNode() {
    val isTable: Boolean
        get() = column == null

    val isPrimary: Boolean
        get() = DasUtil.isPrimary(column)

    val children: List<DbObj>
        get() = if (isTable) children.map { it as DbObj } else emptyList()
}
