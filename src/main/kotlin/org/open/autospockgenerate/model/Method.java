package org.open.autospockgenerate.model;


public class Method {
    public boolean isStatic;

    // 调用该方法的对象名
    public String filed;

    // 方法名
    public String methodCall;

    // 需要的入参
    public ConditionClass[] params;

    public ConditionClass result;
}
