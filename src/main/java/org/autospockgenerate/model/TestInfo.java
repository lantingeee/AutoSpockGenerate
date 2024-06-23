package org.autospockgenerate.model;

import java.util.List;
import java.util.Map;

public class TestInfo {

    // given: 块中用到的 入参构造 (属性可以是变化的)
    public List<ConditionClass> params;

    // 需要 mock 的方法
    public List<TestMethod> needMockTestMethods;

    // 需要测试的方法
    public TestMethod testMethod;


    public List<ConditionClass> getParams() {
        return params;
    }

    public void setParams(List<ConditionClass> params) {
        this.params = params;
    }

    public List<TestMethod> getNeedMockMethods() {
        return needMockTestMethods;
    }

    public void setNeedMockMethods(List<TestMethod> needMockTestMethods) {
        this.needMockTestMethods = needMockTestMethods;
    }

    public TestMethod getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(TestMethod testMethod) {
        this.testMethod = testMethod;
    }

}
