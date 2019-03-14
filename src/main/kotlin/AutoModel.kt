import com.intellij.ide.highlighter.XmlFileType
import com.intellij.ide.util.TreeFileChooserFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile

class AutoModel: AnAction() {

    companion object {
        private const val POPUP_TITLE = "ÔNG GIANG"
        private const val POPUP_FINAL_MESSAGE = "Tạo class %s xong rồi đó mày"
        private const val POPUP_ERROR = "Mày đừng có đùa với tao"
        private const val POPUP_FINAL_ERROR = "Tao mệt quá, nghỉ xíu mày"

        private const val STEP1_TITLE = "Bước 1: Nhập tên class zô mày"
        private const val STEP1_MESSAGE = "Tên class viewmodel:"

        private const val STEP2_TITLE = "Chọn file layout giùm tao"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val packageName = getPackageName(e)
        val manifestPackageName = getManifestPackageName(e)
        val className = getInputClassName() ?: return
        val targetLayoutFile = getTargetLayoutFile(e.project)

        if (packageName == null) {
            e.project.showPopup(POPUP_TITLE, POPUP_ERROR)
            return
        }

        if (manifestPackageName == null) {
            e.project.showPopup(POPUP_TITLE, POPUP_ERROR)
            return
        }

        if (targetLayoutFile !is XmlFile) {
            e.project.showPopup(POPUP_TITLE, POPUP_ERROR)
            return
        }

        if (targetLayoutFile.rootTag == null) {
            e.project.showPopup(POPUP_TITLE, POPUP_ERROR)
            return
        }

        val fileContent = generateViewModelFileContent(
            packageName,
            manifestPackageName,
            className,
            targetLayoutFile.rootTag!!.name,
            targetLayoutFile.name.substringBefore('.'),
            targetLayoutFile.document.toUiComponentInfos()
        )

        createFile(e.getData(PlatformDataKeys.VIRTUAL_FILE)!!.canonicalPath!!, "$className.kt", fileContent)
        e.project.showPopup(POPUP_TITLE, String.format(POPUP_FINAL_MESSAGE, className))
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        val presentation = e.presentation
        val isValid = isContextValid(e.dataContext)
        presentation.isVisible = isValid
        presentation.isEnabled = isValid
    }

    private fun getInputClassName(): String? = Messages.showInputDialog(
        STEP1_MESSAGE,
        STEP1_TITLE,
        Messages.getQuestionIcon(),
        null,
        ClassNameValidator
    )

    private fun getTargetLayoutFile(project: Project?): PsiFile? {
        val factory = TreeFileChooserFactory.getInstance(project)
        val chooser = factory.createFileChooser(
            STEP2_TITLE,
            null,
            XmlFileType.INSTANCE,
            null
        )
        chooser.showDialog()

        return chooser.selectedFile
    }
}