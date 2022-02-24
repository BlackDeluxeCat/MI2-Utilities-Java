package mi2u.ui;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.io.*;
import mindustry.Vars;
import mindustry.core.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class CoreInfoMindow extends Mindow2{
    private Interval interval = new Interval();
    private ObjectIntMap<Item> lastItemsAmt = new ObjectIntMap<>(); 
    private ObjectIntMap<Item> lastLastItemsAmt = new ObjectIntMap<>(); 
    private CoreBuild core;
    private PowerGraphTable pg = new PowerGraphTable(400);
    
    public CoreInfoMindow(){
        super("@coreInfo.MI2U", "@coreInfo.help");
        mindowName = "CoreInfo";
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
        if(!MI2USettings.getBool(mindowName + ".showCoreItems")){
            cont.add().size(80f,20f);
            cont.row();
        }
        
            cont.table(iut -> {
                int i = 0;
                if(MI2USettings.getBool(mindowName + ".showCoreItems")){
                    for(Item item : content.items()){
                        iut.stack(
                            new Image(item.uiIcon),
                            new Table(t -> t.label(() -> core == null ? "" : (lastItemsAmt.get(item) - lastLastItemsAmt.get(item) >= 0 ? "[green]+" : "[red]") + (lastItemsAmt.get(item) - lastLastItemsAmt.get(item))).get().setFontScale(0.6f)).right().bottom()
                            ).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel));
                        //image(item.uiIcon).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel));
                        //TODO leaks garbage
                        iut.label(() -> core == null ? "0" : 
                            UI.formatAmount(core.items.get(item)))
                        .padRight(3).minWidth(52f).left();
            
                        if(++i % 4 == 0){
                            iut.row();
                        }
                        
                    }
                }
        
                //unittypes in a new line
                iut.row();
                i = 0;
                if(MI2USettings.getBool(mindowName + ".showUnits")){
                    for(UnitType type : content.units()){
                        if(type.isHidden()) continue;
                        iut.image(type.uiIcon).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(type.localizedName).style(Styles.outlineLabel));
                        iut.label(() -> core == null ? "0" : UI.formatAmount(player.team().data().countType(type))).padRight(3).minWidth(52f).left();
            
                        if(++i % 5 == 0){
                            iut.row();
                        }
                        
                    }
                }
            });
        

        if(MI2USettings.getBool(mindowName + ".showPowerGraphs")){
            cont.row();
            cont.add(pg).fillX();
        }

    }

    /** can be overrided, should use super.initSettings(), called in rebuild() */
    @Override
    public void initSettings(){
        super.initSettings();
        settings.add(new CheckSettingEntry(mindowName + ".showCoreItems", "@settings.coreInfo.showCoreItems", b -> rebuild()));
        settings.add(new CheckSettingEntry(mindowName + ".showUnits", "@settings.coreInfo.showUnits", b -> rebuild()));
        settings.add(new CheckSettingEntry(mindowName + ".showPowerGraphs", "@settings.coreInfo.showPowerGraphs", b -> rebuild()));
    }
}
