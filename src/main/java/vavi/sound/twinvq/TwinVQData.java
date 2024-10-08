/*
 * TwinVQ decoder
 * Copyright (c) 2009 Vitor Sessak
 *
 * This file is part of Libav.
 *
 * Libav is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * Libav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Libav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package vavi.sound.twinvq;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

import vavi.util.Debug;


/**
 * TwinVQ codebooks. They are coded in a struct so we can use code such as
 * <p>
 * float val = tab.fcb0808l[get_bits(gb, 12)];
 * <pre>
 * without risking a segfault on malformed files.
 * <_pre>
 * The bark_tab_* tables are constructed so that
 * <pre>
 *       /i-1              \
 *       |--               |
 *  bark |\   bark_tab[j]  | == i
 *       |/                |
 *       |--               |
 *       \j=0              /
 * </pre>
 * <p>
 * for some slightly non-conventional bark-scale function
 */
public class TwinVQData {

    static final short[] bark_tab_l08_512 = {
            7, 8, 7, 8, 8, 8, 8, 8, 8, 9,
            9, 10, 10, 11, 11, 12, 12, 14, 15, 16,
            18, 19, 21, 24, 27, 30, 35, 40, 46, 53
    };

    static final short[] bark_tab_l11_512 = {
            6, 6, 6, 6, 6, 6, 7, 6, 7, 7,
            8, 8, 8, 9, 10, 10, 11, 13, 13, 15,
            17, 18, 21, 25, 27, 33, 38, 45, 54, 66
    };

    static final short[] bark_tab_l16_1024 = {
            9, 9, 8, 9, 10, 9, 10, 10, 10, 12,
            11, 13, 13, 14, 16, 17, 19, 20, 24, 26,
            30, 35, 40, 48, 56, 68, 83, 102, 128, 165
    };

    static final short[] bark_tab_l22_1024 = {
            6, 7, 6, 6, 7, 7, 7, 7, 7, 8,
            9, 8, 10, 10, 11, 12, 13, 15, 16, 18,
            21, 24, 27, 33, 38, 46, 55, 68, 84, 107,
            140, 191
    };

    static final short[] bark_tab_l22_512 = {
            3, 3, 3, 4, 3, 3, 4, 3, 4, 4,
            4, 5, 4, 5, 6, 6, 7, 7, 8, 9,
            10, 12, 14, 16, 20, 22, 28, 34, 42, 53,
            71, 95
    };

    static final short[] bark_tab_l44_2048 = {
            5, 6, 5, 6, 5, 6, 6, 6, 6, 6,
            7, 7, 7, 8, 8, 9, 9, 10, 11, 11,
            13, 14, 16, 17, 19, 22, 25, 29, 33, 39,
            46, 54, 64, 79, 98, 123, 161, 220, 320, 512
    };

    static final short[] bark_tab_m08_256 = {
            6, 5, 6, 6, 6, 6, 7, 7, 8, 8,
            9, 10, 11, 13, 15, 18, 20, 25, 31, 39
    };

    static final short[] bark_tab_m11_256 = {
            4, 5, 4, 5, 5, 5, 6, 5, 7, 7,
            8, 9, 10, 12, 15, 17, 22, 28, 35, 47
    };

    static final short[] bark_tab_m16_512 = {
            7, 6, 7, 7, 7, 8, 9, 9, 10, 11,
            14, 15, 18, 22, 27, 34, 44, 59, 81, 117
    };

    static final short[] bark_tab_m22_256 = {
            3, 2, 3, 2, 3, 3, 4, 3, 4, 5,
            5, 7, 8, 9, 13, 16, 22, 30, 44, 70
    };

    static final short[] bark_tab_m22_512 = {
            5, 5, 5, 6, 5, 7, 6, 7, 9, 9,
            11, 13, 15, 20, 24, 33, 43, 61, 88, 140
    };

    static final short[] bark_tab_m44_512 = {
            3, 2, 3, 3, 3, 4, 3, 5, 4, 6,
            7, 8, 10, 14, 18, 25, 36, 55, 95, 208
    };

    static final short[] bark_tab_s08_64 = {
            3, 3, 3, 3, 4, 5, 6, 8, 12, 17
    };

    static final short[] bark_tab_s11_64 = {
            2, 3, 2, 3, 3, 4, 6, 8, 12, 21
    };

    static final short[] bark_tab_s16_128 = {
            3, 4, 4, 4, 5, 7, 10, 16, 26, 49
    };

