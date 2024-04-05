package org.autospockgenerate.generate;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiParameterImpl;
import org.apache.commons.compress.utils.Lists;
import org.autospockgenerate.model.ConditionClass;
import org.autospockgenerate.model.SourceClass;
import org.autospockgenerate.model.SourceClassType;

import java.util.List;

import static org.autospockgenerate.generate.GenerateMethodRegion.buildConditionClass;

public class GenerateFiledMockRegion {

    public static List<SourceClass> generateMockMembers(PsiClass psiFile) throws ClassNotFoundException {
        List<SourceClass> members = Lists.newArrayList();
        PsiField[] allFields = psiFile.getAllFields();
        for (PsiField field : allFields) {
            SourceClass sourceClass = buildConditionClassMock(field.getType());
            members.add(sourceClass);
        }
        return members;
    }

    public static ConditionClass buildConditionClassMock(PsiType filed) throws ClassNotFoundException {
        String presentText = filed.getPresentableText();
        ConditionClass sourceClass = new ConditionClass();
        sourceClass.name = presentText;
        String canonicalText = filed.getCanonicalText();
        sourceClass.importContent = canonicalText;
        sourceClass.testClassMemberName = presentText.toLowerCase() + presentText.substring(1);;
        SourceClassType type = new SourceClassType();
        sourceClass.type = type;
        type.canonicalText = canonicalText;
        return sourceClass;
    }
}
