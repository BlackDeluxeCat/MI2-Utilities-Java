package mi2u;

import arc.func.Cons;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextButton.*;
import arc.struct.*;
import arc.util.Align;
import mi2u.ai.*;
import mi2u.ui.*;
import mindustry.ui.*;

public class MI2UVars {
    public static float titleButtonSize = 28f;
    public static TextButtonStyle textb = Styles.cleart, textbtoggle = Styles.clearToggleMenut;
    public static Cons<TextButton> funcSetTextb = c -> {
        c.getLabel().setAlignment(Align.center);
        c.getLabel().setWrap(false);
        c.getLabelCell().pad(2);
    };

    public static Seq<Mindow2> mindow2s = new Seq<Mindow2>();

    public static FullAI fullAI = new FullAI();

    public static MI2UI mi2ui = new MI2UI();
    public static EmojiMindow emojis = new EmojiMindow();
    public static CoreInfoMindow coreInfo = new CoreInfoMindow();
    public static LogicHelperMindow logicHelper = new LogicHelperMindow();
    public static CustomContainerMindow container = new CustomContainerMindow();
    public static MinimapMindow mindowmap = new MinimapMindow();
    public static MapInfoMindow mapinfo = new MapInfoMindow();

    public static boolean enDistributionReveal = false;

}
