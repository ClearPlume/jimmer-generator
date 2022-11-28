package top.fallenangel.jimmergenerator.model.dummy

import com.intellij.openapi.vfs.newvfs.impl.StubVirtualFile

class DummyVirtualFile(private val fileName: String) : StubVirtualFile() {
    override fun getName() = fileName

    override fun getPath() = fileName
}
