import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.source.xml.XmlFileImpl
import com.intellij.psi.xml.XmlDocument
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.w3c.dom.Document
import org.xml.sax.InputSource
import sun.nio.cs.UTF_32
import java.io.File
import java.io.StringReader
import java.net.URL
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory

private fun isValidActionEvent(e: AnActionEvent): Boolean {
    val virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return false

    if (!virtualFile.isDirectory) {
        return false
    }

    val fullPath = virtualFile.canonicalPath ?: return false

    if (!fullPath.contains("/java/")) {
        return false
    }

    return true
}

fun isContextValid(context: DataContext): Boolean = context.getData(PlatformDataKeys.VIRTUAL_FILE)?.isDirectory ?: false

fun getPackageName(e: AnActionEvent): String? {
    if (!isValidActionEvent(e)) {
        return null
    }
    val virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE)
    return virtualFile!!.canonicalPath?.substringAfterLast("/java/")!!.replace('/', '.')
}

fun getManifestPackageName(e: AnActionEvent): String? {
    if (!isValidActionEvent(e)) {
        return null
    }

    val virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE)

    val manifestFileLocation = virtualFile!!.canonicalPath?.substringBefore("java/") + "AndroidManifest.xml"
    val manifestFile = File(manifestFileLocation)
    val manifestTextContent = manifestFile.inputStream().readBytes().toString(Charsets.UTF_8)

    if (!manifestTextContent.contains("package=\"")) {
        return null
    }

    return manifestTextContent.substringAfter("package=\"").substringBefore('"')
}

fun Project?.showPopup(title: String, message: String) {
    val notification = NotificationGroup("auto_model_plugin", NotificationDisplayType.BALLOON, true)
    notification.createNotification(
        title,
        message,
        NotificationType.INFORMATION,
        null
    ).notify(this)
}

fun XmlDocument?.toUiComponentInfos(): List<UiComponentInfo> {
    if (this == null) {
        return emptyList()
    }

    return this.children.flatMap {
        parseFullPsiElement(it)
    }
}

fun parseFullPsiElement(element: PsiElement): List<UiComponentInfo> {
    if (element !is XmlTag) {
        return emptyList()
    }

    val idAttribute = element.getAttribute("android:id") ?: return emptyList()
    val idValue = idAttribute.value?.substringAfter("/") ?: return emptyList()

    val uiComponentInfo = UiComponentInfo(idValue, element.name, idValue)

    return listOf(uiComponentInfo) + element.children.flatMap {
        parseFullPsiElement(it)
    }
}

fun createFile(path: String, name: String, content: String) {
    val newFile = File(path, name)
    newFile.printWriter().use {
        it.print(content)
    }

    newFile.createNewFile()
}

fun generateViewModelFileContent(
    packageName: String,
    manifestPackageName: String,
    className: String,
    parentClassName: String,
    customViewRefId: String,
    uiComponents: List<UiComponentInfo>
): String = fillContentTemplate(packageName, manifestPackageName, customViewRefId, className, parentClassName, uiComponents)

val ClassNameValidator = object: InputValidator {
    override fun checkInput(inputString: String?): Boolean {
        if (inputString == null) {
            return false
        }

        if (inputString.trim().contains(' ', true)) {
            return false
        }

        return true
    }

    override fun canClose(inputString: String?): Boolean {
        return inputString != null && inputString.isNotEmpty()
    }
}