package mi2u;

import arc.*;
import arc.func.*;
import arc.graphics.Color;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.Element;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.io.*;
import mi2u.map.filters.*;
import mi2u.ui.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.consumers.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

import static mi2u.MI2UVars.textb;
import static mindustry.Vars.*;

/** modify vanilla game*/
public class ModifyFuncs{

    public static void modifyVanilla(){
        modifyVanillaBlockBars();
        Events.on(EventType.ContentInitEvent.class, e2 -> modifyVanillaBlockBars());
        addFilters();
        initBetterTopTable();
        schelogic();
    }

    public static void addFilters(){
        if(!MI2USettings.getBool("modifyFilters")) return;
        addFilter(TranslateTransFilter::new);
        addFilter(ScalingTransFilter::new);
        addFilter(RotateTransFilter::new);
        addFilter(PolarTransFilter::new);
        addFilter(AppliedRegionFilter::new);
        addFilter(AdvancedNoiseFilter::new);
        addFilter(AdvancedOreFilter::new);
        addFilter(GridFilter::new);
        addFilter(CopyPasteFilter::new);
    }

    public static void addFilter(Prov<GenerateFilter> filter){
        var newArr = Arrays.copyOf(Maps.allFilterTypes, Maps.allFilterTypes.length + 1);
        newArr[Maps.allFilterTypes.length] = filter;
        Maps.allFilterTypes = newArr;
        Log.info("Adding New Filters... Filters Size: " + newArr.length);
        GenerateFilter ins = filter.get();
        JsonIO.json.addClassTag(Strings.camelize(ins.getClass().getSimpleName().replace("Filter", "")), ins.getClass());
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

            if(block.hasPower && block.consumesPower && block.consPower != null){
                addBarToBlock(block, "power", entity -> new Bar(() -> block.consPower.buffered ? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status * block.consPower.capacity) ? "<ERROR>" : UI.formatAmount((int)(entity.power.status * block.consPower.capacity))) :
                        Core.bundle.get("bar.power") + ":" + Strings.autoFixed(-entity.power.status * block.consPower.usage * 60f * (entity.canConsume()?entity.timeScale():0),2), () -> Pal.powerBar, () -> Mathf.zero(block.consPower.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status));
            }

            if(block instanceof Turret) addBarToBlock(block, "logicTimer", (Turret.TurretBuild entity) -> new Bar(() -> "Logic Control: " + Strings.autoFixed(entity.logicControlTime, 1), () -> Pal.logicControl, () -> entity.logicControlTime / Turret.logicControlCooldown));
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

    //让原版面板只在方块规划显示(+尺寸缩放)，mod面板则在有hover时显示(+尺寸缩放)。还需要调换原版和mod版的布局，让方块规划紧贴方块选择区
    public static void betterTopTable(){
        if(!MI2USettings.getBool("modifyTopTable", false)) return;

        Table topTable = Reflect.get(ui.hudfrag.blockfrag, "topTable");
        if(topTable == null){
            Log.infoTag("MI2U-Modify", "failed to replace info top-table");
            return;
        }
        Element vanilla = topTable.getChildren().get(topTable.getChildren().size - 1);
        topTable.clearChildren();

        topTable.add(HoverTopTable.hoverInfo).growX();
        topTable.row();
        topTable.add(vanilla).growX().visible(() -> control.input.block != null || MI2Utils.getValue(ui.hudfrag.blockfrag, "menuHoverBlock") != null);
        topTable.row();
        topTable.add(new Element()).height(0.5f).update(t -> {
            var cell = topTable.getCell(vanilla);
            if(cell != null) cell.height(vanilla.getPrefHeight() * (vanilla.visible ? 1f:0f) + 0.5f);
            MI2Utils.setValue(ui.hudfrag.blockfrag, "hover", HoverTopTable.hoverInfo.unit);
        });
        topTable.visible(() -> {
            vanilla.updateVisibility();
            return HoverTopTable.hoverInfo.hasInfo() || vanilla.visible;
        });
    }

