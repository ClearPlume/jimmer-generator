package top.fallenangel.jimmergenerator.ui

import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.CellBuilder
import java.awt.event.ItemEvent
import kotlin.reflect.KMutableProperty

fun <T> radioGroup(prop: KMutableProperty<T>, changed: (T) -> Unit = {}, init: CellBuilderRadioGroup<T>.() -> Unit) {
    CellBuilderRadioGroup(prop, changed).init()
}

class CellBuilderRadioGroup<T>(private val prop: KMutableProperty<T>, private val changed: (T) -> Unit) {
    private val radios = mutableMapOf<T, JBRadioButton>()

    fun Cell.radio(text: String, value: T): CellBuilder<JBRadioButton> {
        val radio = JBRadioButton(text)
        if (value == prop.getter.call()) {
            radio.isSelected = true
        }
        radios[value] = radio.apply {
            addItemListener {
                if (it.stateChange == ItemEvent.SELECTED) {
                    radios.forEach { (v, radio) ->
                        radio.isSelected = v == value
                    }
                    prop.setter.call(value)
                    changed(value)
                }
            }
        }
        return component(radio)
    }
}
