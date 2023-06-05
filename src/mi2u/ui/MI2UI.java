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
import mi2u.io.*;
import mi2u.io.MI2USettings.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

public class MI2UI extends Mindow2{
    public static PopupTable popup = new PopupTable();
    public MapInfoTable mapinfo =  new MapInfoTable();

    private long runTime = 0, lastRunTime = 0, realRunTime = 0, lastRealRun = 0;

    public MI2UI(){
        super("@main.MI2U", "@main.help");

        Events.run(EventType.Trigger.update, () -> {
            if(state.isGame()){
                RtsCommand.desktopFormation();//Independent of inputoverwrite, may bug
                if(!state.isPaused()){
                    realRunTime += Time.timeSinceMillis(lastRealRun);
                }
                runTime += Time.timeSinceMillis(lastRunTime);
                lastRealRun = Time.millis();
                lastRunTime = Time.millis();
            }
        });

        Events.on(EventType.WorldLoadEvent.class, e -> {
            realRunTime = runTime = 0;
            lastRealRun = lastRunTime = Time.millis();
        });
    }

    @Override
    public void init(){
        super.init();
        mindowName = "MI2UI";
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        cont.table(t -> {
            t.table(timet -> {
                timet.label(() -> Iconc.save + Strings.formatMillis(control.saves.getTotalPlaytime())).minWidth(40f).padRight(8f);
                timet.row();
                timet.label(() -> Iconc.play + Strings.formatMillis(runTime)).minWidth(40f).padRight(8f).get();
                timet.row();
                timet.label(() -> Iconc.pause + Strings.formatMillis(realRunTime)).minWidth(40f).get();
            });
            t.add(mapinfo).growX();
        }).growX();

        cont.row();

        cont.table(tt -> {
            if(MI2USettings.getEntry("enDistributionReveal") instanceof CheckEntry ce) tt.add(ce.newTextButton("" + Iconc.zoom + Iconc.blockJunction)).minSize(24f);
            if(MI2USettings.getEntry("enUnitHitbox") instanceof CheckEntry ce) tt.add(ce.newTextButton("" + Iconc.box)).width(36f);
            if(MI2USettings.getEntry("disableUnit") instanceof CheckEntry ce) tt.add(ce.newTextButton("" + Iconc.cancel + Iconc.unitGamma)).width(36f);
            if(MI2USettings.getEntry("disableBullet") instanceof CheckEntry ce) tt.add(ce.newTextButton("" + Iconc.cancel + Iconc.unitScatheMissile)).width(36f);
            if(MI2USettings.getEntry("disableBuilding") instanceof CheckEntry ce) tt.add(ce.newTextButton("" + Iconc.cancel + Iconc.blockDuo)).width(36f);
        });

        cont.row();

        cont.table(tt -> {
            tt.button("" + Iconc.refresh, textb, () -> {
                Call.sendChatMessage("/sync");
            }).minSize(36f).with(funcSetTextb);

            tt.button("@main.buttons.rebuild", textb, MI2UFuncs::unitRebuildBlocks).minSize(36f).with(funcSetTextb);

            //The update rate is based on button.update(), and affected by lagging
            tt.button("Speeding", textbtoggle, SpeedController::switchUpdate).update(b -> {
                b.setChecked(SpeedController.update);
                b.setText(Core.bundle.get("main.buttons.speeding") + "x" + Strings.autoFixed(SpeedController.scl, 2));
                SpeedController.update();
                b.getLabel().setFontScale(1f);
                b.getLabel().layout();
                b.getLabel().setFontScale(Math.min((b.getWidth()- 8f - 16f - 8f) / b.getLabel().getGlyphLayout().width, 1f));
            }).with(funcSetTextb).with(b -> {
                b.margin(4f);
                b.table(bii -> {
                    bii.image(Icon.settingsSmall).size(16f).update(img -> {
                        if(SpeedController.update) img.rotateBy(-1f);
                        else img.setRotation(0f);
                        img.setColor(!b.isChecked() ? Color.white : SpeedController.lowerThanMin() ? Color.scarlet : Color.lime);
                    }).left();
                    bii.add().growX();
                }).growX();
                b.getLabelCell().expand(false, false).fill(false).width(0.5f);
                b.getLabel().setAlignment(Align.right);
                b.getCells().swap(0,1);
            }).growX();
        }).fillX();

        cont.row();

        cont.table(tt -> {
            tt.button("AI\n" + Iconc.settings, textb, () -> {
                popup.clear();
                popup.addCloseButton();
                popup.addDragMove();
                popup.addInGameVisible();
                popup.setSize(300f, 200f);
                popup.margin(4f).setBackground(Styles.black8);
                popup.pane(p -> {
                    for(var mode : fullAI.modes){
                        p.table(mode::buildConfig).growX();
                        p.row();
                    }
                }).growX().update(p -> {
                    Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                    if(e != null && e.isDescendantOf(p)) {
                        p.requestScroll();
                    }else if(p.hasScroll()){
                        Core.scene.setScrollFocus(null);
                    }
                });
                popup.popup();
                popup.update(popup::keepInScreen);
                popup.setPositionInScreen(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f);
            }).grow().minSize(36f).with(funcSetTextb);
            tt.table(ttt -> {
                fullAI.modes.each(mode -> {
                    ttt.button(mode.btext, textbtoggle, () -> {
                        mode.enable = !mode.enable;
                    }).checked(b -> mode.enable).minSize(36f).with(c -> {
                        c.getLabel().setAlignment(Align.center);
                    }).grow();
                });
            }).grow();
        }).growX();

        cont.row();

        cont.collapser(t -> {
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
        }, () -> true).growX().get().setDuration(0.25f).setCollapsed(true, () -> !Vars.control.input.commandMode).setCollapsed(!Vars.control.input.commandMode);

    }