    public static void schelogic(){
        SchematicsDialog.SchematicInfoDialog info = Reflect.get(SchematicsDialog.class, ui.schematics, "info");
        info.shown(Time.runTask(10f, () -> {
            Label l = info.find(e -> e instanceof Label ll && ll.getText().toString().contains("[[" + Core.bundle.get("schematic") + "] "));
            if(l != null){
                info.cont.row();
                info.cont.add("@schematicDialog.nameCheck");
                info.cont.row();
                String schename = l.getText().toString().replace("[[" + Core.bundle.get("schematic") + "] ", "");
                Schematic sche = schematics.all().find(s -> s.name().equals(schename));
                if(sche != null && sche.width <= 128 && sche.height <= 128){
                    info.cont.add("@schematicDialog.sizeCheck");
                    info.cont.row();
                    info.cont.button("@schematicsDialog.details", () -> {}).with(b -> {
                        //float padding = 2f;
                        b.getLabel().setWrap(false);
                        b.clicked(() -> {
                            b.setDisabled(true);
                            SchematicsDialog.SchematicImage image = info.find(e -> e instanceof SchematicsDialog.SchematicImage);
                            if(image == null){
                                Log.infoTag("MI2U-Sche Logic", "Failed to get Sche Image, skip.");
                                return;
                            }
                            Vec2 imagexy = MI2UTmp.v1;
                            imagexy.set(0f, 0f);
                            image.localToParentCoordinates(imagexy);
                            sche.tiles.each(tile -> {
                                int size = tile.block.size;
                                float padding = 2f;
                                float bufferScl = Math.min(image.getWidth() / ((sche.width + padding) * 32f * Scl.scl()), image.getHeight() / ((sche.height + padding) * 32f * Scl.scl()));
                                Vec2 tablexy = new Vec2(tile.x, tile.y);
                                //tablexy.add(padding/2f, padding/2f);
                                tablexy.add(-sche.width/2f, -sche.height/2f);
                                tablexy.scl(32f * Scl.scl());
                                tablexy.scl(bufferScl);
                                tablexy.add(imagexy.x, imagexy.y);
                                tablexy.add(image.getWidth()/2f, image.getHeight()/2f);
                                //Label tl = new Label(tile.block.name);
                                //info.cont.addChild(tl);
                                //tl.setPosition(tablexy.x, tablexy.y);
                                //tl.setSize(size);
                                if(tile.block instanceof LogicBlock){
                                    //tile.config is a byte[] including compressed code and links
                                    try(DataInputStream stream = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream((byte[])tile.config)))){
                                        stream.read();
                                        int bytelen = stream.readInt();
                                        if(bytelen > 1024 * 500) throw new IOException("Malformed logic data! Length: " + bytelen);
                                        byte[] bytes = new byte[bytelen];
                                        stream.readFully(bytes);
                                        /*
                                        links.clear();

                                        int total = stream.readInt();

                                        if(version == 0){
                                            //old version just had links, ignore those

                                            for(int i = 0; i < total; i++){
                                                stream.readInt();
                                            }
                                        }else{
                                            for(int i = 0; i < total; i++){
                                                String name = stream.readUTF();
                                                short x = stream.readShort(), y = stream.readShort();

                                                if(relative){
                                                    x += tileX();
                                                    y += tileY();
                                                }

                                                Building build = world.build(x, y);

                                                if(build != null){
                                                    String bestName = getLinkName(build.block);
                                                    if(!name.startsWith(bestName)){
                                                        name = findLinkName(build.block);
                                                    }
                                                }

                                                links.add(new LogicLink(x, y, name, false));
                                            }
                                        }
                                        */
                                        TextButton bl = new TextButton("" + Iconc.paste);
                                        bl.setStyle(textb);
                                        bl.clicked(() -> {
                                            Core.app.setClipboardText(new String(bytes, charset));
                                        });
                                        info.cont.addChild(bl);
                                        bl.setPosition(tablexy.x, tablexy.y);
                                        bl.setSize(Scl.scl() * 36f * (size <= 1f ? 0.5f:1f));
                                    }catch(Exception ignored){
                                        //invalid logic doesn't matter here
                                    }
                                }
                                if(tile.block instanceof MessageBlock){
                                    TextButton bl = new TextButton("" + Iconc.paste);
                                    bl.setStyle(textb);
                                    bl.clicked(() -> {
                                        Core.app.setClipboardText(tile.config.toString());
                                    });
                                    bl.addListener(new Tooltip(tooltip -> {
                                        tooltip.background(Styles.black5);
                                        tooltip.add(tile.config.toString());
                                    }));
                                    info.cont.addChild(bl);
                                    bl.setPosition(tablexy.x, tablexy.y);
                                    bl.setSize(Scl.scl() * 36f * (size <= 1f ? 0.5f:1f));
                                }
                            });
                        });
                    }).size(100f, 30f).with(c -> {
                        c.getLabel().setAlignment(Align.left);
                        c.getLabel().setWrap(false);
                        c.getLabelCell().pad(2);
                    });
                }
            }
        }));
    }
}
