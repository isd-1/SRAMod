
package originem.utils.SRAMod.visual;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.io.IOException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;

public class SpriteUtils {
    public static final Logger logger = Global.getLogger(SpriteUtils.class);

    public SpriteUtils() {
    }

    public static SpriteAPI getSmoothTrailSprite() {
        return Global.getSettings().getSprite("AL_trail", "trail_smooth");
    }

    public static SpriteAPI getZappyTrailSprite() {
        return Global.getSettings().getSprite("AL_trail", "trail_zappy");
    }

    public static SpriteAPI getZappy2TrailSprite() {
        return Global.getSettings().getSprite("AL_trail", "trail_zap_fringe");
    }

    public static String getSpriteName(String category, String id) {
        return Global.getSettings().getSpriteName(category, id);
    }

    public static void autoLoadTexture(String filename, String extension, int frame) {
        for(int i = 0; i < frame; ++i) {
            String path = filename + (i < 10 ? "0" + i : "" + i) + extension;

            try {
                Global.getSettings().loadTexture(path);
            } catch (IOException var6) {
                logger.log(Level.ERROR, "Error loading " + path);
            }
        }

    }

    public static SpriteAPI getSortedSprite(int index, String filePathIncludePrefixName) {
        String indexStr = index < 10 ? "0" + index : "" + index;
        return Global.getSettings().getSprite(filePathIncludePrefixName + "_" + indexStr + ".png");
    }

    public static String getPreloadSpriteName(String id) {
        return Global.getSettings().getSpriteName("AL_preloadIcon", id);
    }

    public static String getFxSpriteName(String id) {
        return Global.getSettings().getSpriteName("fx", id);
    }

    public static SpriteAPI getFxSprite(String id) {
        return Global.getSettings().getSprite("fx", id);
    }

    public static String generateRandomIndexString(int min, int max) {
        int random = MathUtils.getRandomNumberInRange(min, max);
        String result;
        if (random < 10) {
            result = "0" + random;
        } else {
            result = "" + random;
        }

        return result;
    }
}
