package top.fallenangel.jimmergenerator.util

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.StubVirtualFile
import java.util.*

object Constant {
    const val JIMMER_PACKAGE = "org.babyfish.jimmer.sql"

    val uiBundle: ResourceBundle = ResourceBundle.getBundle("locales.ui", Locale.CHINESE)
    val messageBundle: ResourceBundle = ResourceBundle.getBundle("locales.message", Locale.CHINESE)

    val dummyFile: VirtualFile = object : StubVirtualFile() {
        override fun getName() = "----------"

        override fun getPath() = name

        override fun toString() = "Module: ----------"
    }
}
