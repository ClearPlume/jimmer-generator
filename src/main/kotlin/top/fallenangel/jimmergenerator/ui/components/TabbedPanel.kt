package top.fallenangel.jimmergenerator.ui.components

import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.Row
import java.awt.Component
import javax.swing.Icon
import javax.swing.JTabbedPane

fun Row.tabbedPanel(init: TabbedPanel.() -> Unit): CellBuilder<JTabbedPane> {
    return component(TabbedPanel().apply(init)).constraints(CCFlags.growX)
}

class TabbedPanel(tabPlacement: Int = TOP, tabLayoutPolicy: Int = SCROLL_TAB_LAYOUT) : JTabbedPane(tabPlacement, tabLayoutPolicy) {
    fun tab(title: String, icon: Icon? = null, tip: String = title, content: () -> Component) {
        addTab(title, icon, content(), tip)
    }
}
