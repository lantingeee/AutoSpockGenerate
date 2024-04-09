package org.autospockgenerate.model;


import java.util.List;

public class Method {
    public boolean isStatic;

    // 调用该方法的对象名
    public String filed;

    // 方法名
    public String methodCall;

    // 需要的入参
    public List<ConditionClass> params;

    public ConditionClass result;

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public String getFiled() {
        return filed;
    }

    public void setFiled(String filed) {
        this.filed = filed;
    }

    public String getMethodCall() {
        return methodCall;
    }

    public void setMethodCall(String methodCall) {
        this.methodCall = methodCall;
    }

    public List<ConditionClass> getParams() {
        return params;
    }

    public void setParams(List<ConditionClass> params) {
        this.params = params;
    }

    public ConditionClass getResult() {
        return result;
    }

    public void setResult(ConditionClass result) {
        this.result = result;
    }
}
