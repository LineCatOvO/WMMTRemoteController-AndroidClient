package com.linecat.wmmtcontroller.service;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 事件变化类
 * 用于存储事件通道消息的变化内容
 */
public class EventDelta {
    @SerializedName("keyboard")
    private List<KeyboardEventDelta> keyboard;

    @SerializedName("gamepad")
    private GamepadEventDelta gamepad;

    // 构造函数
    public EventDelta() {
        this.keyboard = List.of();
        this.gamepad = new GamepadEventDelta();
    }

    // 带键盘变化的构造函数
    public EventDelta(List<KeyboardEventDelta> keyboard) {
        this.keyboard = keyboard;
        this.gamepad = new GamepadEventDelta();
    }

    // 带游戏手柄变化的构造函数
    public EventDelta(GamepadEventDelta gamepad) {
        this.keyboard = List.of();
        this.gamepad = gamepad;
    }

    // 完整构造函数
    public EventDelta(List<KeyboardEventDelta> keyboard, GamepadEventDelta gamepad) {
        this.keyboard = keyboard;
        this.gamepad = gamepad;
    }

    // getter方法
    public List<KeyboardEventDelta> getKeyboard() {
        return keyboard;
    }

    public GamepadEventDelta getGamepad() {
        return gamepad;
    }

    // 键盘事件变化类
    public static class KeyboardEventDelta {
        @SerializedName("keyId")
        private String keyId;

        @SerializedName("eventType")
        private String eventType;

        // 事件类型常量
        public static final String EVENT_TYPE_PRESSED = "pressed";
        public static final String EVENT_TYPE_RELEASED = "released";

        // 构造函数
        public KeyboardEventDelta(String keyId, String eventType) {
            this.keyId = keyId;
            this.eventType = eventType;
        }

        // getter方法
        public String getKeyId() {
            return keyId;
        }

        public String getEventType() {
            return eventType;
        }

        // 工厂方法
        public static KeyboardEventDelta pressed(String keyId) {
            return new KeyboardEventDelta(keyId, EVENT_TYPE_PRESSED);
        }

        public static KeyboardEventDelta released(String keyId) {
            return new KeyboardEventDelta(keyId, EVENT_TYPE_RELEASED);
        }
    }

    // 游戏手柄事件变化类
    public static class GamepadEventDelta {
        @SerializedName("buttons")
        private List<GamepadButtonEventDelta> buttons;

        @SerializedName("joysticks")
        private JoystickEventDelta joysticks;

        @SerializedName("triggers")
        private TriggerEventDelta triggers;

        // 构造函数
        public GamepadEventDelta() {
            this.buttons = List.of();
            this.joysticks = new JoystickEventDelta();
            this.triggers = new TriggerEventDelta();
        }

        // 带按钮变化的构造函数
        public GamepadEventDelta(List<GamepadButtonEventDelta> buttons) {
            this.buttons = buttons;
            this.joysticks = new JoystickEventDelta();
            this.triggers = new TriggerEventDelta();
        }

        // getter方法
        public List<GamepadButtonEventDelta> getButtons() {
            return buttons;
        }

        public JoystickEventDelta getJoysticks() {
            return joysticks;
        }

        public TriggerEventDelta getTriggers() {
            return triggers;
        }

        // 游戏手柄按键事件变化类
        public static class GamepadButtonEventDelta {
            @SerializedName("buttonId")
            private String buttonId;

            @SerializedName("eventType")
            private String eventType;

            // 事件类型常量
            public static final String EVENT_TYPE_PRESSED = "pressed";
            public static final String EVENT_TYPE_RELEASED = "released";

            // 构造函数
            public GamepadButtonEventDelta(String buttonId, String eventType) {
                this.buttonId = buttonId;
                this.eventType = eventType;
            }

            // getter方法
            public String getButtonId() {
                return buttonId;
            }

            public String getEventType() {
                return eventType;
            }

            // 工厂方法
            public static GamepadButtonEventDelta pressed(String buttonId) {
                return new GamepadButtonEventDelta(buttonId, EVENT_TYPE_PRESSED);
            }

            public static GamepadButtonEventDelta released(String buttonId) {
                return new GamepadButtonEventDelta(buttonId, EVENT_TYPE_RELEASED);
            }
        }

        // 摇杆事件变化类
        public static class JoystickEventDelta {
            @SerializedName("left")
            private JoystickDelta left;

            @SerializedName("right")
            private JoystickDelta right;

            // 构造函数
            public JoystickEventDelta() {
                this.left = null;
                this.right = null;
            }

            // 带左摇杆变化的构造函数
            public JoystickEventDelta(JoystickDelta left) {
                this.left = left;
                this.right = null;
            }

            // 完整构造函数
            public JoystickEventDelta(JoystickDelta left, JoystickDelta right) {
                this.left = left;
                this.right = right;
            }
            
            // 工厂方法，用于创建只有右摇杆变化的实例
            public static JoystickEventDelta withRight(JoystickDelta right) {
                return new JoystickEventDelta(null, right);
            }

            // getter方法
            public JoystickDelta getLeft() {
                return left;
            }

            public JoystickDelta getRight() {
                return right;
            }

            // 摇杆变化类
            public static class JoystickDelta {
                @SerializedName("x")
                private float x;

                @SerializedName("y")
                private float y;

                // 构造函数
                public JoystickDelta(float x, float y) {
                    this.x = x;
                    this.y = y;
                }

                // getter方法
                public float getX() {
                    return x;
                }

                public float getY() {
                    return y;
                }
            }
        }

        // 扳机事件变化类
        public static class TriggerEventDelta {
            @SerializedName("left")
            private Float left;

            @SerializedName("right")
            private Float right;

            // 构造函数
            public TriggerEventDelta() {
                this.left = null;
                this.right = null;
            }

            // 带左扳机变化的构造函数
            public TriggerEventDelta(float left) {
                this.left = left;
                this.right = null;
            }

            // 带右扳机变化的构造函数
            public TriggerEventDelta(float left, float right) {
                this.left = left;
                this.right = right;
            }

            // getter方法
            public Float getLeft() {
                return left;
            }

            public Float getRight() {
                return right;
            }
        }
    }
}
