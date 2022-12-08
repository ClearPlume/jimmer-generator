package top.fallenangel.jimmergenerator.ui.table.column

import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.ColumnInfo
import top.fallenangel.jimmergenerator.model.DbObj
import top.fallenangel.jimmergenerator.model.type.Class
import top.fallenangel.jimmergenerator.ui.table.TableReference
import javax.swing.DefaultCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.tree.TreePath

class PropertyColumnInfo(private val tableRef: TableReference, name: String) : ColumnInfo<DbObj, String>(name) {
    override fun setValue(item: DbObj, value: String) {
        item.property = value
        if (item.isTable) {
            item.type = Class(value)
        }
        tableRef.table.tableModel.valueForPathChanged(TreePath(item.path), null)
    }

    override fun valueOf(item: DbObj) = item.property

    override fun getRenderer(item: DbObj): TableCellRenderer {
        return TableCellRenderer { _, value, _, _, _, _ ->
            return@TableCellRenderer JBTextField(value as String)
        }
    }

    override fun isCellEditable(item: DbObj) = true

    override fun getEditor(item: DbObj) = DefaultCellEditor(JBTextField())
}
