# 废弃类处理指南

## 1. 废弃类清单及状态

### 1.1 立即可废弃类 (标记为@Deprecated)

| 类名 | 路径 | 理由 | 替代方案 | 优先级 |
|------|------|------|----------|--------|
| LayoutEngine.java | input/ | 功能完全由EnhancedLayoutEngine替代 | EnhancedLayoutEngine | 高 |
| UILayerHandler.java | input/ | 功能由ControlNode替代 | ControlNode | 高 |
| OperationLayerHandler.java | input/ | 功能由ControlAction替代 | ControlAction | 高 |
| MappingLayerHandler.java | input/ | 功能由DeviceMapping替代 | DeviceMapping | 高 |

### 1.2 条件废弃类 (部分功能重复)

| 类名 | 路径 | 理由 | 替代方案 | 优先级 |
|------|------|------|----------|--------|
| Region.java | input/ | 功能由ControlNode替代 | ControlNode | 中 |
| RegionResolver.java | input/ | 功能由ControlNode替代 | ControlNode | 中 |
| LayoutRenderer.java | input/ | 功能与新架构重复 | NewLayoutEngine | 中 |
| InputSender.java | input/ | 可能被新架构替代 | EnhancedLayoutEngine | 低 |

### 1.3 保留观察类 (暂时保留)

| 类名 | 路径 | 理由 | 状态 | 说明 |
|------|------|------|------|------|
| EventNormalizer.java | input/ | 输入标准化功能 | 保留 | 仍需此功能 |
| SafetyController.java | input/ | 安全控制功能 | 保留 | 仍需此功能 |
| ProfileManager.java | input/ | 配置文件管理 | 保留 | 仍需此功能 |
| OutputController.java | input/ | 输出控制功能 | 保留 | 仍需此功能 |

## 2. 废弃类处理标准

### 2.1 废弃条件
一个类符合以下任一条件时，可以标记为废弃：
- 功能完全被新类替代
- 存在更优的设计方案
- 与新架构冲突
- 代码重复且维护成本高

### 2.2 不应废弃的类
即使功能部分重复，也不应废弃的类：
- 核心基础设施类
- 被外部代码引用的公共API
- 测试和调试必需的类
- 仍有业务价值的类

## 3. 废弃类处理流程

### 3.1 标记废弃
```java
/**
 * @deprecated 请使用 {@link com.linecat.wmmtcontroller.core.layout.LayoutEngine} 代替
 * 该类将在 v2.0 版本中移除
 */
@Deprecated
public class OldLayoutEngine {
    // 旧实现
}
```

### 3.2 更新文档
在类的JavaDoc中明确说明：
- 为什么废弃
- 何时会移除
- 如何迁移到替代方案
- 迁移示例

### 3.3 更新依赖
- 修改所有使用废弃类的地方
- 更新单元测试
- 检查是否有反射使用
- 确认没有隐式依赖

## 4. 安全删除检查清单

删除废弃类前必须完成以下检查：

### 4.1 静态检查
- [ ] 确认没有类直接引用
- [ ] 检查是否有反射调用
- [ ] 检查XML布局文件中是否有引用
- [ ] 检查配置文件中是否有引用
- [ ] 检查资源文件中是否有引用

### 4.2 运行时检查
- [ ] 所有单元测试通过
- [ ] 所有集成测试通过
- [ ] 应用可以正常启动
- [ ] 核心功能正常工作
- [ ] 没有运行时异常

### 4.3 代码搜索
```bash
# 搜索可能的引用
grep -r "ClassName" src/
grep -r "import.*ClassName" src/
grep -r "extends.*ClassName" src/
grep -r "implements.*ClassName" src/
```

## 5. 废弃类处理示例

### 5.1 LayoutEngine.java 的处理

**原类:**
```java
package com.linecat.wmmtcontroller.input;

/**
 * 布局引擎
 * 负责执行三层布局处理：UI 层 → Operation 层 → Mapping 层
 */
@Deprecated
public class LayoutEngine {
    // 旧实现
}
```

**更新为:**
```java
package com.linecat.wmmtcontroller.input;

/**
 * @deprecated 旧版布局引擎，请使用 
 * {@link com.linecat.wmmtcontroller.control.EnhancedLayoutEngine} 代替
 * 该类将在 v2.0 版本中移除
 * 
 * 迁移示例:
 * <pre>
 * // 旧代码
 * LayoutEngine oldEngine = new LayoutEngine(outputController);
 * 
 * // 新代码
 * DeviceMapping deviceMapping = new DeviceMapping(...);
 * EnhancedLayoutEngine newEngine = new EnhancedLayoutEngine(deviceMapping);
 * </pre>
 */
@Deprecated
public class LayoutEngine {
    // 保持原有实现，但添加废弃警告
}
```

## 6. 迁移验证

### 6.1 自动化验证脚本
```bash
#!/bin/bash
# 验证废弃类是否还有引用

echo "检查废弃类引用..."
for deprecated_class in "LayoutEngine" "UILayerHandler" "OperationLayerHandler" "MappingLayerHandler"; do
    echo "检查 $deprecated_class..."
    refs=$(grep -r "$deprecated_class" src/ --exclude="*.md" | grep -v "@deprecated" | wc -l)
    if [ $refs -gt 0 ]; then
        echo "警告: $deprecated_class 仍有 $refs 个引用"
        grep -r "$deprecated_class" src/ --exclude="*.md" | grep -v "@deprecated"
    else
        echo "✓ $deprecated_class 没有引用"
    fi
done
```

### 6.2 人工验证
- [ ] 确认替代方案功能完整
- [ ] 确认性能没有下降
- [ ] 确认用户体验一致
- [ ] 确认安全性不受影响

## 7. 回滚计划

如果废弃类的移除导致问题：

1. **立即回滚**: 从版本控制恢复被删除的类
2. **标记问题**: 在issue跟踪系统记录问题
3. **分析原因**: 找出遗漏的依赖或使用场景
4. **修复后重试**: 修正问题后再次尝试废弃

## 8. 最佳实践

### 8.1 废弃策略
- 采用渐进式废弃，不一次性废弃太多类
- 提供充分的迁移文档和示例
- 维护向后兼容一段时间
- 在发布说明中明确废弃计划

### 8.2 沟通机制
- 在团队内部沟通废弃计划
- 在代码审查中关注废弃类使用
- 定期检查废弃类状态
- 收集团队反馈并调整计划

### 8.3 监控机制
- 使用静态分析工具监控废弃类使用
- 在CI/CD流程中加入废弃类检查
- 定期生成废弃类使用报告
- 设定废弃类移除的时间表