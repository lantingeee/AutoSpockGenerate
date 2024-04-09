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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getImportContent() {
        return importContent;
    }

    public void setImportContent(String importContent) {
        this.importContent = importContent;
    }

    public SourceClassType getType() {
        return type;
    }

    public void setType(SourceClassType type) {
        this.type = type;
    }

    public String getTestClassMemberName() {
        return testClassMemberName;
    }

    public void setTestClassMemberName(String testClassMemberName) {
        this.testClassMemberName = testClassMemberName;
    }

    public PsiField getPsiField() {
        return psiField;
    }

    public void setPsiField(PsiField psiField) {
        this.psiField = psiField;
    }

    public SourceClass[] getInnerAttrClass() {
        return innerAttrClass;
    }

    public void setInnerAttrClass(SourceClass[] innerAttrClass) {
        this.innerAttrClass = innerAttrClass;
    }
}
