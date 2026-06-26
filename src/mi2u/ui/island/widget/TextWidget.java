package mi2u.ui.island.widget;

import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.ui.island.*;
import mindustry.ui.*;

public class TextWidget implements WidgetContent{
    public String text;

    public TextWidget(String text){
        this.text = text;
    }

    @Override
    public void rebuild(Island island) {
        island.background(Styles.black3);
        island.add(text).labelAlign(Align.center);
    }

    @Override
    public void buildSettingsTable(Table table, Island island) {
        table.clear();
        table.add("修改名称");
        table.field(text, str -> {
            text = str;
            island.rebuild();
        }).growX();
    }
}
