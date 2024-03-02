package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
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
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.storage.*;

import static arc.Core.*;
import static mi2u.MI2UVars.*;
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
    float lastCount = 0f;

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
        itemused.clear();
        build = null;
        lastCount = 0f;
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

        Draw.alpha(lastCount < 1f ? 0f : 0.8f);
        int i = 0;
        int rows = Mathf.ceil(lastCount / size);
        float rowdy = Math.min(size / rows + 0.1f, 1);

        setSize(iconsize * Math.min(lastCount, size), rows * rowdy * iconsize);

        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        font.getData().setScale(scl / 7f, scl / 7f);
        font.setColor(MI2UTmp.c2.set(Color.white).a(0.9f));
        float amt, dx, dy;
        int amti;

        float halfIconsize = iconsize / 2f, sq34Iconsize = iconsize * 0.75f;

        if(build.liquids != null){
            for(var item : content.liquids()){
                amt = build.liquids.get(item);
                if(amt > 0) liquidused.get(item.id, 1);
                if(liquidused.check(item.id, 1000)) continue;

                dx = Mathf.mod(i, size) * iconsize;
                dy = rowdy * Mathf.floor(i / size) * iconsize;
                Draw.rect(item.uiIcon, x + dx + halfIconsize, y + dy + halfIconsize, sq34Iconsize, sq34Iconsize);

                font.draw(amt < 100f ? Strings.autoFixed(amt, 2) : UI.formatAmount((int)amt), x + dx, y + dy + halfIconsize);

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
                Draw.rect(item.uiIcon, x + dx + halfIconsize, y + dy + halfIconsize, sq34Iconsize, sq34Iconsize);

                font.draw(UI.formatAmount(amti), x + dx, y + dy + halfIconsize);

                i++;
            }
        }

        if(build instanceof ItemTurret.ItemTurretBuild ib){
            for(var ammo : ib.ammo){
                ItemTurret.ItemEntry item = (ItemTurret.ItemEntry)ammo;

                dx = Mathf.mod(i, size) * iconsize;
                dy = rowdy * Mathf.floor(i / size) * iconsize;
                Draw.rect(item.item.uiIcon, x + dx + halfIconsize, y + dy + halfIconsize, sq34Iconsize, sq34Iconsize);
                ammoIcon.draw(x + dx + (1f + 0.4f) * halfIconsize, y + dy + 0.4f * halfIconsize, iconsize / 4f, iconsize / 4f);
                font.draw(UI.formatAmount(item.amount), x + dx, y + dy + halfIconsize);

                i++;
            }
        }
        lastCount = i;

        font.getData().setScale(oldScaleX, oldScaleY);
    }

    public static Drawable ammoIcon = Core.atlas.getDrawable("mi2-utilities-java-ui-ammo");
}