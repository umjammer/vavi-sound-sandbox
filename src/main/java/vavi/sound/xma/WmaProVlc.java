/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;


/**
 * Lazily-constructed singletons for the WMA Pro VLC tables.
 * <p>
 * Direct translation of the {@code WmaProVlc} class in
 * {@code Echo/WmaPro/Vlc.cs}.
 *
 * @see "https://github.com/IsaacMarovitz/Echo"
 */
public final class WmaProVlc {

    private WmaProVlc() {}

    private static Vlc scale;
    private static Vlc scaleRl;
    private static Vlc coef0;
    private static Vlc coef1;
    private static Vlc vec4;
    private static Vlc vec2;
    private static Vlc vec1;

    public static synchronized Vlc scale() {
        if (scale == null) {
            scale = new Vlc(WmaProTables.ScaleLens, WmaProTables.ScaleSyms, WmaProTables.HuffScaleOffset);
        }
        return scale;
    }

    public static synchronized Vlc scaleRl() {
        if (scaleRl == null) {
            scaleRl = new Vlc(WmaProTables.ScaleRlLens, WmaProTables.ScaleRlSyms, WmaProTables.HuffScaleRlOffset);
        }
        return scaleRl;
    }

    public static synchronized Vlc coef0() {
        if (coef0 == null) {
            coef0 = new Vlc(WmaProTables.Coef0Lens, WmaProTables.Coef0Syms, WmaProTables.HuffCoef0Offset);
        }
        return coef0;
    }

    public static synchronized Vlc coef1() {
        if (coef1 == null) {
            coef1 = new Vlc(WmaProTables.Coef1Lens, WmaProTables.Coef1Syms, WmaProTables.HuffCoef1Offset);
        }
        return coef1;
    }

    public static synchronized Vlc vec4() {
        if (vec4 == null) {
            vec4 = new Vlc(WmaProTables.Vec4Lens, WmaProTables.Vec4Syms, WmaProTables.HuffVec4Offset);
        }
        return vec4;
    }

    public static synchronized Vlc vec2() {
        if (vec2 == null) {
            vec2 = new Vlc(WmaProTables.Vec2Lens, WmaProTables.Vec2Syms, WmaProTables.HuffVec2Offset);
        }
        return vec2;
    }

    public static synchronized Vlc vec1() {
        if (vec1 == null) {
            vec1 = new Vlc(WmaProTables.Vec1Lens, WmaProTables.Vec1Syms, WmaProTables.HuffVec1Offset);
        }
        return vec1;
    }
}
