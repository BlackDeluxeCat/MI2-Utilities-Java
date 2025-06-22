package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.*;
import mi2u.game.*;
import mi2u.graphics.*;
import mi2u.io.*;
import mi2u.io.SettingHandler.*;
import mi2u.ui.elements.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.defense.turrets.*;

import static mi2u.MI2UVars.*;
import static mi2u.io.SettingHandler.TextFieldSetting.*;
import static mindustry.Vars.*;

public class MI2UI extends Mindow2{
    public static SettingHandler filterTurretRangeZone = new SettingHandler("MI2UI.filterTurretRangeZone"), filterUnitRangeZone = new SettingHandler("MI2UI.filterUnitRangeZone");
    public static PopupTable popup = new PopupTable();
    public MapInfoTable mapinfo;

    private long runTime = 0, lastRunTime = 0, realRunTime = 0, lastRealRun = 0;

    public boolean showQuickSettings;

    public MI2UI(){
        super("MI2UI");

        Events.run(EventType.Trigger.draw, FpsController::update);

        Events.run(EventType.Trigger.update, () -> {
            if(state.isGame()){
                if(!state.isPaused()){
                    realRunTime += Time.timeSinceMillis(lastRealRun);
                }
                runTime += Time.timeSinceMillis(lastRunTime);
                lastRealRun = Time.millis();
                lastRunTime = Time.millis();

                if(state.rules.mode() == Gamemode.sandbox && !net.active() && state.isGame() && !state.isPaused() && player.unit() != null && control.input.isBuilding && settings.getBool("instantBuild")){
                    player.unit().plans.each(bp -> {
                        var tile = world.tiles.getc(bp.x, bp.y);
                        if(bp.breaking){
                            tile.setAir();
                        }else{
                            Call.beginPlace(player.unit(), bp.block, player.team(), bp.x, bp.y, bp.rotation);
                            if(bp.tile().build instanceof ConstructBlock.ConstructBuild cb) cb.construct(player.unit(), null, 1f, bp.config);
                            player.unit().plans.remove(bp);
                        }
                    });
                }
            }
        });

        Events.on(EventType.WorldLoadEvent.class, e -> {
            realRunTime = runTime = 0;
            lastRealRun = lastRunTime = Time.millis();
        });

        titlePane.clear();
        titlePane.add().growX();
        titlePane.button("Sy\nnc", textb, () -> {
            Call.sendChatMessage("/sync");
        }).color(Color.green).size(buttonSize).with(tb -> {
            tb.getLabel().setFontScale(0.8f);
        });
        titlePane.add(mapinfo = new MapInfoTable()).height(buttonSize);
        titlePane.button("" + Iconc.settings, textbtoggle, () -> {
            showQuickSettings = !showQuickSettings;
        }).size(buttonSize).checked(tb -> showQuickSettings).with(tb -> {
            tb.getLabel().setColor(Color.gold);
        });
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        cont.table(play -> {
            play.table(t -> {
                t.button("DG", textb, MI2UFuncs::cleanGhostBlock).minSize(36f).with(funcSetTextb).disabled(b -> player.team().data().plans.isEmpty()).with(tb -> MI2Utils.tooltip(tb, "@mi2ui.buttons.cleanGhost"));

                t.button("RB", textb, MI2UFuncs::unitRebuildBlocks).minSize(36f).with(funcSetTextb).with(tb -> MI2Utils.tooltip(tb, "@mi2ui.buttons.rebuild"));

                //The update rate is based on button.update(), and affected by lagging
                t.button("", textbtoggle, FpsController::toggle).update(b -> {
                    b.setChecked(FpsController.update);
                    b.setText(Core.bundle.get("mi2ui.buttons.fpsCtrl") + "x" + Strings.autoFixed(FpsController.scl, 2));
                    b.getLabel().setFontScale(1f);
                    b.getLabel().layout();
                    b.getLabel().setFontScale(Mathf.clamp((b.getWidth()- 8f - 16f - 8f) / b.getLabel().getGlyphLayout().width, 0.01f, 1f));
                }).with(funcSetTextb).with(b -> {
                    b.margin(4f);
                    b.table(bii -> {
                        bii.image(Core.atlas.find("mi2-utilities-java-ui-speed")).size(24f).update(img -> {
                            img.setOrigin(Align.center);
                            if(FpsController.update) img.setRotation(Mathf.log2(FpsController.scl) * 45f);
                            else img.setRotation(0f);
                        });
                        bii.add().grow();
                    }).grow();
                    b.getLabelCell().expand(false, false).fill(false).width(0.5f);
                    b.getLabel().setAlignment(Align.right);
                    b.getCells().swap(0,1);
                }).grow();
            }).growX();
            play.row();
            play.table(tt -> {
                tt.button(b -> {
                    b.image(Core.atlas.drawable("mi2-utilities-java-ui-aicfg")).size(24f).pad(8f).scaling(Scaling.fit);
                }, textb, () -> {
                    fullAI.buildConfig();
                    fullAI.cfgTable.popup();
                    fullAI.cfgTable.setPositionInScreen(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f);
                });
                tt.table(tttt -> {
                    tttt.defaults().pad(2f).growX().minWidth(40f);
                    tttt.label(() -> Iconc.save + Strings.formatMillis(control.saves.getTotalPlaytime())).fontScale(0.6f);
                    tttt.row();
                    tttt.label(() -> Iconc.play + Strings.formatMillis(runTime)).fontScale(0.6f);
                    tttt.row();
                    tttt.label(() -> Iconc.pause + Strings.formatMillis(realRunTime)).fontScale(0.6f);
                }).fill().growX();
            }).growX();
        });

        cont.row();

        cont.add(new MCollapser(m -> {
            m.defaults().growX();
            m.table(t -> {
                if(settings.getSetting("disableWreck") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.eyeOff + Iconc.teamDerelict));
                if(settings.getSetting("disableUnit") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.eyeOff + Iconc.alphaaaa));
                if(settings.getSetting("disableBullet") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.eyeOff + Iconc.unitAnthicusMissile));
                if(settings.getSetting("disableBuilding") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.eyeOff + Iconc.blockMechanicalDrill));
            }).row();

