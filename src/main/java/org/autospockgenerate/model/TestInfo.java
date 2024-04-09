package org.autospockgenerate.model;

import java.util.List;
import java.util.Map;

public class TestInfo {

    // given: 块中用到的 入参构造 (属性可以是变化的)
    public List<ConditionClass> params;

    // 需要 mock 的方法
    public List<Method> needMockMethods;

    // 需要测试的方法
    public Method testMethod;

    // where 条件块
    public Map<String, List<String>> varConditionMap;

    public List<ConditionClass> getParams() {
        return params;
    }

    public void setParams(List<ConditionClass> params) {
        this.params = params;
    }

    public List<Method> getNeedMockMethods() {
        return needMockMethods;
    }

    public void setNeedMockMethods(List<Method> needMockMethods) {
        this.needMockMethods = needMockMethods;
    }

    public Method getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(Method testMethod) {
        this.testMethod = testMethod;
    }

    public Map<String, List<String>> getVarConditionMap() {
        return varConditionMap;
    }

    public void setVarConditionMap(Map<String, List<String>> varConditionMap) {
        this.varConditionMap = varConditionMap;
    }
}