    static final short[] bark_tab_s22_128 = {
            3, 2, 3, 4, 4, 6, 9, 14, 26, 57
    };

    static final short[] bark_tab_s44_128 = {
            1, 2, 1, 2, 3, 4, 6, 10, 23, 76
    };

    static final short[] cb0808l0 = initTable("cb0808l0");
    static final short[] cb0808l1 = initTable("cb0808l1");
    static final short[] cb0808s0 = initTable("cb0808s0");
    static final short[] cb0808s1 = initTable("cb0808s1");
    static final short[] cb0808m0 = initTable("cb0808m0");
    static final short[] cb0808m1 = initTable("cb0808m1");
    static final short[] cb1108l0 = initTable("cb1108l0");
    static final short[] cb1108l1 = initTable("cb1108l1");
    static final short[] cb1108s0 = initTable("cb1108s0");
    static final short[] cb1108s1 = initTable("cb1108s1");
    static final short[] cb1108m0 = initTable("cb1108m0");
    static final short[] cb1108m1 = initTable("cb1108m1");
    static final short[] cb1110l0 = initTable("cb1110l0");
    static final short[] cb1110l1 = initTable("cb1110l1");
    static final short[] cb1110s0 = initTable("cb1110s0");
    static final short[] cb1110s1 = initTable("cb1110s1");
    static final short[] cb1110m0 = initTable("cb1110m0");
    static final short[] cb1110m1 = initTable("cb1110m1");
    static final short[] cb1616l0 = initTable("cb1616l0");
    static final short[] cb1616l1 = initTable("cb1616l1");
    static final short[] cb1616s0 = initTable("cb1616s0");
    static final short[] cb1616s1 = initTable("cb1616s1");
    static final short[] cb1616m0 = initTable("cb1616m0");
    static final short[] cb1616m1 = initTable("cb1616m1");
    static final short[] cb2220l0 = initTable("cb2220l0");
    static final short[] cb2220l1 = initTable("cb2220l1");
    static final short[] cb2220s0 = initTable("cb2220s0");
    static final short[] cb2220s1 = initTable("cb2220s1");
    static final short[] cb2220m0 = initTable("cb2220m0");
    static final short[] cb2220m1 = initTable("cb2220m1");
    static final short[] cb2224l0 = initTable("cb2224l0");
    static final short[] cb2224l1 = initTable("cb2224l1");
    static final short[] cb2224s0 = initTable("cb2224s0");
    static final short[] cb2224s1 = initTable("cb2224s1");
    static final short[] cb2224m0 = initTable("cb2224m0");
    static final short[] cb2224m1 = initTable("cb2224m1");
    static final short[] cb2232l0 = initTable("cb2232l0");
    static final short[] cb2232l1 = initTable("cb2232l1");
    static final short[] cb2232s0 = initTable("cb2232s0");
    static final short[] cb2232s1 = initTable("cb2232s1");
    static final short[] cb2232m0 = initTable("cb2232m0");
    static final short[] cb2232m1 = initTable("cb2232m1");
    static final short[] cb4440l0 = initTable("cb4440l0");
    static final short[] cb4440l1 = initTable("cb4440l1");
    static final short[] cb4440s0 = initTable("cb4440s0");
    static final short[] cb4440s1 = initTable("cb4440s1");
    static final short[] cb4440m0 = initTable("cb4440m0");
    static final short[] cb4440m1 = initTable("cb4440m1");
    static final short[] cb4448l0 = initTable("cb4448l0");
    static final short[] cb4448l1 = initTable("cb4448l1");
    static final short[] cb4448s0 = initTable("cb4448s0");
    static final short[] cb4448s1 = initTable("cb4448s1");
    static final short[] cb4448m0 = initTable("cb4448m0");
    static final short[] cb4448m1 = initTable("cb4448m1");
    static final short[] fcb08l = initTable("fcb08l");
    static final short[] fcb08m = initTable("fcb08m");
    static final short[] fcb08s = initTable("fcb08s");
    static final short[] fcb11l = initTable("fcb11l");
    static final short[] fcb11m = initTable("fcb11m");
    static final short[] fcb11s = initTable("fcb11s");
    static final short[] fcb16l = initTable("fcb16l");
    static final short[] fcb16m = initTable("fcb16m");
    static final short[] fcb16s = initTable("fcb16s");
    static final short[] fcb22l_1 = initTable("fcb22l_1");
    static final short[] fcb22m_1 = initTable("fcb22m_1");
    static final short[] fcb22s_1 = initTable("fcb22s_1");
    static final short[] fcb22l_2 = initTable("fcb22l_2");
    static final short[] fcb22m_2 = initTable("fcb22m_2");
    static final short[] fcb22s_2 = initTable("fcb22s_2");
    static final short[] fcb44l = initTable("fcb44l");
    static final short[] fcb44m = initTable("fcb44m");
    static final short[] fcb44s = initTable("fcb44s");
    static final short[] shape08 = initTable("shape08");
    static final short[] shape11 = initTable("shape11");
    static final short[] shape16 = initTable("shape16");
    static final short[] shape22_1 = initTable("shape22_1");
    static final short[] shape22_2 = initTable("shape22_2");
    static final short[] shape44 = initTable("shape44");

