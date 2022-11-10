package top.fallenangel.jimmergenerator.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.BaseBuilder
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.CellBuilder
import java.awt.event.ItemEvent
import javax.swing.ComboBoxModel
import javax.swing.ListCellRenderer
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.reflect.KMutableProperty

class RadioGroupBuilder<T>(property: KMutableProperty<T>)

fun <T> BaseBuilder.radioGroup(property: KMutableProperty<T>, init: RadioGroupBuilder<T>.() -> Unit) {
    RadioGroupBuilder(property).init()
}

fun <T> Cell.radio(text: String, value: T, property: KMutableProperty<T>, init: (JBRadioButton) -> Unit = {}, changed: (T) -> Unit = {}): CellBuilder<JBRadioButton> {
    val radio = JBRadioButton(text).apply {
        init(this)
        addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                property.setter.call(value)
                changed(value)
            }
        }
    }
    return radio()
}

fun <T> Cell.select(items: Array<T>, renderer: ListCellRenderer<T>, property: KMutableProperty<T>, init: (ComboBox<T>) -> Unit = {}, changed: (T) -> Unit = {}): CellBuilder<ComboBox<T>> {
    return select(items.toList(), renderer, property, init, changed)
}

fun <T> Cell.select(items: List<T>, renderer: ListCellRenderer<T>, property: KMutableProperty<T>, init: (ComboBox<T>) -> Unit = {}, changed: (T) -> Unit = {}): CellBuilder<ComboBox<T>> {
    return select(MutableCollectionComboBoxModel(items.toMutableList()), renderer, property, init, changed)
}

@Suppress("UNCHECKED_CAST")
fun <T> Cell.select(model: ComboBoxModel<T>, renderer: ListCellRenderer<T>, property: KMutableProperty<T>, init: (ComboBox<T>) -> Unit = {}, changed: (T) -> Unit = {}): CellBuilder<ComboBox<T>> {
    val combo = ComboBox(model).apply {
        this.renderer = renderer
        init(this)
        addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                property.setter.call(it.item)
                changed(it.item as T)
            }
        }
    }
    return combo().apply { constraints(CCFlags.growX) }
}

fun Cell.text(columns: Int = 0, property: KMutableProperty<String>, init: (JBTextField) -> Unit = {}, changed: (String) -> Unit = {}): CellBuilder<JBTextField> {
    val text = JBTextField(columns).apply {
        init(this)
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) {
                property.setter.call(text)
                changed(text)
            }

            override fun removeUpdate(event: DocumentEvent) {
                property.setter.call(text)
                changed(text)
            }

            override fun changedUpdate(event: DocumentEvent) {}
        })
    }
    return text().apply { constraints(CCFlags.growX) }
}
