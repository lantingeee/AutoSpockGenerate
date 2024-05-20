package org.autospockgenerate.model;

import com.intellij.psi.tree.IElementType;

public class ObjectConditionNode {
    public String nodeName;

    public String className;

    public IElementType operateType;
    public String value;
    public ObjectConditionNode lastNode;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public IElementType getOperateType() {
        return operateType;
    }

    public void setOperateType(IElementType operateType) {
        this.operateType = operateType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    public ObjectConditionNode getLastNode() {
        return lastNode;
    }

    public void setLastNode(ObjectConditionNode lastNode) {
        this.lastNode = lastNode;
    }
}
