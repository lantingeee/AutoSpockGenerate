package org.open.autospockgenerate.generate;

import com.intellij.psi.*;
import org.apache.commons.compress.utils.Lists;
import org.open.autospockgenerate.model.SourceClass;

import java.util.List;

public class GenerateFiledMockRegion {

    public static List<SourceClass> generateMockMembers(PsiClass psiFile) {
        List<SourceClass> members = Lists.newArrayList();
        PsiField[] allFields = psiFile.getAllFields();
        for (PsiField field : allFields) {
            PsiFile fieldPsiFile = field.getContainingFile();
            SourceClass sourceClass = GenerateClassRegion.generateSourceClass(fieldPsiFile);
            members.add(sourceClass);
        }
        return members;
    }
}