    static final float[] lsp22_2 = {
            0.0712f, 0.1830f, 0.4167f, 0.6669f, 0.8738f, 1.0696f, 1.2555f, 1.4426f,
            1.6427f, 1.8138f, 1.9966f, 2.1925f, 2.3872f, 2.5748f, 2.7713f, 2.9597f,
            0.1894f, 0.3942f, 0.5418f, 0.6747f, 0.7517f, 0.8763f, 1.1189f, 1.3072f,
            1.5011f, 1.6790f, 1.8342f, 2.0781f, 2.2929f, 2.4566f, 2.6613f, 2.9204f,
            0.1767f, 0.3403f, 0.5173f, 0.7055f, 0.8899f, 1.0696f, 1.2302f, 1.4111f,
            1.5989f, 1.7751f, 1.9618f, 2.1544f, 2.3454f, 2.5356f, 2.7362f, 2.9315f,
            0.1240f, 0.2361f, 0.4423f, 0.6326f, 0.7729f, 0.9387f, 1.1142f, 1.2847f,
            1.4746f, 1.7126f, 1.9482f, 2.1642f, 2.3536f, 2.5506f, 2.7593f, 2.9197f,
            0.1213f, 0.2782f, 0.5011f, 0.6910f, 0.8564f, 1.0462f, 1.2315f, 1.4232f,
            1.6178f, 1.8028f, 1.9813f, 2.1766f, 2.3670f, 2.5591f, 2.7475f, 2.9403f,
            0.1382f, 0.2995f, 0.4693f, 0.5874f, 0.6929f, 0.8102f, 1.0094f, 1.2960f,
            1.5511f, 1.7607f, 1.9699f, 2.1680f, 2.3367f, 2.5459f, 2.7370f, 2.9105f,
            0.1428f, 0.2690f, 0.3713f, 0.4757f, 0.6664f, 0.9019f, 1.1276f, 1.3674f,
            1.5471f, 1.6695f, 1.8261f, 2.0572f, 2.2753f, 2.4963f, 2.7187f, 2.9114f,
            0.1669f, 0.3085f, 0.4489f, 0.5724f, 0.6934f, 0.8465f, 0.9680f, 1.1641f,
            1.4320f, 1.6841f, 1.8977f, 2.1061f, 2.3118f, 2.5152f, 2.7329f, 2.9274f,
            0.1128f, 0.2709f, 0.4803f, 0.6878f, 0.8673f, 1.0693f, 1.2749f, 1.4657f,
            1.6650f, 1.8434f, 2.0339f, 2.2300f, 2.4003f, 2.5951f, 2.7762f, 2.9465f,
            0.1201f, 0.2345f, 0.4021f, 0.6379f, 0.8651f, 1.0256f, 1.1630f, 1.3250f,
            1.5395f, 1.7808f, 2.0011f, 2.1997f, 2.3618f, 2.5505f, 2.7561f, 2.9351f,
            0.2575f, 0.4163f, 0.5081f, 0.6484f, 0.8570f, 1.0832f, 1.2732f, 1.3933f,
            1.5497f, 1.7725f, 1.9945f, 2.2098f, 2.3514f, 2.5216f, 2.7146f, 2.8969f,
            0.1367f, 0.2656f, 0.4470f, 0.6398f, 0.8146f, 1.0125f, 1.2142f, 1.3960f,
            1.5558f, 1.7338f, 1.9465f, 2.1769f, 2.4031f, 2.5746f, 2.7335f, 2.9046f,
            0.0868f, 0.1723f, 0.2785f, 0.5071f, 0.7732f, 1.0024f, 1.1924f, 1.4220f,
            1.6149f, 1.8064f, 1.9951f, 2.1935f, 2.3777f, 2.5748f, 2.7661f, 2.9488f,
            0.1428f, 0.2592f, 0.3875f, 0.5810f, 0.7513f, 0.9334f, 1.1096f, 1.3565f,
            1.5869f, 1.7788f, 1.9036f, 2.0893f, 2.3332f, 2.5289f, 2.7204f, 2.9053f,
            0.2313f, 0.4066f, 0.4960f, 0.5853f, 0.7799f, 0.9201f, 1.1365f, 1.3499f,
            1.5119f, 1.7641f, 1.9095f, 2.0911f, 2.2653f, 2.4587f, 2.7010f, 2.8900f,
            0.1927f, 0.3424f, 0.4682f, 0.6035f, 0.7330f, 0.8492f, 1.0477f, 1.3083f,
            1.5602f, 1.6945f, 1.7806f, 2.0066f, 2.2566f, 2.4864f, 2.7021f, 2.9180f,
            0.0962f, 0.1933f, 0.3968f, 0.6077f, 0.8083f, 1.0224f, 1.2307f, 1.4344f,
            1.6350f, 1.8173f, 2.0024f, 2.1894f, 2.3812f, 2.5648f, 2.7535f, 2.9483f,
            0.1469f, 0.2679f, 0.4272f, 0.6080f, 0.7949f, 0.9247f, 1.0741f, 1.2722f,
            1.5144f, 1.7679f, 2.0030f, 2.1944f, 2.3890f, 2.5928f, 2.8116f, 2.9555f,
            0.1618f, 0.3917f, 0.6111f, 0.7511f, 0.8325f, 1.0010f, 1.2397f, 1.4147f,
            1.5764f, 1.7359f, 1.9300f, 2.1325f, 2.3096f, 2.5480f, 2.7725f, 2.9697f,
            0.1561f, 0.2634f, 0.4062f, 0.6139f, 0.8059f, 0.9618f, 1.0948f, 1.3179f,
            1.5846f, 1.7622f, 1.9399f, 2.1476f, 2.3330f, 2.5232f, 2.7412f, 2.9554f,
            0.1076f, 0.2320f, 0.3977f, 0.5798f, 0.7707f, 0.9975f, 1.1884f, 1.3793f,
            1.6059f, 1.8038f, 1.9928f, 2.1942f, 2.3881f, 2.5742f, 2.7717f, 2.9547f,
            0.1360f, 0.2493f, 0.3827f, 0.5644f, 0.7384f, 0.9087f, 1.0865f, 1.2902f,
            1.5185f, 1.7246f, 1.9170f, 2.1175f, 2.3324f, 2.5442f, 2.7441f, 2.9437f,
            0.1684f, 0.2990f, 0.4406f, 0.5834f, 0.7305f, 0.9028f, 1.0801f, 1.2756f,
            1.4646f, 1.6514f, 1.8346f, 2.0493f, 2.2594f, 2.4765f, 2.6985f, 2.9089f,
            0.1145f, 0.2295f, 0.3421f, 0.5032f, 0.7007f, 0.9057f, 1.0830f, 1.2733f,
            1.4885f, 1.6897f, 1.8933f, 2.1128f, 2.3188f, 2.5271f, 2.7284f, 2.9266f,
            0.1705f, 0.3815f, 0.6120f, 0.7964f, 0.9342f, 1.0926f, 1.2741f, 1.4645f,
            1.6552f, 1.8040f, 1.9778f, 2.1931f, 2.3836f, 2.5827f, 2.7905f, 2.9494f,
            0.1284f, 0.2622f, 0.4714f, 0.6559f, 0.8004f, 1.0005f, 1.1416f, 1.3163f,
            1.5773f, 1.8144f, 1.9947f, 2.2001f, 2.3836f, 2.5710f, 2.7447f, 2.9262f,
            0.1164f, 0.2882f, 0.5349f, 0.7310f, 0.8483f, 0.9729f, 1.1331f, 1.3350f,
            1.5307f, 1.7306f, 1.9409f, 2.1275f, 2.3229f, 2.5358f, 2.7455f, 2.9447f,
            0.1159f, 0.2646f, 0.4677f, 0.6375f, 0.7771f, 0.9557f, 1.1398f, 1.3514f,
            1.5717f, 1.7512f, 1.9337f, 2.1323f, 2.3272f, 2.5409f, 2.7377f, 2.9212f,
            0.1080f, 0.2143f, 0.3475f, 0.5307f, 0.7358f, 0.9681f, 1.1489f, 1.3289f,
            1.5553f, 1.7664f, 1.9696f, 2.1780f, 2.3676f, 2.5568f, 2.7493f, 2.9347f,
            0.1331f, 0.2430f, 0.3879f, 0.5092f, 0.6324f, 0.8119f, 1.0327f, 1.2657f,
            1.4999f, 1.7107f, 1.9178f, 2.1272f, 2.3296f, 2.5340f, 2.7372f, 2.9353f,
            0.1557f, 0.2873f, 0.4558f, 0.6548f, 0.8472f, 1.0106f, 1.1480f, 1.3281f,
            1.5856f, 1.7740f, 1.9564f, 2.1651f, 2.3295f, 2.5207f, 2.7005f, 2.9151f,
            0.1397f, 0.2761f, 0.4533f, 0.6374f, 0.7510f, 0.8767f, 1.0408f, 1.2909f,
            1.5368f, 1.7560f, 1.9424f, 2.1332f, 2.3210f, 2.5116f, 2.6924f, 2.8886f,
            0.0945f, 0.1653f, 0.3601f, 0.6129f, 0.8378f, 1.0333f, 1.2417f, 1.4539f,
            1.6507f, 1.8304f, 2.0286f, 2.2157f, 2.3975f, 2.5865f, 2.7721f, 2.9426f,
            0.1892f, 0.3863f, 0.4896f, 0.5909f, 0.7294f, 0.9483f, 1.1575f, 1.3542f,
            1.4796f, 1.6535f, 1.9070f, 2.1435f, 2.3281f, 2.4967f, 2.7039f, 2.9222f,
            0.1614f, 0.3129f, 0.5086f, 0.7048f, 0.8730f, 1.0239f, 1.1905f, 1.3799f,
            1.5697f, 1.7503f, 1.9103f, 2.1115f, 2.3235f, 2.5234f, 2.6973f, 2.8957f,
            0.1199f, 0.2590f, 0.4273f, 0.5935f, 0.7542f, 0.9625f, 1.1225f, 1.2998f,
            1.5361f, 1.7102f, 1.9097f, 2.1269f, 2.3157f, 2.5304f, 2.7212f, 2.9175f,
            0.1087f, 0.2373f, 0.4261f, 0.6277f, 0.8092f, 0.9884f, 1.1954f, 1.4077f,
            1.6048f, 1.7799f, 1.9693f, 2.1662f, 2.3426f, 2.5501f, 2.7459f, 2.9257f,
            0.1262f, 0.2216f, 0.3857f, 0.5799f, 0.7148f, 0.8610f, 1.0752f, 1.3306f,
            1.5549f, 1.7605f, 1.9727f, 2.1580f, 2.3612f, 2.5602f, 2.7554f, 2.9372f,
            0.1445f, 0.2832f, 0.4469f, 0.6283f, 0.7991f, 0.9796f, 1.1504f, 1.3323f,
            1.5313f, 1.7140f, 1.8968f, 2.0990f, 2.2826f, 2.4903f, 2.7003f, 2.9031f,
            0.1647f, 0.4068f, 0.5428f, 0.6539f, 0.7682f, 0.8479f, 0.9372f, 1.1691f,
            1.4776f, 1.7314f, 1.9071f, 2.0918f, 2.2774f, 2.5029f, 2.7152f, 2.9221f,
            0.1274f, 0.3052f, 0.5238f, 0.7280f, 0.9229f, 1.1211f, 1.3071f, 1.4784f,
            1.6564f, 1.8235f, 2.0028f, 2.1999f, 2.3763f, 2.5608f, 2.7510f, 2.9356f,
            0.1076f, 0.2195f, 0.4815f, 0.6873f, 0.8241f, 0.9443f, 1.1066f, 1.3687f,
            1.6087f, 1.8105f, 1.9857f, 2.1486f, 2.3505f, 2.5854f, 2.7785f, 2.9376f,
            0.1755f, 0.3089f, 0.4695f, 0.6648f, 0.8315f, 1.0202f, 1.1774f, 1.3554f,
            1.5393f, 1.7141f, 1.9247f, 2.1284f, 2.2983f, 2.4975f, 2.7296f, 2.9401f,
            0.1636f, 0.3166f, 0.4594f, 0.6199f, 0.8161f, 0.9879f, 1.1738f, 1.3642f,
            1.5680f, 1.7633f, 1.9598f, 2.1695f, 2.3692f, 2.5846f, 2.7809f, 2.9563f,
            0.1219f, 0.2662f, 0.4620f, 0.6491f, 0.8353f, 1.0150f, 1.2065f, 1.3944f,
            1.5785f, 1.7631f, 1.9389f, 2.1434f, 2.3400f, 2.5316f, 2.7359f, 2.9513f,
            0.1072f, 0.2258f, 0.3968f, 0.5642f, 0.7222f, 0.9367f, 1.1458f, 1.3347f,
            1.5424f, 1.7373f, 1.9303f, 2.1432f, 2.3451f, 2.5415f, 2.7444f, 2.9394f,
            0.1393f, 0.2950f, 0.4724f, 0.6407f, 0.8034f, 1.0031f, 1.1712f, 1.3552f,
            1.5519f, 1.7411f, 1.9198f, 2.1160f, 2.3238f, 2.5119f, 2.7134f, 2.9205f,
            0.1358f, 0.2613f, 0.4239f, 0.5991f, 0.7643f, 0.9379f, 1.1213f, 1.3115f,
            1.5067f, 1.7031f, 1.8768f, 2.0836f, 2.3092f, 2.5134f, 2.7237f, 2.9286f,
            0.1267f, 0.2695f, 0.4524f, 0.6591f, 0.8396f, 1.0173f, 1.2183f, 1.4205f,
            1.6306f, 1.8162f, 2.0106f, 2.2082f, 2.3773f, 2.5787f, 2.7551f, 2.9387f,
            0.1314f, 0.2529f, 0.3837f, 0.5494f, 0.7446f, 0.9097f, 1.0489f, 1.2385f,
            1.4691f, 1.7170f, 1.9600f, 2.1770f, 2.3594f, 2.5356f, 2.7215f, 2.9088f,
            0.1538f, 0.2931f, 0.4449f, 0.6041f, 0.7959f, 0.9666f, 1.1355f, 1.3214f,
            1.5150f, 1.7230f, 1.9433f, 2.1408f, 2.3459f, 2.5476f, 2.7273f, 2.9330f,
            0.1771f, 0.2834f, 0.4136f, 0.5856f, 0.7516f, 0.9363f, 1.0596f, 1.2462f,
            1.4737f, 1.6627f, 1.8810f, 2.1150f, 2.3202f, 2.5274f, 2.7403f, 2.9490f,
            0.1248f, 0.2494f, 0.4397f, 0.6352f, 0.8226f, 1.0015f, 1.1799f, 1.3458f,
            1.5654f, 1.8228f, 2.0646f, 2.2550f, 2.4161f, 2.5964f, 2.7675f, 2.9383f,
            0.0933f, 0.1993f, 0.3105f, 0.4371f, 0.6417f, 0.8935f, 1.1244f, 1.3508f,
            1.5649f, 1.7595f, 1.9581f, 2.1648f, 2.3639f, 2.5569f, 2.7573f, 2.9468f,
            0.1794f, 0.3229f, 0.4758f, 0.6238f, 0.7821f, 0.9640f, 1.1205f, 1.3116f,
            1.5054f, 1.6803f, 1.8658f, 2.0651f, 2.2793f, 2.4856f, 2.6867f, 2.9105f,
            0.1252f, 0.2397f, 0.3844f, 0.5398f, 0.7044f, 0.8799f, 1.0526f, 1.2270f,
            1.4269f, 1.6412f, 1.8532f, 2.0784f, 2.2957f, 2.5051f, 2.7139f, 2.9210f,
            0.1391f, 0.3494f, 0.5738f, 0.8024f, 1.0098f, 1.2094f, 1.3830f, 1.5509f,
            1.7222f, 1.8782f, 2.0604f, 2.2479f, 2.4154f, 2.5968f, 2.7767f, 2.9450f,
            0.1122f, 0.2180f, 0.4175f, 0.6074f, 0.7559f, 0.9465f, 1.1513f, 1.3340f,
            1.5215f, 1.7491f, 1.9911f, 2.1894f, 2.3433f, 2.5377f, 2.7380f, 2.9183f,
            0.1595f, 0.3029f, 0.4842f, 0.6324f, 0.7874f, 0.9814f, 1.1992f, 1.3554f,
            1.5017f, 1.7274f, 1.9168f, 2.0853f, 2.2964f, 2.5300f, 2.7187f, 2.9041f,
            0.1350f, 0.2747f, 0.4791f, 0.6638f, 0.8050f, 0.9644f, 1.1238f, 1.2987f,
            1.4844f, 1.6754f, 1.8778f, 2.0987f, 2.3279f, 2.5424f, 2.7410f, 2.9356f,
            0.0914f, 0.1727f, 0.3143f, 0.5124f, 0.7123f, 0.9323f, 1.1706f, 1.3821f,
            1.5864f, 1.7828f, 1.9701f, 2.1560f, 2.3445f, 2.5486f, 2.7433f, 2.9372f,
            0.1222f, 0.2359f, 0.3931f, 0.5912f, 0.7776f, 0.9505f, 1.1623f, 1.3723f,
            1.5484f, 1.7316f, 1.9321f, 2.1283f, 2.3148f, 2.5269f, 2.7299f, 2.9213f,
            0.2089f, 0.3872f, 0.5090f, 0.6413f, 0.7967f, 1.0226f, 1.1897f, 1.3908f,
            1.5954f, 1.7202f, 1.8614f, 2.1030f, 2.2973f, 2.5079f, 2.7491f, 2.8944f,
            0.1288f, 0.2423f, 0.4108f, 0.6062f, 0.7688f, 0.9188f, 1.0876f, 1.2866f,
            1.4897f, 1.6910f, 1.9219f, 2.1076f, 2.2805f, 2.5023f, 2.7155f, 2.9203f,
            0.0192f, 0.0462f, 0.0128f, 0.0054f, -0.0156f, -0.0118f, -0.0135f, 0.0030f,
            -0.0120f, 0.0031f, 0.0240f, -0.0451f, -0.0439f, -0.0432f, -0.0527f, -0.0207f,
            0.0253f, 0.0084f, -0.0305f, -0.0144f, 0.0046f, -0.0378f, -0.0467f, -0.0102f,
            0.0280f, 0.0540f, 0.0151f, 0.0437f, 0.0141f, -0.0257f, -0.0058f, 0.0073f,
            0.0107f, 0.0054f, 0.0371f, -0.0105f, 0.0165f, -0.0143f, 0.0148f, 0.0382f,
            -0.0054f, -0.0284f, 0.0001f, -0.0218f, 0.0258f, 0.0517f, 0.0157f, -0.0032f,
            -0.0190f, 0.0343f, 0.0576f, 0.0346f, 0.0392f, -0.0158f, -0.0323f, -0.0578f,
            -0.0617f, -0.0242f, -0.0144f, 0.0188f, 0.0249f, 0.0021f, -0.0422f, -0.0420f,
            0.0750f, 0.0762f, 0.0325f, -0.0066f, 0.0332f, 0.0376f, 0.0388f, 0.0630f,
            0.0525f, 0.0196f, 0.0051f, -0.0484f, -0.0322f, 0.0059f, 0.0132f, 0.0079f,
            0.0237f, 0.0774f, 0.0697f, 0.0184f, -0.0321f, -0.0327f, 0.0274f, 0.0284f,
            0.0057f, 0.0289f, 0.0478f, 0.0142f, -0.0053f, 0.0114f, 0.0292f, -0.0032f,
            -0.0111f, -0.0389f, 0.0282f, 0.0613f, 0.0200f, -0.0006f, 0.0111f, 0.0048f,
            0.0273f, 0.0017f, -0.0369f, 0.0655f, 0.0758f, 0.0555f, 0.0238f, -0.0024f,
            -0.0100f, -0.0419f, -0.0696f, -0.0158f, -0.0479f, -0.0744f, -0.0356f, -0.0245f,
            -0.0400f, -0.0112f, 0.0134f, 0.0001f, -0.0422f, -0.0514f, -0.0081f, 0.0083f,
            -0.0151f, 0.0323f, -0.0001f, -0.0444f, -0.0406f, -0.0214f, -0.0050f, -0.0235f,
            -0.0205f, -0.0264f, -0.0324f, 0.0334f, 0.0392f, 0.0265f, 0.0289f, 0.0180f,
            0.0493f, 0.0227f, 0.0194f, 0.0365f, 0.0544f, 0.0674f, 0.0559f, 0.0732f,
            0.0911f, 0.0942f, 0.0735f, 0.0174f, -0.0113f, -0.0553f, -0.0665f, -0.0227f,
            -0.0259f, -0.0266f, -0.0239f, -0.0379f, 0.0329f, 0.0173f, -0.0210f, -0.0114f,
            -0.0063f, 0.0060f, -0.0089f, -0.0198f, -0.0282f, -0.0080f, -0.0179f, -0.0290f,
            0.0046f, -0.0126f, -0.0066f, 0.0350f, 0.0532f, 0.0235f, 0.0198f, 0.0212f,
            0.0449f, 0.0681f, 0.0677f, -0.0049f, 0.0086f, 0.0120f, 0.0356f, 0.0454f,
            0.0592f, 0.0449f, -0.0271f, -0.0510f, -0.0110f, 0.0234f, 0.0203f, 0.0243f,
            0.0242f, 0.0133f, 0.0098f, 0.0040f, 0.0024f, -0.0005f, -0.0075f, -0.0126f,
            -0.0393f, -0.0052f, 0.0165f, 0.0016f, -0.0193f, 0.0239f, 0.0336f, 0.0029f,
            -0.0586f, -0.0539f, -0.0094f, -0.0664f, -0.0898f, -0.0540f, -0.0066f, 0.0134f,
            -0.0074f, 0.0067f, -0.0521f, -0.0431f, 0.0104f, 0.0690f, 0.0663f, 0.0197f,
            -0.0017f, -0.0518f, -0.0597f, -0.0171f, -0.0054f, -0.0140f, -0.0080f, 0.0172f,
            -0.0362f, -0.0713f, -0.0310f, 0.0096f, 0.0243f, 0.0381f, -0.0062f, -0.0392f,
            -0.0281f, 0.0386f, 0.0461f, 0.0069f, 0.0384f, 0.0080f, -0.0141f, 0.0171f,
            0.3368f, 0.3128f, 0.3304f, 0.3392f, 0.3185f, 0.3037f, 0.2789f, 0.2692f,
            0.2779f, 0.2796f, 0.2891f, 0.2643f, 0.2647f, 0.2593f, 0.2927f, 0.3283f,
            0.4978f, 0.4988f, 0.4969f, 0.4997f, 0.4957f, 0.4985f, 0.4970f, 0.4978f,
            0.4938f, 0.4951f, 0.4994f, 0.4971f, 0.4981f, 0.4983f, 0.4967f, 0.4789f
    };

