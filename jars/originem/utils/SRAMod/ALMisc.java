//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package originem.utils.SRAMod;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public final class ALMisc {
    public ALMisc() {
    }

    public static Vector2f getRandomPointInRect(Vector2f start, Vector2f end, float width) {
        Vector2f result = MathUtils.getRandomPointOnLine(start, end);
        float angle = VectorUtils.getAngle(start, end);
        result = MathUtils.getRandomPointOnLine(MathUtils.getPointOnCircumference(result, width * 0.5F, angle + 90.0F), MathUtils.getPointOnCircumference(result, width * 0.5F, angle - 90.0F));
        return result;
    }

    public static float getDistanceToLine(Vector2f lineStart, Vector2f lineEnd, Vector2f point) {
        float result = 0.0F;
        float a = MathUtils.getDistance(lineStart, point);
        float angle = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(lineStart, lineEnd), VectorUtils.getAngle(lineStart, point)));
        result = (float)(FastTrig.sin(Math.toRadians((double)angle)) * (double)a);
        return result;
    }

    public static Color colorBlend(Color a, Color b, float blendLevel) {
        float previousLevel = 1.0F - blendLevel;
        return new Color((int)Math.max(0.0F, Math.min(255.0F, (float)a.getRed() * previousLevel + (float)b.getRed() * blendLevel)), (int)Math.max(0.0F, Math.min(255.0F, (float)a.getGreen() * previousLevel + (float)b.getGreen() * blendLevel)), (int)Math.max(0.0F, Math.min(255.0F, (float)a.getBlue() * previousLevel + (float)b.getBlue() * blendLevel)), (int)Math.max(0.0F, Math.min(255.0F, (float)a.getAlpha() * previousLevel + (float)b.getAlpha() * blendLevel)));
    }

    public static Color getRandomBlendColor(Color a, Color b) {
        return colorBlend(a, b, MyMath.RANDOM.nextFloat());
    }

    public static String getDigitValue(float value) {
        return getDigitValue(value, 1);
    }

    public static String getDigitValueForce(float value) {
        return getDigitValue(value, 1, true);
    }

    public static String getDigitValue(float value, int digit) {
        return getDigitValue(value, digit, false);
    }

    public static String getDigitValue(float value, int digit, boolean force) {
        return !force && (double)Math.abs((float)Math.round(value) - value) < 1.0E-6 ? "" + Math.round(value) : (new BigDecimal((double)value)).setScale(digit, RoundingMode.HALF_UP).toString();
    }

    public static String coveredInBracket(String origin) {
        return "(" + origin + ")";
    }
}
