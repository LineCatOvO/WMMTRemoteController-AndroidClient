package com.linecat.wmmtcontroller.service;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 状态通道消息类
 * 符合技术设计文档中的State消息格式
 */
public class StateMessage {
    @SerializedName("type")
    private final String type = "state";

    @SerializedName("stateId")
    private long stateId;

    @SerializedName("clientSendTs")
    private long clientSendTs;

    @SerializedName("keyboardState")
    private List<KeyboardEvent> keyboardState;

    @SerializedName("gamepadState")
    private GamepadState gamepadState;

    @SerializedName("flags")
    private List<String> flags;

    // 构造函数
    public StateMessage(long stateId, List<KeyboardEvent> keyboardState, GamepadState gamepadState) {
        this.stateId = stateId;
        this.clientSendTs = System.currentTimeMillis();
        this.keyboardState = keyboardState;
        this.gamepadState = gamepadState;
        this.flags = List.of();
    }

    // 带零输出标志的构造函数
    public StateMessage(long stateId, List<KeyboardEvent> keyboardState, GamepadState gamepadState, boolean zeroOutput) {
        this(stateId, keyboardState, gamepadState);
        if (zeroOutput) {
            this.flags = List.of("zero-output");
        }
    }

    // getter方法
    public String getType() {
        return type;
    }

    public long getStateId() {
        return stateId;
    }

    public long getClientSendTs() {
        return clientSendTs;
    }

    public List<KeyboardEvent> getKeyboardState() {
        return keyboardState;
    }

    public GamepadState getGamepadState() {
        return gamepadState;
    }

    public List<String> getFlags() {
        return flags;
    }

    // 游戏手柄状态类
    public static class GamepadState {
        @SerializedName("buttons")
        private List<GamepadButtonEvent> buttons;

        @SerializedName("joysticks")
        private Joysticks joysticks;

        @SerializedName("triggers")
        private Triggers triggers;

        // 构造函数
        public GamepadState(List<GamepadButtonEvent> buttons, Joysticks joysticks, Triggers triggers) {
            this.buttons = buttons;
            this.joysticks = joysticks;
            this.triggers = triggers;
        }

        // getter方法
        public List<GamepadButtonEvent> getButtons() {
            return buttons;
        }

        public Joysticks getJoysticks() {
            return joysticks;
        }

        public Triggers getTriggers() {
            return triggers;
        }

        // 摇杆状态类
        public static class Joysticks {
            @SerializedName("left")
            private Joystick left;

            @SerializedName("right")
            private Joystick right;

            // 构造函数
            public Joysticks(Joystick left, Joystick right) {
                this.left = left;
                this.right = right;
            }

            // getter方法
            public Joystick getLeft() {
                return left;
            }

            public Joystick getRight() {
                return right;
            }
        }

        // 单个摇杆状态类
        public static class Joystick {
            @SerializedName("x")
            private float x;

            @SerializedName("y")
            private float y;

            @SerializedName("deadzone")
            private float deadzone;

            // 构造函数
            public Joystick(float x, float y, float deadzone) {
                this.x = x;
                this.y = y;
                this.deadzone = deadzone;
            }

            // getter方法
            public float getX() {
                return x;
            }

            public float getY() {
                return y;
            }

            public float getDeadzone() {
                return deadzone;
            }
        }

        // 扳机状态类
        public static class Triggers {
            @SerializedName("left")
            private float left;

            @SerializedName("right")
            private float right;

            // 构造函数
            public Triggers(float left, float right) {
                this.left = left;
                this.right = right;
            }

            // getter方法
            public float getLeft() {
                return left;
            }

            public float getRight() {
                return right;
            }
        }
    }
}
