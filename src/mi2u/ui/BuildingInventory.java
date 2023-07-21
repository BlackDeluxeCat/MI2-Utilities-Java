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
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.storage.*;

import static arc.Core.graphics;
import static mi2u.MI2UVars.fadeBackground;
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
    float scl, iconsize, size;

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
        if(b == null) return;
        if(b.liquids != null || state.isGame() && ((b.block.itemCapacity >= 10 && b.items != null && !(b instanceof StorageBlock.StorageBuild sb && sb.linkedCore != null) && !(b instanceof StackConveyor.StackConveyorBuild)))){
            if(used.add(b.id)) pool.obtain().setBuilding(b);
        }
    }

    public BuildingInventory(){}

    public void setBuilding(Building b){
        this.build = b;
        size = b.block.size;
        group.addChild(this);
    }

    @Override
    public void act(float delta){
        super.act(delta);
        if(!vaild()){
            remove();
            return;
        }
        Core.camera.project(MI2UTmp.v3.set(-1f, -1f).scl(size * tilesize / 2f).add(build.x, build.y));
        setPosition(MI2UTmp.v3.x, MI2UTmp.v3.y);
    }

    public boolean vaild(){
        return RendererExt.enDistributionReveal && RendererExt.drevealInventory && state.isGame() && build != null && build.isValid() && ids.contains(build.id);
    }

    @Override
    public boolean remove(){
        if(build != null) used.remove(build.id);
        build = null;
        pool.free(this);
        return super.remove();
    }

    @Override
    public void draw(){
        if(!vaild() || (state.rules.fog && build.inFogTo(player.team()))) return;
        scl = graphics.getWidth() / Core.camera.width * (size > 1 ? 1f : 0.7f);
        if(scl < 1.5f) return;

        Draw.reset();
        fadeBackground.draw(x, y, width, height);
        iconsize = tilesize * scl;

        int i = 0;
        float count = content.items().count(ii -> !itemused.check(ii.id, 1000)) + content.liquids().count(ii -> !liquidused.check(ii.id, 1000));
        int rows = Mathf.ceil(count / size);
        float rowdy = Math.min(size / rows + 0.1f, 1);

        setSize(iconsize * Math.min(count, size), rows * rowdy * iconsize);

        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        font.getData().setScale(scl / 7f, scl / 7f);
        font.setColor(Color.white);
        float amt, dx, dy;
        int amti;

        if(build.liquids != null){
            for(var item : content.liquids()){
                amt = build.liquids.get(item);
                if(amt > 0) liquidused.get(item.id, 1);
                if(liquidused.check(item.id, 1000)) continue;

                dx = Mathf.mod(i, size) * iconsize;
                dy = rowdy * Mathf.floor(i / size) * iconsize;
                Draw.rect(item.uiIcon, x + dx + iconsize / 2f, y + dy + iconsize / 2f, iconsize * 0.75f, iconsize * 0.75f);

                font.draw(amt < 100f ? Strings.autoFixed(amt, 2) : UI.formatAmount((int)amt), x + dx, y + dy + iconsize / 2f);

                i++;
            }
        }

        if(build.items != null){
            for(var item : content.items()){
                amti = build.items.get(item);
                if(amti > 0) itemused.get(item.id, 1);
                if(itemused.check(item.id, 1000)) continue;

                dx = Mathf.mod(i, size) * iconsize;
                dy = rowdy * Mathf.floor(i / size) * iconsize;
                Draw.rect(item.uiIcon, x + dx + iconsize / 2f, y + dy + iconsize / 2f, iconsize * 0.75f, iconsize * 0.75f);

                font.draw(UI.formatAmount(amti), x + dx, y + dy + iconsize / 2f);

                i++;
            }
        }

        font.getData().setScale(oldScaleX, oldScaleY);
    }
}