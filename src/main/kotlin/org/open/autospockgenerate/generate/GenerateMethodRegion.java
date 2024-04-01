package org.open.autospockgenerate.generate;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.compress.utils.Lists;
import org.open.autospockgenerate.model.ConditionClass;
import org.open.autospockgenerate.model.Method;
import org.open.autospockgenerate.model.SourceClass;
import org.open.autospockgenerate.model.TestInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GenerateMethodRegion {
    public static List<TestInfo> generateAllMethod(PsiMethod[] allMethods, List<SourceClass> members) {
        List<TestInfo> testInfos = Lists.newArrayList();
        for (PsiMethod method : allMethods) {
            JvmParameter[] parameters = method.getParameters();
            TestInfo testInfo = new TestInfo();
            testInfo.params = buildParamClass(parameters);
            testInfo.needMockMethods = buildMockMethod(method, members);
            testInfo.testMethod = buildInvokeMethod(method);
            testInfo.varConditionMap = new HashMap<>();
            testInfos.add(testInfo);
        }
        return testInfos;
    }

    public static List<ConditionClass> buildParamClass(JvmParameter[] parameters) {
        ArrayList<ConditionClass> conditionClasses = Lists.newArrayList();
        if (parameters == null || parameters.length == 0) {
            return conditionClasses;
        }
        for (JvmParameter parameter : parameters) {
            PsiElement sourceElement = parameter.getSourceElement();
            PsiFile containingFile = sourceElement.getContainingFile();
            ConditionClass sourceClass = (ConditionClass) GenerateClassRegion.generateSourceClass(containingFile);
            conditionClasses.add(sourceClass);
        }
        return conditionClasses;
    }

    public static List<Method> buildMockMethod(PsiMethod method, List<SourceClass> members) {
        ArrayList<Method> methods = Lists.newArrayList();
        for (SourceClass member : members) {
            PsiElement childOfType = PsiTreeUtil.getChildOfType(method, member.aClass);
            // TODO: 获取 方法体内部 涉及到的 全局变量，以及所使用到的变量的方法
            //            childOfType.me

        }
        return methods;
    }

    // TODO: IMPL
    public static Method buildInvokeMethod(PsiMethod method) {
        Method result = new Method();
        result.isStatic = true;
        result.filed = "";
        result.methodCall = "call";
        result.params = null;
        result.result = null;
        return result;
    }
}
