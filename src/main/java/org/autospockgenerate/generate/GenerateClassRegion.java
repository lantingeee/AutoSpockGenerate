package org.autospockgenerate.generate;

import org.autospockgenerate.model.SourceClassType;
import com.intellij.psi.*;
import org.autospockgenerate.model.SourceClass;
import org.autospockgenerate.util.ClassNameUtil;
import org.autospockgenerate.util.FiledNameUtil;

public class GenerateClassRegion {
    public static SourceClass generateSourceClass(PsiFile psiFile) {

        PsiJavaFile javaFile = (PsiJavaFile) psiFile;
        PsiClass[] classes = javaFile.getClasses();
        PsiClass aClass = classes[0];

        // 创建文件
        // 准备数据: 类、被 mock 变量、方法
        String directory = psiFile.getContainingDirectory().getName();
        String className = aClass.getName();

        SourceClass sourceClass = new SourceClass();
        sourceClass.name = ClassNameUtil.getClassName(className);
        sourceClass.packageName = directory;
        sourceClass.testClassMemberName = FiledNameUtil.lowerName(className);
        SourceClassType type = new SourceClassType();
        sourceClass.type = type;
        type.canonicalText = className;
        return sourceClass;
    }
}
