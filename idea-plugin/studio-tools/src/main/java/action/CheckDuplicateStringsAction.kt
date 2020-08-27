package action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.internal.io.IoUtils
import org.jetbrains.annotations.Nls
import org.jsoup.Jsoup
import java.io.IOException
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.HashMap
import javax.swing.Icon

/**
 * @author weiping@atlasv.com
 * 2020年08月17日11:31:03
 */
class CheckDuplicateStringsAction : AnAction {
    constructor() : super() {}
    constructor(
        @Nls(capitalization = Nls.Capitalization.Title) text: String?,
        @Nls(capitalization = Nls.Capitalization.Sentence) description: String?,
        icon: Icon?
    ) : super(text, description, icon) {
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file =
            e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)

        try {
            val inputStream = file.inputStream
            val content = IOUtils.toString(inputStream, Charset.defaultCharset())
            val document = Jsoup.parse(content)

            /**
             * document.getElementsByTag("string").get(i).html()
             */
            val elements = document.getElementsByTag("string")
            val size = elements.size
            val map =
                HashMap<String, ArrayList<String>>()
            for (i in 0 until size) {
                val element = elements[i]
                val name = element.attributes()["name"]
                val value = element.html()
                val sameNames = map[value] ?: ArrayList()
                sameNames.add(name)
                map[value] = sameNames
            }
            val duplicateMap = map.filter {
                it.value.size > 1
            }
            println(duplicateMap)
            alertSameStrings(e, duplicateMap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun alertSameStrings(
        event: AnActionEvent,
        map: Map<String, ArrayList<String>>
    ) {
        val currentProject: Project? = event.project
        val dlgTitle: String = event.presentation.description
        if (map.isEmpty()) {
            Messages.showMessageDialog(
                currentProject,
                "没有找到取值相同的字符串",
                dlgTitle,
                Messages.getInformationIcon()
            )
            return
        }
        val dlgMsg = StringBuffer("Find duplicate strings, are they necessary?\n\n")
        map.forEach {
            dlgMsg.append("--------------------------\n")
            for (str in it.value) {
                dlgMsg.append("${str}=${it.key}\n")
            }
        }

        val projectDir = currentProject?.guessProjectDir()
        var studioToolsDir = projectDir?.findChild("studioTools")
        if (studioToolsDir?.exists() != true) {
            studioToolsDir = projectDir?.createChildDirectory(null, "studioTools")
        }
        var reportsDir = studioToolsDir?.findChild("reports")
        if (reportsDir?.exists() != true) {
            reportsDir = studioToolsDir?.createChildDirectory(null, "reports")
        }
        var targetFile = reportsDir?.findChild("duplicate-strings.txt")
        if (targetFile?.exists() == true) {
            targetFile.delete(null)
        }
        targetFile = reportsDir?.createChildData(null, "duplicate-strings.txt")
        targetFile?.getOutputStream(null)?.use {
            IOUtils.write(dlgMsg.toString(), it, Charset.defaultCharset())
        }

        Messages.showMessageDialog(
            currentProject,
            "A report is generated: rootProjectDir/studioTools/reports/duplicate-strings.txt",
            dlgTitle,
            Messages.getInformationIcon()
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = supportFile(e)
    }

    /**
     * 选中string.xml时展示此选项
     */
    private fun supportFile(e: AnActionEvent): Boolean {
        val files = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)
        return files.size == 1 && !file.isDirectory && file.name == "strings.xml"
    }

    override fun isDumbAware(): Boolean {
        return false
    }
}