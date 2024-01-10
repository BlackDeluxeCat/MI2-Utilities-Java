package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.ui.elements.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mi2u.MI2UVars.*;

import java.lang.reflect.*;

public class EmojiMindow extends Mindow2{
    public static TextButtonStyle textbStyle = Styles.nonet;
    public static Cons<TextButton> funcSetTextb = c -> {
        c.getLabel().setAlignment(Align.left);
        c.getLabel().setWrap(false);
        c.getLabelCell().pad(2);
    };

    public Table listt, tablet;
    public boolean listMode = false;
    private int tmpindex = 0;

    public EmojiMindow() {
        super("Emojis", "@emoji.MI2U", "@emoji.help");

        var map = new ObjectMap<String, String>();
        listt = new Table();
        tablet = new Table();
        tablet.background(Styles.black6);

        try{
            map.clear();
            Field[] fs = Iconc.class.getFields();
            for(Field f : fs){
                if(f.getType() == char.class){
                    map.put(f.getName(), Reflect.get(Iconc.class, f) + "");
                }
            }
            var keyseq = map.keys().toSeq();
            keyseq.sort();

            tmpindex = 0;
            for(var name : keyseq){
                var emoji = map.get(name);
                tablet.button(emoji, textb, () -> {
                    Core.app.setClipboardText(emoji);
                }).with(funcSetTextb).get().getLabel().setFontScale(1.5f);
                if(++tmpindex > 8){
                    tablet.row();
                    tmpindex = 0;
                }
            }

            keyseq.each(name -> {
                listt.button(name, textbStyle, () -> {
                    Core.app.setClipboardText(name);
                }).growX().with(funcSetTextb);
                var emoji = map.get(name);
                listt.button(emoji, textb, () -> {
                    Core.app.setClipboardText(emoji);
                }).with(funcSetTextb).get().getLabel().setFontScale(1.5f);
                listt.row();
            });
        }catch(Exception ignore){Log.err(ignore);}

        titlePane.table(t -> {
            t.button("" + Iconc.list, textbtoggle, () -> {
                listMode = !listMode;
                rebuild();
            }).height(titleButtonSize).update(b -> {
                b.setChecked(listMode);
            }).growX();
        }).grow().minWidth(32f);
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        cont.table(t -> {
            //TODO search bar

            t.pane(listMode ? listt : tablet).maxHeight(Core.graphics.getHeight() / 3f).growX().update(p -> {
                Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                if(e != null && e.isDescendantOf(p)){
                    p.requestScroll();
                }else if(p.hasScroll()){
                    Core.scene.setScrollFocus(null);
                }
            });
        });
    }
}
