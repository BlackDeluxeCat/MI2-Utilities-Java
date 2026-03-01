package mi2u.io;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mi2u.*;
import mi2u.ui.elements.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.ui.*;

import java.util.regex.*;

import static mi2u.MI2Utilities.MOD;
import static mindustry.Vars.*;

public class ModUpdateChecker {
    public static final String GIT_URL = "https://github.com/BlackDeluxeCat/MI2-Utilities-Java";
    public static final String API_URL = "https://api.github.com/repos/BlackDeluxeCat/MI2-Utilities-Java/releases";
    public static final String REPO_NAME = "BlackDeluxeCat/MI2-Utilities-Java";

    // 附件信息类
    public static class ReleaseAsset {
        public String fileName;
        public String modVersion;        // 从文件名提取的mod版本
        public String mindustryVersion;  // 从文件名提取的Mindustry版本
        public String downloadUrl;
        public String releaseName;       // release的tag_name
        public String releaseBody;       // release的描述
        public String releaseUrl;        // 用于调用release下载方法
        public boolean exactMatch;       // 是否与当前版本完全匹配
        public boolean compatible;       // 是否兼容当前Mindustry版本

        public ReleaseAsset(String fileName, String modVersion, String mindustryVersion,
                            String downloadUrl, String releaseName, String releaseBody,
                            boolean exactMatch, boolean compatible) {
            this.fileName = fileName;
            this.modVersion = modVersion;
            this.mindustryVersion = mindustryVersion;
            this.downloadUrl = downloadUrl;
            this.releaseName = releaseName;
            this.releaseBody = releaseBody;
            this.exactMatch = exactMatch;
            this.compatible = compatible;
        }

        @Override
        public String toString() {
            return formatVersion(modVersion, mindustryVersion) +
                       (exactMatch ? " " + Core.bundle.get("update.status.installed") : "") +
                       (!compatible ? " " + Core.bundle.get("update.status.incompatible") : "");
        }
    }

    public PopupTable dialog;
    public Table versionListTable;
    public int sign = 0; // 0:检查中, 1:成功, -1:失败
    public final Interval timer = new Interval();
    public float autoCloseDelay = 900f;
    public Seq<ReleaseAsset> allAssets = new Seq<>();

    public void checkInBackgroundAndPopupIfNeeded(){
        Http.get(API_URL, res -> {
            try{
                var json = new JsonReader().parse(res.getResultAsString());
                Seq<ReleaseAsset> fetchedAssets = parseReleases(json);

                if(hasCompatibleNewerVersion(fetchedAssets)){
                    Core.app.post(() -> {
                        if(dialog == null || !dialog.shown){
                            show(fetchedAssets);
                        }
                    });
                }
            }catch(Exception e){
                Log.err(Core.bundle.get("update.error.parseFailed"), e);
            }
        }, e -> Log.err(Core.bundle.get("update.error.fetchFailed"), e));
    }

    private boolean hasCompatibleNewerVersion(Seq<ReleaseAsset> assets){
        if(MOD == null || MOD.meta == null) return false;
        String currentModVersion = MOD.meta.version;
        return assets.contains(asset -> asset.compatible && compareModVersions(asset.modVersion, currentModVersion) > 0);
    }

    private void show(@Nullable Seq<ReleaseAsset> prefetchedAssets) {
        if(prefetchedAssets == null) return;
        allAssets.clear();
        allAssets.addAll(prefetchedAssets);
        sign = 1;
        autoCloseDelay = 1200f;

        dialog = new PopupTable() {
            {
                setBackground(Styles.black5);
                margin(8f);
                setPositionInScreen((Core.graphics.getWidth() - getPrefWidth()) / 2,
                    (Core.graphics.getHeight() - getPrefHeight()) / 2);

                timer.get(1);

                update(() -> {
                    //toFront();
                    if (timer.check(0, autoCloseDelay)) hide();
                });

                hovered(() -> timer.get(1));
                addDragMove();

                // 标题栏
                this.image().color(Color.coral).growX().height(2f).row();
                this.add("@update.title").growX().with(l -> l.setFontScale(1.2f)).row();
                this.image().color(Color.coral).growX().height(2f).row();

                // 已安装信息
                this.table(infoTable -> {
                    infoTable.table(sub -> {
                        sub.add(Core.bundle.get("update.installedVersion") + ": " +
                                    formatVersion(MOD.meta.version, MOD.meta.minGameVersion)).left();
                    }).padLeft(10f);
                }).growX().padTop(5f).row();

                // 查询到的版本列表
                this.table(git -> {
                    git.add(Iconc.github + " " + Core.bundle.get("update.availableVersions"))
                        .color(Color.lightGray).left().pad(4f);
                    git.button(Iconc.link + " " + Core.bundle.get("update.githubRepo"),
                            MI2UVars.textb, () -> Core.app.openURI(GIT_URL)).grow()
                        .tooltip(Core.bundle.get("update.openLink"))
                        .get().getLabel().setFontScale(0.7f);
                }).growX().padTop(5f).row();

                this.pane(scroll -> {
                    versionListTable = scroll;
                    scroll.defaults().growX();
                    buildVersionList(scroll);
                }).minWidth(400f).maxHeight(300f).row();

                // 底部按钮
                this.table(bottomTable -> {
                    bottomTable.defaults().growX().height(50f);

                    bottomTable.button("", MI2UVars.textb, this::hide)
                        .update(b -> b.setText(Core.bundle.get("update.close") + " (" +
                                                   Strings.fixed((autoCloseDelay - timer.getTime(0)) / 60, 1) + "s)"));
                }).growX().row();
            }
        };

        dialog.popup();
    }

