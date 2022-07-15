package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.scene.Element;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.MI2UFuncs;
import mi2u.MI2Utilities;
import mi2u.game.SpeedController;
import mi2u.io.MI2USettings.*;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.ui.Styles;

import static mi2u.MI2UVars.*;
import static mi2u.MI2UFuncs.*;

public class MI2UI extends Mindow2{

    public MI2UI(){
        super("@main.MI2U", "@main.help");
    }

    @Override
    public void init(){
        super.init();
        closable = false;
        mindowName = "MI2UI";
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();

        cont.table(tt -> {
            tt.button("" + Iconc.refresh, textb, () -> {
                Call.sendChatMessage("/sync");
            }).minSize(36f).with(funcSetTextb);
        
            tt.button("@main.buttons.rebuild", textb, MI2UFuncs::unitRebuildBlocks).minSize(36f).with(funcSetTextb);

            tt.button("" + Iconc.zoom + Iconc.blockJunction, textbtoggle, () -> {
                enDistributionReveal = !enDistributionReveal;
            }).update(b -> {
                b.setChecked(enDistributionReveal);
            }).minSize(36f).with(funcSetTextb);
        });

        cont.row();

        cont.table(tt -> {
            tt.button("AI\n" + Iconc.settings, textb, () -> {
                var popup = new PopupTable();
                popup.addCloseButton();
                popup.addDragMove();
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
            }).growY().minSize(36f).with(funcSetTextb);
            tt.table(ttt -> {
                fullAI.modes.each(mode -> {
                    ttt.button(mode.btext, textbtoggle, () -> {
                        mode.enable = !mode.enable;
                    }).update(b -> {
                        b.setChecked(mode.enable);
                    }).minSize(36f).with(c -> {
                        c.getLabel().setAlignment(Align.center);
                    });
                });
            });
        }).growX();

        cont.row();

        //TODO the update rate is based on button.update(), and affected by lagging
        cont.table(t -> {
            t.button("Speeding", textbtoggle, SpeedController::switchUpdate).update(b -> {
                b.setChecked(SpeedController.update);
                b.setText(Core.bundle.get("main.buttons.speeding") + "x" + Strings.autoFixed(SpeedController.scl, 2));
                SpeedController.update();
            }).with(funcSetTextb).with(b -> {
                b.margin(4f);
                b.image(Icon.settingsSmall).size(16f).update(img -> {
                    if(SpeedController.update) img.rotateBy(-1f);
                    else img.setRotation(0f);
                    img.setColor(!b.isChecked() ? Color.white : SpeedController.lowerThanMin() ? Color.scarlet : Color.lime);
                });
            }).growX();
        }).name("speed control").growX();

    }

    @Override
    public void initSettings(){
        super.initSettings();

        settings.add(new CheckEntry("showEmojis", "@settings.main.emoji", false, b -> emojis.addTo(b?Core.scene.root:null)));
        settings.add(new CheckEntry("showCoreInfo", "@settings.main.coreInfo", false, b -> coreInfo.addTo(b?Core.scene.root:null)));
        settings.add(new CheckEntry("showMindowMap", "@settings.main.mindowMap", false, b -> mindowmap.addTo(b?Core.scene.root:null)));
        settings.add(new CheckEntry("showMapInfo", "@settings.main.mapInfo", false, b -> mapinfo.addTo(b?Core.scene.root:null)));
        settings.add(new CheckEntry("showLogicHelper", "@settings.main.logicHelper", true, b -> logicHelper.addTo(b?Vars.ui.logic:null)));
        settings.add(new CheckEntry("showUIContainer", "@settings.main.container", false, b -> container.addTo(b?Core.scene.root:null)));

        settings.add(new CheckEntry("enPlayerCursor", "@settings.main.playerCursor", true, null));
        settings.add(new CheckEntry("enOverdriveZone", "@settings.main.overdriveZone", false, null));
        settings.add(new CheckEntry("enMenderZone", "@settings.main.menderZone", false, null));
        settings.add(new CheckEntry("enSpawnZone", "@settings.main.spawnZone", true, null));

        settings.add(new CheckEntry("disableWreck", "@settings.main.disableWreck", false, null));
        settings.add(new CheckEntry("disableUnit", "@settings.main.disableUnit", false, null));
        settings.add(new CheckEntry("disableBullet", "@settings.main.disableBullet", false, null));
        settings.add(new CheckEntry("disableBuilding", "@settings.main.disableBuilding", false, null));

        settings.add(new CheckEntry("enUnitHpBar", "@settings.main.unitHpBar", false, null));
        settings.add(new CheckEntry("enUnitLogic", "@settings.main.unitLogic", false, null));
        settings.add(new CheckEntry("enUnitLogicTimer", "@settings.main.unitLogicTimer", false, null));
        settings.add(new CheckEntry("enUnitPath", "@settings.main.unitPath", false, null));

        settings.add(new CollapseGroupEntry("SpeedController", ""){
            ChooseEntry choose1 = new ChooseEntry("speedctrl.basefps", "@settings.main.speedctrl.basefps", new String[]{"10", "20", "30", "60", "120", "144"}, null);
            ChooseEntry choose3 = new ChooseEntry("speedctrl.cutoff", "@settings.main.speedctrl.cutoff", new String[]{"50", "100", "200", "300"}, s -> String.valueOf(Strings.parseInt(s)/100f));
            {
                setDefaultHeader("@settings.main.speedctrl");
                builder = t -> {
                    choose1.build(t);
                    t.row();
                    choose3.build(t);
                };
            }
        });

        settings.add(new CollapseGroupEntry("InputExtension", ""){
            CheckEntry check1 = new CheckEntry("inputReplace", "@settings.main.inputReplace", false, null);
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

        settings.add(new CheckEntry("modifyBlockBars", "@settings.main.modifyBlockBars", false, null));
        settings.add(new CheckEntry("modifyTopTable", "@settings.main.modifyTopTable", false, null));
        settings.add(new CheckEntry("modifyFilters", "@settings.main.modifyMapFilters", true, null));

        settings.add(new CollapseGroupEntry("UpdateCheck", ""){
            CheckEntry check1 = new CheckEntry("enableUpdate", "@settings.main.enableUpdate", true, b -> {
                if(b) MI2Utilities.checkUpdate();
            });
            FieldEntry field2 = new FieldEntry("ghApi", "@settings.main.ghApi", Vars.ghApi, null, null, null);

            {
                setDefaultHeader("@settings.main.updateCheck");
                builder = t -> {
                    check1.build(t);
                    t.row();
                    field2.build(t);
                };
            }
        });
    }
}
