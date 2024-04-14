package org.autospockgenerate.generate;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiParameterImpl;
import org.apache.commons.compress.utils.Lists;
import org.autospockgenerate.model.ConditionClass;
import org.autospockgenerate.model.SourceClass;
import org.autospockgenerate.model.SourceClassType;
import org.autospockgenerate.util.ClassNameUtil;

import java.util.List;
public class GenerateFiledMockRegion {

    public static List<SourceClass> generateMockMembers(PsiClass psiFile) throws ClassNotFoundException {
        List<SourceClass> members = Lists.newArrayList();
        PsiField[] allFields = psiFile.getAllFields();
        for (PsiField field : allFields) {
            SourceClass sourceClass = buildConditionClassMock(field);
            // logger 属性名的跳过
            if (sourceClass.getName().equalsIgnoreCase("logger")) {
                continue;
            }
            members.add(sourceClass);
        }
        return members;
    }

    public static ConditionClass buildConditionClassMock(PsiField field) {
        PsiType filedType = field.getType();
        return buildConditionClassByType(filedType, field);
    }
    public static ConditionClass buildConditionClassByType(PsiType oriType, PsiField field) {
        String presentText = oriType.getPresentableText();
        ConditionClass sourceClass = new ConditionClass();
        sourceClass.name = ClassNameUtil.getClassName(presentText);
        String canonicalText = oriType.getCanonicalText();
        sourceClass.importContent = canonicalText;
        sourceClass.testClassMemberName = presentText.substring(0, 1).toLowerCase() + presentText.substring(1);
        SourceClassType type = new SourceClassType();
        sourceClass.type = type;
        type.canonicalText = canonicalText;
        sourceClass.psiField = field;
        return sourceClass;
    }

}
