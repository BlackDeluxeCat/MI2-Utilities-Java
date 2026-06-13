# MI2U 重构方向

本文记录 Island UI 架构讨论后的当前重构方向。文档面向维护者和 AI 助手，不是严格 checklist，也不应过度约束实操。

旧版早期规划已归档到 `docs/archived/`。

## 当前优先级

当前主线是先把新的 `Island/Widget` UI 系统调到可用。

其他小重构应优先服务这条主线。全项目债务可以逐步降低，但不要让大范围重写抢在 UI 基础设施之前。

## 工作原则

- 优先做能让新 UI 更容易实现或调试的小改动。
- 新 UI 开发期间保留旧 UI 正常运行。
- 大多数相关 widget 迁移完成、替代方案被证明之前，不删除旧 `Mindow2` 窗口。
- 不把完整 Clean Architecture 重写作为第一步目标。
- 包名在早期实现中保持可调整。
- AI 助手负责杂活、样板代码和机械跟进；架构决策由维护者主导。

## 新 UI 优先

第一条实际工作线是新的 overlay 系统：

- 建立 `Island/Content/Layout/Capability` 骨架。
- 建立编辑模式 overlay manager。
- 建立简单文本 widget。
- 验证子 Element 到祖先 Island 的 capability event 冒泡。
- 验证面包屑树编辑。
- 验证保存/恢复。

这条工作线可以独立于旧 UI 迁移推进。

## 旧 UI 策略

旧 UI 在新 Island UI 开发期间继续并存。

不要为了架构洁癖迁移旧窗口。只有当新 widget 模型能自然承载某个旧窗口内容时，再做迁移。

迁移方向大概率是把旧窗口内容改造成支持多实例的 `WidgetContent`。部分旧 UI 可以重写，而不是机械包一层，因为新 widget 模型可能让实现更简单。

## Renderer 方向

渲染相关代码后续肯定会改，但第一目标不是拆完整个 renderer。

近期 renderer 小重构应服务新 UI：

- 暴露 widget 可消费的 renderer 相关数据。
- 在有价值时，把 UI 控制面与渲染执行逻辑分开。
- 避免让新 UI 直接伸进大型 renderer 内部。
- 在行为稳定的前提下抽小型 adapter surface。

大型 renderer 分解可以等 widget 系统跑起来后，再根据真实 UI 需求决定。

## 设置与序列化

新 UI 使用 Arc `Json` 和稳定 class tag 保存多态 overlay 状态。

现有设置基础设施可以在有帮助时复用，但第一版 overlay 持久化可以先简单直接。关键是 overlay 的序列化模型必须明确、可恢复。

项目里已有 `SettingHandler.registerJsonClass` 这类通过 Arc settings 注册 Json class tag 的做法。新 UI 可以基于这个思路建立自己的 `IslandJsonRegistry`。

## 全局状态

`MI2UVars` 等旧全局状态可继续服务旧模块。

新 UI 应尽量避免新增不必要的静态导入和隐藏依赖。如果新 UI 需要共享服务，优先考虑小型 context 对象或 manager 持有引用，而不是默认伸手拿全局变量。

这只是方向，不是硬禁令。实用性优先，避免为了形式引入过多仪式感。

## AI 助手角色

AI 应作为实现助手，而不是架构负责人。

适合交给 AI 的任务：

- 根据图生成重复类骨架。
- 补序列化样板。
- 注册 Json tag。
- 在模式证明后搬运简单 UI 内容到 widget。
- 设计变化后同步文档。
- 做窄范围机械清理。

不适合当前交给 AI 主导的任务：

- 全量重写旧 UI。
- 过早删除 `Mindow2`。
- 没有具体 UI 需求就大拆 `RendererExt`。
- 未经维护者确认就重画包边界。
- 把方向文档变成僵硬流程门禁。

## 延后处理的债务

项目仍然有更广泛的技术债，但应在新 UI 系统真实跑起来后再根据证据排期。

延后主题包括：

- `RendererExt` 深度拆分。
- `FullAI` 结构整理。
- 输入替换边界。
- 旧设置分散化。
- 旧窗口基础设施移除。

这些问题不必现在抽象求解。等新 widget 系统暴露真实需求后，再重新讨论会更稳。
