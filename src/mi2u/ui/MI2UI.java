package mi2u.ui;

import arc.Core;
import arc.func.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
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
        });
        
        cont.row();

        cont.table(tt -> {
            tt.button("@main.buttons.unitHpBar", textbtoggle, () -> {
                enUnitHealthBar = !enUnitHealthBar;
            }).update(b -> {
                b.setChecked(enUnitHealthBar);
            }).with(funcSetTextb);

            tt.button("@main.buttons.unitLogic", textbtoggle, () -> {
                enUnitLogic = !enUnitLogic;
            }).update(b -> {
                b.setChecked(enUnitLogic);
            }).with(funcSetTextb);

            tt.button("@main.buttons.unitLogicTimer", textbtoggle, () -> {
                enUnitLogicTimer = !enUnitLogicTimer;
            }).update(b -> {
                b.setChecked(enUnitLogicTimer);
            }).with(funcSetTextb);

            tt.button("@main.buttons.unitPath", textbtoggle, () -> {
                enUnitPath = !enUnitPath;
            }).update(b -> {
                b.setChecked(enUnitPath);
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
        });

    }
    
}
