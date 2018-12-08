// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.dto;

public class VisNode {

    private final String id;
    private final String group;
    private final String label;
    private final String title;
    private final String parentId;

    private VisNode(String id, String group, String label, String title, String parentId) {
        this.id = id;
        this.group = group;
        this.label = label;
        this.title = title;
        this.parentId = parentId;
    }

    public static VisNode bean(String id, String label, String contextId) {
        return new VisNode(id, "bean", label, null, contextId);
    }

    public static VisNode app(String id, String label) {
        return new VisNode(id, "app", label, null, null);
    }

    public static VisNode context(String id, String label, String title, boolean isRoot, String parentId) {
        return new VisNode(id, isRoot ? "rootCtx" : "childCtx", label, title, parentId);
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

    public String getParentId() {
        return parentId;
    }
}
