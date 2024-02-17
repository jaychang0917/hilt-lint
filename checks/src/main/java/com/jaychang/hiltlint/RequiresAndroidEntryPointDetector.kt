package com.jaychang.hiltlint

import com.android.resources.ResourceFolderType
import com.android.resources.ResourceType
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.AnnotationInfo
import com.android.tools.lint.detector.api.AnnotationUsageInfo
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import com.android.tools.lint.detector.api.XmlScannerConstants
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiUtil
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getParentOfType
import org.w3c.dom.Element
import java.io.File
import java.util.EnumSet

class RequiresAndroidEntryPointDetector : Detector(), SourceCodeScanner, XmlScanner {
    private val annotationUsageData = mutableSetOf<AnnotationUsageData>()
    private val viewModelMethodCallData = mutableSetOf<ViewModelMethodCallData>()
    private val layoutResourceData = mutableSetOf<ResourceNodeData>()
    private val navigationResourceData = mutableSetOf<ResourceNodeData>()
    private val resourceReferenceData = mutableSetOf<ResourceReferenceData>()
    private var isProjectUseViewBinding = false

    override fun beforeCheckRootProject(context: Context) {
        annotationUsageData.clear()
        viewModelMethodCallData.clear()
        layoutResourceData.clear()
        navigationResourceData.clear()
        resourceReferenceData.clear()
        isProjectUseViewBinding = context.project.buildVariant.buildFeatures.viewBinding
    }

    override fun afterCheckRootProject(context: Context) {
        reportAnnotationUsageIssues(context)
        reportViewModelMethodCallIssues(context)
        reportLayoutResourceIssues(context)
        reportNavigationResourceIssues(context)
        isProjectUseViewBinding = false
    }

    override fun applicableAnnotations(): List<String> {
        return listOf(ANNOTATION_REQUIRES_ANDROID_ENTRY_POINT, ANNOTATION_ANDROID_ENTRY_POINT)
    }

    override fun visitAnnotationUsage(
        context: JavaContext,
        element: UElement,
        annotationInfo: AnnotationInfo,
        usageInfo: AnnotationUsageInfo
    ) {
        if (annotationInfo.annotated == null) return

        val data = AnnotationUsageData(
            context = context,
            annotated = annotationInfo.annotated!!,
            annotation = annotationInfo.qualifiedName,
            usage = usageInfo.usage,
            location = context.getLocation(usageInfo.usage)
        )
        annotationUsageData.add(data)
    }

    private fun reportAnnotationUsageIssues(context: Context) {
        for (data in annotationUsageData) {
            if (!data.usage.isParentAndroidComponent(data.context)) continue
            val isAnnotatedFragment = data.annotated.extendsClass(data.context, CLASS_FRAGMENT)
            val isUsageActivity = data.usage.extendsClass(data.context, CLASS_ACTIVITY)
            // Hilt Fragments must be attached to an @AndroidEntryPoint Activity
            val isFragmentUsedInActivity = isAnnotatedFragment && isUsageActivity
            val isAnnotatedView = data.annotated.extendsClass(data.context, CLASS_VIEW)
            val isUsageFragment = data.usage.extendsClass(data.context, CLASS_FRAGMENT)
            // Hilt view must be attached to an @AndroidEntryPoint Fragment or Activity.
            val isViewUsedInActivityOrFragment = isAnnotatedView && (isUsageActivity || isUsageFragment)
            if (isFragmentUsedInActivity || isViewUsedInActivityOrFragment) {
                if (!data.usage.isParentAnnotatedAndroidEntryPoint()) {
                    val message = "${data.annotated.getFqName()} is annotated with @AndroidEntryPoint, ${data.usage.getParentClassName()} must be annotated with @AndroidEntryPoint."
                    context.report(ISSUE, data.location, message)
                }
            } else if (data.annotation == ANNOTATION_REQUIRES_ANDROID_ENTRY_POINT) {
                if (!data.usage.isParentAnnotatedAndroidEntryPoint()) {
                    val message = "${data.annotated.getFqName()} is annotated with @RequiresAndroidEntryPoint, ${data.usage.getParentClassName()} must be annotated with @AndroidEntryPoint."
                    context.report(ISSUE, data.location, message)
                }
            }
        }
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf(METHOD_VIEW_MODELS)
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val data = ViewModelMethodCallData(
            context = context,
            target = node,
            location = context.getCallLocation(node, includeArguments = true, includeReceiver = false)
        )
        viewModelMethodCallData.add(data)
    }

