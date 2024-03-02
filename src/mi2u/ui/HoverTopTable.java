package mi2u.ui;

import arc.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.*;
import mi2u.struct.*;
import mi2u.ui.elements.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import java.lang.reflect.*;

import static mindustry.Vars.*;

public class HoverTopTable extends PopupTable{
    public static HoverTopTable hoverInfo = new HoverTopTable();

    private final EventType.UnitDamageEvent singleDamageEvent = MI2Utils.getValue(BulletType.class, "bulletDamageEvent");
    private final EventType.UnitDamageEvent splashDamageEvent = MI2Utils.getValue(Damage.class, "bulletDamageEvent");
    Field fBar_Name = MI2Utils.getField(Bar.class, "name");

    public Displayable lastUnit, lastBuild; Tile tile;
    public Building build;
    public Unit unit;
    public Table unitt, buildt, tilet;
    Table floort, oret, blockt;

    FloatDataRecorder buildDps = new FloatDataRecorder(16), unitDps = new FloatDataRecorder(16);
    float uDstk, bDstk;
    Interval dpstimer = new Interval(4);

    public HoverTopTable(){
        initChild();
        build();
        visible(() -> Vars.state.isGame() && Vars.ui.hudfrag.shown && hasInfo());

        Core.scene.root.hovered(this::cleanHover);

        Events.run(EventType.Trigger.update, () -> {
            if(state.isGame() && !Core.scene.hasMouse()) hovered();
            if(state.isMenu()) cleanHover();
        });

        //伤害事件不带伤害值，手算
        Events.on(EventType.UnitDamageEvent.class, e -> {
            if(!this.hasParent()) return;
            Bullet b = e.bullet;

            float rawDamage = e == splashDamageEvent ? Mathf.lerp(1f - (b.type.scaledSplashDamage ? Math.max(0, e.unit.dst(b) - e.unit.type.hitSize/2) : e.unit.dst(b)) / b.type.splashDamageRadius, 1f, 0.4f) * b.type.splashDamage : b.damage;
            float damage = b.type.pierceArmor ? rawDamage : Damage.applyArmor(rawDamage, e.unit.armor);
            if(unit != null && b.owner == unit) uDstk += damage * b.type.damageMultiplier(b) / e.unit.healthMultiplier;
            if(build != null && b.owner == build) bDstk += damage * b.type.damageMultiplier(b) / e.unit.healthMultiplier;
        });

        //对建筑的直伤和溅射共用一个事件，两种伤害不能直接区分。溅射内爆不发事件，其他溅射采用射线爆炸。因此任何带溅射的伤害计算均不可靠。
        Events.on(EventType.BuildDamageEvent.class, e -> {
            if(!this.hasParent()) return;
            Bullet b = e.source;
            //只计算直伤类子弹。
            if(b.type.splashDamageRadius >= 0f) return;

            float damage = b.type.buildingDamageMultiplier * (b.type.pierceArmor ? b.damage : Damage.applyArmor(b.damage, e.build.block.armor));
            if(unit != null && b.owner == unit) uDstk += damage * e.source.type.damageMultiplier(b) / state.rules.blockHealth(e.build.team);
            if(build != null && b.owner == build) bDstk += damage * e.source.type.damageMultiplier(b) / state.rules.blockHealth(e.build.team);
        });
    }

