# 新旧架构对比分析

## 1. 旧架构 (Legacy Architecture)

### 1.1 结构特点

- **集中式设计**：所有控制逻辑集中在少数几个类中
- **职责混淆**：UI层、业务逻辑层、映射层职责不清晰
- **紧耦合**：组件之间高度耦合，难以独立修改

### 1.2 主要组件

- `LayoutEngine`：负责整个布局处理流程
- `UILayerHandler`：UI层处理
- `OperationLayerHandler`：操作层处理
- `MappingLayerHandler`：映射层处理
- `Region`：区域定义
- `RegionResolver`：区域解析

### 1.3 存在问题

- 包结构混乱，类职责不清
- 难以扩展和维护
- 测试困难
- 代码重复

## 2. 新架构 (New Architecture)

### 2.1 结构特点

- **分层架构**：清晰的三层架构设计
- **职责分离**：每层职责明确
- **松耦合**：组件间依赖关系清晰

### 2.2 主要组件

#### 2.2.1 UI层 (com.linecat.wmmtcontroller.control.ui)

- `ControlNode`：控制节点基类
- `ButtonControlNode`：按钮控制节点
- `AxisControlNode`：轴控制节点
- `GyroControlNode`：陀螺仪控制节点
- `UINodeManager`：UI节点管理器

#### 2.2.2 Operation层 (com.linecat.wmmtcontroller.control.operation)

- `ControlAction`：控制动作类
- `OperationNodeManager`：操作节点管理器
- `ControlArchitectureExample`：架构使用示例

#### 2.2.3 Mapping层 (com.linecat.wmmtcontroller.control.mapping)

- `DeviceMapping`：设备映射类
- `MappingNodeManager`：映射节点管理器
- `ControlLayerCoordinator`：三层协调器
- `ThreeTierControlManager`：三层总控管理器

#### 2.2.4 布局引擎层 (com.linecat.wmmtcontroller.core.layout)

- `NewLayoutEngine`：新版布局引擎
- `EnhancedLayoutEngine`：增强版布局引擎

### 2.3 优势

- 包结构清晰，职责分明
- 易于扩展和维护
- 便于单元测试
- 支持模块化开发

## 3. 迁移策略

### 3.1 渐进式迁移

1. **并行运行**：新旧架构并行，通过适配器切换
2. **功能验证**：逐步验证新架构功能
3. **平滑切换**：通过开关控制使用新旧架构
4. **逐步替换**：最终完全替换旧架构

### 3.2 兼容性保障

- 保留旧架构组件，标记为废弃
- 通过适配器确保无缝切换
- 提供回滚机制

## 4. 性能对比

### 4.1 内存占用

- **旧架构**：组件集中，内存占用较高
- **新架构**：模块化设计，内存占用更优

### 4.2 扩展性

- **旧架构**：扩展困难，容易影响其他功能
- **新架构**：易于扩展，影响范围可控

### 4.3 维护性

- **旧架构**：修改困难，风险较高
- **新架构**：模块化维护，风险可控

## 5. 使用示例

### 5.1 旧架构使用方式

```java
// 旧架构
LayoutEngine layoutEngine = new LayoutEngine(outputController);
layoutEngine.setContext(context);
layoutEngine.init();

InputState inputState = layoutEngine.executeLayout(rawInput, frameId);
```

### 5.2 新架构使用方式

```java
// 新架构
DeviceMapping deviceMapping = new DeviceMapping("default_mapping", "默认映射", DeviceMapping.MappingType.KEYBOARD);
EnhancedLayoutEngine enhancedLayoutEngine = new EnhancedLayoutEngine(deviceMapping);

InputState inputState = enhancedLayoutEngine.executeLayout(rawInput, frameId);
```

### 5.3 迁移期间使用方式

```java
// 迁移期间（通过适配器）
LayoutEngine legacyEngine = new LayoutEngine(outputController);
EnhancedLayoutEngine newEngine = new EnhancedLayoutEngine(deviceMapping);
LayoutEngineAdapter adapter = new LayoutEngineAdapter(legacyEngine, newEngine);

// 根据需要切换
adapter.setUseNewEngine(true); // 使用新架构
InputState inputState = adapter.executeLayout(rawInput, frameId);
```

## 6. 总结

新架构相比旧架构具有明显优势：

- 结构更清晰
- 职责更明确
- 扩展性更好
- 维护更容易

通过渐进式迁移策略，可以确保系统稳定性的同时享受新架构带来的优势。
