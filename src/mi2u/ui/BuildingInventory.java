package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mi2u.*;
import mi2u.graphics.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.blocks.storage.*;

import static arc.Core.graphics;
import static mindustry.Vars.*;

public class BuildingInventory extends Element{
    public static IntSet ids = new IntSet(), used = new IntSet();
    public static WidgetGroup group = new WidgetGroup();
    public static Pool<BuildingInventory> pool = new Pool<>(){
        @Override
        protected BuildingInventory newObject(){
            return new BuildingInventory();
        }
    };
    public static Font font = Fonts.outline;
    Building build;
    float scl, iconsize;

    MI2Utils.IntervalMillis itemused = new MI2Utils.IntervalMillis(content.items().size), liquidused = new MI2Utils.IntervalMillis(content.liquids().size);

    public static void init(){
        Events.on(EventType.WorldLoadEvent.class, e -> {
            used.clear();
            group.clear();
        });
        group.setFillParent(true);
        group.touchable = Touchable.disabled;
        ui.hudGroup.addChildAt(ui.hudGroup.getChildren().indexOf(Core.scene.find("overlaymarker")) - 1, group);
    }

    public static void get(Building b){
        if(state.isGame() && ((b.block.itemCapacity >= 10 && b.items != null && !(b instanceof StorageBlock.StorageBuild sb && sb.linkedCore != null)) || b.liquids != null)){
            if(used.add(b.id)) pool.obtain().setBuilding(b);
        }
    }

    public BuildingInventory(){}

    public void setBuilding(Building b){
        this.build = b;
        group.addChild(this);
    }

    @Override
    public void act(float delta){
        super.act(delta);
        Core.camera.project(MI2UTmp.v3.set(-1f, -1f).scl(build.block.size * tilesize / 2f).add(build.x, build.y));
        setPosition(MI2UTmp.v3.x, MI2UTmp.v3.y);
        if(!vaild()) remove();
    }

    public boolean vaild(){
        return RendererExt.enDistributionReveal && RendererExt.drevealInventory && state.isGame() && build != null && build.isValid() && ids.contains(build.id);
    }

    @Override
    public boolean remove(){
        if(build != null) used.remove(build.id);
        build = null;
        return super.remove();
    }

    @Override
    public void draw(){
        if(!vaild() || (state.rules.fog && build.inFogTo(player.team()))) return;
        super.draw();
        Draw.alpha(1f);
        Draw.color(Color.white);
        Styles.black3.draw(x, y, width, height);

        Draw.alpha(1f);
        Draw.color(Color.white);
        int i = 0;
        int size = build.block.size;
        float count = content.items().count(ii -> build.items != null && (build.items.has(ii) || !itemused.check(ii.id, 1000))) + content.liquids().count(ii -> build.liquids != null && (build.liquids.get(ii) > 0f || !liquidused.check(ii.id, 1000)));
        float rows = Mathf.ceil(count / (float)size);
        float rowdy = Math.min(size / rows + 0.1f, 1);
        scl = graphics.getWidth() / Core.camera.width * (size > 1 ? 1f : 0.7f);
        iconsize = tilesize * scl;

        setSize(iconsize * Math.min(count, size), rows * rowdy * iconsize);

        font.setColor(1f, 1f, 1f, 0.5f);
        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        font.getData().setScale(scl / 7f, scl / 7f);
        var cache = font.getCache();
        cache.setColor(Color.white);
        float amt, dx, dy;
        int amti;

        if(build.liquids != null){
            for(var item : content.liquids()){
                amt = build.liquids.get(item);
                if(amt > 0) liquidused.get(item.id, 1);
                if(liquidused.check(item.id, 1000)) continue;

                dx = Mathf.mod(i, size) * iconsize;
                dy = rowdy * Mathf.floor(i / (float)size) * iconsize;
                Draw.rect(item.uiIcon, x + dx + iconsize / 2f, y + dy + iconsize / 2f, iconsize * 0.75f, iconsize * 0.75f);

                cache.clear();
                cache.addText(amt < 100f ? Strings.autoFixed(amt, 2) : UI.formatAmount((int)amt), x + dx, y + dy + iconsize / 2f);
                cache.draw(0.8f);

                i++;
            }
        }

        if(build.items != null){
            for(var item : content.items()){
                amti = build.items.get(item);
                if(amti > 0) itemused.get(item.id, 1);
                if(itemused.check(item.id, 1000)) continue;

                dx = Mathf.mod(i, size) * iconsize;
                dy = rowdy * Mathf.floor(i / (float)size) * iconsize;
                Draw.rect(item.uiIcon, x + dx + iconsize / 2f, y + dy + iconsize / 2f, iconsize * 0.75f, iconsize * 0.75f);

                cache.clear();
                cache.addText(UI.formatAmount(amti), x + dx, y + dy + iconsize / 2f);
                cache.draw(0.8f);

                i++;
            }
        }

        font.getData().setScale(oldScaleX, oldScaleY);
    }
}