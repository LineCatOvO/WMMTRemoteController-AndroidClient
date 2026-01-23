package com.linecat.wmmtcontroller.model;

/**
 * 格式化输入消息模型
 * 用于适配服务端期望的InputMessage格式
 */
public class FormattedInputMessage {
    private String type = "input";
    private Data data;
    private InputMetadata metadata;

    public FormattedInputMessage(InputState inputState) {
        this.type = "input";
        this.metadata = new InputMetadata();
        this.metadata.setTimestamp(System.currentTimeMillis());
        this.data = new Data(inputState);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public InputMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(InputMetadata metadata) {
        this.metadata = metadata;
    }

    public static class Data {
        public Long frameId;
        public String runtimeStatus;
        public String[] keyboard;
        public Mouse mouse;
        public Joystick joystick;

        public Data(InputState inputState) {
            this.frameId = inputState.getFrameId();
            this.runtimeStatus = inputState.getRuntimeStatus();
            this.keyboard = inputState.getKeyboard().toArray(new String[inputState.getKeyboard().size()]);
            this.mouse = new Mouse(inputState.getMouse());
            this.joystick = new Joystick(inputState.getJoystick());
        }
    }

    public static class Mouse {
        public float x;
        public float y;
        public boolean left;
        public boolean right;
        public boolean middle;

        public Mouse(InputState.MouseState mouseState) {
            this.x = mouseState.getX();
            this.y = mouseState.getY();
            this.left = mouseState.isLeft();
            this.right = mouseState.isRight();
            this.middle = mouseState.isMiddle();
        }
    }

    public static class Joystick {
        public float x;
        public float y;
        public float deadzone;
        public float smoothing;

        public Joystick(InputState.JoystickState joystickState) {
            this.x = joystickState.getX();
            this.y = joystickState.getY();
            this.deadzone = joystickState.getDeadzone();
            this.smoothing = joystickState.getSmoothing();
        }
    }


}