// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.dto;

public class BeanInfoDto {
    private final String id;
    private final String name;

    public BeanInfoDto(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
