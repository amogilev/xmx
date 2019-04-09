// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.ucfg.OptionDescription;
import com.gilecode.ucfg.SectionDescription;
import com.gilecode.xmx.cfg.Properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfigUtils {

    private static int totalSize(List<SectionDescription>[] lists) {
        int sz = 0;
        for (List<SectionDescription> list : lists) {
            sz += list.size();
        }
        return sz;
    }

    @SafeVarargs
    static List<SectionDescription> unionList(List<SectionDescription>...lists) {
        List<SectionDescription> total = new ArrayList<>(totalSize(lists));
        for (List<SectionDescription> list : lists) {
            total.addAll(list);
        }
        return Collections.unmodifiableList(total);
    }

    public static List<SectionDescription> adviceSections(String className, String methodName, String adviceClass) {

        SectionDescription classSection = new SectionDescription(
                "App=*;Class=" + className,
                null,
                new OptionDescription(Properties.SP_MANAGED, true),
                new OptionDescription(Properties.CLASS_MAX_INSTANCES, -1));

        SectionDescription adviceSection = new SectionDescription(
                "App=*;Class=" + className + ";" +
                        "Method=\"" + methodName + "\"",
                null,
                new OptionDescription(Properties.M_ADVICES, "xmx-advices.jar:" + adviceClass));

        return Arrays.asList(classSection, adviceSection);
    }
}
