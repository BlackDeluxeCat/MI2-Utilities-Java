package mi2u;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.util.*;
import arc.util.serialization.*;
import mi2u.graphics.*;
import mi2u.input.*;
import mi2u.io.*;
import mi2u.ui.elements.*;
import mi2u.ui.stats.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.mod.*;
import mindustry.ui.*;

import java.util.regex.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;


public class MI2Utilities extends Mod{
    public static Mods.LoadedMod MOD;

    public ModUpdateChecker updateChecker = new ModUpdateChecker();

    public MI2Utilities(){
        Events.on(ClientLoadEvent.class, e -> {
            MOD = mods.getMod(MI2Utilities.class);
            MOD.meta.subtitle = MOD.meta.version;
            MI2UVars.init();
            InputUtils.init();

            //anyone need max size < vanilla size, open an issue on Github
            maxSchematicSize = Math.max(maxSchematicSize, mi2ui.settings.getInt("maxSchematicSize", 64));
            mi2ui.settings.putInt("maxSchematicSize", maxSchematicSize);

            if(mi2ui.settings.getBool("inputReplace")){
                control.setInput(mobile ? MobileInputExt.getInstance() : DesktopInputExt.getInstance());
            }

            Time.runTask(40f, () -> {
                mi2ui.addTo(Core.scene.root);
                if(mi2ui.settings.getBool("showEmojis")) emojis.addTo(emojis.hasParent() ? emojis.parent : Core.scene.root);
                if(mi2ui.settings.getBool("showCoreInfo")) coreInfo.addTo(coreInfo.hasParent() ? coreInfo.parent : Core.scene.root);
                if(mi2ui.settings.getBool("showMinimap")) mindowmap.addTo(mindowmap.hasParent() ? mindowmap.parent : Core.scene.root);
                if(mi2ui.settings.getBool("showLogicHelper")) logicHelper.addTo(logicHelper.hasParent() ? logicHelper.parent : ui.logic);

                RendererExt.init();
                ModifyFuncs.modifyVanilla();
            });

            //popup too early will cause font rendering bug.
            Time.runTask(140f, () -> {
                if(mi2ui.settings.getBool("enableUpdate")) updateChecker.checkInBackgroundAndPopupIfNeeded();
            });
        });

        Events.on(FileTreeInitEvent.class, e -> Core.app.post(MI2UShaders::load));
    }
}