    private fun reportViewModelMethodCallIssues(context: Context) {
        for (data in viewModelMethodCallData) {
            if (!data.target.isParentAndroidComponent(data.context)) continue
            val parent = data.target.uastParent ?: continue
            val typeArgumentName = data.target.typeArguments.firstOrNull()?.canonicalText ?: continue
            val viewModel = data.context.evaluator.findClass(typeArgumentName) ?: continue
            val isHiltViewModel = viewModel.hasAnnotation(ANNOTATION_HILT_VIEW_MODEL)
            if (isHiltViewModel && !parent.isParentAnnotatedAndroidEntryPoint()) {
                val message = "${viewModel.qualifiedName} is annotated with @HiltViewModel, ${parent.getParentClassName()} must be annotated with @AndroidEntryPoint."
                context.report(ISSUE, data.location, message)
            }
        }
    }

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        return folderType == ResourceFolderType.LAYOUT || folderType == ResourceFolderType.NAVIGATION
    }

    override fun getApplicableElements(): Collection<String>? {
        return XmlScannerConstants.ALL
    }

    override fun visitElement(context: XmlContext, element: Element) {
        val data = ResourceNodeData(
            file = context.file,
            node = element,
            location = context.getElementLocation(element)
        )
        when(context.resourceFolderType) {
            ResourceFolderType.LAYOUT -> layoutResourceData.add(data)
            ResourceFolderType.NAVIGATION -> navigationResourceData.add(data)
            else -> error("Resource type ${context.resourceFolderType} is not support.")
        }
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(USimpleNameReferenceExpression::class.java, UQualifiedReferenceExpression::class.java)
    }

    // visitResourceReference() doesn't work for view binding, so we need to find all view binding references.
    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
                visitViewBindingReference(node)
            }

            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                visitViewBindingReference(node)
            }

            private fun visitViewBindingReference(node: UReferenceExpression) {
                val type = node.getExpressionType() ?: return
                val isViewBinding = context.evaluator.extendsClass(PsiUtil.resolveClassInType(type), CLASS_VIEW_BINDING)
                if (isViewBinding) {
                    val data = ResourceReferenceData(
                        context = context,
                        className = node.getParentClassName(),
                        referenced = node,
                        resourceFilename = type.toLayoutFilename(),
                        resourceType = ResourceType.LAYOUT
                    )
                    resourceReferenceData.add(data)
                }
            }

            private fun PsiType.toLayoutFilename(): String {
                val result = StringBuilder()
                for ((index, char) in presentableText.withIndex()) {
                    val c = if (char.isUpperCase() && index == 0) {
                        char.lowercaseChar()
                    } else if (char.isUpperCase()){
                        "_${char.lowercaseChar()}"
                    } else {
                        char
                    }
                    result.append(c)
                }
                return result.toString().removeSuffix("_binding")
            }
        }
    }

    override fun appliesToResourceRefs(): Boolean {
        return true
    }
    
    override fun visitResourceReference(
        context: JavaContext,
        node: UElement,
        type: ResourceType,
        name: String,
        isFramework: Boolean
    ) {
        val isLayoutOrNavigation = type == ResourceType.LAYOUT || type == ResourceType.NAVIGATION
        val isNotAdded = resourceReferenceData.none { it.resourceFilename == name }
        if (isLayoutOrNavigation && isNotAdded) {
            val data = ResourceReferenceData(
                context = context,
                className = node.getParentClassName(),
                referenced = node,
                resourceFilename = name,
                resourceType = type
            )
            resourceReferenceData.add(data)
        }
    }

    private fun reportLayoutResourceIssues(reportContext: Context) {
        if (resourceReferenceData.isEmpty() || layoutResourceData.isEmpty()) return

        val evaluator = resourceReferenceData.first().context.evaluator
        val referencedFilenames = resourceReferenceData.map { it.resourceFilename }
        val resourceData = layoutResourceData
            // Only used nodes
            .filter { it.file.nameWithoutExtension in referencedFilenames }
            // Only node annotated with @AndroidEntryPoint
            .filter { evaluator.findClass(it.node.getPresentableNodeName())?.hasAnnotation(
                ANNOTATION_ANDROID_ENTRY_POINT
            ) == true }
            // Map to its usage
            .associateWith { resourceReferenceData.firstOrNull { ref -> it.file.nameWithoutExtension == ref.resourceFilename } }
            .filter { it.value != null }

        for ((targetData, referenceData) in resourceData) {
            val referenced = referenceData?.referenced ?: continue
            val isReferenceAnnotatedAndroidEntryPoint = referenced.isParentAnnotatedAndroidEntryPoint()
            if (!isReferenceAnnotatedAndroidEntryPoint) {
                val message = "${targetData.node.getPresentableNodeName()} is annotated with @AndroidEntryPoint, ${referenced.getParentClassName()} must be annotated with @AndroidEntryPoint."
                reportContext.report(ISSUE, targetData.location, message)
            }
        }
    }

    private fun reportNavigationResourceIssues(reportContext: Context) {
        if (resourceReferenceData.isEmpty() || navigationResourceData.isEmpty()) return

        // Find navigation resource's corresponding usage, e.g. nav_test -> TestActivity
        val navigationReferenceData = mutableListOf<Pair<String, String>>()
        val navigationReferenceFromLayout = layoutResourceData
            .mapNotNull {
                val nodeValue = it.node.getAttributeNode("app:navGraph")?.nodeValue
                val navGraphFilename = nodeValue?.replace("@navigation/", "")
                if (navGraphFilename?.isNotBlank() == true) {
                    navGraphFilename to it.file.nameWithoutExtension
                } else {
                    null
                }
            }
            .mapNotNull { navLayout ->
                val resourceReference = resourceReferenceData.firstOrNull { it.resourceFilename == navLayout.second }
                if (resourceReference != null) {
                    navLayout.first to resourceReference.className
                } else {
                    null
                }
            }
        val navigationReferenceFromCode = resourceReferenceData
            .filter { it.resourceType == ResourceType.NAVIGATION }
            .map { it.resourceFilename to it.className }
        navigationReferenceData.addAll(navigationReferenceFromLayout)
        navigationReferenceData.addAll(navigationReferenceFromCode)

        val evaluator = resourceReferenceData.first().context.evaluator
        val resourceData = navigationResourceData
            // Only node annotated with @AndroidEntryPoint
            .filter { evaluator.findClass(it.node.getPresentableNodeName())?.hasAnnotation(
                ANNOTATION_ANDROID_ENTRY_POINT
            ) == true }
            // Map to its usage
            .associateWith { node ->
                resourceReferenceData.firstOrNull { ref ->
                    val navigationReference = navigationReferenceData.firstOrNull { it.first == node.file.nameWithoutExtension }
                    ref.className == navigationReference?.second
                }
            }
            .filter { it.value != null }

        for ((targetData, referenceData) in resourceData) {
            val referenced = referenceData?.referenced ?: continue
            val isReferenceAnnotatedAndroidEntryPoint = referenced.isParentAnnotatedAndroidEntryPoint()
            if (!isReferenceAnnotatedAndroidEntryPoint) {
                val message = "${targetData.node.getPresentableNodeName()} is annotated with @AndroidEntryPoint, ${referenced.getParentClassName()} must be annotated with @AndroidEntryPoint."
                reportContext.report(ISSUE, targetData.location, message)
            }
        }
    }

    private fun Element.getPresentableNodeName(): String {
        return when(localName) {
            TAG_FRAGMENT, TAG_FRAGMENT_CONTAINER_VIEW -> getAttributeNode("android:name")?.nodeValue ?: ""
            else -> localName
        }
    }

    private fun UElement.isParentAnnotatedAndroidEntryPoint(): Boolean {
        val hostClass = getParentOfType(UClass::class.java) ?: return false
        return hostClass.annotations.any { it.hasQualifiedName(ANNOTATION_ANDROID_ENTRY_POINT) }
    }

    private fun UElement.isParentAndroidComponent(context: JavaContext): Boolean {
        val hostClass = getParentOfType(UClass::class.java) ?: return false
        val isApplication = context.evaluator.extendsClass(hostClass, CLASS_APPLICATION)
        val isActivity = context.evaluator.extendsClass(hostClass, CLASS_ACTIVITY)
        val isFragment = context.evaluator.extendsClass(hostClass, CLASS_FRAGMENT)
        val isView = context.evaluator.extendsClass(hostClass, CLASS_VIEW)
        return isApplication || isActivity || isFragment || isView
    }

    private data class AnnotationUsageData(
        val context: JavaContext,
        val annotated: PsiElement,
        val annotation: String,
        val usage: UElement,
        val location: Location
    )

    private data class ViewModelMethodCallData(
        val context: JavaContext,
        val target: UCallExpression,
        val location: Location
    )

    private data class ResourceNodeData(
        val file: File,
        val node: Element,
        val location: Location
    )

    private data class ResourceReferenceData(
        val context: JavaContext,
        val className: String,
        val referenced: UElement,
        val resourceFilename: String,
        val resourceType: ResourceType
    )

    companion object {
        private const val ANNOTATION_ANDROID_ENTRY_POINT = "dagger.hilt.android.AndroidEntryPoint"
        private const val ANNOTATION_REQUIRES_ANDROID_ENTRY_POINT = "com.jaychang.hiltlint.RequiresAndroidEntryPoint"
        private const val ANNOTATION_HILT_VIEW_MODEL = "dagger.hilt.android.lifecycle.HiltViewModel"
        private const val CLASS_APPLICATION = "android.app.Application"
        private const val CLASS_ACTIVITY = "androidx.activity.ComponentActivity"
        private const val CLASS_FRAGMENT = "androidx.fragment.app.Fragment"
        private const val CLASS_VIEW = "android.view.View"
        private const val TAG_FRAGMENT = "fragment"
        private const val TAG_FRAGMENT_CONTAINER_VIEW = "androidx.fragment.app.FragmentContainerView"
        private const val CLASS_VIEW_BINDING = "androidx.viewbinding.ViewBinding"
        private const val METHOD_VIEW_MODELS = "viewModels"

        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "RequiresAndroidEntryPoint",
            briefDescription = "Hilt type must be attached to a hilt type with @AndroidEntryPoint annotation",
            explanation = "Hilt type must be attached to a hilt type with @AndroidEntryPoint annotation",
            category = Category.CORRECTNESS,
            priority = 10,
            severity = Severity.ERROR,
            implementation = Implementation(
                RequiresAndroidEntryPointDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.RESOURCE_FILE)
            )
        )
    }
}
