package mi2u.ui.island.widget;

import mi2u.ui.island.*;

public class IslandConfigureWidget implements WidgetContent{
    @Override
    public void rebuild(Island island) {

    }

    public Island getRoot(Island island) throws Exception {
        int c = 100;
        for(int i = 0; i < c; i++){
            var parent = island.getParentIsland();
            if(parent == null) return island;
            island = parent;
        }
        throw new Exception("获取根岛屿失败，已上浮深度：" + c);
    }
}
