// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.dto;

public class VisNode {

    private final String id;
    private final String group;
    private final String label;
    private final String title;

    private VisNode(String id, String group, String label, String title) {
        this.id = id;
        this.group = group;
        this.label = label;
        this.title = title;
    }

    public static VisNode bean(String id, String label) {
        return new VisNode(id, "bean", label, null);
    }

    public static VisNode beansCluster(String path, int count) {
        return new VisNode(path, "beanCluster", count + " beans...", null);
    }

    public static VisNode app(String id, String label) {
        return new VisNode(id, "app", label, null);
    }

    public static VisNode context(String id, String label, String title, boolean isRoot) {
        return new VisNode(id, isRoot ? "rootCtx" : "childCtx", label, title);
    }

    public String getId() {
        return id;
    }

    public String getGroup() {
        return group;
    }

    public String getLabel() {
        return label;
    }

    public String getTitle() {
        return title;
    }
}
