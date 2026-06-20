package mi2u.ui;

import arc.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.ui.island.*;
import mi2u.ui.island.children.*;
import mi2u.ui.island.widget.*;
import mindustry.ui.*;

//TODO将islandConfigureContainer抽离为独立widgetisland
public class IslandOverlayManager {
    public boolean editMode = true;
    /** 根节点，不被序列化 */
    public Island root;
    /** island设置面板 */
    public Island islandConfigureIsland;

    public IslandOverlayManager(){}

    public void onClientLoad(){
        root = new Island("root", new ChildrenContent(new RootStackLayout()));
        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;

        islandConfigureIsland = new Island("ConfigurePanel", new IslandConfigureWidget(new IslandConfigureWidget.IslandOverlayAccess() {
            @Override
            public Island getRoot() {
                return root;
            }
        }));
        islandConfigureIsland.layout.x = Core.graphics.getWidth() / 2f;
        islandConfigureIsland.layout.y = Core.graphics.getHeight() / 2f;
        IslandUtils.addChild(root, islandConfigureIsland);
        Core.scene.add(root);
        root.rebuild();
    }
}
