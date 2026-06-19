package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import mi2u.ui.island.*;
import mi2u.ui.island.children.*;
import mindustry.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mi2u.MI2UVars.funcSetTextb;
import static mi2u.MI2UVars.textb;

public class IslandConfigurePanel extends Table {
    protected Island target;
    // 框架
    protected Table infoTable = new Table();
    protected Table configTable = new Table();

    // 设置页
    protected IslandBranchTable parentSelectorTable = new IslandBranchTable();
    protected IslandWidgetShop widgetShop = new IslandWidgetShop();
    public Cons<Island> callDelete;
    public Cons2<Island, Island> callCreateChildInsideParent;

    public IslandConfigurePanel(Cons2<Island, Island> callCreateChildInsideParent, Cons<Island> callDelete) {
        this.callCreateChildInsideParent = callCreateChildInsideParent;
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
        configTable.background(Styles.black3);

        clear();
        add(infoTable).padBottom(10f);
        row();
        add(configTable);
    }

    public void rebuildIslandInfo(Table table, Island island){
        table.clear();
        if(island == null){
            table.add("未选中");
            return;
        }

        table.add("名称: " + island.name);
        table.row();
        table.table(t -> {
            t.button("岛屿设置", textb, () -> rebuildConfigLayout(configTable, target)).with(funcSetTextb);

            t.button("内容设置", textb, () -> rebuildConfigContent(configTable, target)).with(funcSetTextb);

            t.button("更改父级", textb, () -> rebuildConfigParentChange(configTable, target)).with(funcSetTextb);

            if(island.content instanceof ChildrenContent){
                t.button("添加子级组件", textb, () -> rebuildConfigAddChild(configTable, target)).with(funcSetTextb);
            }

            if(island.parent instanceof Island){
                t.add("拖拽移动").with(l -> {
                    l.addListener(new InputListener(){
                        float touchDownX, touchDownY;
                        float originalX, originalY;

                        @Override
                        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                            touchDownX = x;
                            touchDownY = y;
                            originalX = island.layout.x;
                            originalY = island.layout.y;
                            l.setColor(Pal.accent);
                            Core.graphics.cursor(Graphics.Cursor.SystemCursor.hand);
                            return true;
                        }

                        @Override
                        public void touchDragged(InputEvent event, float x, float y, int pointer) {
                            super.touchDragged(event, x, y, pointer);
                            island.layout.x = originalX + x - touchDownX;
                            island.layout.y = originalY + y - touchDownY;
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

            t.spacerX(() -> 20f);   // 删除键视觉远离其他功能键
            t.button("[scarlet]删除", textb, () -> {
                Vars.ui.showConfirm("将移除该岛屿及其包含的所有内容，确定吗？", () -> callDelete.get(island));
            }).disabled(b -> island.getParentIsland() == null).with(funcSetTextb);

        });
    }

    public void rebuildConfigAddChild(Table t, Island parent){
        t.clear();
        widgetShop.onCreate = child -> callCreateChildInsideParent.get(child, parent);
        t.add(widgetShop);
    }

    public void rebuildConfigLayout(Table t, Island island){
        t.clear();
        island.layout.buildSettingsTable(t);
    }

    public void rebuildConfigContent(Table t, Island island){
        t.clear();
        island.content.buildSettingsTable(t, island);
    }

    public void rebuildConfigParentChange(Table t, Island island){
        parentSelectorTable.build();
        parentSelectorTable.setTarget(island);
        parentSelectorTable.onConfirm = island::setParentIsland;
        parentSelectorTable.canConfirmValidation = isle -> isle.content instanceof ChildrenContent cc && cc.canAddChild(island);
        t.clear();
        t.add(parentSelectorTable);
    }
}
