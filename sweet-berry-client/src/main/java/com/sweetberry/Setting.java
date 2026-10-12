package com.sweetberry;

public class Setting {
    public final String name;
    public final boolean isBool;
    public double value, min, max, step;

    private Setting(String name, boolean isBool, double value, double min, double max, double step) {
        this.name = name; this.isBool = isBool; this.value = value;
        this.min = min; this.max = max; this.step = step;
    }

    public static Setting number(String name, double def, double min, double max, double step) {
        return new Setting(name, false, def, min, max, step);
    }

    public static Setting bool(String name, boolean def) {
        return new Setting(name, true, def ? 1 : 0, 0, 1, 1);
    }

    public boolean getBool() { return value >= 0.5; }
    public int getInt() { return (int) Math.round(value); }

    public void setFromPercent(double p) {
        p = Math.max(0, Math.min(1, p));
        double v = min + (max - min) * p;
        if (step > 0) v = Math.round(v / step) * step;
        value = Math.max(min, Math.min(max, v));
    }

    public double percent() { return (value - min) / (max - min); }
}
