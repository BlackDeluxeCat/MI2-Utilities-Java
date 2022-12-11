package mi2u.input;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.logic.*;

import static mindustry.Vars.*;

public class InputUtils{
    public static Building forceTapped = null;
    public static ObjectMap<Block, Boolf<Building>> tapAccess = new ObjectMap<>();    //Boolf checking which block tapping is accessable regardless of self team.

    public static void init(){
        for(var b : content.blocks()){
            if(b instanceof LogicBlock lb) tapAccess.put(lb, build -> lb.accessible());
        }
    }
    public static void forceTap(@Nullable Building build, boolean includeSelfTeam){
        if(build == null) return;
        Log.info(build.interactable(player.team()) + ", " + (tapAccess.containsKey(build.block) ? tapAccess.get(build.block).get(build) : "false0"));
        if(!includeSelfTeam && (build.interactable(player.team()) && (!tapAccess.containsKey(build.block) || tapAccess.get(build.block).get(build)))) return;//handled by vanilla
        Log.info("in");
        var inv = control.input.inv;
        var config = control.input.config;

        if(build == forceTapped){
            inv.hide();
            config.hideConfig();
            return;
        }

        var ptm = state.playtestingMap;
        state.playtestingMap = state.map;

        if(build.block.configurable){
            if((!config.isShown() && build.shouldShowConfigure(player)) //if the config fragment is hidden, show
                    //alternatively, the current selected block can 'agree' to switch config tiles
                    || (config.isShown() && config.getSelected().onConfigureBuildTapped(build))){
                config.showConfig(build);
            }
            //otherwise...
        }else if(!config.hasConfigMouse()){ //make sure a configuration fragment isn't on the cursor
            //then, if it's shown and the current block 'agrees' to hide, hide it.
            if(config.isShown() && config.getSelected().onConfigureBuildTapped(build)){
                config.hideConfig();
            }
        }

        //consume tap event if necessary
        if(build.block.synthetic() && (build.block.allowConfigInventory)){
            if(build.block.hasItems && build.items.total() > 0){
                inv.showFor(build);
            }
        }

        state.playtestingMap = ptm;
    }
}
