package top.fallenangel.jimmergenerator.util

import com.intellij.mock.MockModule
import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VirtualFile

object Constant {
    val dummyModule: Module = MockModule(DialogBuilder()).apply { name = "----------" }
    val dummyFile: VirtualFile = MockVirtualFile("----------")
}
