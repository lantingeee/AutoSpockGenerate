package org.autospockgenerate.generate;

import com.intellij.lang.jvm.types.JvmType;
import com.intellij.psi.impl.source.PsiParameterImpl;
import org.autospockgenerate.model.*;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GenerateMethodRegion {
    public static List<TestInfo> generateAllMethod(PsiMethod[] allMethods, List<SourceClass> members) throws ClassNotFoundException {
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

    public static List<ConditionClass> buildParamClass(JvmParameter[] parameters) throws ClassNotFoundException {
        ArrayList<ConditionClass> conditionClasses = Lists.newArrayList();
        if (parameters == null || parameters.length == 0) {
            return conditionClasses;
        }
        for (JvmParameter parameter : parameters) {
            ConditionClass conditionClass = buildConditionClass(parameter);
            conditionClasses.add(conditionClass);
        }
        return conditionClasses;
    }

    public static ConditionClass buildConditionClass(JvmParameter parameter) throws ClassNotFoundException {
        PsiElement sourceElement = parameter.getSourceElement();
        PsiType filed = ((PsiParameterImpl) parameter).getType();
        String presentText = filed.getPresentableText();
        ConditionClass sourceClass = new ConditionClass();
        sourceClass.name = presentText;
        String canonicalText = filed.getCanonicalText();
        sourceClass.importContent = canonicalText;
        sourceClass.testClassMemberName = presentText.toLowerCase() + presentText.substring(1);;
        SourceClassType type = new SourceClassType();
        sourceClass.type = type;
        sourceClass.aClass = Class.forName(canonicalText);
        type.canonicalText = canonicalText;
        return sourceClass;
    }


    public static List<Method> buildMockMethod(PsiMethod oriMethod, List<SourceClass> members) {
        ArrayList<Method> methods = Lists.newArrayList();
        for (SourceClass member : members) {
            Method method = new Method();
            @NotNull PsiElement[] children = oriMethod.getChildren();
            PsiElement childOfType = PsiTreeUtil.getChildOfType(oriMethod, member.aClass);
            // TODO: 获取 方法体内部 涉及到的 全局变量，以及所使用到的变量的方法
            //            childOfType.me
            methods.add(method);
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
