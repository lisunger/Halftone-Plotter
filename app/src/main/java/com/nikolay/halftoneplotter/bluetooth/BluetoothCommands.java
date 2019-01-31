package com.nikolay.halftoneplotter.bluetooth;

public class BluetoothCommands {
    public static final int COMMAND_UP =    1;
    public static final int COMMAND_DOWN =  2;
    public static final int COMMAND_LEFT =  3;
    public static final int COMMAND_RIGHT = 4;
    public static final int COMMAND_DOT =   5;
    public static final int COMMAND_STOP =  6;

    public static final int VALUE_UP =      35; // 1 degree rotation is 5.68 steps
    public static final int VALUE_DOWN =    35; // 1 degree rotation is 5.68 steps
    public static final int VALUE_LEFT =    3;
    public static final int VALUE_RIGHT =   3;

    public static final int ROTATION_BYJ =  2048;
    public static final int ROTATION_NEMA = 200;
}
