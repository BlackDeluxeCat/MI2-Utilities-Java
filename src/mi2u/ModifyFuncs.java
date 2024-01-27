package mi2u;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.io.*;
import mi2u.ui.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

/** modify vanilla game*/
public class ModifyFuncs{

    public static void modifyVanilla(){
        modifyVanillaBlockBars();
        Events.on(EventType.ContentInitEvent.class, e2 -> modifyVanillaBlockBars());
        initBetterTopTable();
        settingsMenuDialog();
    }

    public static void modifyVanillaBlockBars(){
        if(!MI2USettings.getBool("modifyBlockBars")) return;
        content.blocks().each(block -> {
            addBarToBlock(block, "health", e -> new Bar(() -> Core.bundle.format("stat.health") + ":" + Strings.autoFixed(e.health(), 3) + "(" + Strings.autoFixed(e.health * 100 / e.maxHealth, 2) + "%)", () -> Pal.health, e::healthf));

            if(block.hasLiquids){
                //Anuke
                boolean added = false;

                for(var consl : block.consumers){
                    if(consl instanceof ConsumeLiquid liq){
                        added = true;
                        addLiquidBarToBlock(block, liq.liquid);
                    }else if(consl instanceof ConsumeLiquids multi){
                        added = true;
                        for(var stack : multi.liquids){
                            addLiquidBarToBlock(block, stack.liquid);
                        }
                    }
                }

                //nothing was added, so it's safe to add a dynamic liquid bar (probably?)
                if(!added){
                    addBarToBlock(block, "liquid", entity -> new Bar(
                            () -> entity.liquids.current() == null || entity.liquids.get(entity.liquids.current()) <= 0.001f ? Core.bundle.get("bar.liquid") : entity.liquids.current().localizedName + ":" + Strings.autoFixed(entity.liquids.get(entity.liquids.current()), 2) + "/" + block.liquidCapacity,
                            () -> entity.liquids.current() == null ? Color.clear : entity.liquids.current().barColor(),
                            () -> entity.liquids.current() == null ? 0f : entity.liquids.get(entity.liquids.current()) / block.liquidCapacity)
                    );
                }
            }

            if(block instanceof HeatCrafter hc){
                addBarToBlock(block, "heat", (HeatCrafter.HeatCrafterBuild entity) ->
                        new Bar(() ->
                                Core.bundle.format("bar.heatpercent", (int)entity.heat, (int)(entity.efficiencyScale() * 100)) + "/" + (int)hc.heatRequirement + " x" + hc.maxEfficiency,
                                () -> Pal.lightOrange,
                                () -> entity.heat / hc.heatRequirement));
            }

            if(block instanceof ImpactReactor || block instanceof BeamNode){
                //do nothing
            }else if(block.hasPower && block.consumesPower && block.consPower != null){
                addBarToBlock(block, "power", entity -> new Bar(() -> block.consPower.buffered ? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status * block.consPower.capacity) ? "<ERROR>" : UI.formatAmount((int)(entity.power.status * block.consPower.capacity))) :
                        Core.bundle.get("bar.power") + ":" + Strings.autoFixed((entity.status() == BlockStatus.active ? 1f : 0f) * entity.efficiency() * -entity.power.status * block.consPower.usage * 60f * (entity.canConsume()?entity.timeScale():0),2), () -> Pal.powerBar, () -> Mathf.zero(block.consPower.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status));
            }

            if(block instanceof Turret tu){
                addBarToBlock(block, "logicTimer", (Turret.TurretBuild entity) -> new Bar(() -> "Logic Control: " + Strings.autoFixed(entity.logicControlTime, 1), () -> Pal.logicControl, () -> entity.logicControlTime / Turret.logicControlCooldown));
                if(tu.heatRequirement > 0f){
                    addBarToBlock(block, "heat", (Turret.TurretBuild entity) ->
                            new Bar(() ->
                                    Core.bundle.format("bar.heatpercent", (int)entity.heat, (int)(entity.efficiencyScale() * 100)) + "/" + (int)tu.heatRequirement + " x" + tu.maxHeatEfficiency,
                                    () -> Pal.lightOrange,
                                    () -> entity.heat / tu.heatRequirement));
                }
            }
        });
    }

    public static <T extends Building> void addBarToBlock(Block block, String name, Func<T, Bar> sup){
        block.addBar(name, sup);
    }

    public static void addLiquidBarToBlock(Block block, Liquid liq){
        block.addBar("liquid-" + liq.name, entity -> !liq.unlockedNow() ? null : new Bar(
                () -> liq.localizedName + ":" + Strings.autoFixed(entity.liquids.get(liq), 2) + "/" + block.liquidCapacity,
                liq::barColor,
                () -> entity.liquids.get(liq) / block.liquidCapacity
        ));
    }

    public static void initBetterTopTable(){
        Events.on(EventType.WorldLoadEvent.class, event -> {
            Time.runTask(10f, ModifyFuncs::betterTopTable);
        });

        Events.on(EventType.UnlockEvent.class, event -> {
            if(event.content instanceof Block){
                Time.runTask(10f, ModifyFuncs::betterTopTable);
            }
        });
    }

    public static void betterTopTable(){
        if(MI2USettings.getBool("modifyTopTable", false)){
            Table topTable = Reflect.get(ui.hudfrag.blockfrag, "topTable");
            if(topTable == null){
                Log.infoTag("MI2U-Modify", "failed to replace info top-table");
                return;
            }

            Element vanilla = topTable.getChildren().get(topTable.getChildren().size - 1);
            topTable.clearChildren();

            //HoverTopTable是完全和方块info共用table的，所以无法将原版info拆出来做浮窗。
            if(!MI2USettings.getBool("topTableFollowMouse", false)){
                topTable.add(HoverTopTable.hoverInfo).growX();
                HoverTopTable.hoverInfo.touchable = Touchable.enabled;
            }else{
                var h = HoverTopTable.hoverInfo;
                h.touchable = Touchable.disabled;
                h.hide();
                h.popup();
                h.update(() -> {
                    h.toFront();
                    h.setPositionInScreen(Core.input.mouseX() + 30f, Core.input.mouseY() + 30f);
                    if(state.isMenu()) h.hide();
                });
            }
            topTable.row();
            topTable.add(vanilla).growX().visible(() -> control.input.block != null || MI2Utils.getValue(ui.hudfrag.blockfrag, "menuHoverBlock") != null);
            topTable.row();
            topTable.add(new Element()).height(0.5f).update(t -> {
                var cell = topTable.getCell(vanilla);
                if(cell != null) cell.height(vanilla.getPrefHeight() * (vanilla.visible ? 1f:0f) / Scl.scl() + 0.5f);   //affected by ui scale, I don't know why it's necessary here.
                MI2Utils.setValue(ui.hudfrag.blockfrag, "hover", HoverTopTable.hoverInfo.unit);
            });
            topTable.visible(() -> {
                vanilla.updateVisibility();
                return HoverTopTable.hoverInfo.hasInfo() || vanilla.visible;
            });
        }

        if(MI2USettings.getBool("modifyBlockSelectTable", false)){
            Table blockCatTable = MI2Utils.getValue(ui.hudfrag.blockfrag, "blockCatTable");

            ((Table)blockCatTable.getCells().first().get()).getCells().first().height(Mathf.clamp(MI2USettings.getInt("blockSelectTableHeight", 194), 50, 1000));
            blockCatTable.getCells().get(1).height(Mathf.clamp(MI2USettings.getInt("blockSelectTableHeight", 194) + 52, 50, 1000));
        }
    }

    public static void settingsMenuDialog(){
        ui.settings.addCategory("@mindow2.settings.title", new TextureRegionDrawable(new TextureRegion(MI2Utilities.MOD.iconTexture)), st ->{
            st.add("@mindow2.settings.allIntro");
            st.row();
            st.pane(t -> {
                t.name = "Mindow Help";
                for(var m : mindow2s){
                    if(!m.mindowName.equals("")) t.button(Iconc.infoCircle + Core.bundle.get(m.titleText.substring(1)), textb, m::showHelp).with(funcSetTextb).pad(4f);
                }
            }).with(p -> {
                p.setForceScroll(true, false);
            }).growX();
            st.row();
            st.table(t -> {
                int index = 0;
                for(var entry : MI2USettings.entries){
                    t.add("" + ++index).size(32f).color(Color.gray);
                    t.table(entry::build).growX();
                    t.row();
                }
            });
            st.row();
            st.button("@mi2u.settings.cleanUp", textb, () -> {
                var dialog = new BaseDialog("@mi2u.settings.cleanUp");
                dialog.addCloseButton();
                dialog.buttons.button("@clear", Icon.refresh, () -> ui.showConfirm("@mi2u.settings.removeAllConf", MI2USettings.map::clear));
                dialog.cont.pane(t -> {
                    MI2USettings.map.each((name, setting) -> {
                        t.button("" + Iconc.cancel, textb, null).size(24f).with(b -> {
                            b.clicked(() -> ui.showConfirm(Core.bundle.get("mi2u.settings.removeConf") + name, () -> {
                                MI2USettings.map.remove(name);
                                MI2USettings.modified = true;
                                b.setDisabled(true);
                            }));
                        });
                        t.labelWrap(name).width(200f);
                        t.add(setting.get()).width(200f);
                        t.row();
                    });
                }).growY();
                dialog.show();
            }).growX().height(64f);
        });
    }
}
