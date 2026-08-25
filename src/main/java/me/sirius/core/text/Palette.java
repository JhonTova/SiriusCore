package me.sirius.core.text;

public final class Palette {

    private static final double GOLDEN_ANGLE = 137.508;

    private Palette() {
    }

    public static String hex(int index) {
        return fromHsv((index * GOLDEN_ANGLE) % 360.0, 0.85f, 1.0f);
    }

    public static String rainbow(int index, double stepDegrees, float saturation, float value) {
        double hue = (index * stepDegrees) % 360.0;
        if (hue < 0) {
            hue += 360.0;
        }
        return fromHsv(hue, clamp(saturation), clamp(value));
    }

    public static String evenly(int index, int total) {
        double hue = total <= 0 ? 0 : (360.0 / total) * (index % total);
        return fromHsv(hue, 0.8f, 0.95f);
    }

    private static String fromHsv(double hue, float saturation, float value) {
        double c = value * saturation;
        double x = c * (1 - Math.abs((hue / 60.0) % 2 - 1));
        double m = value - c;

        double r;
        double g;
        double b;
        if (hue < 60)       { r = c; g = x; b = 0; }
        else if (hue < 120) { r = x; g = c; b = 0; }
        else if (hue < 180) { r = 0; g = c; b = x; }
        else if (hue < 240) { r = 0; g = x; b = c; }
        else if (hue < 300) { r = x; g = 0; b = c; }
        else                { r = c; g = 0; b = x; }

        return String.format("#%02x%02x%02x",
                Math.round((r + m) * 255),
                Math.round((g + m) * 255),
                Math.round((b + m) * 255));
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
