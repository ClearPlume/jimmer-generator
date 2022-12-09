package top.fallenangel.jimmergenerator.ui.table

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.ColumnInfo
import top.fallenangel.jimmergenerator.model.DbObj
import javax.swing.Box
import javax.swing.DefaultCellEditor
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

class NullableColumnInfo(name: String) : ColumnInfo<DbObj, Boolean>(name) {
    override fun valueOf(item: DbObj): Boolean {
        return item.type.nullable
    }

    override fun getRenderer(item: DbObj): TableCellRenderer {
        return TableCellRenderer { _, value, _, _, _, _ ->
            return@TableCellRenderer if (item.isTable) {
                Box.createHorizontalBox()
            } else {
                JBCheckBox("", value as Boolean)
            }
        }
    }

    override fun getWidth(table: JTable) = 70

    override fun isCellEditable(item: DbObj) = false

    override fun getEditor(item: DbObj) = DefaultCellEditor(JBCheckBox())
}
