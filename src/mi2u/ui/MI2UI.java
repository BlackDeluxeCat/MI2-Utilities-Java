package mi2u.ui;

import arc.Core;
import arc.func.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.io.MI2USettings;
import mindustry.gen.*;

import static mi2u.MI2UVars.*;

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
            tt.button("" + Iconc.save, textb, () -> {
                MI2USettings.save();
            }).with(funcSetTextb).padRight(12f);

            tt.button("" + Iconc.refresh, textb, () -> {
                Call.sendChatMessage("/sync");
            }).with(funcSetTextb);
        
            tt.button("@main.buttons.rebuild", textb, () -> {
                unitRebuildBlocks();
            }).with(funcSetTextb);
        });
        
        cont.row();

        cont.table(tt -> {
            tt.button("@main.buttons.unitHpBar", textbtoggle, () -> {
                MI2USettings.putBool("enUnitHpBar", !MI2USettings.getBool("enUnitHpBar"));
            }).update(b -> {
                b.setChecked(MI2USettings.getBool("enUnitHpBar"));
            }).with(funcSetTextb);

            tt.button("@main.buttons.unitLogic", textbtoggle, () -> {
                MI2USettings.putBool("enUnitLogic", !MI2USettings.getBool("enUnitLogic"));
            }).update(b -> {
                b.setChecked(MI2USettings.getBool("enUnitLogic"));
            }).with(funcSetTextb);

            tt.button("@main.buttons.unitLogicTimer", textbtoggle, () -> {
                MI2USettings.putBool("enUnitLogicTimer", !MI2USettings.getBool("enUnitLogicTimer"));
            }).update(b -> {
                b.setChecked(MI2USettings.getBool("enUnitLogicTimer"));
            }).with(funcSetTextb);

            tt.button("@main.buttons.unitPath", textbtoggle, () -> {
                MI2USettings.putBool("enUnitPath", !MI2USettings.getBool("enUnitPath"));
            }).update(b -> {
                b.setChecked(MI2USettings.getBool("enUnitPath"));
            }).with(funcSetTextb);
        });
        
        cont.row();

        cont.table(rqb -> {      
            rqb.button("@main.buttons.mapInfo", textb, () -> {
                //mapInfo.show();
            }).with(funcSetTextb);
        });

        cont.row();

        //mindow buttons
        cont.table(tt -> {
            tt.button("@main.buttons.coreInfo", textbtoggle, () -> {
                coreInfo.addTo(coreInfo.hasParent() ? null : Core.scene.root);
            }).update(b -> {
                b.setChecked(coreInfo.hasParent());
            }).with(funcSetTextb);

            tt.button("@main.buttons.emoji", textbtoggle, () -> {
                emojis.addTo(emojis.hasParent() ? null : Core.scene.root);
            }).update(b -> {
                b.setChecked(emojis.hasParent());
            }).with(funcSetTextb);

            tt.button("@main.buttons.container", textbtoggle, () -> {
                container.addTo(container.hasParent() ? null : Core.scene.root);
            }).update(b -> {
                b.setChecked(container.hasParent());
            }).with(funcSetTextb);
        });

    }
    
    @Override
    public boolean loadSettingsRaw(){
        if(!super.loadSettingsRaw()) return false;
        if(MI2USettings.getBool(mindowName + ".show.emojis")) emojis.addTo(emojis.hasParent() ? null : Core.scene.root);
        if(MI2USettings.getBool(mindowName + ".show.coreInfo")) coreInfo.addTo(coreInfo.hasParent() ? null : Core.scene.root);
        return true;
    }

    @Override
    public boolean saveSettings(){
        if(!super.saveSettings()) return false;
        MI2USettings.putBool(mindowName + ".show.emojis", emojis.hasParent());
        MI2USettings.putBool(mindowName + ".show.coreInfo", coreInfo.hasParent());
        return true;
    }

}
