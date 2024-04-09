package org.autospockgenerate.model;

import java.util.List;
import java.util.Map;

public class ConditionClass extends SourceClass {
    public Map<String, List<String>> varCondition;

    public Map<String, List<String>> getVarCondition() {
        return varCondition;
    }

    public void setVarCondition(Map<String, List<String>> varCondition) {
        this.varCondition = varCondition;
    }
}
