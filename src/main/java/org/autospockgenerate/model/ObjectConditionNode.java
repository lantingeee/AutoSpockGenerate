package org.autospockgenerate.model;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

public class ObjectConditionNode {
    public String nodeName;

    public String className;

    public IElementType operateType;

    public String value;
    public ObjectConditionNode previousNode;

    public PsiElement element;

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

    public ObjectConditionNode getPreviousNode() {
        return previousNode;
    }

    public void setPreviousNode(ObjectConditionNode previousNode) {
        this.previousNode = previousNode;
    }

    public PsiElement getElement() {
        return element;
    }

    public void setElement(PsiElement element) {
        this.element = element;
    }
}
