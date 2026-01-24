# 安卓端项目结构迁移计划

## 1. 迁移目标

将当前混乱的包结构和新旧架构混合的状态，迁移到清晰、模块化的三层架构：

- **UI层**：ControlNode及相关管理器
- **Operation层**：ControlAction及相关管理器  
- **Mapping层**：DeviceMapping及相关管理器

## 2. 迁移前准备

### 2.1 备份当前状态
```bash
# 确保所有更改已提交
git add .
git commit -m "Backup before structure migration"
```

### 2.2 验证当前系统
```bash
# 确保当前系统可以正常编译和运行
./gradlew build
```

## 3. 分阶段迁移计划

### 阶段1：包结构调整 (第1周)

#### 3.1 创建新的包结构
```
src/main/java/com/linecat/wmmtcontroller/
├── control/
│   ├── ui/                 # UI层相关类
│   │   ├── ControlNode.java
│   │   ├── UINodeManager.java
│   │   ├── ButtonControlNode.java
│   │   ├── AxisControlNode.java
│   │   └── GyroControlNode.java
│   ├── operation/          # Operation层相关类
│   │   ├── ControlAction.java
│   │   ├── OperationNodeManager.java
│   │   └── ControlArchitectureExample.java
│   └── mapping/            # Mapping层相关类
│       ├── DeviceMapping.java
│       ├── MappingNodeManager.java
│       └── ControlLayerCoordinator.java
├── core/
│   ├── input/              # 输入处理核心
│   ├── layout/             # 布局处理
│   │   ├── LayoutEngine.java (旧版，标记为@Deprecated)
│   │   ├── NewLayoutEngine.java
│   │   ├── EnhancedLayoutEngine.java
│   │   ├── LayoutSnapshot.java
│   │   ├── LayoutLoader.java
│   │   └── LayoutManager.java
│   └── script/             # 脚本引擎
│       ├── InputScriptEngine.java
│       ├── JsInputScriptEngine.java
│       ├── ScriptProfile.java
│       └── ProfileManager.java
├── model/                  # 数据模型
├── service/                # 服务层
├── network/                # 网络通信
│   └── transport/
├── database/               # 数据库
├── floatwindow/            # 浮窗管理
├── monitor/                # 系统监控
├── migration/              # 迁移工具
└── util/                   # 工具类
```

#### 3.2 移动类文件
```bash
# 移动Control包中的UI层类到control.ui
mv src/main/java/com/linecat/wmmtcontroller/control/{ControlNode.java,ButtonControlNode.java,AxisControlNode.java,GyroControlNode.java,UINodeManager.java} \
   src/main/java/com/linecat/wmmtcontroller/control/ui/

# 移动Control包中的Operation层类到control.operation
mv src/main/java/com/linecat/wmmtcontroller/control/{ControlAction.java,OperationNodeManager.java,ControlArchitectureExample.java} \
   src/main/java/com/linecat/wmmtcontroller/control/operation/

# 移动Control包中的Mapping层类到control.mapping
mv src/main/java/com/linecat/wmmtcontroller/control/{DeviceMapping.java,MappingNodeManager.java,ControlLayerCoordinator.java} \
   src/main/java/com/linecat/wmmtcontroller/control/mapping/

# 移动布局相关类到core.layout
mkdir -p src/main/java/com/linecat/wmmtcontroller/core/layout
mv src/main/java/com/linecat/wmmtcontroller/input/{LayoutEngine.java,NewLayoutEngine.java,EnhancedLayoutEngine.java,LayoutLoader.java,LayoutManager.java,LayoutRenderer.java} \
   src/main/java/com/linecat/wmmtcontroller/core/layout/

# 移动脚本相关类到core.script
mkdir -p src/main/java/com/linecat/wmmtcontroller/core/script
mv src/main/java/com/linecat/wmmtcontroller/input/{InputScriptEngine.java,JsInputScriptEngine.java,ScriptProfile.java,ProfileManager.java,ScriptContext.java,ScriptTestHarness.java} \
   src/main/java/com/linecat/wmmtcontroller/core/script/
```

#### 3.3 更新import语句
需要批量更新所有Java文件中的import语句：

```java
// 从
import com.linecat.wmmtcontroller.control.ControlNode;
import com.linecat.wmmtcontroller.control.ControlAction;
import com.linecat.wmmtcontroller.control.DeviceMapping;

// 到
import com.linecat.wmmtcontroller.control.ui.ControlNode;
import com.linecat.wmmtcontroller.control.operation.ControlAction;
import com.linecat.wmmtcontroller.control.mapping.DeviceMapping;
```

### 阶段2：标记旧类为废弃 (第2周)

