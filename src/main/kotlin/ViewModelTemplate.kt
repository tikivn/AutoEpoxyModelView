fun fillContentTemplate(
    packageName: String,
    manifestPackageName: String,
    customViewRefId: String,
    className: String,
    parentClassName: String,
    uiComponents: List<UiComponentInfo>
): String {
    val uiComponentDefinitions = uiComponents.joinToString("\n") {
        "    private val ${it.name}: ${it.type} by lazy { findViewById<${it.type}>(R.id.${it.refId}) }"
    }

    val packageImport = "package $packageName"
    val parentImport = "import android.widget.$parentClassName"
    val fileRImports = listOf(
        "import $manifestPackageName.R",
        "import $manifestPackageName.R2"
    )

    val uiComponentImports = listOf(parentImport) + uiComponents
        .filterNot { it.type.contains('.') }
        .distinctBy { it.type }
        .map { "import android.widget.${it.type}" }

    val imports = (listOf(packageImport, "") + uiComponentImports + fileRImports).distinct().joinToString("\n")

    return  "$imports\n" +
            "import android.content.Context\n" +
            "import android.util.AttributeSet\n" +
            "import com.airbnb.epoxy.ModelView\n" +
            "\n" +
            "@ModelView(\n" +
            "    defaultLayout = R2.layout.$customViewRefId\n" +
            ")\n" +
            "class $className @JvmOverloads constructor(\n" +
            "    context: Context,\n" +
            "    attrs: AttributeSet? = null,\n" +
            "    defStyleAttr: Int = 0\n" +
            ") : $parentClassName(context, attrs, defStyleAttr) {\n\n" +
            "$uiComponentDefinitions\n" +
            "}"
}

