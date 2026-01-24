# 从现有架构迁移到三层控制架构指南

## 概述

本文档介绍如何将现有的基于Region的布局系统迁移到新的三层控制架构（ControlNode、ControlAction、DeviceMapping）。

## 三层架构介绍

### 0. 三层架构总控管理器

- **职责**：统一管理UI层、Operation层和Mapping层，提供完整的三层架构控制
- **实现类**：
  - `ThreeTierControlManager`：整合三个层级管理器的总控

### 1. ControlNode层（控制节点层）

- **职责**：对应三层架构中的UI层，负责接收原始输入（触控、陀螺仪等），执行传感器级输入处理，输出归一化后的抽象值
- **实现类**：
  - `ButtonControlNode`：按钮类型的控制节点
  - `AxisControlNode`：轴类型的控制节点（如摇杆、滑块）
  - `GyroControlNode`：陀螺仪类型的控制节点

### 2. ControlAction层（控制动作层）

- **职责**：对应三层架构中的Operation层，负责定义抽象控制语义（方向盘、油门、刹车、按钮类操作）和执行抽象控制语义处理
- **特性**：包含死区、平滑、曲线等处理算法

### 3. DeviceMapping层（设备映射层）

- **职责**：对应三层架构中的Mapping层，负责将抽象语义映射到设备输出和执行设备适配
- **特性**：支持键盘、游戏手柄、鼠标等多种设备映射

## 各层管理器介绍

### UI层管理器

- **职责**：作为UI层的总控，负责管理所有ControlNode节点，处理节点的渲染和事件，统一向Operation层传递数据
- **实现类**：
  - `UINodeManager`：UI节点管理器

### Operation层管理器

- **职责**：作为Operation层的总控，负责管理所有ControlAction动作，处理动作的语义转换和处理算法，统一向Mapping层传递数据
- **实现类**：
  - `OperationNodeManager`：Operation节点管理器

### Mapping层管理器

- **职责**：作为Mapping层的总控，负责管理所有DeviceMapping映射，处理设备映射和适配，统一生成最终的输入状态
- **实现类**：
  - `MappingNodeManager`：Mapping节点管理器

## 迁移步骤

### 步骤1：引入新架构类

将新的三层架构类引入到项目中：

- `ControlNode.java`
- `ControlAction.java`
- `DeviceMapping.java`
- `ControlLayerCoordinator.java`
- `NewLayoutEngine.java`
- `UINodeManager.java`
- `OperationNodeManager.java`
- `MappingNodeManager.java`
- `ThreeTierControlManager.java`

### 步骤2：使用转换器迁移现有布局

使用`LayoutToControlNodeConverter`将现有的Region布局转换为ControlNode体系：

```java
// 从现有布局快照转换为控制节点
LayoutSnapshot layoutSnapshot = // 从现有系统获取
List<ControlNode> controlNodes = LayoutToControlNodeConverter.convertLayoutToControlNodes(layoutSnapshot);

// 或者反向转换
List<ControlNode> controlNodes = // 从新架构获取
LayoutSnapshot layoutSnapshot = LayoutToControlNodeConverter.convertControlNodesToLayout(controlNodes);
```

### 步骤3：更新布局引擎

可以选择使用新的`NewLayoutEngine`或`EnhancedLayoutEngine`替代原有的`LayoutEngine`：

**使用EnhancedLayoutEngine（推荐）：**

```java
// 创建设备映射
DeviceMapping deviceMapping = new DeviceMapping("default_mapping", "默认映射", DeviceMapping.MappingType.KEYBOARD);

// 创建增强版布局引擎
EnhancedLayoutEngine enhancedLayoutEngine = new EnhancedLayoutEngine(deviceMapping);

// 加载现有布局
enhancedLayoutEngine.loadLayoutFromSnapshot(existingLayoutSnapshot);
```

**使用NewLayoutEngine（原始版本）：**

```java
// 创建设备映射
DeviceMapping deviceMapping = new DeviceMapping("default_mapping", "默认映射", DeviceMapping.MappingType.KEYBOARD);

// 创建新布局引擎
NewLayoutEngine newLayoutEngine = new NewLayoutEngine(deviceMapping);

// 加载现有布局
newLayoutEngine.loadLayoutFromSnapshot(existingLayoutSnapshot);
```

### 步骤4：集成到服务

在`InputRuntimeService`中逐步替换原有布局引擎：

```java
// 在InputRuntimeService中
private NewLayoutEngine newLayoutEngine;

private void initializeComponents() {
    // 初始化新的布局引擎
    DeviceMapping deviceMapping = new DeviceMapping("default_mapping", "默认映射", DeviceMapping.MappingType.KEYBOARD);
    newLayoutEngine = new NewLayoutEngine(deviceMapping);
    
    // 其他初始化代码...
}
```

## 优势

### 1. 更好的模块化

- 每一层职责明确，易于单独测试和维护
- 可以独立修改某一层而不影响其他层

### 2. 更强的扩展性

- 可以轻松添加新的控制节点类型
- 支持多种设备映射配置

### 3. 更好的性能

- 三层架构提供了清晰的数据流
- 减少了不必要的重复计算

## 迁移注意事项

### 1. 兼容性

- 现有的布局文件仍可使用，通过转换器自动转换
- 逐步迁移，可以保持旧系统作为备用

### 2. 测试

- 在迁移过程中，需要充分测试各类输入场景
- 确保转换后的控制节点行为与原Region一致

### 3. 性能

- 新架构可能会略微增加内存使用，但会提高处理效率
- 建议在真实设备上测试性能表现

## 未来发展方向

### 1. 高级控制节点

- 可以轻松添加复杂的手势识别节点
- 支持AI增强的输入预测节点

### 2. 动态映射

- 支持根据游戏类型动态调整设备映射
- 提供用户自定义映射界面

### 3. 输入预处理

- 在ControlAction层可以添加高级预处理算法
- 支持输入平滑、预测等功能

## 增强版布局引擎

`EnhancedLayoutEngine.java` 是一个新的布局引擎实现，它使用了完整的三层架构管理器，提供了：

1. 与现有系统的兼容性
2. 更好的管理能力
3. 更简单的API

使用方法：

```java
// 创建设备映射
DeviceMapping deviceMapping = new DeviceMapping("default_mapping", "默认映射", DeviceMapping.MappingType.KEYBOARD);

// 创建增强版布局引擎
EnhancedLayoutEngine enhancedEngine = new EnhancedLayoutEngine(deviceMapping);

// 加载布局
enhancedEngine.loadLayoutFromSnapshot(layoutSnapshot);

// 处理输入
InputState inputState = enhancedEngine.executeLayout(rawInput, frameId);
```

## 使用示例

`ControlArchitectureExample.java` 提供了一个完整的使用示例，演示了如何：

1. 创建三层控制管理器
2. 配置设备映射
3. 添加控制节点
4. 处理输入流程

使用方法：

```java
// 创建控制架构示例
ControlArchitectureExample example = new ControlArchitectureExample();

// 处理输入
InputState inputState = example.processInput(rawInput, frameId);

// 或者直接使用管理器
ThreeTierControlManager manager = example.getControlManager();
// 添加更多的节点、动作或映射
manager.addControlNode(newNode);
manager.addControlAction(newAction);
manager.addDeviceMapping(newMapping);
```
