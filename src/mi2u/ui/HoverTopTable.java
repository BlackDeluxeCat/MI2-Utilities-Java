package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.core.UI;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class HoverTopTable extends Table {
    public static HoverTopTable hoverInfo = new HoverTopTable();

    public Displayable lastUnit, lastBuild; Tile tile, lastTile;
    public Building build;
    public Unit unit;
    public Table unitt, buildt, tilet;
    Table floort, oret, blockt;

    public HoverTopTable(){
        initChild();
        build();
    }

    public void initChild(){
        unitt = new Table();
        buildt = new Table();
        tilet = new Table();

        unitt.update(() -> {
            if(unit == lastUnit) return;
            lastUnit = unit;
            unitt.clear();
            if(unit != null){
                display(unitt, unit);
            }
        });

        buildt.update(() -> {
            if(build == lastBuild) return;
            lastBuild = build;
            buildt.clear();
            if(build != null){
                build.display(buildt);
            }
        });

        tilet.defaults().growX();
        tilet.table(t -> {
            t.left();
            floort = new Table(tt -> {
                tt.add(new Image(){
                    Block last;
                    TextureRegionDrawable icon = new TextureRegionDrawable();
                    {
                        update(() -> {
                            if(tile == null || tile.floor() == last) return;
                            last = tile.floor();
                            this.setDrawable(tile != null ? icon.set(tile.floor().uiIcon) : null);
                        });
                    }
                }).size(8 * 4);
                tt.label(() -> tile != null ? tile.floor().localizedName : "").left().padLeft(5);
            });

            oret = new Table(tt -> {
                tt.add(new Image(){
                    Block last;
                    TextureRegionDrawable icon = new TextureRegionDrawable();
                    {
                        update(() -> {
                            if(tile == null || tile.overlay() == last) return;
                            last = tile.overlay();
                            this.setDrawable((tile != null && tile.overlay() != null && tile.overlay() != Blocks.air) ? icon.set(tile.overlay().uiIcon) : null);
                        });
                    }
                }).size(8 * 4);
                tt.label(() -> tile != null && tile.overlay() != null && tile.overlay() != Blocks.air ? tile.overlay().localizedName : "").left().padLeft(5);
            });

            blockt = new Table(tt -> {
                tt.add(new Image(){
                    Block last;
                    TextureRegionDrawable icon = new TextureRegionDrawable();
                    {
                        update(() -> {
                            if(tile == null || tile.block() == last) return;
                            last = tile.block();
                            this.setDrawable((tile != null && tile.block() instanceof Prop || tile.block() instanceof TreeBlock) ? icon.set(tile.block().uiIcon) : null);
                        });
                    }
                }).size(8 * 4);
                tt.label(() -> (tile != null && tile.block() instanceof Prop || tile.block() instanceof TreeBlock) ? tile.block().localizedName : "").left().padLeft(5);
            });

        }).left().update(t -> {
            floort.act(0.1f);
            blockt.act(0.1f);
            oret.act(0.1f);
            boolean row = (blockt.getPrefWidth() + floort.getPrefWidth() + oret.getPrefWidth()) >= 250f;
            boolean rebuild = row != (t.getColumns() == 1) || t.getCells().size == 0;
            if(!rebuild) return;
            t.clear();
            Log.info(blockt.getPrefWidth() + floort.getPrefWidth() + oret.getPrefWidth());
            t.add(floort).left();
            if(row) t.row();
            t.add(oret).left();
            if(row) t.row();
            t.add(blockt).left();
        });
    }

    public void build(){
        clear();
        table().growX().update(t -> {
            t.clear();
            t.defaults().growX().padBottom(4f);

            if(build != null){
                t.add(buildt);
                t.row();
            }

            if(unit != null){
                t.add(unitt);
                t.row();
            }

            if(tile != null){
                t.add(tilet);
                t.row();
            }

            addColorBar(t);
            t.row();
        });
    }

    /** base on Anuke's*/
    public void display(Table table, Unit unit){
        table.table(t -> {
            t.left();
            t.add(new Image(unit.type.uiIcon)).size(iconMed).scaling(Scaling.fit);
            t.labelWrap(unit.type.localizedName).left().padLeft(5).growX();
            t.labelWrap(unit.team.localized()).left().width(20f).padLeft(5).color(unit.team.color);
        }).growX().left();
        table.row();

        table.table(bars -> {
            bars.defaults().growX().height(20f).pad(4);

            bars.add(new Bar(() -> Core.bundle.get("stat.health") + ":" + Strings.autoFixed(unit.health(), 3) + "(" + Strings.fixed(unit.health * 100 / unit.maxHealth, 0) + "%) + " + Strings.autoFixed(unit.shield, 2), () -> Pal.health, unit::healthf).blink(Color.white));
            bars.row();

            if(state.rules.unitAmmo){
                bars.add(new Bar(unit.type.ammoType.icon() + " " + Core.bundle.get("stat.ammo"), unit.type.ammoType.barColor(), () -> unit.ammo / unit.type.ammoCapacity));
                bars.row();
            }

            for(Ability ability : unit.abilities){
                ability.displayBars(unit, bars);
            }

            if(unit instanceof Payloadc payload){
                bars.add(new Bar(() -> Core.bundle.get("stat.payloadcapacity") + ":" + Strings.autoFixed(payload.payloadUsed(), 2), () -> Pal.items, () -> payload.payloadUsed() / unit.type().payloadCapacity));
                bars.row();

                var count = new float[]{-1};
                bars.table().update(t -> {
                    if(count[0] != payload.payloadUsed()){
                        payload.contentInfo(t, 8 * 2, 270);
                        count[0] = payload.payloadUsed();
                    }
                }).growX().left().height(0f).pad(0f);
            }
        }).growX();

        table.row();

        table.table().update(t -> {
            for(var effect : content.statusEffects()){
                if(!unit.hasEffect(effect) || t.find(effect.name) != null) continue;
                t.labelWrap("").growX().name(effect.name).update(l -> {
                    if(!unit.hasEffect(effect)){
                        l.remove();
                        return;
                    }
                    float duration = unit.getDuration(effect);
                    l.setText(effect.emoji() + effect.localizedName + ": " + (duration > 3600f ? UI.formatTime(duration) : Strings.autoFixed(duration / 60f, 2)));
                });
                t.row();
            }
        }).growX();

        table.row();

        table.label(() -> Blocks.microProcessor.emoji() + (unit.controller() instanceof LogicAI ? Core.bundle.get("units.processorcontrol") : "") + " " + (long)unit.flag).growX().wrap().left();

        table.row();

        table.label(() -> Core.bundle.format("lastcommanded", unit.lastCommanded)).growX().wrap().left();
    }

    /** Returns the thing being hovered over. */
    @Nullable
    public void hovered(){
        Vec2 v = this.stageToLocalCoordinates(Core.input.mouse());

        //if the mouse intersects the table or the UI has the mouse, no hovering can occur
        if(Core.scene.hasMouse() || this.hit(v.x, v.y, false) != null) return;

        //check for a unit
        unit = Units.closestOverlap(null, Core.input.mouseWorldX(), Core.input.mouseWorldY(), 5f, u -> true);

        //check tile being hovered over
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(hoverTile != null){
            //if the tile has a building, display it
            build = hoverTile.build;

            //if the tile has a drop, display the drop
            //if(hoverTile.drop() != null || hoverTile.wallDrop() != null){
                tile = hoverTile;
            //}
        }else{
            build = null;
            tile = null;
        }
    }

    public boolean hasInfo(){
        hovered();
        return unit != null || tile != null || build != null;
    }

    public void addColorBar(Table table){
        table.table(Tex.whiteui).height(4f).growX().pad(4f,0f,4f,0f).color(Color.grays(0.2f));
    }
}
