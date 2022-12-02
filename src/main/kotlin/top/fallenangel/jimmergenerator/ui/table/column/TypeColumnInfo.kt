package top.fallenangel.jimmergenerator.ui.table.column

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.ColumnInfo
import top.fallenangel.jimmergenerator.ui.table.MappingData
import javax.swing.DefaultCellEditor
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class TypeColumnInfo(name: String) : ColumnInfo<MappingData, String>(name) {
    override fun setValue(item: MappingData, value: String) {
        item.type = value
    }

    override fun valueOf(item: MappingData) = item.type

    override fun getRenderer(item: MappingData): TableCellRenderer {
        return TableCellRenderer { _, value, _, _, _, _ ->
            return@TableCellRenderer JBTextField(value as String)
        }
    }

    override fun isCellEditable(item: MappingData) = true

    override fun getEditor(item: MappingData): TableCellEditor {
        val editor = DefaultCellEditor(ComboBox(arrayOf("String", "Int", "Boolean")))
        editor.clickCountToStart = 2
        return editor
    }
}
