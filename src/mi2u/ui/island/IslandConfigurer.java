package mi2u.ui.island;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.*;
import mi2u.io.*;
import mi2u.ui.*;
import mi2u.ui.elements.*;
import mi2u.ui.island.children.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mi2u.MI2UVars.*;

public class IslandConfigurer extends Table{
    public IslandOverlayAccess access;

    public Island selectedIsland;

    /** 左侧功能按钮 */
    protected Table buttonsTable;
    /** 中部主设置面板 */
    protected IslandConfigurePanel islandConfigurePanel;
    /** 右侧分支导航面板 */
    protected IslandBranchTable branchTable;

    public IslandConfigurer(){
        buttonsTable = new Table();
        buttonsTable.background(Styles.black3);
        rebuildButtonsTable(buttonsTable);

        branchTable = new IslandBranchTable();
        branchTable.background(Styles.black3);
        branchTable.build();
        branchTable.canConfirmValidation = isle -> selectedIsland != isle;
        branchTable.onConfirm = isle -> setSelectedIsland(isle);

        islandConfigurePanel = new IslandConfigurePanel(
            (child, parent) -> {
                addChildIsland(child, parent);
            },
            removed -> {
                removeChildIsland(removed);
            }
        );

        add(buttonsTable).padRight(10f).bottom();
        add(islandConfigurePanel).bottom();
        add(branchTable).bottom().padLeft(10f).minWidth(100f);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        keepInStage();
        pack();
    }

    public void selectRoot(){
        setSelectedIsland(getAccess().getRoot());
    }

    public void setAccess(IslandOverlayAccess access){
        this.access = access;
        selectRoot();
    }

    public IslandOverlayAccess getAccess(){
        return access;
    }

    public void setSelectedIsland(@Nullable Island island){
        selectedIsland = island;
        islandConfigurePanel.setTarget(selectedIsland);
        branchTable.setTarget(selectedIsland);
    }

    public void addChildIsland(Island island, Island parent){
        IslandUtils.addChild(parent, island);
        branchTable.setTarget(branchTable.getTarget()); // 只刷新左侧分支导航器
    }

    public void removeChildIsland(Island removed){
        if(removed.getParentIsland() != null && removed.getParentIsland().content instanceof ChildrenContent cc){
            cc.removeChild(removed);
        }
        if(selectedIsland == removed) setSelectedIsland(getAccess().getRoot());
    }

    public void rebuildButtonsTable(Table table){
        table.table(t -> {
            t.add("布局编辑器").growX();
            t.add("" + Iconc.move).with(l -> {
                l.addListener(new InputListener(){
                    float originalX, originalY;
                    final Vec2 vTouchDown = new Vec2();
                    final Vec2 vTouchDragging = new Vec2();

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                        localToStageCoordinates(vTouchDown.set(x, y));
                        originalX = IslandConfigurer.this.x;
                        originalY = IslandConfigurer.this.y;
                        l.setColor(Pal.accent);
                        Core.graphics.cursor(Graphics.Cursor.SystemCursor.hand);
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer) {
                        super.touchDragged(event, x, y, pointer);
                        localToStageCoordinates(vTouchDragging.set(x, y));
                        IslandConfigurer.this.x = originalX + vTouchDragging.x - vTouchDown.x;
                        IslandConfigurer.this.y = originalY + vTouchDragging.y - vTouchDown.y;
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                        super.touchUp(event, x, y, pointer, button);
                        l.setColor(Color.white);
                        Core.graphics.restoreCursor();
                    }
                });
            });
        }).expandX().pad(4f);
        table.row();
        table.stack(
            new Element(){
                {
                    setFillParent(true);
                }
                @Override
                public void draw() {
                    super.draw();
                    float factor = getAccess().getAutoSaveCooldown();
                    Draw.color(Pal.items, 0.3f + 0.5f * Interp.exp10In.apply(Interp.exp5In.apply(factor)));
                    Fill.rect(MI2UTmp.r1.set(this.x, this.y, Interp.exp5In.apply(factor) * this.getWidth(), this.getHeight()));
                }
            },
            new Table(tt -> {
                tt.add(new CombinationIcon(t -> t.add("" + Iconc.save).pad(4f)).bottomRight(t -> t.add("" + Iconc.refresh).fontScale(0.5f)));
            })
        ).pad(4f).grow();
        table.row();
        table.button(Iconc.paste + "导入布局树", textb, () -> {
            getAccess().loadFromJson(Core.app.getClipboardText());
        }).with(funcSetTextb);
        table.row();
        table.button(Iconc.copy + "导出布局树", textb, () -> {
            Core.app.setClipboardText(JsonUtils.json.prettyPrint(getAccess().getRoot()));
        }).with(funcSetTextb);
        table.row();
        table.button(Iconc.copy + "导出选中的分支", textb, () -> {
            Core.app.setClipboardText(IslandUtils.island2Json(islandConfigurePanel.getTarget()));
        }).with(funcSetTextb);
        table.row();
        table.button("重新生成IDs", textb, () -> {
            IslandUtils.runRecursive(getAccess().getRoot(), island -> {
                island.id = Island.newId();
            });
        }).with(funcSetTextb);
    }
}
