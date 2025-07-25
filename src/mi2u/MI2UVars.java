package mi2u;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.struct.*;
import arc.util.*;
import mi2u.ai.*;
import mi2u.ui.*;
import mi2u.ui.elements.*;
import mindustry.gen.*;
import mindustry.ui.*;

public class MI2UVars{
    public static float buttonSize = 32f;
    public static Drawable mindowTitleBarBackground, monitorTitleBarBackground;
    public static TextButtonStyle textb = Styles.cleart, textbtoggle = new TextButtonStyle(Styles.clearTogglet){{
        up = Styles.none;
    }};
    public static Cons<TextButton> funcSetTextb = c -> {
        c.getLabel().setAlignment(Align.center);
        c.getLabel().setWrap(false);
        c.margin(6f);
    };
    public static Cons<Element> funcSetScrollFocus = p -> {
        Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
        if(e != null && e.isDescendantOf(p)){
            p.requestScroll();
        }else if(p.hasScroll()){
            Core.scene.setScrollFocus(null);
        }
    };

    public static Drawable fadeBackground;

    public static Seq<Mindow2> mindow2s = new Seq<>();

    public static FullAI fullAI = new FullAI();

    public static MI2UI mi2ui;
    public static EmojiMindow emojis;
    public static CoreInfoMindow coreInfo;
    public static LogicHelperMindow logicHelper;
    public static MinimapMindow mindowmap;

    public static WaveInfoMindow waveInfo;
    public static AIMindow aiMindow;
    public static WorldFinderMindow finderMindow;
    public static MonitorCanvas monitorCanvas;

    public static MapInfoDialog mapInfo;

    public static void init(){
        var whiteui = (TextureRegionDrawable)Tex.whiteui;
        mindowTitleBarBackground = whiteui.tint(1f, 0.1f, 0.2f, 0.3f);
        monitorTitleBarBackground = whiteui.tint(0.1f, 0.2f, 1f, 0.3f);

        Pixmap fade = new Pixmap(128, 128);
        for(int x = 0; x < fade.width; x++){
            for(int y = 0; y < fade.height; y++){
                FloatFloatf func = f -> Mathf.pow(f, 0.6f);
                fade.set(x, y, MI2UTmp.c2.set(Color.black).a(0.4f * func.get(
                    Math.min(x, Math.min(y, Math.min(fade.width-x-1, fade.height-y-1))) / (fade.width/2f)
                )));
            }
        }
        Core.atlas.addRegion("fadeBackground", new TextureRegion(new Texture(fade)));
        fadeBackground = new TextureRegionDrawable(Core.atlas.find("fadeBackground"));

        mi2ui = new MI2UI();
        emojis = new EmojiMindow();
        coreInfo = new CoreInfoMindow();
        logicHelper = new LogicHelperMindow();
        mindowmap = new MinimapMindow();

        waveInfo = new WaveInfoMindow();
        aiMindow = new AIMindow();
        finderMindow = new WorldFinderMindow();
        monitorCanvas = new MonitorCanvas();

        mapInfo = new MapInfoDialog();
    }
}
