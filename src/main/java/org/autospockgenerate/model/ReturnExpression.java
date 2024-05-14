package org.autospockgenerate.model;

import com.intellij.psi.tree.IElementType;

public class ReturnExpression {

    public String path;

    public String classType;

    public String value;

    public String name;
    public IElementType operateType;

    public IElementType getOperateType() {
        return operateType;
    }

    public void setOperateType(IElementType operateType) {
        this.operateType = operateType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
