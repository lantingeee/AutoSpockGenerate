package org.autospockgenerate.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

public class FiledNameUtil {
    public static String name(String fileName) {
        String className = fileName.substring(0, fileName.indexOf("."));
        return lowerName(className);
    }

    public static String lowerName(String name) {
        if (StringUtils.isEmpty(name)) {
            return "";
        }
        if (StringUtils.startsWith(name, "List")) {
            name = name.replace("<", "");
            name = name.replace(">", "");
        }
        String appendName = name.substring(0, 1).toLowerCase() + name.substring(1);
        if (Lists.newArrayList("long", "int", "float").contains(appendName)) {
            return appendName + "0";
        }
        return name;
    }
}
