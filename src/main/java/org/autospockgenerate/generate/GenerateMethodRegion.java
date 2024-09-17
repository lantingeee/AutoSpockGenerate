package org.autospockgenerate.generate;

import com.ibm.icu.impl.Pair;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.compiled.ClsTypeElementImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiParameterImpl;
import com.intellij.psi.impl.source.tree.java.PsiIdentifierImpl;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.util.PsiTreeUtil;
import groovy.util.logging.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.autospockgenerate.collector.ConditionCollector;
import org.autospockgenerate.model.*;
import org.autospockgenerate.util.ClassNameUtil;
import org.autospockgenerate.util.FiledNameUtil;
import org.jetbrains.annotations.NotNull;

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

            // 如何 增加 分支的覆盖率呢？
            // 1.对 入参 做条件语句，可以通过 多个 Test 方法，每个方法的入参不同来实现
            // 2.对 Mock返回值 做条件语句，需要解析 mock 的方法返回值有哪些条件引用，然后生成对应的条件语句
            // 3.对 方法返回值 做条件语句
            // service.metaClass.retrieveData = { -> "mocked data" }
            testInfo.needMockTestMethods = buildNeedMockedMethod(method, members);
            testInfo.testMethod = buildInvokeTestMethod(psiFile, method);
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
    public static List<TestMethod> buildNeedMockedMethod(PsiMethod oriMethod, List<SourceClass> members) {
        List<TestMethod> needMockedMethod = Lists.newArrayList();
        // 获取 oriMethod 方法内部  使用全局变量的所有方法
        for (SourceClass member : members) {
            List<TestMethod> fieldUsage = findFieldUsage(oriMethod, member.psiField);
            needMockedMethod.addAll(fieldUsage);
        }
        return needMockedMethod;
    }

    public static List<TestMethod> findFieldUsage(PsiMethod oriMethod, PsiField field) {
        ArrayList<TestMethod> needMockedMethod = Lists.newArrayList();
        // 获取 oriMethod 方法内部  使用全局变量的方法
        // 假设 youField 是我们想要查找引用的成员变量 PsiField 对象
        // 遍历方法体内的所有表达式，查找方法调用
        PsiCodeBlock body = oriMethod.getBody();
        if (body == null) {
            return needMockedMethod;
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
                        collectConditionsFromBody(call, oriMethod.getBody(), Lists.newArrayList());
                System.out.println("find mockMethod -------" + resolvedMethod.getName());
                needMockedMethod.add(buildNeedMockedMethod(resolvedMethod, field, conditions));
            }
        }
        return needMockedMethod;
    }

    public static TestMethod buildNeedMockedMethod(PsiMethod psiMethod, PsiField field, List<PsiIfStatement> conditions) {
        TestMethod result = new TestMethod();
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

        // 根据对 返回值的 condition 去反构造 mock 方法的返回值
        List<ObjectConditionNode> conditionNodes = buildObjectConditionNode(psiMethod, field, conditions);
        result.mockedResponse = buildPrepareResponse(returnType, conditionNodes);
        return result;
    }

    public static MockedResponse buildPrepareResponse(PsiType oriType, List<ObjectConditionNode> nodes) {
        MockedResponse response = new MockedResponse();
        StringBuilder initResp = new StringBuilder();
        List<String> declareStatements = Lists.newArrayList();
        Map<String, Integer> indexMap = new HashMap<>();
        for (ObjectConditionNode lastNode : nodes) {
            // TODO: 判断 nodes 里是否使用到了 oriType
            ObjectConditionNode temp = lastNode;
            if (temp == null || temp.getNodeName() == null) {
                // mock 的对象为空的情况 跳出
                break;
            }
            ImmutablePair<String, String> curName = filledStatementFromNode(temp, initResp, oriType, indexMap, declareStatements);
            ObjectConditionNode tempPrevious = temp.getPreviousNode();
            while (tempPrevious != null) {
                // 有父节点
                ImmutablePair<String, String> names = filledStatementFromNode(tempPrevious, initResp, oriType, indexMap, declareStatements);
                initResp.append(names.left).append(".set").append(temp.getNodeName()).append("(").append(curName.left).append(");\n");
                initResp.append(names.right).append(".set").append(temp.getNodeName()).append("(").append(curName.right).append(");\n");
                tempPrevious = tempPrevious.getPreviousNode();
            }
        }
        response.responseConditionsStr = initResp.toString();
        response.declareStatements = declareStatements;
        return response;
    }

    public static ImmutablePair<String, String> filledStatementFromNode(ObjectConditionNode currentNode,
                                                                        StringBuilder initResp, PsiType oriType,
                                                                        Map<String, Integer> indexMap,
                                                                        List<String> declareStatements) {
        int tIndex = Optional.ofNullable(indexMap.get(currentNode.getNodeName())).orElse(1);
        String previous1 = currentNode.getNodeName() + tIndex++;
        String previous2 = currentNode.getNodeName() + tIndex++;
        indexMap.put(currentNode.getNodeName(), tIndex);
        if (oriType.getPresentableText().equals(currentNode.getClassName())) {
            declareStatements.add(previous1);
            declareStatements.add(previous2);
        }

        StringBuilder var1 = new StringBuilder(currentNode.getClassName()).append(" ").append(previous1);
        StringBuilder var2 = new StringBuilder(currentNode.getClassName()).append(" ").append(previous2);
        var1.append(" = ").append(initClassStatement(currentNode.getClassName())).append("\n");
        var2.append(" = ").append(initClassStatement(currentNode.getClassName())).append("\n");
        // 初始化当前 node 的声明语句
        if (currentNode.getOperateType().equals(JavaTokenType.EQEQ.toString())) {

            initResp.append(var1);
            initResp.append(var2);
        } else if (currentNode.getOperateType().equals("EMPTY")) {

            initResp.append(var1);
            initResp.append(var2);

            PsiElement element = currentNode.getElement();
            PsiType returnType = getReturnType((PsiMethodCallExpression) element);
            PsiType[] parameters = ((PsiClassType) returnType).getParameters();
            String presentableText = parameters[0].getPresentableText();

            StringBuilder iElem = new StringBuilder(presentableText).append(" e").append(presentableText).append(1);
            String elemName = iElem.toString();
            indexMap.put(returnType.getPresentableText(), 1);

            iElem.append(" = ").append(initClassStatement(presentableText)).append("\n");
            initResp.append(iElem);
            initResp.append(previous2).append(".add(").append(elemName).append(");\n");
        }
        return new ImmutablePair<>(previous1, previous2);
    }

    public static String initClassStatement(String className) {
        if ("Long".equals(className) || "long".equals(className)) {
            return "123L";
        } else if ("Integer".equals(className) || "int".equals(className)) {
            return "123";
        } else if ("Boolean".equals(className) || "bool".equals(className)) {
            return "true";
        }
        return "new " + className + "();";
    }

    public static ConditionClass buildConditionClassByType1(PsiType oriType, PsiField field) {
        String presentText = oriType.getPresentableText();
        ConditionClass sourceClass = new ConditionClass();
        sourceClass.name = ClassNameUtil.getClassName(presentText);
        String canonicalText = oriType.getCanonicalText();
        sourceClass.importContent = canonicalText;
        sourceClass.testClassMemberName = presentText.substring(0, 1).toLowerCase() + presentText.substring(1);
        SourceClassType type = new SourceClassType();
        sourceClass.type = type;
        type.canonicalText = canonicalText;
        sourceClass.psiField = field;
        return sourceClass;
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
    public static List<ObjectConditionNode> buildObjectConditionNode(PsiMethod psiMethod, PsiField field, List<PsiIfStatement> statements) {
        List<ObjectConditionNode> conditionNodeList = Lists.newArrayList();
        // 如果没有条件语句，直接返回空对象即可
        // TODO

        // 根据 返回值上的条件去反构造返回值，以达到 条件覆盖的目的
        for (PsiIfStatement statement : statements) {
            PsiExpression condition = statement.getCondition();
            PsiElement[] children = condition.getChildren();
            for (PsiElement child : children) {
                if (child instanceof PsiBinaryExpression binary) {
                    // 构造 一个链表，返回值 Node 代表 子属性，node.previousNode 可以获取 其父对象
                    ObjectConditionNode conditionNode = findObjectConditionNode(binary);
                    conditionNodeList.add(conditionNode);
                } else if (child instanceof PsiMethodCallExpression) {
                    conditionNodeList.add(findReturnExpressionByCall((PsiMethodCallExpression) child));
                }
            }
        }
        return conditionNodeList;
    }

    public static ObjectConditionNode findObjectConditionNode(PsiBinaryExpression binary) {
        // 二元表达式 等号
        ObjectConditionNode lastNode = new ObjectConditionNode();
        lastNode.operateType = binary.getOperationTokenType().toString();
        // TODO 可能为 左 也可能为 右
        PsiElement firstChild = binary.getFirstChild();
        // 等号右边 lastChild.text 是值， 等号左侧 firstChild 为 被判断的对象的路径
        PsiElement lastChild = binary.getLastChild();
        lastNode.value = lastChild.getText();

        String path = convertToJsonPath(firstChild.getText().replace("()", ""));
        // TODO:1 通过 查询该路径，找到引用链，构造 previousNode
        // 如果直接为对象 引用
        if (firstChild instanceof PsiReferenceExpression) {
            PsiElement resolve = ((PsiReferenceExpression) firstChild).resolve();
            String variableName = ((PsiVariable) resolve).getName();
            lastNode.nodeName = variableName;
            lastNode.className = ((PsiReferenceExpressionImpl) firstChild).getType().getPresentableText();
        } else {
            lastNode = retrospectMethodNode(firstChild);
        }
        return lastNode;
    }

    public static ObjectConditionNode retrospectMethodNode(PsiElement element) {
        if (element == null) {
            return null;
        }
        // 如果是个方法调用, 此方法层无需内探，内层需要继续追溯
        ObjectConditionNode chainNode = new ObjectConditionNode();
        if (element instanceof PsiMethodCallExpression) {
            PsiElement[] cElements = element.getChildren()[0].getChildren();
            for (PsiElement cElement : cElements) {
                if (cElement instanceof PsiIdentifierImpl) {
                    chainNode.nodeName = cElement.getText().replace("get", "");
                }
            }
            chainNode.className = ((PsiMethodCallExpressionImpl) element).getType().getPresentableText();
            if (isListReturnType((PsiMethodCallExpression) element)) {
                chainNode.operateType = "EMPTY";
            } else {
                chainNode.operateType = JavaTokenType.EQEQ.toString();
            }
//            chainNode.value = "null";
            chainNode.element = element;

            PsiMethodCallExpression elemExp = (PsiMethodCallExpression) element;
            // 获取"C()"的限定符"B"
            PsiReferenceExpression qualifier = (PsiReferenceExpression) elemExp.getMethodExpression().getQualifierExpression();
            ObjectConditionNode nextNode = null;
            if (qualifier instanceof PsiMethodCallExpression) {
                // 例如 A.getB().getC();  获取"C()"的限定符"B" , 从"B"回溯到对应的PsiMethodCallExpression
                PsiMethodCallExpression enclosingCall = getEnclosingMethodCallExpression(qualifier);
                nextNode = retrospectMethodNode(enclosingCall);
            } else if (qualifier instanceof PsiReferenceExpression) {
                // 如果限定符是变量引用，解析其引用
                nextNode = retrospectMethodNode(qualifier);
            }
            chainNode.setPreviousNode(nextNode);
            return chainNode;
        } else if (element instanceof PsiReferenceExpression qualifier) {
            // 以 A.getB().getC() 为例，这里是追溯到 A 对象了
            chainNode.className = qualifier.getType().getPresentableText();
            chainNode.operateType = JavaTokenType.EQEQ.toString();
            chainNode.value = "null";
            chainNode.element = element;
            chainNode.nodeName = element.getText().replace("get", "");
        }
        return chainNode;
    }

    private static boolean isListReturnType(PsiMethodCallExpression element) {
        PsiType returnType = getReturnType(element);
        if (returnType != null) {
            return returnType.getPresentableText().contains("List");
        }
        return false;
    }
    private static PsiClassType getListType(PsiMethodCallExpression element) {
        PsiType returnType = getReturnType(element);
        if (returnType instanceof PsiClassType) {
            PsiClassType.ClassResolveResult result = ((PsiClassType)returnType).resolveGenerics();
            PsiClass clazz = result.getElement();
            if (clazz != null && clazz.getName().equals("List")) {
                return (PsiClassType) returnType;
            }
        }
        return null;
    }


    private static PsiType getReturnType(PsiMethodCallExpression element) {
        PsiMethod method = element.resolveMethod();
        if (method != null) {
            return method.getReturnType();
        }
        return null;
    }

    private static PsiClass getClassFromType(PsiType type) {
        if (type instanceof PsiClassType) {
            PsiClassType.ClassResolveResult result = ((PsiClassType) type).resolveGenerics();
            PsiClass clazz = result.getElement();
            if (clazz != null) {
                return clazz;
            }
        }
        return null;
    }

    public static PsiMethodCallExpression getEnclosingMethodCallExpression(PsiReferenceExpression referenceExpression) {

        PsiElement parent = referenceExpression.getParent();

        while (parent != null && !(parent instanceof PsiMethodCallExpression)) {
            parent = parent.getParent();
        }
        if (parent instanceof PsiMethodCallExpression) {
            return (PsiMethodCallExpression) parent;
        }
        return null;
    }

    public static ObjectConditionNode findReturnExpressionByCall(PsiMethodCallExpression callExpression) {
        PsiElement firstChild = callExpression.getFirstChild();
        PsiElement lastChild = callExpression.getLastChild();
        String methodName = firstChild.getText();
        ObjectConditionNode conNode = new ObjectConditionNode();

        if ("CollectionUtils.isEmpty".equalsIgnoreCase(methodName)) {
            conNode.operateType = JavaTokenType.EQEQ.toString();
            conNode.value = "null";
            conNode.className = "java.util.ArrayList";
            conNode.element = callExpression;
            String[] split = callExpression.getText().split("\\.");
            conNode.nodeName = split[split.length - 1];

            if (lastChild.getFirstChild() != null && lastChild.getFirstChild().getText().equals("(")
                    && lastChild.getLastChild() != null && lastChild.getLastChild().getText().equalsIgnoreCase(")")) {
                PsiElement nextSibling = lastChild.getFirstChild().getNextSibling();
                conNode = retrospectMethodNode(nextSibling);
            }
        }
        return conNode;
    }

    private static String findJsonPath(PsiElement firstChild, PsiField field) {
        String canonicalText = ((PsiReferenceExpressionImpl) firstChild).getCanonicalText();

        if (firstChild.getReference().equals(field.getReference())) {
            return ".";
        }
        // TODO
        return "";
    }

    public static String convertToJsonPath(String javaGetterPath) {
        // 假设输入字符串格式为 "response.getOrderList" 形式
        // 使用正则表达式移除get或is前缀（忽略大小写），并转换为小写，然后替换点号为JSON路径的点分隔符，并在前面加上 "$"
        String jsonPath = javaGetterPath.replaceAll("(?i)^get|^is", "") // 移除get或is前缀
                .replace(".", "") // 移除原点号，因为我们将在最后统一添加JSON路径的点分隔符
                .toLowerCase(); // 转换为小写，虽然JSON路径不区分大小写，但保持一致性
        // 添加JSON路径的起点和点分隔符
        jsonPath = "$." + jsonPath.replace(".", "\\.");
        return jsonPath;
    }
}
