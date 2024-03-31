package org.open.autospockgenerate.generate;

import com.intellij.psi.*;
import org.open.autospockgenerate.model.SourceClass;
import org.open.autospockgenerate.model.SourceClassType;

public class GenerateClassRegion {
    public static SourceClass generateSourceClass(PsiFile psiFile) {

        PsiJavaFile javaFile = (PsiJavaFile) psiFile;
        PsiClass[] classes = javaFile.getClasses();
        PsiClass aClass = classes[0];

        // 创建文件
        // 准备数据: 类、被 mock 变量、方法
        String directory = psiFile.getContainingDirectory().getName();
//        String name = psiFile.getName();
        String className = aClass.getName();

        assert className != null;
        char c = className.charAt(0);
        SourceClass sourceClass = new SourceClass();
        sourceClass.name = className;
        sourceClass.packageName = directory;
        sourceClass.testClassMemberName = String.valueOf(c).toLowerCase() + className.substring(1);;
        SourceClassType type = new SourceClassType();
        sourceClass.type = type;
        type.canonicalText = className;
        return sourceClass;
    }
}
