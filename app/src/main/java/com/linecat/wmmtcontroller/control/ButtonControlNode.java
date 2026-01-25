package com.linecat.wmmtcontroller.control;

/**
 * 按钮控制节点
 * 处理按钮类型的控制输入
 */
public class ButtonControlNode implements ControlNode {
    private String id;
    private String buttonType; // 按钮类型，如 A, B, X, Y, LB, RB 等
    private boolean pressed;

    public ButtonControlNode(String id, String buttonType) {
        this.id = id;
        this.buttonType = buttonType;
        this.pressed = false;
    }

    @Override
    public void handleAction(ControlAction action) {
        switch (buttonType.toLowerCase()) {
            case "a":
                if (action.getButtonA() != null) {
                    this.pressed = action.getButtonA();
                }
                break;
            case "b":
                if (action.getButtonB() != null) {
                    this.pressed = action.getButtonB();
                }
                break;
            case "x":
                if (action.getButtonX() != null) {
                    this.pressed = action.getButtonX();
                }
                break;
            case "y":
                if (action.getButtonY() != null) {
                    this.pressed = action.getButtonY();
                }
                break;
            case "shoulder_l":
                if (action.getShoulderL() != null) {
                    this.pressed = action.getShoulderL();
                }
                break;
            case "shoulder_r":
                if (action.getShoulderR() != null) {
                    this.pressed = action.getShoulderR();
                }
                break;
            case "trigger_l":
                if (action.getTriggerL() != null) {
                    this.pressed = action.getTriggerL();
                }
                break;
            case "trigger_r":
                if (action.getTriggerR() != null) {
                    this.pressed = action.getTriggerR();
                }
                break;
            default:
                // 可能是键盘按键，通过索引访问
                try {
                    int keyCode = Integer.parseInt(buttonType);
                    if (keyCode >= 0 && keyCode < action.getKeys().length) {
                        this.pressed = action.getKey(keyCode);
                    }
                } catch (NumberFormatException e) {
                    // 如果不是数字，则不处理
                }
                break;
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void update() {
        // 按钮节点不需要特殊更新逻辑
    }

    public boolean isPressed() {
        return pressed;
    }

    public String getButtonType() {
        return buttonType;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }
}