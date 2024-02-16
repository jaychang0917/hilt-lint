package com.jaychang.hiltlint

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParentOfType

internal fun PsiElement.getFqName(): String? = when (val element = namedUnwrappedElement) {
    is PsiMember -> element.getName()?.let { name ->
        val prefix = element.containingClass?.qualifiedName
        (if (prefix != null) "$prefix.$name" else name)
    }

    is KtNamedDeclaration -> element.fqName.toString()
    else -> null
}

internal fun PsiElement.extendsClass(context: JavaContext, clazzName: String): Boolean {
    val elementClass = PsiUtil.getTopLevelClass(this)
    return context.evaluator.extendsClass(elementClass, clazzName)
}

internal fun UElement.extendsClass(context: JavaContext, clazzName: String): Boolean {
    val elementClass = getParentOfType(UClass::class.java)
    return context.evaluator.extendsClass(elementClass, clazzName)
}

internal fun UElement.getParentClassName(): String {
    return getParentOfType(UClass::class.java)?.qualifiedName ?: ""
}