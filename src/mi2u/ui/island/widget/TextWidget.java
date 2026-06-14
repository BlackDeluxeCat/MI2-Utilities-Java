package mi2u.ui.island.widget;

import arc.scene.ui.layout.*;
import mi2u.ui.island.*;

public class TextWidget extends Table implements WidgetContent{
    @Override
    public String name() {
        return name;
    }

    @Override
    public void build(Island island) {
        island.add(name);
    }
}
