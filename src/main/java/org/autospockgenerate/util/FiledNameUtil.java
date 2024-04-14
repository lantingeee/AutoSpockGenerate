package org.autospockgenerate.util;

import org.apache.commons.lang3.StringUtils;

public class FiledNameUtil {
    public static String name(String fileName) {
        String className = fileName.substring(0, fileName.indexOf("."));
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

    public static String lowerName(String name) {
        if (StringUtils.isEmpty(name)) {
            return "";
        }
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }
}