            m.table(t -> {
                if(settings.getSetting("enUnitHpBar") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.unitDagger));
                if(settings.getSetting("enBlockHpBar") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.teamDerelict));
                if(settings.getSetting("enUnitLogic") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.blockMicroProcessor + Iconc.unitDagger));
                if(settings.getSetting("enUnitPath") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.planet + Iconc.unitDagger));
            }).row();

            m.table(t -> {
                if(settings.getSetting("enDistributionReveal") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.zoom + Iconc.distribution));
                if(settings.getSetting("drevealBridge") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.blockBridgeConveyor));
                if(settings.getSetting("drevealJunction") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.blockJunction));
                if(settings.getSetting("drevealUnloader") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.blockUnloader));
                if(settings.getSetting("drevealInventory") instanceof CheckSetting ce) t.add(ce.miniButton("" + Iconc.blockVault));
            }).row();
        }, true).setDirection(true, true).setCollapsed(true, () -> !showQuickSettings)).growX();
    }

    @Override
    public void initSettings(){
        super.initSettings();

        settings.title("graphics.ui");

        settings.checkPref("showEmojis", false, b -> emojis.addTo(b ? Core.scene.root:null));
        settings.checkPref("showCoreInfo", false, b -> coreInfo.addTo(b ? Core.scene.root:null));
        settings.checkPref("showMinimap", false, b -> mindowmap.addTo(b ? Core.scene.root:null));
        settings.checkPref("showLogicHelper", false, b -> logicHelper.addTo(b ? ui.logic:null));

        settings.title("graphics.zone");

        settings.checkPref("enPlayerCursor", true);
        settings.checkPref("enOverdriveZone", false).tag(false, false, true);
        settings.checkPref("enMenderZone", false).tag(false, false, true);
        settings.checkPref("enSpawnZone", true);
        settings.checkPref("enTurretRangeZone", false);
        settings.entry("graphics.filterTurretRangeZone", (entry, t) -> {
            t.button(entry.title, () -> {
                new BaseDialog(entry.title){{
                    addCloseButton();
                    cont.pane(t -> {
                        int col = Math.max(1, Mathf.floor(Core.scene.getWidth() / Scl.scl(160f)));
                        for(var c : content.blocks().select(b -> b instanceof BaseTurret)){
                            TextButton button = new TextButton(c.localizedName, textbtoggle);
                            button.add(new Image(c.uiIcon)).size(32f).pad(8f);
                            button.getCells().reverse();
                            button.clicked(() -> {
                                filterTurretRangeZone.putBool(c.name, button.isChecked());
                                for(var block : content.blocks()){
                                    RendererExt.filterTurretRangeZone[block.id] = MI2UI.filterTurretRangeZone.getBool(block.name, true);
                                }
                            });
                            t.add(button).size(140, 100).pad(4f).update(tb -> tb.setChecked(filterTurretRangeZone.getBool(c.name, true)));
                            if(t.getChildren().size % col == 0) t.row();
                        }
                    }).with(p -> p.setForceScroll(true, true));
                    show();
                }};
            }).growX();
        });
        settings.sliderPref("turretZoneColorStyle", 0, 0, 1, 1, s -> {
            if(s == 1) return "Anti Air";
            return "-";
        });
        settings.checkPref("enUnitRangeZone", false);
        settings.entry("graphics.filterUnitRangeZone", (entry, t) -> {
            t.button(entry.title, () -> {
                new BaseDialog(entry.title){{
                    addCloseButton();
                    cont.pane(t -> {
                        int col = Math.max(1, Mathf.floor(Core.scene.getWidth() / Scl.scl(160f)));
                        for(var c : content.units()){
                            TextButton button = new TextButton(c.localizedName, textbtoggle);
                            button.add(new Image(c.uiIcon)).size(32f).pad(8f);
                            button.getCells().reverse();
                            button.clicked(() -> {
                                filterUnitRangeZone.putBool(c.name, button.isChecked());
                                for(var block : content.units()){
                                    RendererExt.filterUnitRangeZone[block.id] = MI2UI.filterUnitRangeZone.getBool(block.name, true);
                                }
                            });
                            t.add(button).size(140, 100).pad(4f).update(tb -> tb.setChecked(filterUnitRangeZone.getBool(c.name, true)));
                            if(t.getChildren().size % col == 0) t.row();
                        }
                    }).with(p -> p.setForceScroll(true, true));
                    show();
                }};
            }).growX();
        });

        settings.title("graphics.distributionReveal");

        settings.checkPref("enDistributionReveal", true);
        settings.checkPref("drevealBridge", true);
        settings.checkPref("drevealJunction", true);
        settings.checkPref("drevealUnloader", true);
        settings.checkPref("drevealInventory", false).tag(false, false, true);

        settings.title("graphics.overlay");

        settings.checkPref("enBlockHpBar", true).tag(false, false, true);
        settings.checkPref("enUnitHpBar", true).tag(false, false, true);
        settings.checkPref("unitHpBarDamagedOnly", true);
        settings.checkPref("enUnitHitbox", false).tag(false, false, true);
        settings.checkPref("enUnitLogic", false).tag(false, false, true);
        settings.checkPref("enUnitPath", false).tag(false, false, true);
        settings.sliderPref("enUnitPath.length", 60, 5, 300, String::valueOf).tag(false, false, true);

        settings.title("graphics.drawGroups");

        settings.checkPref("disableWreck", false);
        settings.checkPref("disableUnit", false);
        settings.checkPref("disableBullet", false);
        settings.checkPref("disableBuilding", false);

        settings.title("game.fpsCtrl");

        settings.sliderPref("fpsCtrl.cutoff", 1, 0, 5, 1, s -> s + "fps");

        settings.title("input");

        settings.checkPref("inputReplace", true).tag(true, false, false);
        settings.checkPref("instantBuild", false);
        settings.checkPref("forceTapTile", false);
        settings.checkPref("edgePanning", false);

        settings.title("modify");

        settings.textPref("blockSelectTableHeight", String.valueOf(194), TextField.TextFieldFilter.digitsOnly, s -> Strings.parseInt(s) >= 60 && Strings.parseInt(s) <= 810, null, intParser).tag(false, true, false);
        settings.checkPref("modifyBlockBars", false).tag(true, false, false);
        settings.checkPref("replaceTopTable", false).tag(false, true, false);
        settings.checkPref("modTopTableFollowMouse", false).tag(false, true, false);
        settings.sliderPref("maxSchematicSize", 64, 32, 1024, 16, String::valueOf, s -> Vars.maxSchematicSize = s);
        settings.textPref("maxZoom", String.valueOf(renderer.maxZoom), TextField.TextFieldFilter.floatsOnly, s -> Strings.parseFloat(s) > renderer.minZoom && Strings.parseFloat(s) <= 100, s -> renderer.maxZoom = Strings.parseFloat(s), floatParser);
        settings.textPref("minZoom", String.valueOf(renderer.minZoom), TextField.TextFieldFilter.floatsOnly, s -> Strings.parseFloat(s) < renderer.maxZoom && Strings.parseFloat(s) > 0.01f, s -> renderer.minZoom = Strings.parseFloat(s), floatParser);

        settings.checkPref("enableUpdate", true);
    }
}
