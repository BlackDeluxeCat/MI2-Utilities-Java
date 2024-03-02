package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.*;
import mi2u.game.*;
import mi2u.input.*;
import mi2u.io.SettingHandler.*;
import mi2u.ui.elements.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.world.blocks.*;

import static mi2u.MI2UVars.*;
import static mi2u.io.SettingHandler.TextFieldSetting.*;
import static mindustry.Vars.*;

public class MI2UI extends Mindow2{
    public static PopupTable popup = new PopupTable();
    public MapInfoTable mapinfo =  new MapInfoTable();

    private long runTime = 0, lastRunTime = 0, realRunTime = 0, lastRealRun = 0;

    protected TabsTable tabs = new TabsTable(true);;
    public int tabId;

    public MI2UI(){
        super("MI2UI", "@main.MI2U", "@main.help");

        Events.run(EventType.Trigger.update, () -> {
            if(state.isGame()){
                RtsCommand.desktopFormation();//Independent from inputoverwrite, may bug
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

                //RTS form table
                if(control.input.commandMode) tabId = 2;
            }
        });

        Events.on(EventType.WorldLoadEvent.class, e -> {
            realRunTime = runTime = 0;
            lastRealRun = lastRunTime = Time.millis();
        });

        titlePane.clear();
        titlePane.table(t -> {
            t.defaults().size(titleButtonSize);
            t.button("" + Iconc.play, textb, () -> {
                tabId = 0;
                tabs.toggle(tabId);
            }).color(Color.yellow);
            t.button("" + Iconc.info, textb, () -> {
                tabId = 1;
                tabs.toggle(tabId);
            }).color(Color.cyan);
            t.button("" + Iconc.settings, textb, () -> {
                tabId = 2;
                tabs.toggle(tabId);
            }).color(Color.green);
        });

        var play = new Table();
        play.table(t -> {
            t.button("" + Iconc.refresh, textb, () -> {
                Call.sendChatMessage("/sync");
            }).minSize(36f).with(funcSetTextb);

            t.button("@main.buttons.rebuild", textb, MI2UFuncs::unitRebuildBlocks).minSize(36f).with(funcSetTextb);

            //The update rate is based on button.update(), and affected by lagging
            t.button("Speeding", textbtoggle, SpeedController::switchUpdate).update(b -> {
                b.setChecked(SpeedController.update);
                b.setText(Core.bundle.get("main.buttons.speeding") + "x" + Strings.autoFixed(SpeedController.scl, 2));
                SpeedController.update();
                b.getLabel().setFontScale(1f);
                b.getLabel().layout();
                b.getLabel().setFontScale(Mathf.clamp((b.getWidth()- 8f - 16f - 8f) / b.getLabel().getGlyphLayout().width, 0.01f, 1f));
            }).with(funcSetTextb).with(b -> {
                b.margin(4f);
                b.table(bii -> {
                    bii.image(Core.atlas.find("mi2-utilities-java-ui-speed")).size(24f).update(img -> {
                        img.setOrigin(Align.center);
                        if(SpeedController.update) img.setRotation(Mathf.log2(SpeedController.scl) * 45f);
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
                b.image(Core.atlas.drawable("mi2-utilities-java-ui-aicfg")).size(24f).scaling(Scaling.fit);
            }, textb, () -> {
                popup.clear();
                popup.addCloseButton();
                popup.addDragBar();
                popup.addInGameVisible();
                popup.setSize(300f, 200f);
                popup.margin(4f).setBackground(Styles.black8);
                popup.image().color(Color.cyan).growX().height(8f);
                popup.row();
                popup.pane(p -> {
                    for(var mode : fullAI.modes){
                        p.table(t -> {
                            t.setBackground(Mindow2.gray2);
                            t.button(b -> {
                                b.image().grow().update(img -> img.setColor(mode.enable ? Color.acid : Color.red));
                            }, textb, () -> {
                                mode.enable = !mode.enable;
                            }).size(16f);
                            if(mode.bimg != null){
                                t.image(mode.bimg).size(18f).scaling(Scaling.fit);
                            }else{
                                t.add(mode.btext).color(Color.sky).left();
                            }
                            t.label(() -> mode.configUIExpand ? "-" : ">").grow().get().clicked(() -> {
                                mode.configUIExpand = !mode.configUIExpand;
                            });
                        }).growX().minHeight(18f).padTop(8f);
                        p.row();
                        p.add(new MCollapser(mode::buildConfig, true).setCollapsed(false, () -> !mode.configUIExpand)).growX();
                        p.row();
                    }
                }).growX().update(p -> {
                    Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                    if(e != null && e.isDescendantOf(p)) {
                        p.requestScroll();
                    }else if(p.hasScroll()){
                        Core.scene.setScrollFocus(null);
                    }
                }).maxHeight(Core.graphics.getHeight()*0.6f/Scl.scl());
                popup.popup();
                popup.update(popup::keepInScreen);
                popup.setPositionInScreen(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f);
            }).grow();
            tt.table(ttt -> {
                fullAI.modes.each(mode -> {
                    ttt.button(bbb -> {
                        if(mode.bimg != null){
                            bbb.image(mode.bimg).size(24f).scaling(Scaling.fit).pad(4f);
                        }else{
                            bbb.add(mode.btext).align(Align.center);
                        }
                    }, textbtoggle, () -> {
                        mode.enable = !mode.enable;
                    }).checked(b -> mode.enable).minSize(32f).grow();
                });
            }).grow();
        });

        var info = new Table();
        info.table(t -> {
            t.table(timet -> {
                timet.label(() -> Iconc.save + Strings.formatMillis(control.saves.getTotalPlaytime())).minWidth(40f).padRight(8f);
                timet.row();
                timet.label(() -> Iconc.play + Strings.formatMillis(runTime)).minWidth(40f).padRight(8f).get();
                timet.row();
                timet.label(() -> Iconc.pause + Strings.formatMillis(realRunTime)).minWidth(40f).get();
            });
            t.add(mapinfo).growX();
        }).growX();

        var set = new Table();
        set.table(t -> {
            t.defaults().minWidth(16f);
            if(settings.getSetting("disableWreck") instanceof CheckSetting ce) t.add(ce.miniButton());
            if(settings.getSetting("disableUnit") instanceof CheckSetting ce) t.add(ce.miniButton());
            if(settings.getSetting("disableBullet") instanceof CheckSetting ce) t.add(ce.miniButton());
            if(settings.getSetting("disableBuilding") instanceof CheckSetting ce) t.add(ce.miniButton());
            t.row();
            if(settings.getSetting("enUnitHpBar") instanceof CheckSetting ce) t.add(ce.miniButton());
            if(settings.getSetting("enBlockHpBar") instanceof CheckSetting ce) t.add(ce.miniButton());
            if(settings.getSetting("enUnitLogic") instanceof CheckSetting ce) t.add(ce.miniButton());
            if(settings.getSetting("enUnitHitbox") instanceof CheckSetting ce) t.add(ce.miniButton());
            if(settings.getSetting("enUnitPath") instanceof CheckSetting ce) t.add(ce.miniButton());
        }).growX();

        set.row();
        set.table(t -> {
            t.defaults().minSize(16f);
            if(settings.getSetting("enDistributionReveal") instanceof CheckSetting ce) t.add(ce.miniButton());
            if(settings.getSetting("drevealBridge") instanceof CheckSetting ce) t.add(ce.miniButton());
            if(settings.getSetting("drevealJunction") instanceof CheckSetting ce) t.add(ce.miniButton());
            if(settings.getSetting("drevealUnloader") instanceof CheckSetting ce) t.add(ce.miniButton());
            if(settings.getSetting("drevealInventory") instanceof CheckSetting ce) t.add(ce.miniButton());
        });

        tabs.queue(play, info, set);
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        cont.add(tabs);
        tabs.toggle(tabId);

        cont.row();

        cont.add(new MCollapser(t -> {
            t.button("@main.buttons.createForm", textbtoggle, () -> {
                RtsCommand.creatingFormation = !RtsCommand.creatingFormation;
            }).checked(b -> RtsCommand.creatingFormation).minSize(36f).growX().with(c -> {
                c.getLabel().setAlignment(Align.center);
            });
            t.row();
            t.table(tt -> {
                for(int i = 0; i < 10; i++){
                    int ii = i;
                    if(i == 5) tt.row();

                    var button = tt.button("" + Mathf.mod(ii + 1, 10), textb, () -> {
                        if(RtsCommand.creatingFormation){
                            RtsCommand.createFormation(Vars.control.input.selectedUnits, ii);
                        }else{
                            RtsCommand.callFormation(ii);
                        }
                    }).disabled(b -> !RtsCommand.creatingFormation && !RtsCommand.checkFormation(ii)).update(b -> {
                        boolean check = RtsCommand.checkFormation(ii);
                        b.setDisabled(!RtsCommand.creatingFormation && !check);
                        b.getLabel().setColor(RtsCommand.creatingFormation ? check ? Color.cyan : Color.acid : Color.white);
                    }).minSize(36f).with(c -> {
                        c.getLabel().setAlignment(Align.center);
                    }).get();

                    var label = new Label(() -> "" + RtsCommand.countFormation(ii));
                    label.setFontScale(0.65f);
                    label.setFillParent(true);
                    label.setAlignment(Align.bottomRight);
                    button.addChild(label);
                }
            }).growX();
        }, true).setDirection(false, true).setCollapsed(false, () -> !control.input.commandMode)).growX();
    }

    @Override
    public void initSettings(){
        super.initSettings();

        settings.title("graphics.ui");

        settings.checkPref("showEmojis", false, b -> emojis.addTo(b ? Core.scene.root:null));
        settings.checkPref("showCoreInfo", false, b -> coreInfo.addTo(b ? Core.scene.root:null));
        settings.checkPref("showMindowMap", false, b -> mindowmap.addTo(b ? Core.scene.root:null));
        settings.checkPref("showLogicHelper", false, b -> logicHelper.addTo(b ? ui.logic:null));

        settings.title("graphics.zone");

        settings.checkPref("enPlayerCursor", true);
        settings.checkPref("enOverdriveZone", false).tag(false, false, true);
        settings.checkPref("enMenderZone", false).tag(false, false, true);
        settings.checkPref("enSpawnZone", true);
        settings.checkPref("enTurretRangeZone", false);
        settings.checkPref("enUnitRangeZone", false);

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
        settings.sliderPref("unitHpBarStyle", 0, 0, 1, 1, s -> {
            if(s == 0) return "x[accent][[i]";
            return "三";
        });
        settings.checkPref("enUnitHitbox", false).tag(false, false, true);
        settings.checkPref("enUnitLogic", false).tag(false, false, true);
        settings.checkPref("enUnitPath", false).tag(false, false, true);
        settings.textPref("enUnitPath.length", String.valueOf(60), TextField.TextFieldFilter.digitsOnly, s -> Strings.parseInt(s) >= 10 && Strings.parseInt(s) <= 300, null, intParser).tag(false, false, true);

        settings.title("graphics.drawGroups");

        settings.checkPref("disableWreck", false);
        settings.checkPref("disableUnit", false);
        settings.checkPref("disableBullet", false);
        settings.checkPref("disableBuilding", false);

        settings.title("game.speedctrl");

        settings.textPref("speedctrl.basefps", String.valueOf(60), TextField.TextFieldFilter.digitsOnly, s -> Strings.parseInt(s) >= 10 && Strings.parseInt(s) <= 600, null, intParser);
        settings.sliderPref("speedctrl.cutoff", 1, 0, 5, 1, s -> s + "fps");

        settings.title("input");

        settings.checkPref("inputReplace", true, b -> {
            if(b){
                control.setInput(mobile ? MobileInputExt.mobileExt : DesktopInputExt.desktopExt);
            }else{
                control.setInput(mobile ? new MobileInput() : new DesktopInput());
            }
        }).tag(true, false, false);
        settings.checkPref("instantBuild", false);
        settings.sliderPref("rtsFormDoubleTap", 300, 25, 1000, 25, s -> s + "ms");
        settings.checkPref("forceTapTile", false);
        settings.checkPref("edgePanning", false);

        settings.title("modify");

        settings.textPref("blockSelectTableHeight", String.valueOf(194), TextField.TextFieldFilter.digitsOnly, s -> Strings.parseInt(s) >= 60 && Strings.parseInt(s) <= 810, null, intParser).tag(false, true, false);
        settings.checkPref("modifyBlockBars", false).tag(false, true, false);
        settings.checkPref("replaceTopTable", false).tag(false, true, false);
        settings.checkPref("modTopTableFollowMouse", false).tag(false, true, false);
        settings.sliderPref("maxSchematicSize", 64, 32, 1024, 16, s -> String.valueOf(s), s -> Vars.maxSchematicSize = s);
        settings.textPref("maxZoom", String.valueOf(renderer.maxZoom), TextField.TextFieldFilter.floatsOnly, s -> Strings.parseFloat(s) > renderer.minZoom && Strings.parseFloat(s) <= 100, s -> renderer.maxZoom = Strings.parseFloat(s), floatParser);
        settings.textPref("minZoom", String.valueOf(renderer.minZoom), TextField.TextFieldFilter.floatsOnly, s -> Strings.parseFloat(s) < renderer.maxZoom && Strings.parseFloat(s) > 0.01f, s -> renderer.minZoom = Strings.parseFloat(s), floatParser);

        settings.checkPref("enableUpdate", true);
    }
}
