package mi2u;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.io.*;
import mi2u.map.filters.*;
import mindustry.content.Liquids;
import mindustry.core.UI;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.JsonIO;
import mindustry.maps.Maps;
import mindustry.maps.filters.GenerateFilter;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.ui.*;
import mindustry.ui.dialogs.SchematicsDialog;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.Turret;
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
            /*
            if(block.hasLiquids){
                Func<Building, Liquid> current;
                if(block.consumes.has(ConsumeType.liquid) && block.consumes.get(ConsumeType.liquid) instanceof ConsumeLiquid){
                    Liquid liquid = block.consumes.<ConsumeLiquid>get(ConsumeType.liquid).liquid;
                    current = entity -> liquid;
                }else{
                    current = entity -> entity.liquids == null ? Liquids.water : entity.liquids.current();
                }
                addBarToBlock(block, "liquid", entity -> new Bar(() -> entity.liquids.get(current.get(entity)) <= 0.001f ? Core.bundle.get("bar.liquid") : (current.get(entity).localizedName + ":" + Strings.autoFixed(entity.liquids.get(current.get(entity)),2) + "/" + block.liquidCapacity),
                        () -> current.get(entity).barColor(), () -> entity == null || entity.liquids == null ? 0f : entity.liquids.get(current.get(entity)) / block.liquidCapacity));
            }

            if(block.hasPower && block.consumes.hasPower()){
                ConsumePower cons = block.consumes.getPower();
                boolean buffered = cons.buffered;
                float capacity = cons.capacity;

                addBarToBlock(block, "power", entity -> new Bar(() -> buffered ? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status * capacity) ? "<ERROR>" : UI.formatAmount((int)(entity.power.status * capacity))) :
                        Core.bundle.get("bar.power") + ":" + Strings.autoFixed(-entity.power.status * cons.usage * 60f * (entity.cons().valid()?entity.timeScale():0),2), () -> Pal.powerBar, () -> Mathf.zero(cons.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status));
            }
            */

            if(block instanceof Turret) addBarToBlock(block, "logicTimer", (Turret.TurretBuild entity) -> new Bar(() -> "Logic Control: " + Strings.autoFixed(entity.logicControlTime, 1), () -> Pal.logicControl, () -> entity.logicControlTime / Turret.logicControlCooldown));

        });
    }

    public static <T extends Building> void addBarToBlock(Block block, String name, Func<T, Bar> sup){
        block.addBar(name, sup);
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
