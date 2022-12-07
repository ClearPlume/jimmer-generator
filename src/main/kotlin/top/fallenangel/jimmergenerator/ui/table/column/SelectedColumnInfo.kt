package top.fallenangel.jimmergenerator.ui.table.column

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ThreeStateCheckBox
import top.fallenangel.jimmergenerator.model.DbObj
import top.fallenangel.jimmergenerator.ui.table.TableReference
import javax.swing.DefaultCellEditor
import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.tree.TreePath

class SelectedColumnInfo(private val tableRef: TableReference, name: String) : ColumnInfo<DbObj, Boolean>(name) {
    override fun setValue(item: DbObj, value: Boolean) {
        when {
            item.isTable && value -> {
                item.selected = true
                item.children.forEach { it.selected = true }
                tableRef.table.tableModel.valueForPathChanged(TreePath(item.path), true)
            }

            !item.isTable || value -> {
                item.selected = value
                item.children.forEach { it.selected = value }
                tableRef.table.tableModel.valueForPathChanged(TreePath(item.path), value)
            }

            else -> {
                val allSelected = item.children.all { it.selected }
                if (allSelected) {
                    item.selected = false
                    item.children.forEach { it.selected = false }
                    tableRef.table.tableModel.valueForPathChanged(TreePath(item.path), false)
                } else {
                    item.selected = true
                    item.children.forEach { it.selected = true }
                    tableRef.table.tableModel.valueForPathChanged(TreePath(item.path), true)
                }
            }
        }
    }

    override fun valueOf(item: DbObj): Boolean {
        return item.selected
    }

    override fun getRenderer(item: DbObj): TableCellRenderer {
        return TableCellRenderer { _, value, _, _, _, _ ->
            return@TableCellRenderer if (!item.isTable) {
                ThreeStateCheckBox(if (value as Boolean) ThreeStateCheckBox.State.SELECTED else ThreeStateCheckBox.State.NOT_SELECTED)
            } else {
                val allSelected = item.children.all { it.selected }
                if (allSelected) {
                    ThreeStateCheckBox(ThreeStateCheckBox.State.SELECTED)
                } else {
                    val allNotSelected = item.children.all { !it.selected }
                    if (allNotSelected) {
                        ThreeStateCheckBox(ThreeStateCheckBox.State.NOT_SELECTED)
                    } else {
                        ThreeStateCheckBox(ThreeStateCheckBox.State.DONT_CARE)
                    }
                }
            }
        }
    }

    override fun getWidth(table: JTable) = 20

    override fun isCellEditable(item: DbObj) = true

    override fun getEditor(item: DbObj) = DefaultCellEditor(JBCheckBox())
}
