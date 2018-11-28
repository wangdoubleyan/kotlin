/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IfThenToSafeAccessInspection
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfThenToElvisIntention
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

object SmartCastImpossibleInIfThenFactory : KotlinIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val element = diagnostic.psiElement as? KtNameReferenceExpression ?: return emptyList()
        val ifExpression =
            element.getStrictParentOfType<KtContainerNodeForControlStructureBody>()?.parent as? KtIfExpression ?: return emptyList()

        val ifThenToSafeAccess = IfThenToSafeAccessInspection(stableElementNeeded = false)
        val ifThenToElvis = IfThenToElvisIntention(stableElementNeeded = false)

        return listOf(
            createQuickFix(
                ifExpression,
                { ifThenToSafeAccess.fixText(it) },
                { ifThenToSafeAccess.isApplicable(it) },
                { ifExpr, project, editor -> ifThenToSafeAccess.applyTo(ifExpr.ifKeyword, project, editor) }
            ),
            createQuickFix(
                ifExpression,
                { ifThenToElvis.text },
                { ifThenToElvis.isApplicableTo(it) },
                { ifExpr, _, editor -> ifThenToElvis.applyTo(ifExpr, editor) }
            )
        )
    }

    private fun createQuickFix(
        ifExpression: KtIfExpression,
        fixText: (KtIfExpression) -> String,
        isApplicable: (KtIfExpression) -> Boolean,
        applyTo: (KtIfExpression, project: Project, editor: Editor?) -> Unit
    ): KotlinQuickFixAction<KtIfExpression> {
        return object : KotlinQuickFixAction<KtIfExpression>(ifExpression) {
            override fun getText() = fixText(ifExpression)

            override fun getFamilyName() = text

            override fun isAvailable(project: Project, editor: Editor?, file: KtFile) = element?.let { isApplicable(it) } ?: false

            override fun invoke(project: Project, editor: Editor?, file: KtFile) {
                element?.also { applyTo(it, project, editor) }
            }
        }
    }
}