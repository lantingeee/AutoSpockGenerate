package org.autospockgenerate.model;

import java.util.List;

public class MockedResponse {

    // 声明这些对象的语法
    public List<String> declareStatements;

    // 返回的 mock 列表
    public String responseConditionsStr;

    public List<String> getDeclareStatements() {
        return declareStatements;
    }

    public void setDeclareStatements(List<String> declareStatements) {
        this.declareStatements = declareStatements;
    }

    public String getResponseConditionsStr() {
        return responseConditionsStr;
    }

    public void setResponseConditionsStr(String responseConditionsStr) {
        this.responseConditionsStr = responseConditionsStr;
    }
}