#### 2.1 为旧架构类添加@Deprecated注解

**LayoutEngine.java**:
```java
@Deprecated
@Stable("Use EnhancedLayoutEngine instead")
public class LayoutEngine {
    // ... 保持原有实现
}
```

**各处理器类**:
```java
@Deprecated
public class UILayerHandler {
    // ... 保持原有实现
}

@Deprecated  
public class OperationLayerHandler {
    // ... 保持原有实现
}

@Deprecated
public class MappingLayerHandler {
    // ... 保持原有实现
}
```

### 阶段3：适配器开发 (第3周)

#### 3.1 创建新旧架构适配器

```java
// src/main/java/com/linecat/wmmtcontroller/util/LayoutEngineAdapter.java
package com.linecat.wmmtcontroller.util;

import com.linecat.wmmtcontroller.core.layout.LayoutEngine;
import com.linecat.wmmtcontroller.control.mapping.EnhancedLayoutEngine;
import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.model.InputState;

/**
 * 布局引擎适配器
 * 用于在新旧架构之间进行适配
 */
public class LayoutEngineAdapter {
    
    private final LayoutEngine legacyEngine;  // 旧版引擎
    private final EnhancedLayoutEngine newEngine;  // 新版引擎
    private boolean useNewEngine = false;  // 控制使用哪个引擎
    
    public LayoutEngineAdapter(LayoutEngine legacyEngine, EnhancedLayoutEngine newEngine) {
        this.legacyEngine = legacyEngine;
        this.newEngine = newEngine;
    }
    
    public InputState executeLayout(RawInput rawInput, long frameId) {
        if (useNewEngine && newEngine != null) {
            return newEngine.executeLayout(rawInput, frameId);
        } else {
            return legacyEngine.executeLayout(rawInput, frameId);
        }
    }
    
    public void setUseNewEngine(boolean useNew) {
        this.useNewEngine = useNew;
    }
    
    public boolean isNewEngineEnabled() {
        return useNewEngine;
    }
}
```

### 阶段4：逐步迁移 (第4-6周)

#### 4.1 修改InputRuntimeService使用新架构

```java
public class InputRuntimeService extends Service {
    // ...
    
    // 移除旧的layoutEngine字段
    // private LayoutEngine layoutEngine;
    
    // 添加新的布局引擎
    private EnhancedLayoutEngine enhancedLayoutEngine;
    private LayoutEngineAdapter layoutEngineAdapter;
    
    private void initializeComponents() {
        // ...
        
        // 创建输出控制器
        outputController = new OutputController();
        
        // 创建新布局引擎
        DeviceMapping defaultMapping = new DeviceMapping("default", "Default Mapping", DeviceMapping.MappingType.KEYBOARD);
        enhancedLayoutEngine = new EnhancedLayoutEngine(defaultMapping);
        
        // 创建适配器，初始使用旧引擎保证兼容性
        LayoutEngine legacyEngine = new LayoutEngine(outputController);
        layoutEngineAdapter = new LayoutEngineAdapter(legacyEngine, enhancedLayoutEngine);
        
        // ...
    }
    
    // 逐步切换到新引擎的方法
    public void switchToNewEngine() {
        layoutEngineAdapter.setUseNewEngine(true);
        Log.d(TAG, "Switched to new layout engine");
    }
}
```

### 阶段5：测试与验证 (第7周)

#### 5.1 全面测试
- 单元测试：确保所有类都能正常编译和运行
- 集成测试：验证新旧架构切换的平滑性
- 功能测试：验证所有输入功能正常工作
- 性能测试：确保新架构性能不低于旧架构

#### 5.2 清理废弃类
在确认新架构稳定运行后，删除废弃的类：
- LayoutEngine.java
- UILayerHandler.java
- OperationLayerHandler.java
- MappingLayerHandler.java
- LayoutRenderer.java
- Region.java
- RegionResolver.java

## 4. 回滚计划

如果迁移过程中出现问题，可以：

1. 恢复到迁移前的备份点
2. 临时禁用新架构，继续使用旧架构
3. 逐步修复问题而不是完全回滚

## 5. 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 编译错误 | 高 | 逐步迁移，每次迁移后验证 |
| 运行时错误 | 高 | 完整测试，保留旧架构作为后备 |
| 性能下降 | 中 | 性能基准测试 |
| 功能丢失 | 高 | 功能回归测试 |

## 6. 成功指标

- [ ] 所有类正常编译
- [ ] 系统正常运行
- [ ] 新架构功能完整
- [ ] 旧架构可选择性启用
- [ ] 包结构清晰合理
- [ ] 代码质量提升