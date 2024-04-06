package org.autospockgenerate.model;


import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;

public class SourceClass {

    // ClassName
    public String name;

    public String packageName;

    public String importContent;

    public SourceClassType type;

    public String testClassMemberName;

    // 该类的原始对象
    public PsiField psiField;

//    // 初始化该对象的语句
//    public String initExpression;

    public SourceClass[] innerAttrClass;

}