    static final byte[] tab7 = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1,
                    0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
                    0, 0, 0,
            0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0,
            0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1,
                    0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
                    0, 0, 0,
            0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0,
            0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0,
            0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0,
                    0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0,
                    1, 0, 1,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                    0, 0, 0,
            0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0
    };

    static final byte[] tab8 = {
            0, 0, 0, 1, 1,
            0, 1, 0, 0, 1,
            1, 1, 0, 0, 0,
            1, 0, 0, 1, 0,
            0, 0, 0, 1, 1,
            0, 1, 0, 0, 1,
            1, 1, 0, 0, 0,
            1, 0, 0, 1, 0,
            0, 0, 0, 1, 1,
            0, 1, 0, 0, 1,
            1, 1, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 1, 0, 1, 0
    };
    static final byte[] tab9 = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1,
                    1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0,
                    0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    static final byte[] tab10 = {
            1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0,
                    0, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 1, 0,
            1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0,
                    0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0,
            1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 0,
                    0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 0,
            1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0,
                    0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1,
                    0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1,
            0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1,
                    0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1,
            1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1,
                    0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0,
            0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0,
                    0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1
    };

    static final byte[] tab11 = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0
    };

    static final byte[] tab12 = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0,
            0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0,
            0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1,
    };

    static final class tab {

        final int size;
        final byte[] tab;

        public tab(int size, byte[] tab) {
            this.size = size;
            this.tab = tab;
        }
    }

    static final tab[] tabs = {
            new tab(0, null),
            new tab(5, tab8), new tab(5, tab8), new tab(15, tab12),
            new tab(5, tab8), new tab(25, tab10), new tab(15, tab12),
            new tab(35, tab7), new tab(5, tab8), new tab(45, tab9),
            new tab(25, tab10), new tab(55, tab11), new tab(15, tab12)
    };

    private static short[] initTable(String f) {
        List<Short> l = new ArrayList<>();
        Scanner s = new Scanner(TwinVQData.class.getResourceAsStream(f + ".txt"));
        s.useDelimiter("[\\s,]+");
        while (s.hasNextShort()) {
            l.add(s.nextShort());
        }
Debug.println(Level.FINEST, f + ": " + l.size());
        return l.stream().collect(() -> ShortBuffer.allocate(l.size()), ShortBuffer::put, (left, right) -> {}).array();
    }
}