    private void buildVersionList(Table table) {
        table.clear();

        // 分组：可更新、精确匹配、兼容版本、不兼容版本
        Seq<ReleaseAsset> updatableVersions = allAssets.select(a -> a.compatible && isNewerVersion(a.modVersion));
        Seq<ReleaseAsset> exactMatches = allAssets.select(a -> a.exactMatch);
        Seq<ReleaseAsset> compatibleVersions = allAssets.select(a -> a.compatible && !a.exactMatch && !isNewerVersion(a.modVersion));
        Seq<ReleaseAsset> incompatibleVersions = allAssets.select(a -> !a.compatible && !a.exactMatch);

        // 显示可更新版本
        if (!updatableVersions.isEmpty()) {
            for (ReleaseAsset asset : updatableVersions) {
                addVersionButton(table, asset, Color.green, "update.status.updatable");
            }
        }

        // 显示精确匹配
        if (!exactMatches.isEmpty()) {
            for (ReleaseAsset asset : exactMatches) {
                addVersionButton(table, asset, Color.green, "update.status.installed");
            }
        }

        // 显示兼容版本
        if (!compatibleVersions.isEmpty()) {
            for (ReleaseAsset asset : compatibleVersions) {
                addVersionButton(table, asset, Color.white, "update.status.compatible");
            }
        }

        // 显示不兼容版本
        if (!incompatibleVersions.isEmpty()) {
            for (ReleaseAsset asset : incompatibleVersions) {
                addVersionButton(table, asset, Color.gray, "update.status.incompatible");
            }
        }
    }

    private boolean isNewerVersion(String modVersion){
        if(MOD == null || MOD.meta == null) return false;
        return compareModVersions(modVersion, MOD.meta.version) > 0;
    }

    private void addVersionButton(Table table, ReleaseAsset asset, Color color, String statusKey) {
        table.button(b -> {
                b.table(inner -> {
                    inner.add(asset.modVersion + " (Mindustry " + asset.mindustryVersion + ")")
                        .color(color).left().growX();
                    inner.add(Core.bundle.get(statusKey))
                        .color(color.cpy().mul(0.8f)).right().padLeft(10f);
                }).growX();
            }, Styles.flatBordert, () -> showDownloadConfirm(asset))
            .growX().height(40f).padBottom(2f).row();
    }

    private void showDownloadConfirm(ReleaseAsset asset) {
        // 使用Mindustry的确认对话框
        ui.showConfirm(Core.bundle.get("update.confirm.title"),
            Core.bundle.format("update.confirm.message",
                asset.modVersion,
                asset.mindustryVersion,
                (asset.releaseBody.length() > 200 ? asset.releaseBody.substring(0, 200) + "..." : asset.releaseBody)),
            () -> {
                // 调用mod安装方法
                ui.mods.githubImportMod(REPO_NAME, true, asset.releaseUrl.substring(asset.releaseUrl.lastIndexOf("/") + 1));
                dialog.hide();
            });
    }

    private Seq<ReleaseAsset> parseReleases(JsonValue releasesJson) {
        String currentModVersion = MOD.meta.version;
        Seq<ReleaseAsset> parsedAssets = new Seq<>();

        for (JsonValue release : releasesJson) {
            String releaseName = release.getString("tag_name", "");
            String releaseBody = release.getString("body", "");
            String url = release.getString("url", "");
            JsonValue assets = release.get("assets");

            if (assets != null) {
                for (JsonValue asset : assets) {
                    ReleaseAsset parsed = parseAsset(asset, releaseName, releaseBody, currentModVersion);
                    if (parsed != null) {
                        parsed.releaseUrl = url;
                        parsedAssets.add(parsed);
                    }
                }
            }
        }

        // 按版本号降序排序
        parsedAssets.sort((a, b) -> compareModVersions(b.modVersion, a.modVersion));
        return parsedAssets;
    }

    private ReleaseAsset parseAsset(JsonValue assetJson, String releaseName, String releaseBody, String currentModVersion) {
        String fileName = assetJson.getString("name", "");

        // 解析文件名格式: mi2-utilities-java.1.10.0-154.2.jar
        Pattern pattern = Pattern.compile("^(.+?)\\.([\\w.-]+)-([\\w.-]+)\\.jar$");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.matches()) {
            String repoName = matcher.group(1);
            String modVersion = matcher.group(2);
            String mindustryVersion = matcher.group(3);

            boolean exactMatch = modVersion.equals(currentModVersion);
            boolean compatible = isCompatibleMindustryVersion(mindustryVersion);

            return new ReleaseAsset(
                fileName,
                modVersion,
                mindustryVersion,
                assetJson.getString("browser_download_url", ""),
                releaseName,
                releaseBody,
                exactMatch,
                compatible
            );
        } else {
            Log.warn("MI2U Update Check", Core.bundle.format("update.error.nonStandardFile", fileName));
        }
        return null;
    }

    private boolean isCompatibleMindustryVersion(String requiredVersion) {
        try {
            // 使用Mindustry的Version.isAtLeast方法检查兼容性
            // 这里简化处理：如果当前版本 >= 要求版本，则认为兼容
            return Version.isAtLeast(requiredVersion);
        } catch (Exception e) {
            Log.err(Core.bundle.get("update.error.versionCheck"), e);
            return false;
        }
    }

    // 简单的版本号比较
    private int compareModVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Strings.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Strings.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    public static String formatVersion(String modVersion, String minGameVersion) {
        return (modVersion + " (Mindustry " + minGameVersion + ")");
    }
}
