package mi2u;

import arc.Core;
import arc.func.*;
import arc.math.geom.QuadTree;
import arc.math.geom.Rect;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.struct.*;
import arc.util.*;
import mi2u.ai.*;
import mi2u.ui.*;
import mi2u.ui.elements.*;
import mi2u.uiExtend.*;
import mindustry.ui.*;

import static mindustry.Vars.state;
import static mindustry.Vars.ui;

public class MI2UVars{
    public static float titleButtonSize = 32f;
    public static TextButtonStyle textb = Styles.flatt, textbtoggle = Styles.flatTogglet;
    public static Cons<TextButton> funcSetTextb = c -> {
        c.getLabel().setAlignment(Align.center);
        c.getLabel().setWrap(false);
        c.margin(6f);
    };

    public static Drawable fadeBackground;

    public static Seq<Mindow2> mindow2s = new Seq<>();
    public static QuadTree<SnapElement> snapTables = new QuadTree<>(new Rect(0, 0, Core.scene.getWidth(), Core.scene.getHeight()));

    public static FullAI fullAI = new FullAI();

    public static MI2UI mi2ui = new MI2UI();
    public static EmojiMindow emojis = new EmojiMindow();
    public static CoreInfoMindow coreInfo = new CoreInfoMindow();
    public static LogicHelperMindow logicHelper = new LogicHelperMindow();
    public static MinimapMindow mindowmap = new MinimapMindow();

    public static Seq<SnapWindow> windows = new Seq<>();
    public static TestTable1 testTable1 = new TestTable1();
    public static TestTable2 testTable2 = new TestTable2();
    public static TestTable3 testTable3 = new TestTable3();
    public static TestTable4 testTable4 = new TestTable4();


    public static void initTables(){
        testTable1.addTo(Core.scene.root);
        testTable1.visible(() -> state.isGame() && ui.hudfrag.shown);
        testTable2.addTo(Core.scene.root);
        testTable2.visible(() -> state.isGame() && ui.hudfrag.shown);
        testTable3.addTo(Core.scene.root);
        testTable3.visible(() -> state.isGame() && ui.hudfrag.shown);
        testTable4.addTo(Core.scene.root);
        testTable4.visible(() -> state.isGame() && ui.hudfrag.shown);
    }
}
