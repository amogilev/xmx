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
        nodes.add(VisNode.app(name, name));
    }

    public void addRootContext(String appName, String path, String label, String tooltip) {
        nodes.add(VisNode.context(path, label, tooltip, true, appName));
        edges.add(new VisEdge(appName, path));
    }

    public void addChildContext(String appName, String parentId, String path, String label, String tooltip) {
        nodes.add(VisNode.context(path, label, tooltip, false, parentId));
        edges.add(new VisEdge(appName, path));
        edges.add(new VisEdge(parentId, path));
    }

    public void addBean(String contextId, String path, String label) {
        nodes.add(VisNode.bean(path, label, contextId));
        edges.add(new VisEdge(contextId, path));
    }

    public void addBeansCluster(String contextId, int beansCount) {
        String path = contextId + "#";
        nodes.add(VisNode.beansCluster(path, contextId, beansCount));
        edges.add(new VisEdge(contextId, path));
    }
}
