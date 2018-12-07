// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.dto;

public class VisNode {

    private final String id;
    private final String group;
    private final String label;
    private final String title;

    public VisNode(String id, String group, String label) {
        this (id, group, label, null);
    }

    public VisNode(String id, String group, String label, String title) {
        this.id = id;
        this.group = group;
        this.label = label;
        this.title = title;
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
