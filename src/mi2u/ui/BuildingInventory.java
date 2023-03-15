package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mi2u.*;
import mi2u.graphics.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static arc.Core.graphics;
import static mindustry.Vars.*;

public class BuildingInventory extends Element{
    public static IntSet ids = new IntSet(), used = new IntSet();
    public static WidgetGroup group = new WidgetGroup();
    Building build;

    MI2Utils.IntervalMillis itemused = new MI2Utils.IntervalMillis(content.items().size);

    public static void init(){
        Events.on(EventType.WorldLoadEvent.class, e -> {
            used.clear();
            group.clear();
        });
        group.setFillParent(true);
        group.touchable = Touchable.disabled;
        Vars.ui.hudGroup.addChildBefore(Core.scene.find("overlaymarker"), group);
    }

    public static void get(Building b){
        if(state.isGame() && b.block.itemCapacity >= 10 && b.items != null){
            if(used.add(b.id)) new BuildingInventory(b);
        }
    }

    public BuildingInventory(Building b){
        this.build = b;
        group.addChild(this);
    }

    @Override
    public void act(float delta){
        super.act(delta);
        float scl = graphics.getWidth() / Core.camera.width;
        setSize(build.block.size * tilesize * scl);
        Core.camera.project(MI2UTmp.v3.set(build.x, build.y));
        setPosition(MI2UTmp.v3.x - width / 2f, MI2UTmp.v3.y - height / 2f);
        if(state.isMenu() || build == null || !build.isValid() || !ids.contains(build.id)){
            remove();
            used.remove(build.id);
        }
    }

    @Override
    public void draw(){
        if(!RendererExt.enDistributionReveal) return;
        super.draw();

        int i = 0;
        int size = build.block.size;
        float count = content.items().count(ii -> build.items.has(ii));
        float rows = Mathf.ceil(count / (float)size);
        float rowdy = Math.min(size / rows + 0.1f, 1);
        float scl = graphics.getWidth() / Core.camera.width;
        float iconsize = tilesize * scl;

        Draw.alpha(1f);
        Draw.color(Color.white);

        Font font = Fonts.outline;
        font.setColor(1f, 1f, 1f, 0.5f);
        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        font.getData().setScale(scl / 7f, scl / 7f);
        var cache = font.getCache();
        cache.setColor(Color.white);

        for(var item : content.items()){
            int amt = build.items.get(item);
            if(itemused.check(item.id, 5000) && amt <= 0) continue;
            itemused.get(item.id, 1);

            float dx = Mathf.mod(i, size) * iconsize;
            float dy = rowdy * Mathf.floor(i / (float)size) * iconsize;
            Draw.rect(item.uiIcon, x + dx + iconsize / 2f, y + dy + iconsize / 2f, iconsize * 0.75f, iconsize * 0.75f);

            cache.clear();
            cache.addText(UI.formatAmount(amt), x + dx, y + dy + iconsize / 2f);
            cache.draw(0.8f);

            i++;
        }

        font.getData().setScale(oldScaleX, oldScaleY);
    }
}