package action

import com.android.SdkConstants
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
class CheckSameSizePicsAction : AnAction {
    constructor() : super() {}
    constructor(
        @Nls(capitalization = Nls.Capitalization.Title) text: String?,
        @Nls(capitalization = Nls.Capitalization.Sentence) description: String?,
        icon: Icon?
    ) : super(text, description, icon) {
    }

    override fun actionPerformed(e: AnActionEvent) {
        val folder =
            e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)

        try {
            val map = HashMap<Long, ArrayList<String>>()
            val files = folder.children.filter { !it.isDirectory }
            for (f in files) {
                if (f.name.endsWith(SdkConstants.DOT_PNG) || f.name.endsWith(SdkConstants.DOT_JPG) || f.name.endsWith(
                        SdkConstants.DOT_WEBP
                    ) || f.name.endsWith(SdkConstants.DOT_GIF) || f.name.endsWith(SdkConstants.DOT_JPEG)
                ) {
                    val length = f.length
                    val names = map[length] ?: ArrayList()
                    names.add(f.name)
                    map[length] = names
                }
            }
            val duplicateMap = map.filter { it.value.size > 1 }
            println(duplicateMap)
            alertSameLengthFiles(e, duplicateMap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun alertSameLengthFiles(
        event: AnActionEvent,
        map: Map<Long, ArrayList<String>>
    ) {
        val currentProject: Project? = event.project
        val dlgTitle: String = event.presentation.description
        if (map.isEmpty()) {
            Messages.showMessageDialog(
                currentProject,
                "没有找到大小完全一样的图片",
                dlgTitle,
                Messages.getInformationIcon()
            )
            return
        }
        val dlgMsg = StringBuffer("Find pics with the same size, check if they are duplicated\n\n")
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
        var targetFile = reportsDir?.findChild("duplicate-size-pics.txt")
        if (targetFile?.exists() == true) {
            targetFile.delete(null)
        }
        targetFile = reportsDir?.createChildData(null, "duplicate-size-pics.txt")
        targetFile?.getOutputStream(null)?.use {
            IOUtils.write(dlgMsg.toString(), it, Charset.defaultCharset())
        }

        Messages.showMessageDialog(
            currentProject,
            "A report is generated: rootProjectDir/studioTools/reports/duplicate-size-pics.txt",
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
        val file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)
        return file.isDirectory && (file.name.startsWith("drawable") || file.name.startsWith("mipmap"))
    }

    override fun isDumbAware(): Boolean {
        return false
    }
}