package org.autospockgenerate.collector;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiLocalVariableImpl;

import java.util.List;

public class ConditionCollector {

    public static List<PsiIfStatement> collectConditionsFromBody(PsiMethodCallExpression callExpr, PsiCodeBlock body, List<PsiIfStatement> conditions) {
        for (PsiElement child : body.getChildren()) {
            if (child instanceof PsiIfStatement) {
                PsiIfStatement ifStmt = (PsiIfStatement) child;
                if (isConditionRelatedToReturnValue(callExpr, ifStmt.getCondition())) {
                    conditions.add(ifStmt);
                }
            } else if (child instanceof PsiCodeBlock) {
                // 如果遇到嵌套的代码块，递归处理
                collectConditionsFromBody(callExpr, (PsiCodeBlock) child, conditions);
            }
        }
        return conditions;
    }

    private static boolean isConditionRelatedToReturnValue(PsiMethodCallExpression callExpr, PsiExpression condition) {
        // 这里简化处理，实际可能需要更复杂的逻辑来判断条件是否与调用返回值相关
        // 例如，考虑表达式是否包含调用的结果，或者是否有更复杂的逻辑关联
        // 此处仅为示例，实际实现可能需要遍历条件表达式树并检查
        return condition.getText().contains(((PsiLocalVariableImpl) callExpr.getParent()).getName());
    }
}
