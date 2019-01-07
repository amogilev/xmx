// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.dto;

/**
 * Represents the name of a single or multiple Spring bean definitions (e.g. in different contexts)
 */
public class BeanNameDto {

    /**
     * The name of the Spring bean
     */
    private final String name;

    /**
     * The count of bean definitions with this name
     */
    private final int count;

    public BeanNameDto(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }
}
