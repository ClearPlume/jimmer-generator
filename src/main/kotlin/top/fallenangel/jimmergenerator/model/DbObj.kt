package top.fallenangel.jimmergenerator.model

import com.intellij.database.model.DasColumn
import com.intellij.database.util.DasUtil
import com.intellij.ui.CheckedTreeNode
import com.jetbrains.rd.util.EnumSet
import icons.DatabaseIcons
import top.fallenangel.jimmergenerator.model.type.Annotation
import top.fallenangel.jimmergenerator.model.type.Class
import javax.swing.Icon

data class DbObj(
    val column: DasColumn?,
    var selected: Boolean,
    val name: String,
    var property: String,
    var businessKey: Boolean,
    var type: Class,
    val annotations: MutableList<Annotation>,
    val remark: String?
) : CheckedTreeNode() {
    val isTable: Boolean
        get() = column == null

    val isPrimary: Boolean
        get() = DasUtil.isPrimary(column)

    val icon: Icon
        get() {
            val traits = EnumSet.noneOf(DbObjTrait::class.java)

            if (isTable) traits.add(DbObjTrait.TABLE) else traits.add(DbObjTrait.COLUMN)
            if (isPrimary) traits.add(DbObjTrait.PRIMARY)
            if (DasUtil.isIndexColumn(column)) traits.add(DbObjTrait.INDEX)
            if (DasUtil.isForeign(column)) traits.add(DbObjTrait.FOREIGN)
            if (column?.isNotNull == true) traits.add(DbObjTrait.NOTNULL)

            return when (traits.map { 1 shl it.ordinal }.reduce { t, trait -> t or trait }) {
                // TABLE
                0b000001 -> DatabaseIcons.Table
                // PRIMARY (INDEX + NOTNULL + COLUMN)
                0b110110 -> DatabaseIcons.ColGoldKeyDotIndex
                // INDEX + NOTNULL (COLUMN)
                0b110100 -> DatabaseIcons.ColDotIndex
                // INDEX (COLUMN)
                0b100100 -> DatabaseIcons.ColIndex
                // FOREIGN + NOTNULL (COLUMN)
                0b111000 -> DatabaseIcons.ColBlueKeyDot
                // FOREIGN (COLUMN)
                0b101000 -> DatabaseIcons.ColBlueKey
                // NOTNULL (COLUMN)
                0b110000 -> DatabaseIcons.ColDot
                // COLUMN
                else -> DatabaseIcons.Col
            }
        }

    val children: List<DbObj>
        get() = if (isTable) children.map { it as DbObj } else emptyList()
}
