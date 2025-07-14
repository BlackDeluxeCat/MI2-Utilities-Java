package mi2u.ui.stats;

import arc.graphics.*;
import arc.scene.ui.layout.*;
import mi2u.ui.elements.*;
import mindustry.graphics.*;

import static mi2u.MI2UVars.buttonSize;

public class HealthBM extends BuildingMonitor{
    public HealthBM(){
        super();
        w = 8;
        h = 1;
    }

    @Override
    public void build(Table table){
        table.clear();
        table.add(new MI2Bar(() ->  b == null ? "" : ("   " + b.health + "/" + b.maxHealth), () -> Pal.health, () -> b == null ? 0f : b.health / b.maxHealth).blink(Color.white)).growX().height(buttonSize);
    }
}
