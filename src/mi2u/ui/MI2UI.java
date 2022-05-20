package mi2u.ui;

import arc.Core;
import arc.func.*;
import arc.graphics.Color;
import arc.scene.actions.Actions;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.MI2UTmp;
import mi2u.io.MI2USettings.*;
import mindustry.Vars;
import mindustry.entities.units.AIController;
import mindustry.gen.*;
import mindustry.ui.Styles;

import static mi2u.MI2UVars.*;
import static mi2u.MI2UFuncs.*;

public class MI2UI extends Mindow2{
    public static Cons<TextButton> funcSetTextb = c -> {
        c.getLabel().setAlignment(Align.left);
        c.getLabel().setWrap(false);
        c.getLabelCell().pad(2);
    };

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
        settings.add(new CheckEntry("enableUpdate", "@settings.main.enableUpdate", false, null));

        settings.add(new CheckEntry("showEmojis", "@settings.main.emoji", false, b -> emojis.addTo(b?Core.scene.root:null)));
        settings.add(new CheckEntry("showCoreInfo", "@settings.main.coreInfo", false, b -> coreInfo.addTo(b?Core.scene.root:null)));
        settings.add(new CheckEntry("showMindowMap", "@settings.main.mindowMap", false, b -> mindowmap.addTo(b?Core.scene.root:null)));
        settings.add(new CheckEntry("showMapInfo", "@settings.main.mapInfo", false, b -> mapinfo.addTo(b?Core.scene.root:null)));
        settings.add(new CheckEntry("showLogicHelper", "@settings.main.logicHelper", true, b -> logicHelper.addTo(b?Vars.ui.logic:null)));

        settings.add(new CheckEntry("enPlayerCursor", "@settings.main.playerCursor", true, null));
        settings.add(new CheckEntry("enOverdriveZone", "@settings.main.overdriveZone", true, null));

        settings.add(new CheckEntry("disableWreck", "@settings.main.disableWreck", false, null));
        settings.add(new CheckEntry("disableUnit", "@settings.main.disableUnit", false, null));
        settings.add(new CheckEntry("disableBullet", "@settings.main.disableBullet", false, null));
        settings.add(new CheckEntry("disableBuilding", "@settings.main.disableBuilding", false, null));

        settings.add(new CheckEntry("enUnitHpBar", "@settings.main.unitHpBar", false, null));
        settings.add(new CheckEntry("enUnitLogic", "@settings.main.unitLogic", false, null));
        settings.add(new CheckEntry("enUnitLogicTimer", "@settings.main.unitLogicTimer", false, null));
        settings.add(new CheckEntry("enUnitPath", "@settings.main.unitPath", false, null));

        settings.add(new CollapseGroupEntry("InputExtension", ""){
            private CheckEntry check1 = new CheckEntry("inputReplace", "@settings.main.inputReplace", false, null);
            private CheckEntry check2 = new CheckEntry("forceTapTile", "@settings.main.forceTapTile", false, null);
            {
                collapsep = () -> !check1.value;
                headBuilder = t -> check1.build(t);
                builder = t -> check2.build(t);
            }
        });

        settings.add(new CheckEntry("modifyBlockBars", "@settings.main.modifyBlockBars", false, null));
        settings.add(new CheckEntry("modifyTopTable", "@settings.main.modifyTopTable", false, null));
        settings.add(new CheckEntry("modifyFilters", "@settings.main.modifyMapFilters", true, null));
    }
}
