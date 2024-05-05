package org.autospockgenerate.collector;

import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiIfStatement;

import java.util.List;

public class ConditionCollector {

//    public static List<PsiIfStatement> findConditionsUsingReturnValue(PsiMethod methodA, PsiMethod methodB) {
//        List<PsiIfStatement> conditions = new ArrayList<>();
//        for (PsiReference reference : ReferencesSearch.search(methodB)) {
//            if (reference.getElement().getContainingFile() != methodA.getContainingFile()) continue;
//            if (!(reference instanceof PsiMethodCallExpression)) continue;
//
//            PsiMethodCallExpression callExpression = (PsiMethodCallExpression) reference.getElement();
//            if (!isMethodCallWithinMethod(methodA, callExpression)) continue;
//
//            PsiExpression returnValue = callExpression;
//            collectConditionsUsingExpression(returnValue, methodA.getBody(), conditions);
//        }
//        return conditions;
//    }
//
//    private static boolean isMethodCallWithinMethod(PsiMethod containingMethod, PsiMethodCallExpression callExpression) {
//        PsiElement parent = callExpression;
//        while (parent != null && !(parent instanceof PsiMethod)) {
//            parent = parent.getParent();
//        }
//        return parent == containingMethod;
//    }
    public static List<PsiIfStatement> collectConditionsUsingExpression(PsiExpression expression,
                                                                        PsiElement context,
                                                         List<PsiIfStatement> conditions) {
        if (expression == null || context == null) {
            return conditions;
        }
        PsiElement parent = expression.getParent();
        while (parent != null) {
            if (parent instanceof PsiIfStatement && ((PsiIfStatement) parent).getCondition() == expression) {
                conditions.add((PsiIfStatement) parent);
            } else if (parent instanceof PsiBinaryExpression) {
                PsiBinaryExpression binaryExpr = (PsiBinaryExpression) parent;
                if (binaryExpr.getROperand() == expression || binaryExpr.getLOperand() == expression) {
                    // 继续向上遍历，可能嵌套在更复杂的条件中 TODO
//                    collectConditionsUsingExpression((PsiExpression) parent, context, conditions);
                    System.out.println("继续向上遍历，可能嵌套在更复杂的条件中");
                }
            } else if (parent == context) {
                // 已到达方法体边界，停止遍历
                break;
            }
            parent = parent.getParent();
        }
        return conditions;
    }
}
