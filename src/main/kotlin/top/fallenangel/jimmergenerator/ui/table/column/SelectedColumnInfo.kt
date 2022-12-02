package top.fallenangel.jimmergenerator.ui.table.column

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.ColumnInfo
import top.fallenangel.jimmergenerator.ui.table.MappingData
import top.fallenangel.jimmergenerator.ui.table.TableReference
import javax.swing.DefaultCellEditor
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.TableCellRenderer
import javax.swing.tree.TreePath

class SelectedColumnInfo(private val tableRef: TableReference, name: String) : ColumnInfo<MappingData, Boolean>(name) {
    override fun setValue(item: MappingData, value: Boolean) {
        item.selected = value
        item.children.forEach { it.selected = value }
        SwingUtilities.invokeLater { tableRef.table.tableModel.valueForPathChanged(TreePath(item.path), value) }
    }

    override fun valueOf(item: MappingData): Boolean {
        return item.selected
    }

    override fun getRenderer(item: MappingData): TableCellRenderer {
        return TableCellRenderer { _, value, _, _, _, _ ->
            return@TableCellRenderer JBCheckBox(null, value as Boolean)
        }
    }

    override fun getWidth(table: JTable) = 20

    override fun isCellEditable(item: MappingData) = true

    override fun getEditor(item: MappingData) = DefaultCellEditor(JBCheckBox())
}
