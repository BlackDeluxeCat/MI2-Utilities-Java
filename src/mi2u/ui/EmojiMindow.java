package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.*;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.io.SettingHandler;
import mi2u.ui.elements.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.ui.*;

import java.lang.reflect.*;

import static arc.Core.bundle;
import static mi2u.MI2UVars.*;
import static mindustry.Vars.ui;

public class EmojiMindow extends Mindow2{
    public static Cons<TextButton> funcSetTextButton = c -> {
        c.getLabel().setAlignment(Align.left);
        c.getLabel().setWrap(false);
    };

    public static Cons<ImageButton> funcSetImageButton = b -> {
        b.getImageCell().size(32).pad(4).scaling(Scaling.fit);
    };

    public StringMap emojiMap = new StringMap();
    public Seq<String> orderedKey = new Seq<>();
    public OrderedMap<String, TextureRegion> regionMap = new OrderedMap<>();

    public Table listT, iconT;
    public TextField search;
    public boolean listMode = false;

    public String filter = "";

    public EmojiMindow() {
        super("Emojis");

        updateEmoji();

        listT = new Table();
        iconT = new Table();

        listT.marginLeft(4f).marginRight(4f);
        iconT.marginLeft(4f).marginRight(4f);

        rebuildEmojiTable();

        titlePane.table(t -> t.button(Iconc.list + "  " + bundle.get("emoji.switchDisplay"), Styles.nonet, () -> {
            listMode = !listMode;
            rebuild();
        }).height(titleButtonSize).update(b -> b.setChecked(listMode)).growX()).grow().minWidth(32f);
    }

    public void rebuildEmojiTable(){
        int count = 0;
        int emojiPerRow = settings.getInt("emojiPerRow", 9);

        listT.clear();
        iconT.clear();

        for (String key : regionMap.keys()) {
            String emoji = emojiMap.get(key);
            if (filter.isEmpty() || key.toLowerCase().contains(filter.toLowerCase()) || filter.contains(emoji)) {
                TextureRegionDrawable region = new TextureRegionDrawable(regionMap.get(key));

                count++;

                iconT.button(region, Styles.clearNonei, () -> copy(emoji)).with(funcSetImageButton).tooltip(key);
                if (count % emojiPerRow == 0) iconT.row();
                listT.button(key, Styles.nonet, () -> copy(key)).growX().with(funcSetTextButton).width((emojiPerRow - 1) * 40f);
                listT.button(region, Styles.clearNonei, () -> copy(emoji)).with(funcSetImageButton);
                listT.row();
            }
        }
        if (count == 0){
            iconT.center().label(() -> "[lightgray]"+Iconc.cancel + " No Result[]").width(emojiPerRow * 40f).height(40f);
            listT.center().label(() -> "[lightgray]"+Iconc.cancel + " No Result[]").width(emojiPerRow * 40f).height(40f);
        }else if (count < emojiPerRow){
            for (int i = 0; i < emojiPerRow - count; i++){
                iconT.image().color(Color.clear).size(40f);
            }
        }
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        cont.table(t -> {
            t.table(s -> {
                s.image(Icon.zoom).size(40f).padLeft(4f);
                search = s.field(filter, text -> {
                    filter = text == null ? "" : text;
                    rebuildEmojiTable();
                }).padBottom(4).left().growX().get();
                search.setMessageText("@players.search");
            }).growX().padRight(8f).row();

            t.pane(listMode ? listT : iconT).maxHeight(Core.graphics.getHeight() / 3f).growX().update(p -> {
                p.setStyle(Styles.smallPane);
                p.setScrollingDisabled(true, false);
                Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                if(e != null && e.isDescendantOf(p)){
                    p.requestScroll();
                }else if(p.hasScroll()){
                    Core.scene.setScrollFocus(null);
                }
            });
        });
    }

    @Override
    public void initSettings(){
        super.initSettings();
        settings.sliderPref("emojiPerRow", 9, 6, 12, 1, i -> "" + i, i -> rebuildEmojiTable());
    }

    private void updateEmoji(){
        for(Field f : Iconc.class.getFields()){
            if(f.getType() == char.class){
                emojiMap.put(f.getName(), Reflect.get(Iconc.class, f) + "");
            }
        }
        orderedKey = emojiMap.keys().toSeq();
        orderedKey.sort();

        for(String name : orderedKey){
            String key = Fonts.unicodeToName(emojiMap.get(name).charAt(0));
            if (key != null){
                TextureRegion region = Fonts.getLargeIcon(key);
                regionMap.put(name, region);
            }
        }

    }

    private void copy(String text){
        Core.app.setClipboardText(text);
        ui.showInfoFade("@copied");
    }
}
