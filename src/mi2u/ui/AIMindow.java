package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.ai.*;
import mi2u.ui.elements.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mi2u.MI2UVars.*;

public class AIMindow extends Mindow2{
    public AIMindow(){
        super("AI");
        setVisibleInGame();

        titlePane.defaults().growX().pad(2f).height(buttonSize);
        titlePane.button("Add New", textb, () -> {
            new BaseDialog(""){{
                addCloseButton();
                cont.pane(p -> {
                    p.defaults().pad(2f);
                    FullAI.all.each(meta -> {
                        p.image().width(2f).growY();
                        p.button(meta.name, textb, () -> {
                            fullAI.modes.add(meta.prov.get());
                            rebuild();
                            hide();
                        }).with(tb -> {
                            tb.getLabelCell().color(Color.cyan).fontScale(1.3f);
                        }).size(360f, 200f);
                    });
                    p.row();
                    FullAI.all.each(meta -> {
                        p.image().width(2f).growY();
                        p.pane(t -> t.labelWrap(meta.intro).grow().labelAlign(Align.topLeft)).grow().with(pp -> pp.setScrollingDisabledX(true));
                    });
                }).with(p -> {
                    p.setScrollingDisabledY(true);
                });
                show();
            }};
        });

        titlePane.button("Save & Close", textb, () -> {
            fullAI.saveModes();
            close();
        });

        titlePane.button("Close", textb, this::close);
    }

    @Override
    public void setupCont(Table cont){
        fullAI.modeFlush();
        cont.clear();
        cont.margin(2f).setBackground(Styles.black8);

        cont.table(t -> {

        }).growX();

        cont.row();

        cont.pane(p -> {
            p.table(t -> {
                t.name = "cfg";
                for(var mode : fullAI.modes){
                    t.table(mode::buildTitle).growX().get().setBackground(Styles.grayPanel);
                    t.row();
                    t.add(new MCollapser(tt -> {
                        mode.buildConfig(tt);
                        tt.marginBottom(16f);
                    }, true).setCollapsed(false, () -> !mode.configUIExpand).setDirection(false, true)).growX();
                    t.row();
                }
            }).growX();
        }).maxHeight(Core.graphics.getHeight() / 2f / Scl.scl()).width(440f).update(p -> {
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(p)){
                p.requestScroll();
            }else if(p.hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        });
    }
}
