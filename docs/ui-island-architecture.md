# MI2U Island UI 架构

本文记录 MI2U v2 当前的 `Island/Widget` UI 架构方向。文档面向维护者和 AI 助手，不作为公开贡献者指南。

主架构图仍以 drawio 为准：

- `docs/mi2u v2 reboot.drawio`

旧版早期规划已归档到 `docs/archived/`，只作为历史参考。

## 目标

新 UI 系统的目标是提供一套由小组件组合而成的 overlay 模型。它应当能在旧 `Mindow2` 窗口迁移前独立跑起来。

第一目标不是迁移全部旧 UI，而是先让新 overlay 系统能显示、能编辑、能拖拽、能保存恢复，并且足够好调试。

## 包结构方向

当前包结构先按下面方向使用，但实操中保留修改空间：

- `mi2u.ui.island`
- `mi2u.ui.island.children`
- `mi2u.ui.island.capability`
- `mi2u.ui.capability`
- `mi2u.ui.widget`
- `mi2u.ui.islandManager`

包名不是永久 API。比包名更重要的是模型边界和职责划分。

## 核心模型

### Island

`Island` 是唯一的运行时 UI 外壳。

它继承 `arc.scene.ui.layout.Table`，并持有：

- `name`
- `showCondition`
- `content: IslandContent?`
- `IslandLayout`
- capabilities

`Island` 是被选中、拖拽、缩放、最小化、序列化和编辑的基本单位。运行时不再拆出 `WidgetIsland` 和 `ChildrenIsland` 两套元素。

### IslandContent

`IslandContent` 是内容多态接口。

它负责把内容构建进 Island、暴露自己的设置 UI，并序列化自己的状态。

预期形态：

```text
IslandContent extends JsonSerializable
  + build(Island)
  + hasSetting(): boolean
  + buildSettingsTable(Table)
```

`build(Island)` 可以拿到 `Island` 引用，但内容应把它当作只读上下文。树结构修改、reparent、overlay 级状态变更都应走 overlay/editor 层，不应由任意 widget 代码直接修改。

### WidgetContent

`WidgetContent` 是具体用户小组件的接口，并应继承 `IslandContent`。

具体 widget 直接实现 `WidgetContent`。第一个测试小组件应当是一个简单的 `Table` 文本框组件，用来调试 UI 系统本身，避免第一轮就引入旧功能复杂度。

Widget 默认应支持多实例。除非功能明确需要共享状态，否则 widget 应保存自己的实例状态，而不是依赖全局单例。

### ChildrenContent

`ChildrenContent` 实现 `IslandContent`，持有子 `Island` 序列和一个 `ChildrenLayout`。

它是逻辑容器的数据模型。`Island` 外壳保持统一，只是内容策略不同。

### IslandLayout

`IslandLayout` 描述 Island 外壳的尺寸和布局约束，例如固定尺寸、延展标记和权重。

它不应拥有树结构。父子关系属于 overlay 树和 `ChildrenContent`。

### ChildrenLayout

`ChildrenLayout` 负责排列 `ChildrenContent` 内部的 children。

预期实现包括：

- `ColumnLayout`
- `RowLayout`
- `TabbedLayout`

Layout 实现可以持有可序列化状态。例如 `TabbedLayout` 可能需要保存当前 tab 下标。

## 能力模型

Capability 是挂在 Element 上的交互模块。适配 Island 的具体能力放在 `mi2u.ui.island.capability`。

预期层级：

```text
ElementCapability<T extends Element> extends EventListener, JsonSerializable
IslandCapability implements ElementCapability<Island>
DragCapability extends IslandCapability
SnapCapability extends IslandCapability
ResizeCapability extends IslandCapability
MinimizeCapability extends IslandCapability
ChildrenSelectCapability extends IslandCapability
```

Capability event 复用 Arc `SceneEvent` 冒泡。子 Element，例如拖拽把手，发出 capability event；事件向上冒泡，直到某个祖先 Island 的 capability 接受它。

第一轮能力实验要证明下面这条链路：

```text
drag handle Element
  -> fire DragCapabilityEvent
  -> ancestor Island DragCapability accepts
  -> handle uses the responder to move that Island
```

事件和监听器允许专门化。大多数 capability event 预期与 listener 类型一对一，后续允许小范围二对一或三对一。

Capability event 必须定义唯一响应者。向上冒泡时不能让多个祖先同时模糊处理同一条命令。

## Overlay Manager 与编辑模式

`IslandOverlayManager` 是 overlay 地图的总管，职责重是设计预期。

它拥有或协调：

- Arc `WidgetGroup` root
- root 级 Islands
- 保存/加载
- 编辑模式
- `WidgetShop`
- `IslandConfigurePanel`

真实 root 是 Arc `WidgetGroup`，不是 mod 自定义的 Children Island。若实操中需要统一 root 和 children 容器的操作接口，可在实现阶段补一个适配层，不必现在硬定。

编辑器 UI 只在编辑模式可见，包括 widget shop、配置面板、面包屑树编辑和其他树操作控件。

## 面包屑树编辑

当前设计移除独立的 `LayoutTreeConfigureTable`。它的有用功能合并到配置面板的面包屑区域。

面包屑显示当前单条路径。路径上：

- Widget island 显示为 label。
- Children island 显示为 label + dropdown button。
- Widget label 不接受 drop。
- Children label/dropdown target 可以接受 drop。
- Children dropdown 罗列所有 child label，并提供上下调序按钮。

把 Island 拖到合法 Children Island 面包屑目标上，会把它追加到该 parent 的 children 末尾。顺序再由 parent 的 dropdown 调整。

树编辑必须有校验步骤。至少要拒绝：拖到自己、拖到自己的后代、拖到 widget content、无实际变化的移动。

## 拖拽语义

编辑模式下，Island 拖拽同时可能承担三种含义：

1. 在 overlay 上移动 Island。
2. 如果具备 snap capability，则应用吸附逻辑。
3. 如果 drop 到面包屑树目标，则修改树结构。

实现时应把拖拽视为临时交互，在释放鼠标时一次性提交最终结果。这样可以避免一次手势中位置、吸附、树结构各自半提交导致状态互相污染。

## 序列化

使用 Arc `Json` 序列化。

Arc `Json` 在实际运行类型与已知类型不同、或已知类型为空时，可以写入名为 `class` 的类型字段。它也支持通过 `addClassTag` 注册 class tag。

新 UI 系统应集中注册多态类型的 class tag，例如：

- contents
- layouts
- capabilities
- concrete widgets

持久化布局数据应优先使用稳定短 tag，而不是完全限定类名。这个系统还在塑形阶段，包名和类名可能变化。

如果某个值只通过方法暴露，Arc 反射不会自动序列化它。这类状态必须由所属 `JsonSerializable` 手动写入。

## 第一轮调试切片

第一轮实现建议按下面顺序验证：

1. 编辑模式外观。
2. 拖放创建新组件。
3. 一个基于 `Table` 文本框的纯文本 widget。
4. 面包屑编辑器行为。
5. 拖拽把手 widget 与 capability event 实验。
6. 保存与恢复。

这是调试路径，不是永久产品路线图。

## 与旧 UI 的关系

新系统可用前，旧 UI 继续保留。

不要因为新 overlay 出现就立即删除 `Mindow2` 或旧窗口。旧 UI 迁移应在新 `Widget/Island` 系统足够稳定、多个 widget 已验证后再逐步进行。
