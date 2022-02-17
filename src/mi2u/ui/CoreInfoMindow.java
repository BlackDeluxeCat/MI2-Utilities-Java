package mi2u.ui;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.core.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class CoreInfoMindow extends Mindow2{
    private Interval interval = new Interval();
    private ObjectSet<Item> usedItems = new ObjectSet<>();
    private ObjectIntMap<Item> lastItemsAmt = new ObjectIntMap<>(); 
    private ObjectIntMap<Item> lastLastItemsAmt = new ObjectIntMap<>(); 
    private ObjectSet<UnitType> usedUnits = new ObjectSet<>();
    private CoreBuild core;
    
    public CoreInfoMindow(){
        super("@coreInfo.MI2U", "coreInfo.help");
    }

    public void resetUsed(){
        usedItems.clear();
        background(null);
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        cont.update(() -> {
            core = Vars.player.team().core();

            if(state.isGame() && core != null && interval.get(60f)){
                for(Item item : content.items()){
                    lastLastItemsAmt.put(item, lastItemsAmt.get(item));
                    lastItemsAmt.put(item, core.items.get(item));
                }
            }
        });

        int i = 0;

        for(Item item : content.items()){
            cont.stack(
                new Image(item.uiIcon),
                new Table(t -> t.label(() -> core == null ? "" : (lastItemsAmt.get(item) - lastLastItemsAmt.get(item) >= 0 ? "[green]+" : "[red]") + (lastItemsAmt.get(item) - lastLastItemsAmt.get(item))).get().setFontScale(0.6f)).right().bottom()
                ).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel));
            //image(item.uiIcon).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel));
            //TODO leaks garbage
            cont.label(() -> core == null ? "0" : 
                UI.formatAmount(core.items.get(item)))
            .padRight(3).minWidth(52f).left();

            if(++i % 4 == 0){
                cont.row();
            }
            
        }

        //unittypes in a new line
        cont.row();
        i = 0;

        for(UnitType type : content.units()){
            if(type.isHidden()) continue;
            cont.image(type.uiIcon).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(type.localizedName).style(Styles.outlineLabel));
            cont.label(() -> core == null ? "0" : UI.formatAmount(player.team().data().countType(type))).padRight(3).minWidth(52f).left();

            if(++i % 5 == 0){
                cont.row();
            }
            
        }

    }
}
