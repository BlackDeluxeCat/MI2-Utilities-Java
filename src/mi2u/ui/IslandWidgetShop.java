package mi2u.ui;

import arc.func.*;
import arc.scene.ui.layout.*;
import mi2u.ui.island.*;
import mi2u.ui.island.widget.*;

import static mi2u.MI2UVars.funcSetTextb;
import static mi2u.MI2UVars.textb;

public class IslandWidgetShop extends Table {
    public Cons<Island> onCreate;

    public IslandWidgetShop(){
        rebuild();
    }

    public void setOnCreate(Cons<Island> onCreate){
        this.onCreate = onCreate;
    }

    public void click(Island island){
        onCreate.get(island);
    }

    /** 重建组件商城 */
    public void rebuild(){
        clear();
        button("测试文本", textb, () -> {
            var widget = new TextWidget();
            widget.name = "Cat Rin";
            var island = new Island("TestText", widget);
            click(island);
        }).with(funcSetTextb);
    }
}
