// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

public class DescriptorSpecialMethodSpec extends DescriptorMethodSpec {

    private final String specialName;

    public DescriptorSpecialMethodSpec(int modifiers, String name, String descriptor, String specialName) {
        super(modifiers, name, descriptor);
        this.specialName = specialName;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public String getSpecialName() {
        return specialName;
    }
}
