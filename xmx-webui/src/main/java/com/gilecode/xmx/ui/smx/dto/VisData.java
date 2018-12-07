// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.dto;

import java.util.ArrayList;
import java.util.List;

public class VisData {

    private final List<VisNode> nodes = new ArrayList<>();
    private final List<VisEdge> edges = new ArrayList<>();

    public VisData() {
    }

    public List<VisNode> getNodes() {
        return nodes;
    }

    public List<VisEdge> getEdges() {
        return edges;
    }

    // TODO: probably mcve business logic to VisDataService or smth like that
    public void addApp(String name) {
        nodes.add(new VisNode(name, "app", name));
    }

    public String addRootContext(String appName, String path, String label, String tooltip) {
        return addContext(appName, true, path, label, tooltip);
    }

    public String addChildContext(String parentId, String path, String label, String tooltip) {
        return addContext(parentId, false, path, label, tooltip);
    }

    private String addContext(String parentId, boolean isRoot, String path, String label, String tooltip) {
        nodes.add(new VisNode(path, isRoot ? "rootCtx" : "childCtx", label, tooltip));
        edges.add(new VisEdge(parentId, path));
        return path;

    }

    public void addBean(String contextId, String name, String label) {
        String path = contextId + "#" + name;
        nodes.add(new VisNode(path, "bean", label));
        edges.add(new VisEdge(contextId, path));
    }
}
