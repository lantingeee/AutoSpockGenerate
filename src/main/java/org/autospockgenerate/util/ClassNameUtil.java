package org.autospockgenerate.util;

import kotlinx.html.COL;
import org.apache.commons.lang3.StringUtils;

public class ClassNameUtil {
    public static String getClassName(String className) {
        if (StringUtils.isEmpty(className)) {
            return className;
        }
        if (className.contains("List")) {
            return "ArrayList";
        }
        return className;
    }
}
