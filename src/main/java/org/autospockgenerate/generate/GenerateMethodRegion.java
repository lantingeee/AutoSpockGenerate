package org.autospockgenerate.generate;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.source.PsiParameterImpl;
import com.intellij.psi.util.PsiTreeUtil;
import groovy.util.logging.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.autospockgenerate.model.*;
import org.autospockgenerate.util.ClassNameUtil;
import org.autospockgenerate.util.FiledNameUtil;

import java.util.*;

@Slf4j
public class GenerateMethodRegion {
    public static List<TestInfo> generateAllMethod(PsiFile psiFile, PsiMethod[] allMethods, List<SourceClass> members) throws ClassNotFoundException {
        List<TestInfo> testInfos = Lists.newArrayList();

        for (PsiMethod method : allMethods) {
            PsiClass declaringClass = method.getContainingClass();
            // 如果方法的声明类不是Object类，则添加到结果列表中
            if (declaringClass instanceof ClsClassImpl && CommonClassNames.JAVA_LANG_OBJECT.
                    equalsIgnoreCase(((ClsClassImpl) declaringClass).getStub().getQualifiedName())) {
                continue;
            }
            JvmParameter[] parameters = method.getParameters();
            TestInfo testInfo = new TestInfo();
            testInfo.params = buildParamClass(parameters);
            testInfo.needMockMethods = buildMockMethod(method, members);
            testInfo.testMethod = buildInvokeTestMethod(psiFile, method, testInfo);
            // TODO
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
        if (!(parameter instanceof PsiParameterImpl)) {
            System.err.println("sourceElement is not a PsiParameter, " + parameter.toString());
            return null;
        }
        PsiType filed = ((PsiParameterImpl) parameter).getType();
        String presentText = filed.getPresentableText();
        ConditionClass sourceClass = new ConditionClass();
        sourceClass.name = ClassNameUtil.getClassName(presentText);
        String canonicalText = filed.getCanonicalText();
        sourceClass.importContent = canonicalText;
        sourceClass.testClassMemberName = FiledNameUtil.lowerName(presentText);

        SourceClassType type = new SourceClassType();
        sourceClass.type = type;
        System.out.println("canonicalText: --------" + canonicalText);
        type.canonicalText = canonicalText;
        return sourceClass;
    }

    /**
     * @param oriMethod 原始代码的代码块
     * @param members   成员变量列表
     * @return 需要被mock 的方法
     */
    public static List<Method> buildMockMethod(PsiMethod oriMethod, List<SourceClass> members) {
        ArrayList<Method> methods = Lists.newArrayList();
        // 获取 oriMethod 方法内部  使用全局变量的所有方法
        for (SourceClass member : members) {
            List<Method> fieldUsage = findFieldUsage(oriMethod, member.psiField);
            methods.addAll(fieldUsage);
        }
        return methods;
    }
    public static List<Method> findFieldUsage(PsiMethod oriMethod, PsiField field) {
        ArrayList<Method> methods = Lists.newArrayList();
        // 获取 oriMethod 方法内部  使用全局变量的方法
        // 假设 youField 是我们想要查找引用的成员变量 PsiField 对象
        // 遍历方法体内的所有表达式，查找方法调用
        PsiCodeBlock body = oriMethod.getBody();
        if (body == null) {
            return methods;
        }
        for (PsiStatement statement : body.getStatements()) {
            // 搜索方法调用
            Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.collectElementsOfType(statement, PsiMethodCallExpression.class);
            for (PsiMethodCallExpression call : methodCalls) {
                // 获取被调用的方法引用
                PsiReferenceExpression methodExpression = call.getMethodExpression();
                PsiMethod resolvedMethod = (PsiMethod) methodExpression.resolve();
                // 检查该方法是否属于BService类或其子类
                PsiType fieldType = field.getType();
                PsiClassType classType = (PsiClassType) fieldType;
                // 获取类类型的 PsiClass
                PsiClass fieldClass = classType.resolve();
                PsiClass invokeClass = resolvedMethod.getContainingClass();
                if (!fieldClass.getName().equalsIgnoreCase(invokeClass.getName())) {
                    continue;
                }
                System.out.println("find mockMethod -------" + resolvedMethod.getName());

                // 构造方法名和入参
                Method method = buildInvokeMockMethod(resolvedMethod, field);
                PsiType returnType = resolvedMethod.getReturnType();
                // 构建返回值
                method.result = GenerateFiledMockRegion.buildConditionClassByType(returnType, field);
                methods.add(method);

                methods.add(buildInvokeMockMethod(resolvedMethod, field));
            }
        }
        return methods;
    }
    public static Method buildInvokeMockMethod(PsiMethod psiMethod, PsiField field) {
        Method result = new Method();
        result.isStatic = true;
        result.filed = field.getName();
        result.methodCall = psiMethod.getName();
        PsiParameterList parameterList = psiMethod.getParameterList();
        List<ConditionClass> params = Lists.newArrayList();
        for (PsiParameter parameter : parameterList.getParameters()) {
            PsiType type = parameter.getType();
            PsiClassType classType = (PsiClassType) type;
            ConditionClass conClass = GenerateFiledMockRegion.buildConditionClassByType(classType, field);
            params.add(conClass);
        }
        result.params = params;
        PsiType returnType = psiMethod.getReturnType();
        result.result = GenerateFiledMockRegion.buildConditionClassByType(returnType, field);
        return result;
    }
    public static Method buildInvokeTestMethod(PsiFile psiFile, PsiMethod psiMethod, TestInfo testInfo) {

        Method result = new Method();
        result.isStatic = true;
        result.filed = FiledNameUtil.name(psiFile.getName());
        result.methodCall = psiMethod.getName();
        PsiParameterList parameterList = psiMethod.getParameterList();
        List<ConditionClass> params = Lists.newArrayList();
        for (PsiParameter parameter : parameterList.getParameters()) {
            PsiType type = parameter.getType();
            if (!(type instanceof PsiClassType classType)) {
                continue;
            }
            ConditionClass conClass = GenerateFiledMockRegion.buildConditionClassByType(classType, null);
            params.add(conClass);
        }
        result.params = params;
        PsiType returnType = psiMethod.getReturnType();
        if (returnType == null) {
            return result;
        }
        result.result = GenerateFiledMockRegion.buildConditionClassByType(returnType, null);
        return result;
    }

}
