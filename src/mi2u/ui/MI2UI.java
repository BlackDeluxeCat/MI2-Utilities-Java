package mi2u.ui;

import arc.Core;
import arc.func.*;
import arc.graphics.Color;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.entities.units.AIController;
import mindustry.gen.*;

import static mi2u.MI2UVars.*;
import static mi2u.MI2UFuncs.*;

public class MI2UI extends Mindow2{
    public static Cons<TextButton> funcSetTextb = c -> {
        c.getLabel().setAlignment(Align.left);
        c.getLabel().setWrap(false);
        c.getLabelCell().pad(2);
    };

    public MI2UI() {
        super("@main.MI2U", "@main.help");
        closable = false;
        mindowName = "MI2UI";
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        cont.table(tt -> {
            tt.button("" + Iconc.refresh, textb, () -> {
                Call.sendChatMessage("/sync");
            }).minHeight(36f).with(funcSetTextb);
        
            tt.button("@main.buttons.rebuild", textb, () -> {
                unitRebuildBlocks();
            }).minHeight(36f).with(funcSetTextb);

            tt.button("" + Iconc.zoom + Iconc.blockJunction, textbtoggle, () -> {
                enDistributionReveal = !enDistributionReveal;
            }).update(b -> {
                b.setChecked(enDistributionReveal);
            }).minHeight(36f).with(funcSetTextb);
        });

        cont.row();

        cont.table(tt -> {
            tt.add("AI");
            fullAI.modes.each(mode -> {
                tt.button(mode.btext, textbtoggle, () -> {
                    mode.enable = !mode.enable;
                }).update(b -> {
                    b.setChecked(mode.enable);
                }).minSize(30f).with(c -> {
                    c.getLabel().setAlignment(Align.center);
                });
            });
        });
        
        cont.row();
        cont.image().color(Color.pink).growX().height(2f);
        cont.row();
        cont.table(tt -> {
            tt.button("@main.buttons.container", textbtoggle, () -> {
                container.addTo(container.hasParent() ? null : Core.scene.root);
            }).minHeight(36f).update(b -> {
                b.setChecked(container.hasParent());
            }).with(funcSetTextb);
        });

    }

    @Override
    public void initSettings(){
        super.initSettings();
        settings.add(new CheckSettingEntry("enableUpdate", "@settings.main.enableUpdate"));
        settings.add(new CheckSettingEntry("showEmojis", "@settings.main.emoji", b -> emojis.addTo(b?Core.scene.root:null)));
        settings.add(new CheckSettingEntry("showCoreInfo", "@settings.main.coreInfo", b -> coreInfo.addTo(b?Core.scene.root:null)));
        settings.add(new CheckSettingEntry("showMindowMap", "@settings.main.mindowMap", b -> mindowmap.addTo(b?Core.scene.root:null)));
        settings.add(new CheckSettingEntry("showMapInfo", "@settings.main.mapInfo", b -> mapinfo.addTo(b?Core.scene.root:null)));
        settings.add(new CheckSettingEntry("showLogicHelper", "@settings.main.logicHelper", b -> logicHelper.addTo(b?Vars.ui.logic:null)));
        settings.add(new CheckSettingEntry("enPlayerCursor", "@settings.main.playerCursor"));
        settings.add(new CheckSettingEntry("enOverdriveZone", "@settings.main.overdriveZone"));
        settings.add(new CheckSettingEntry("disableWreck", "@settings.main.disableWreck"));
        settings.add(new CheckSettingEntry("disableUnit", "@settings.main.disableUnit"));
        settings.add(new CheckSettingEntry("disableBullet", "@settings.main.disableBullet"));
        settings.add(new CheckSettingEntry("disableBuilding", "@settings.main.disableBuilding"));
        settings.add(new CheckSettingEntry("enUnitHpBar", "@settings.main.unitHpBar"));
        settings.add(new CheckSettingEntry("enUnitLogic", "@settings.main.unitLogic"));
        settings.add(new CheckSettingEntry("enUnitLogicTimer", "@settings.main.unitLogicTimer"));
        settings.add(new CheckSettingEntry("enUnitPath", "@settings.main.unitPath"));
        settings.add(new CheckSettingEntry("modifyBlockBars", "@settings.main.modifyBlockBars"));
        settings.add(new CheckSettingEntry("modifyFilters", "@settings.main.modifyMapFilters"));
        settings.add(new CheckSettingEntry("inputReplace", "@settings.main.inputReplace"));
        settings.add(new CheckSettingEntry("forceTapTile", "@settings.main.forceTapTile"));
    }
}
