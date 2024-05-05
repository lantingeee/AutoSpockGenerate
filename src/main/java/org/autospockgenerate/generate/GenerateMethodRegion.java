package org.autospockgenerate.generate;

import com.fasterxml.jackson.jr.ob.JSON;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.intellij.json.JsonUtil;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.source.PsiParameterImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.io.jackson.JacksonUtil;
import groovy.util.logging.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.autospockgenerate.collector.ConditionCollector;
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

            // 1.对 入参 做条件语句
            // 2.对 Mock返回值 做条件语句
            // 3.对 方法返回值 做条件语句
            // service.metaClass.retrieveData = { -> "mocked data" } )
            testInfo.needMockTestMethods = buildNeedMockMethod(method, members);
            testInfo.testMethod = buildInvokeTestMethod(psiFile, method);
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
    public static List<TestMethod> buildNeedMockMethod(PsiMethod oriMethod, List<SourceClass> members) {
        ArrayList<TestMethod> testMethods = Lists.newArrayList();
        // 获取 oriMethod 方法内部  使用全局变量的所有方法
        for (SourceClass member : members) {
            List<TestMethod> fieldUsage = findFieldUsage(oriMethod, member.psiField);
            testMethods.addAll(fieldUsage);
        }
        return testMethods;
    }
    public static List<TestMethod> findFieldUsage(PsiMethod oriMethod, PsiField field) {
        ArrayList<TestMethod> testMethods = Lists.newArrayList();
        // 获取 oriMethod 方法内部  使用全局变量的方法
        // 假设 youField 是我们想要查找引用的成员变量 PsiField 对象
        // 遍历方法体内的所有表达式，查找方法调用
        PsiCodeBlock body = oriMethod.getBody();
        if (body == null) {
            return testMethods;
        }
        for (PsiStatement statement : body.getStatements()) {
            // 搜索方法调用
            Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.collectElementsOfType(statement, PsiMethodCallExpression.class);

            // 为方法调用 集合中的每个方法调用
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
                // 获取调用返回值表达式
                List<PsiIfStatement> conditions = ConditionCollector.
                        collectConditionsUsingExpression(call, oriMethod, Lists.newArrayList());
                System.out.println("find mockMethod -------" + resolvedMethod.getName());
                testMethods.add(buildNeedMockMethod(oriMethod, field, conditions));
            }
        }
        return testMethods;
    }

    public static TestMethod buildNeedMockMethod(PsiMethod psiMethod, PsiField field, List<PsiIfStatement> conditions) {
        TestMethod result = new TestMethod();
        result.isStatic = true;
        result.filed = field.getName();
        result.methodCall = psiMethod.getName();
        // 根据对 返回值的 condition 去反构造返回值
        result.returnExpressions = buildReturnExpression(psiMethod, conditions);
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
    public static TestMethod buildInvokeTestMethod(PsiFile psiFile, PsiMethod psiMethod) {

        TestMethod result = new TestMethod();
        result.isStatic = true;
        result.filed = FiledNameUtil.name(psiFile.getName());
        result.methodCall = psiMethod.getName();
//        result.returnExpressions = buildReturnExpression(psiMethod); TODO
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

    // 对于方法调用的 Mock 的返回值 列表
    public static List<ReturnExpression> buildReturnExpression(PsiMethod psiMethod, List<PsiIfStatement> conditions){
        List<ReturnExpression> returnExpList = Lists.newArrayList();
        // 如果没有条件语句，直接返回空对象即可
        // TODO

        // 根据 对 返回值做的条件去反构造返回值，以达到 条件覆盖的目的
//        for (PsiIfStatement condition : conditions) {
//            ReturnExpression returnExpression = new ReturnExpression();
//            returnExpression.expression = condition.getThenBranch().getText();
//            returnExpression.condition = condition.getCondition().getText();
//            returnExpList.add(returnExpression);
//        }
        Gson x = new Gson();
        System.out.println(x.toJson(conditions));
        return returnExpList;
    }

}
