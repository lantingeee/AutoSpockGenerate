package org.open.autospockgenerate.model;

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

}
