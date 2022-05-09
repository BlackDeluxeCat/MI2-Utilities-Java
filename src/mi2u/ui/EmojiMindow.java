package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.Blocks;
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

    public boolean listMode = false;
    private int tmpindex = 0;
    private IconCategory category = IconCategory.contents;
    private ObjectMap<String, String> map = new ObjectMap<String, String>();

    public EmojiMindow() {
        super("@emoji.MI2U", "@emoji.help");
        mindowName = "Emojis";
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        cont.table(t -> {
            t.table(tt -> {
                tt.button("" + Iconc.list, textbtoggle, () -> {
                    listMode = !listMode;
                    rebuild();
                }).size(titleButtonSize).update(b -> {
                    b.setChecked(listMode);
                });

                tt.button("" + Blocks.message.emoji(), textbtoggle, () -> {
                    category = IconCategory.contents;
                    rebuild();
                }).size(titleButtonSize).update(b -> {
                    b.setChecked(category == IconCategory.contents);
                });

                tt.button("" + Iconc.terrain, textbtoggle, () -> {
                    category = IconCategory.iconc;
                    rebuild();
                }).size(titleButtonSize).update(b -> {
                    b.setChecked(category == IconCategory.iconc);
                });
            });

            t.row();

            try{
                map.clear();
                if(category == IconCategory.contents){
                    map.putAll((ObjectMap<String, String>)Reflect.get(Fonts.class, "stringIcons")); 
                }else{
                    Field[] fs = Iconc.class.getFields();
                    for(Field f : fs){
                        if(f.getType() == char.class){
                            map.put(f.getName(), Reflect.get(Iconc.class, f) + "");
                        }
                    }
                }

                t.pane(tt -> {
                    if(listMode){
                        map.each((name, emoji) -> {
                            tt.button(name, textbStyle, () -> {
                                Core.app.setClipboardText(name);
                            }).growX().with(funcSetTextb);
                            tt.button(emoji, textbStyle, () -> {
                                Core.app.setClipboardText(emoji);
                            }).growX().with(funcSetTextb);
                            tt.row();
                        });
                    }else{
                        tmpindex = 0;
                        map.each((name, emoji) -> {
                            tt.button(emoji, textbStyle, () -> {
                                Core.app.setClipboardText(emoji);
                            }).growX().with(funcSetTextb);
                            if(++tmpindex > 8){
                                tt.row();
                                tmpindex = 0;
                            }
                        });
                    }
                }).maxHeight(Core.graphics.getHeight() / 3).growX().update(p -> {
                    Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                    if(e != null && e.isDescendantOf(p)){
                        p.requestScroll();
                    }else if(p.hasScroll()){
                        Core.scene.setScrollFocus(null);
                    }
                });
            }catch(Exception e){
                t.row();
                t.pane(tt-> {tt.add(e.toString());});
            }
        });
    }

    public enum IconCategory{
        contents, iconc;
    }
}
