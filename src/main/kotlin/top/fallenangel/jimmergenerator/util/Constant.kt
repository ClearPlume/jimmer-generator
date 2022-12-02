package top.fallenangel.jimmergenerator.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import top.fallenangel.jimmergenerator.model.dummy.DummyModule
import top.fallenangel.jimmergenerator.model.dummy.DummyVirtualFile
import java.util.*

object Constant {
    val uiBundle: ResourceBundle = ResourceBundle.getBundle("locales.ui", Locale.CHINESE)
    val messageBundle: ResourceBundle = ResourceBundle.getBundle("locales.message", Locale.CHINESE)

    val disposable: Disposable = DialogBuilder()
    val dummyModule: Module = DummyModule("----------")
    val dummyFile: VirtualFile = DummyVirtualFile("----------")
}
