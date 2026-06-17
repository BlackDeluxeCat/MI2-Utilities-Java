package mi2u.ui.island.widget;

import arc.scene.ui.layout.*;
import mi2u.ui.island.*;
import mindustry.ui.*;

public class TextWidget extends Table implements WidgetContent{
    @Override
    public String name() {
        return name;
    }

    @Override
    public void rebuild(Island island) {
        island.background(Styles.black3);
        island.add(name);
    }

    @Override
    public void buildSettingsTable(Table table, Island island) {
        table.clear();
        table.add("修改名称");
        table.field(name, str -> {
            name = str;
            island.rebuild();
        }).growX();
    }
}
