package top.fallenangel.jimmergenerator.ui.table

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.ColumnInfo
import top.fallenangel.jimmergenerator.model.DbObj
import javax.swing.Box
import javax.swing.DefaultCellEditor
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

class BusinessKeyColumnInfo(name: String) : ColumnInfo<DbObj, Boolean>(name) {
    override fun setValue(item: DbObj, value: Boolean) {
        item.businessKey = value
    }

    override fun valueOf(item: DbObj): Boolean {
        return item.businessKey
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

    override fun getWidth(table: JTable) = 100

    override fun isCellEditable(item: DbObj) = !item.isTable

    override fun getEditor(item: DbObj) = DefaultCellEditor(JBCheckBox())
}
