package org.open.autospockgenerate.generate;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import org.apache.commons.compress.utils.Lists;
import org.open.autospockgenerate.model.TestInfo;

import java.util.List;

public class GenerateMethodRegion {
    public static List<TestInfo> generateAllMethod(PsiMethod[] allMethods, PsiField[] allFields) {
        List<TestInfo> testInfos = Lists.newArrayList();

        for (PsiMethod allMethod : allMethods) {

        }

        return testInfos;
    }

}