    public void initChild(){
        unitt = new Table();
        buildt = new Table();
        tilet = new Table();

        unitt.update(() -> {
            if(unit == lastUnit){
                if(dpstimer.get(0, 60f)){//record per 0.5s
                    if(dpstimer.check(2, 60f)) unitDps.add(uDstk);
                    uDstk = 0;
                }
                return;
            }
            lastUnit = unit;
            unitt.clear();
            unitDps.reset();
            dpstimer.get(2,1f);
            if(unit != null){
                display(unitt, unit);
                unitt.row();
                unitt.add("").left().update(l -> {
                    l.setText("DPS: " + Strings.fixed(unitDps.avg(), 2) + ", Max: " + Strings.fixed(unitDps.max(), 2));
                    l.setFontScale(Mathf.zero(unitDps.avg(), 0.01f) ? 0.01f : 0.75f);
                });
            }
        });

        buildt.update(() -> {
            if(build == lastBuild){
                if(dpstimer.get(1, 60f)){
                    if(dpstimer.check(3, 60f)) buildDps.add(bDstk);
                    bDstk = 0;
                }
                return;
            }
            lastBuild = build;
            buildt.clear();
            buildDps.reset();
            dpstimer.get(3,1f);
            if(build != null){
                buildt.table(t -> {
                    t.add(build.team.localized()).left().color(build.team.color);
                    t.add("(" + World.conv(build.x) + "," + World.conv(build.y) + ")").left();
                });
                buildt.row();
                var team = build.team;
                build.team = player.team();
                build.display(buildt);
                build.team = team;
                buildt.row();
                buildt.add("").left().update(l -> {
                    l.setText("DPS: " + Strings.fixed(buildDps.avg(), 2) + ", Max: " + Strings.fixed(buildDps.max(), 2));
                    l.setFontScale(Mathf.zero(buildDps.avg(), 0.01f) ? 0.01f : 0.75f);
                });
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
                            if(tile != null && tile.floor() != last) last = tile.floor();
                            this.setDrawable(tile != null ? icon.set(tile.floor().uiIcon) : null);
                        });
                    }
                }).maxSize(8 * 4);
                tt.label(() -> tile != null ? tile.floor().localizedName : "").left().padLeft(5);
            });

            oret = new Table(tt -> {
                tt.add(new Image(){
                    Block last;
                    TextureRegionDrawable icon = new TextureRegionDrawable();
                    {
                        update(() -> {
                            if(tile != null && tile.overlay() != last) last = tile.overlay();
                            this.setDrawable((tile != null && tile.overlay() != null && tile.overlay() != Blocks.air) ? icon.set(tile.overlay().uiIcon) : null);
                        });
                    }
                }).maxSize(8 * 4);
                tt.label(() -> tile != null && tile.overlay() != null && tile.overlay() != Blocks.air ? tile.overlay().localizedName : "").left().padLeft(5);
            });

            blockt = new Table(tt -> {
                tt.add(new Image(){
                    Block last;
                    TextureRegionDrawable icon = new TextureRegionDrawable();
                    {
                        update(() -> {
                            if(tile != null && tile.block() != last) last = tile.block();
                            this.setDrawable((tile != null && (tile.block() instanceof Prop || tile.block() instanceof TreeBlock)) ? icon.set(tile.block().uiIcon) : null);
                        });
                    }
                }).maxSize(8 * 4);
                tt.label(() -> (tile != null && (tile.block() instanceof Prop || tile.block() instanceof TreeBlock)) ? tile.block().localizedName : "").left().padLeft(5);
            });

        }).left().update(t -> {
            floort.act(0.1f);
            blockt.act(0.1f);
            oret.act(0.1f);
            boolean row = (blockt.getPrefWidth() + floort.getPrefWidth() + oret.getPrefWidth()) >= 250f;
            boolean rebuild = row != (t.getColumns() == 1) || t.getCells().size == 0;
            if(!rebuild) return;
            t.clear();
            t.add(floort).left();
            if(row) t.row();
            t.add(oret).left();
            if(row) t.row();
            t.add(blockt).left();
        });
    }

    public void build(){
        clear();
        table(t -> {
            t.clear();
            t.background(Styles.black3);

            t.defaults().growX().padBottom(4f);

            t.add(buildt).minWidth(200f);
            t.row();

            t.add(unitt).minWidth(200f);
            t.row();

            t.add(tilet);
        }).growX();
    }

    /** base on Anuke's*/
    public void display(Table table, Unit unit){
        unit.type.display(unit, table);   //this will not fit some mod ui

        Table uiType = table.find(e -> e instanceof Table t && t.getChildren().size == 2 && t.getChildren().get(1) instanceof Label l && l.textEquals(unit.type.localizedName));
        if(uiType != null){
            uiType.clear();
            uiType.table(t -> {
                t.left();
                t.add(new Image(unit.type.uiIcon)).size(iconMed).scaling(Scaling.fit);
                t.labelWrap(unit.type.localizedName + " | " + unit.team.localized()).left().padLeft(5).growX().color(unit.team.color);
            }).growX().left();
        }

        Bar hpBar = table.find(e -> e instanceof Bar bar && MI2Utils.getValue(fBar_Name, bar).equals(Core.bundle.get("stat.health")));
        if(hpBar != null){
            hpBar.set(() -> Core.bundle.get("stat.health") + ":" + Strings.autoFixed(unit.health(), 3) + "(" + Strings.fixed(unit.health * 100 / unit.maxHealth, 0) + "%) + " + Strings.autoFixed(unit.shield, 2), unit::healthf, Pal.health);
        }

        if(unit instanceof PayloadUnit payload){
            Bar payloadBar = table.find(e -> e instanceof Bar bar && MI2Utils.getValue(fBar_Name, bar).equals(Core.bundle.get("stat.payloadcapacity")));
            if(payloadBar != null){
                payloadBar.set(() -> Core.bundle.get("stat.payloadcapacity") + ":" + Strings.autoFixed(payload.payloadUsed(), 2), () -> payload.payloadUsed() / unit.type().payloadCapacity, Pal.items);
            }
        }

        table.row();

        /*
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
         */

        //table.row();

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

        //table.row();

        //table.label(() -> Blocks.microProcessor.emoji() + (unit.controller() instanceof LogicAI ? Core.bundle.get("units.processorcontrol") : "") + " " + (long)unit.flag).growX().wrap().left();

        //table.row();

        //table.label(() -> Core.bundle.format("lastcommanded", unit.lastCommanded)).growX().wrap().left();
    }

    /** Returns the thing being hovered over. */
    @Nullable
    public void hovered(){
        //check for a unit
        unit = Units.closestOverlap(null, Core.input.mouseWorldX(), Core.input.mouseWorldY(), 5f, u -> true);

        //check tile being hovered over
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(hoverTile != null){
            //if the tile has a building, display it
            build = hoverTile.build != null && state.rules.fog && hoverTile.build.inFogTo(player.team()) ? null : hoverTile.build;
            MI2Utils.setValue(ui.hudfrag.blockfrag, "nextFlowBuild", build);

            tile = hoverTile;
        }else{
            build = null;
            MI2Utils.setValue(ui.hudfrag.blockfrag, "nextFlowBuild", build);
            tile = null;
        }
    }

    public void cleanHover(){
        build = null;
        unit = null;
        tile = null;
    }

    public boolean hasInfo(){
        return unit != null || tile != null || build != null;
    }

    public void setHovered(Object u){
        if(u == null) return;
        cleanHover();
        if(u instanceof Unit uu){
            unit = uu;
        }
        if(u instanceof Building bb){
            build = bb;
        }
        if(u instanceof Tile tt){
            tile = tt;
        }
    }
}
