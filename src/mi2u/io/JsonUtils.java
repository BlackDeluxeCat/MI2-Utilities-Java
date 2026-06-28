package mi2u.io;

import arc.util.*;
import arc.util.serialization.*;

public class JsonUtils{
    public static Json json = new Json();
    private static boolean initialized = false;

    /** 在 ClientLoadEvent 最前面调用一次。 */
    public static void init(){
        if(initialized) return;
        initialized = true;

        json.setIgnoreUnknownFields(true);
        json.setUsePrototypes(false);
        json.setIgnoreDeprecated(true);
    }

    /** 注册 class tag（防重复）。 */
    public static void registerClass(String tag, Class<?> type){
        if(json.getClass(tag) != null){
            Log.warn("JsonUtils: duplicate class tag '@' for @", tag, type);
            return;
        }
        json.addClassTag(tag, type);
    }
}
