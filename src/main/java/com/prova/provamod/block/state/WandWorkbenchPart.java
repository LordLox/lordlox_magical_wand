package com.prova.provamod.block.state;

import net.minecraft.util.StringRepresentable;

public enum WandWorkbenchPart implements StringRepresentable {
    LEFT("left"),
    RIGHT("right");

    private final String name;

    WandWorkbenchPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
