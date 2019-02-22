package com.nikolay.halftoneplotter.utils;

public class Instruction {
    private int command;
    private int value;

    public Instruction(int command, int value) {
        this.command = command;
        this.value = value;
    }

    public int getCommand() {
        return command;
    }

    public void setCommand(int command) {
        this.command = command;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
