package mi2u;

import arc.func.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.struct.*;
import arc.util.*;
import mi2u.ai.*;
import mi2u.ui.*;
import mi2u.ui.elements.*;
import mindustry.ui.*;

public class MI2UVars{
    public static float buttonSize = 32f;
    public static TextButtonStyle textb = Styles.cleart, textbtoggle = new TextButtonStyle(Styles.clearTogglet){{
        up = Styles.none;
    }};
    public static Cons<TextButton> funcSetTextb = c -> {
        c.getLabel().setAlignment(Align.center);
        c.getLabel().setWrap(false);
        c.margin(6f);
    };

    public static Drawable fadeBackground;

    public static Seq<Mindow2> mindow2s = new Seq<>();

    public static FullAI fullAI = new FullAI();

    public static MI2UI mi2ui = new MI2UI();
    public static EmojiMindow emojis = new EmojiMindow();
    public static CoreInfoMindow coreInfo = new CoreInfoMindow();
    public static LogicHelperMindow logicHelper = new LogicHelperMindow();
    public static MinimapMindow mindowmap = new MinimapMindow();

    public static WaveInfoMindow waveInfo = new WaveInfoMindow();
    public static AIMindow aiMindow = new AIMindow();
    public static WorldFinderMindow finderMindow = new WorldFinderMindow();
    public static MonitorMindow monitorMindow = new MonitorMindow();

    public static MapInfoDialog mapInfo = new MapInfoDialog();
}
