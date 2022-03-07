package mi2u;

import arc.scene.ui.TextButton.*;
import arc.struct.*;
import mi2u.ui.*;

import mindustry.ui.*;

public class MI2UVars {
    public static float titleButtonSize = 28f;
    public static TextButtonStyle textb = Styles.cleart, textbtoggle = Styles.clearToggleMenut;

    public static Seq<Mindow2> mindow2s = new Seq<Mindow2>();

    public static MI2UI mi2ui = new MI2UI();
    public static EmojiMindow emojis = new EmojiMindow();
    public static CoreInfoMindow coreInfo = new CoreInfoMindow();
    public static LogicHelperMindow logicHelper = new LogicHelperMindow();
    public static CustomContainerMindow container = new CustomContainerMindow();
    public static MinimapMindow mindowmap = new MinimapMindow();
    public static MapInfoMindow mapinfo = new MapInfoMindow();

    public static boolean enDistributionReveal = false;

    public static void init(){
        //Styles
        Mindow2.init();

        //add to mindow2s
        mi2ui.registerName();
        emojis.registerName();
        coreInfo.registerName();
        logicHelper.registerName();
        mindowmap.registerName();
        mapinfo.registerName();

        //load ui
        mi2ui.loadUISettings();
        emojis.loadUISettings();
        coreInfo.loadUISettings();
        logicHelper.loadUISettings();
        mindowmap.loadUISettings();
        mapinfo.loadUISettings();
    }
}