    @Override
    public void initSettings(){
        super.initSettings();

        settings.add(new CheckEntry("showEmojis", "@settings.main.emoji", false, b -> emojis.addTo(b?Core.scene.root:null)));
        settings.add(new CheckEntry("showCoreInfo", "@settings.main.coreInfo", false, b -> coreInfo.addTo(b?Core.scene.root:null)));
        settings.add(new CheckEntry("showMindowMap", "@settings.main.mindowMap", false, b -> mindowmap.addTo(b?Core.scene.root:null)));
        settings.add(new CheckEntry("showLogicHelper", "@settings.main.logicHelper", true, b -> logicHelper.addTo(b?Vars.ui.logic:null)));

        settings.add(new CheckEntry("enPlayerCursor", "@settings.main.playerCursor", true, null));
        settings.add(new CheckEntry("enOverdriveZone", "@settings.main.overdriveZone", false, null));
        settings.add(new CheckEntry("enMenderZone", "@settings.main.menderZone", false, null));
        settings.add(new FieldEntry("flashZoneAlpha", "@settings.main.flashZoneAlpha", String.valueOf(50), TextField.TextFieldFilter.digitsOnly, s -> Strings.parseInt(s) >= 0 && Strings.parseInt(s) <= 150, null));
        settings.add(new CheckEntry("enSpawnZone", "@settings.main.spawnZone", true, null));
        settings.add(new CheckEntry("enDistributionReveal", "@settings.main.distributionReveal", true, null));

        settings.add(new CollapseGroupEntry("DistributionReveal", ""){
            CheckEntry check1 = new CheckEntry("enDistributionReveal", "@settings.main.distributionReveal", true, null);
            CheckEntry check2 = new CheckEntry("drevealBridge", "@settings.main.dreveal.bridge", true, null);
            CheckEntry check3 = new CheckEntry("drevealJunction", "@settings.main.dreveal.junction", true, null);
            CheckEntry check4 = new CheckEntry("drevealUnloader", "@settings.main.dreveal.unloader", true, null);
            CheckEntry check5 = new CheckEntry("drevealInventory", "@settings.main.dreveal.inventory", true, null);
            {
                collapsep = () -> !check1.value;
                headBuilder = t -> check1.build(t);
                builder = t -> {
                    check2.build(t);
                    t.row();
                    check3.build(t);
                    t.row();
                    check4.build(t);
                    t.row();
                    check5.build(t);
                };
            }
        });

        settings.add(new CheckEntry("enBlockHpBar", "@settings.main.blockHpBar", false, null));
        settings.add(new CheckEntry("enTurretZone", "@settings.main.enTurretZone", false, null));
        settings.add(new CheckEntry("enUnitHpBar", "@settings.main.unitHpBar", false, null));
        settings.add(new CheckEntry("enUnitHpBarDamagedOnly", "@settings.main.unitHpBarDamagedOnly", true, null));
        settings.add(new CheckEntry("enUnitHitbox", "@settings.main.unitHitbox", false, null));
        settings.add(new CheckEntry("enUnitLogic", "@settings.main.unitLogic", false, null));
        settings.add(new CheckEntry("enUnitPath", "@settings.main.unitPath", false, null));
        settings.add(new FieldEntry("enUnitPath.length", "@settings.main.enUnitPath.length", String.valueOf(40), TextField.TextFieldFilter.digitsOnly, s -> Strings.parseInt(s) >= 10 && Strings.parseInt(s) <= 300, null));
        settings.add(new CheckEntry("enUnitRangeZone", "@settings.main.enUnitRangeZone", false, null));

        settings.add(new CheckEntry("disableWreck", "@settings.main.disableWreck", false, null));
        settings.add(new CheckEntry("disableUnit", "@settings.main.disableUnit", false, null));
        settings.add(new CheckEntry("disableBullet", "@settings.main.disableBullet", false, null));
        settings.add(new CheckEntry("disableBuilding", "@settings.main.disableBuilding", false, null));

        settings.add(new CollapseGroupEntry("SpeedController", ""){
            FieldEntry field1 = new FieldEntry("speedctrl.basefps", "@settings.main.speedctrl.basefps", String.valueOf(60), TextField.TextFieldFilter.digitsOnly, s -> Strings.parseInt(s) >= 10 && Strings.parseInt(s) <= 600, null);
            ChooseEntry choose3 = new ChooseEntry("speedctrl.cutoff", "@settings.main.speedctrl.cutoff", new String[]{"25","50", "100", "200", "300"}, s -> String.valueOf(Strings.parseInt(s)/100f), null);
            {
                setDefaultHeader("@settings.main.speedctrl");
                builder = t -> {
                    field1.build(t);
                    t.row();
                    choose3.build(t);
                };
            }
        });

        settings.add(new CollapseGroupEntry("InputExtension", ""){
            CheckEntry check1 = new CheckEntry("inputReplace", "@settings.main.inputReplace", false, b -> {
                if(b){
                    if(Vars.mobile){
                        MobileInputExt.mobileExt.replaceInput();
                    }else{
                        DesktopInputExt.desktopExt.replaceInput();
                    }
                }
            });
            CheckEntry check2 = new CheckEntry("forceTapTile", "@settings.main.forceTapTile", false, null);
            CheckEntry check3 = new CheckEntry("edgePanning", "@settings.main.edgePanning", true, null);
            {
                collapsep = () -> !check1.value;
                headBuilder = t -> check1.build(t);
                builder = t -> {
                    check2.build(t);
                    t.row();
                    if(!Vars.mobile) check3.build(t);
                };
            }
        });

        settings.add(new FieldEntry("maxSchematicSize", "@settings.main.maxSchematicSize", String.valueOf(32), TextField.TextFieldFilter.digitsOnly, s -> Strings.parseInt(s) >= 16 && Strings.parseInt(s) <= 512, s -> Vars.maxSchematicSize = Mathf.clamp(Strings.parseInt(s), 16, 512)));

        settings.add(new FieldEntry("blockSelectTableHeight", "@settings.main.blockSelectTableHeight", String.valueOf(194), TextField.TextFieldFilter.digitsOnly, s -> Strings.parseInt(s) >= 100 && Strings.parseInt(s) <= 1000, null));

        settings.add(new FieldEntry("rtsFormDoubleTap", "@settings.main.rtsFormDoubleTap", "300", TextField.TextFieldFilter.digitsOnly, s -> Strings.parseInt(s) > 0, s -> RtsCommand.doubleTapInterval = Strings.parseInt(s)));

        settings.add(new CheckEntry("modifyBlockBars", "@settings.main.modifyBlockBars", false, null));
        settings.add(new CollapseGroupEntry("HoverTableExtension", ""){
            CheckEntry check1 = new CheckEntry("modifyTopTable", "@settings.main.modifyTopTable", false, null);
            CheckEntry check2 = new CheckEntry("topTableFollowMouse", "@settings.main.topTableFollowMouse", false, null);
            {
                collapsep = () -> !check1.value;
                headBuilder = t -> check1.build(t);
                builder = t -> check2.build(t);
            }
        });

        settings.add(new CheckEntry("enableUpdate", "@settings.main.enableUpdate", true, b -> {
            if(b) MI2Utilities.checkUpdate();
        }));

        settings.add(new CheckEntry("showUIContainer", "@settings.main.container", false, b -> container.addTo(b?Core.scene.root:null)));
    }
}
