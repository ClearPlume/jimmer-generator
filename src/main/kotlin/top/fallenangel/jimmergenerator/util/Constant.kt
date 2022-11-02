package top.fallenangel.jimmergenerator.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import top.fallenangel.jimmergenerator.model.dummy.DummyModule
import top.fallenangel.jimmergenerator.model.dummy.DummyVirtualFile

object Constant {
    val disposable: Disposable = DialogBuilder()
    val dummyModule: Module = DummyModule("----------")
    val dummyFile: VirtualFile = DummyVirtualFile("----------")
}
