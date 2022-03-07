package mi2u.ui;

import arc.Core;
import arc.func.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
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
            }).with(funcSetTextb);
        
            tt.button("@main.buttons.rebuild", textb, () -> {
                unitRebuildBlocks();
            }).with(funcSetTextb);

            tt.button("" + Iconc.zoom + Iconc.blockJunction, textbtoggle, () -> {
                enDistributionReveal = !enDistributionReveal;
            }).update(b -> {
                b.setChecked(enDistributionReveal);
            }).with(funcSetTextb);
        });
        
        cont.row();

        cont.table(tt -> {
            tt.button("@main.buttons.container", textbtoggle, () -> {
                container.addTo(container.hasParent() ? null : Core.scene.root);
            }).update(b -> {
                b.setChecked(container.hasParent());
            }).with(funcSetTextb);
        });

    }

    @Override
    public void initSettings(){
        super.initSettings();
        settings.add(new CheckSettingEntry("showEmojis", "@settings.main.emoji", b -> emojis.addTo(b?Core.scene.root:null)));
        settings.add(new CheckSettingEntry("showCoreInfo", "@settings.main.coreInfo", b -> coreInfo.addTo(b?Core.scene.root:null)));
        settings.add(new CheckSettingEntry("showMindowMap", "@settings.main.mindowMap", b -> mindowmap.addTo(b?Core.scene.root:null)));
        settings.add(new CheckSettingEntry("showMapInfo", "@settings.main.mapInfo", b -> mapinfo.addTo(b?Core.scene.root:null)));
        settings.add(new CheckSettingEntry("enUnitHpBar", "@settings.main.unitHpBar"));
        settings.add(new CheckSettingEntry("enUnitLogic", "@settings.main.unitLogic"));
        settings.add(new CheckSettingEntry("enUnitLogicTimer", "@settings.main.unitLogicTimer"));
        settings.add(new CheckSettingEntry("enUnitPath", "@settings.main.unitPath"));
    }
}
