package top.fallenangel.jimmergenerator.model

import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasNamed
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.DasUtil
import com.intellij.ui.CheckedTreeNode
import icons.DatabaseIcons
import top.fallenangel.jimmergenerator.component.SettingStorageComponent
import top.fallenangel.jimmergenerator.enums.DBType
import top.fallenangel.jimmergenerator.enums.DbObjTrait
import top.fallenangel.jimmergenerator.enums.Language
import top.fallenangel.jimmergenerator.model.type.Annotation
import top.fallenangel.jimmergenerator.model.type.Class
import top.fallenangel.jimmergenerator.model.type.Parameter
import top.fallenangel.jimmergenerator.util.Constant
import javax.swing.Icon

data class DbObj(
    val column: DasColumn?,
    var selected: Boolean,
    val objName: String,
    var property: String,
    var businessKey: Boolean,
    var type: Class,
    val annotations: MutableList<Annotation>,
    val remark: String?
) : CheckedTreeNode(), DasNamed {
    val isTable: Boolean
        get() = column == null

    private val isPrimary: Boolean
        get() = DasUtil.isPrimary(column)

    val icon: Icon
        get() {
            var traits = if (isTable) {
                DbObjTrait.TABLE or 0
            } else {
                DbObjTrait.COLUMN or 0
            }
            if (isPrimary) traits = DbObjTrait.PRIMARY or traits
            if (DasUtil.isIndexColumn(column)) traits = DbObjTrait.INDEX or traits
            if (DasUtil.isForeign(column)) traits = DbObjTrait.FOREIGN or traits
            if (column?.isNotNull == true) traits = DbObjTrait.NOTNULL or traits

            return when (traits) {
                // TABLE
                0b000001 -> DatabaseIcons.Table
                // PRIMARY (INDEX + NOTNULL + COLUMN)
                0b110110 -> DatabaseIcons.ColGoldKeyDotIndex
                // FOREIGN + NOTNULL + INDEX (COLUMN)
                0b111100 -> DatabaseIcons.ColBlueKeyDotIndex
                // FOREIGN + NOTNULL (COLUMN)
                0b111000 -> DatabaseIcons.ColBlueKeyDot
                // FOREIGN + INDEX (COLUMN)
                0b101100 -> DatabaseIcons.ColBlueKeyIndex
                // FOREIGN (COLUMN)
                0b101000 -> DatabaseIcons.ColBlueKey
                // INDEX + NOTNULL (COLUMN)
                0b110100 -> DatabaseIcons.ColDotIndex
                // INDEX (COLUMN)
                0b100100 -> DatabaseIcons.ColIndex
                // NOTNULL (COLUMN)
                0b110000 -> DatabaseIcons.ColDot
                // COLUMN
                else -> DatabaseIcons.Col
            }
        }

    val children: List<DbObj>
        get() = if (isTable) {
            children().toList().filterIsInstance<DbObj>()
        } else {
            emptyList()
        }

    fun captureAnnotations(language: Language, dbType: DBType) {
        annotations.clear()
        val nameAnnotation = if (isTable) "Table" else "Column"
        val needNameAnnotation = objName.replace("_", "").lowercase() != property.lowercase()
        if (needNameAnnotation) {
            val needQuotes = objName.contains(Regex("\\W|^\\d"))
            annotations.add(
                Annotation(
                    nameAnnotation, Constant.JIMMER_PACKAGE, listOf(
                        Parameter("name", if (needQuotes) "${dbType.l}$objName${dbType.r}" else objName, Class("String"))
                    )
                )
            )
        }

        if (isTable) {
            annotations.addAll(SettingStorageComponent.tableDefaultAnnotations)
        } else {
            if (isPrimary) {
                annotations.add(Annotation("Id", Constant.JIMMER_PACKAGE, emptyList()))
                annotations.add(
                    Annotation(
                        "GeneratedValue", Constant.JIMMER_PACKAGE,
                        listOf(Parameter("strategy", "GenerationType.IDENTITY", Class("GenerationType", Constant.JIMMER_PACKAGE)))
                    )
                )
            }
            if (language == Language.JAVA && type.nullable) {
                annotations.add(Annotation("Null", "javax.validation.constraints", emptyList()))
            }
            if (businessKey) {
                annotations.add(Annotation("Key", Constant.JIMMER_PACKAGE, emptyList()))
            }
        }
        annotations.sortBy { it.toString().length }
    }

    override fun getName() = objName

    override fun getKind(): ObjectKind {
        return if (isTable) {
            ObjectKind.TABLE
        } else {
            ObjectKind.COLUMN
        }
    }

    override fun isQuoted() = false
}
