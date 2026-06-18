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

import static mi2u.MI2UVars.funcSetTextb;
import static mi2u.MI2UVars.textb;

public class IslandConfigurePanel extends Table {
    public Island target;
    protected Table infoTable = new Table();
    protected Table configTable = new Table();
    protected IslandBranchTable parentSelectorTable = new IslandBranchTable();
    protected IslandWidgetShop widgetShop = new IslandWidgetShop();
    public Cons<Island> callCreateChild, callDelete;

    public IslandConfigurePanel(Cons<Island> callCreateChild, Cons<Island> callDelete) {
        this.callCreateChild = callCreateChild;
        this.callDelete = callDelete;
        widgetShop.onCreate = callCreateChild;
    }

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

    public void rebuild(){
        clear();
        add(infoTable);
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
                t.button("添加子级组件", textb, () -> {

                    rebuildWidgetShop(isle -> {
                        if(island == root){
                            isle.layout.x = root.getWidth() / 2f;
                            isle.layout.y = root.getHeight() / 2f;
                        }
                        addIsland(isle, island);
                    });
                    cfgSubTable.clear();
                    cfgSubTable.add(widgetShop);
                }).with(funcSetTextb);
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
        t.clear();
        t.add(parentSelectorTable);
    }
}
