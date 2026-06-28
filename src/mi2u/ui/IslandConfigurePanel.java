package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.ui.island.*;
import mi2u.ui.island.capability.*;
import mi2u.ui.island.children.*;
import mindustry.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mi2u.MI2UVars.*;

public class IslandConfigurePanel extends Table {
    protected Island target;
    // 框架
    protected Table infoTable = new Table();
    protected Table configTable = new Table();

    // 设置页
    protected IslandBranchTable parentSelectorTable = new IslandBranchTable();
    protected IslandWidgetShop widgetShop = new IslandWidgetShop();
    public Cons<Island> callDelete;
    public Cons2<Island, Island> callSetChildParent;

    public IslandConfigurePanel(Cons2<Island, Island> callSetChildParent, Cons<Island> callDelete) {
        this.callSetChildParent = callSetChildParent;
        this.callDelete = callDelete;
        rebuild();
    }

    /**
     * @param target 设置页所显示的目标岛屿
     */
    public void setTarget(Island target){
        if(target != null && this.target != target){
            this.target = target;
            rebuildIslandInfo(infoTable, target);
            rebuildConfigContent(configTable, target);
        }
    }

    public Island getTarget(){
        return target;
    }

    // 以下是自构建相关方法
    public void rebuild(){
        infoTable.background(Styles.black3);
        infoTable.margin(10f);
        configTable.background(Styles.black3);
        configTable.margin(10f);

        clear();
        add(infoTable).padBottom(10f).growX();
        row();
        add(configTable).growX();
    }

    public void rebuildIslandInfo(Table table, Island island){
        table.clear();
        if(island == null){
            table.add("未选中");
            return;
        }

        table.add("名称: " + island.name).labelAlign(Align.left).growX();
        table.row();
        table.table(t -> {
            t.button("岛屿设置", textb, () -> rebuildConfigLayout(configTable, target)).with(funcSetTextb);

            t.button("内容设置", textb, () -> rebuildConfigContent(configTable, target)).with(funcSetTextb);

            if(island.getParentIsland() != null){
                t.button("更改父级", textb, () -> rebuildConfigParentChange(configTable, target)).with(funcSetTextb);
            }

            if(island.content instanceof ChildrenContent){
                t.button("添加子级组件", textb, () -> rebuildConfigAddChild(configTable, target)).with(funcSetTextb);
            }

            if(island.parent instanceof Island){
                t.add("拖拽移动").with(l -> {
                    l.addListener(new InputListener(){
                        float originalX, originalY;
                        final Vec2 vTouchDown = new Vec2();
                        final Vec2 vTouchDragging = new Vec2();

                        @Override
                        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                            localToStageCoordinates(vTouchDown.set(x, y));
                            originalX = island.layout.x;
                            originalY = island.layout.y;
                            l.setColor(Pal.accent);
                            Core.graphics.cursor(Graphics.Cursor.SystemCursor.hand);
                            return true;
                        }

                        @Override
                        public void touchDragged(InputEvent event, float x, float y, int pointer) {
                            super.touchDragged(event, x, y, pointer);
                            localToStageCoordinates(vTouchDragging.set(x, y));
                            island.layout.x = originalX + vTouchDragging.x - vTouchDown.x;
                            island.layout.y = originalY + vTouchDragging.y - vTouchDown.y;
                        }

                        @Override
                        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                            super.touchUp(event, x, y, pointer, button);
                            l.setColor(Color.white);
                            Core.graphics.restoreCursor();
                        }
                    });
                });
            }

            if(island.getParentIsland() != null){
                t.spacerX(() -> 20f);   // 删除键视觉远离其他功能键
                t.button("[scarlet]删除", textb, () -> {
                    Vars.ui.showConfirm("将移除该岛屿及其包含的所有内容，确定吗？", () -> callDelete.get(island));
                }).disabled(b -> island.getParentIsland() == null).with(funcSetTextb);
            }

        }).growX();
    }

    public void rebuildConfigAddChild(Table t, Island parent){
        t.clear();
        widgetShop.onCreate = child -> callSetChildParent.get(child, parent);
        t.add(widgetShop);
    }

    public void rebuildConfigLayout(Table t, Island island){
        t.clear();
        t.table(tt -> {
            island.layout.buildSettingsTable(tt);   // 委托可能不是个好主意？
        }).growX();

        t.row();

        t.table(tt -> {
            tt.button("拖拽能力", textbtoggle, () -> {
                var cap = island.getCapabilities().find(e -> e instanceof DragCapability);
                if(cap != null){
                    island.getCapabilities().remove(cap);
                }else{
                    var newCap = new DragCapability();
                    island.addCapability(newCap);
                }
                island.rebuild();
            }).checked(b -> island.getCapabilities().contains(cap -> cap instanceof DragCapability)).with(funcSetTextb);

            if(island.getCapabilities().contains(cap -> cap instanceof TabSelectCapability)){
                tt.button("子级选中能力", textbtoggle, null).checked(true).with(funcSetTextb);
            }
        }).growX().left();
    }

    public void rebuildConfigContent(Table t, Island island){
        t.clear();
        island.content.buildSettingsTable(t, island);
    }

    public void rebuildConfigParentChange(Table t, Island island){
        parentSelectorTable.build();
        parentSelectorTable.setTarget(island);
        parentSelectorTable.onConfirm = parent -> callSetChildParent.get(island, parent);
        parentSelectorTable.canConfirmValidation = parent -> parent != island && parent.content instanceof ChildrenContent cc && cc.canAddChild(island);
        t.clear();
        t.add(parentSelectorTable);
    }
}
