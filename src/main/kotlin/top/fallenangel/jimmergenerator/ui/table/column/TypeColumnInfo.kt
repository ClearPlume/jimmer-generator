package top.fallenangel.jimmergenerator.ui.table.column

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.ColumnInfo
import top.fallenangel.jimmergenerator.component.SettingStorageComponent
import top.fallenangel.jimmergenerator.model.DbObj
import top.fallenangel.jimmergenerator.model.type.Class
import javax.swing.DefaultCellEditor
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class TypeColumnInfo(name: String) : ColumnInfo<DbObj, String>(name) {
    override fun setValue(item: DbObj, value: String) {
        item.type = Class(
            value.substringAfterLast('.'),
            value.substringBeforeLast('.', ""),
            item.type.nullable
        )
    }

    override fun valueOf(item: DbObj) = item.type.name

    override fun getRenderer(item: DbObj): TableCellRenderer {
        return TableCellRenderer { _, value, _, _, _, _ ->
            return@TableCellRenderer JBTextField(value as String)
        }
    }

    override fun isCellEditable(item: DbObj) = !item.isTable

    override fun getEditor(item: DbObj): TableCellEditor {
        val typeMappings = SettingStorageComponent.storage.state.typeMappings
        val types = (typeMappings.map { it.java } + typeMappings.map { it.javaPrimitives }).filterNot { it.isNullOrBlank() }.toTypedArray()
        val editor = DefaultCellEditor(ComboBox(types))
        editor.clickCountToStart = 2
        return editor
    }
}
