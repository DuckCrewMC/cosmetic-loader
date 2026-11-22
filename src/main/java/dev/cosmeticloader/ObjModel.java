package dev.cosmeticloader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A triangle mesh: either loaded from a Wavefront .obj (with its texture coordinates) or one of the
 * built-in shapes. Always centered and scaled to fit half a block.
 */
public final class ObjModel {
    public static final String[] SHAPES = {"cube", "square", "triangle", "pyramid", "sphere"};
    private static final float[] NO_UV = {0.5f, 0.5f};

    private final float[] pos; // 9 floats per triangle
    private final float[] nrm; // 9 floats per triangle
    private final float[] uv;  // 6 floats per triangle (u, v with v = 0 at the top of the image)

    private ObjModel(float[] pos, float[] nrm, float[] uv) { this.pos = pos; this.nrm = nrm; this.uv = uv; }

    private static int idx(String s, int size) {
        int i = Integer.parseInt(s);
        return i > 0 ? i - 1 : size + i;
    }

    // ---- .obj files -------------------------------------------------------

    public static ObjModel parse(Path file) throws IOException {
        List<float[]> v = new ArrayList<>(), n = new ArrayList<>(), vt = new ArrayList<>();
        List<float[][]> tris = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            String[] t = line.trim().split("\\s+");
            if (t.length < 3) continue;
            switch (t[0]) {
                case "v" -> { if (t.length >= 4) v.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])}); }
                case "vn" -> { if (t.length >= 4) n.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])}); }
                case "vt" -> vt.add(new float[]{Float.parseFloat(t[1]), 1f - Float.parseFloat(t[2])}); // obj v points up, images point down
                case "f" -> {
                    if (t.length >= 4) {
                        int k = t.length - 1;
                        int[] vi = new int[k], ti = new int[k], ni = new int[k];
                        for (int i = 0; i < k; i++) {
                            String[] p = t[i + 1].split("/");
                            vi[i] = idx(p[0], v.size());
                            ti[i] = p.length > 1 && !p[1].isEmpty() ? idx(p[1], vt.size()) : -1;
                            ni[i] = p.length > 2 && !p[2].isEmpty() ? idx(p[2], n.size()) : -1;
                        }
                        for (int i = 1; i < k - 1; i++) {
                            float[] a = v.get(vi[0]), b = v.get(vi[i]), c = v.get(vi[i + 1]);
                            float[] fn = faceNormal(a, b, c);
                            tris.add(new float[][]{a, b, c,
                                    ni[0] >= 0 ? n.get(ni[0]) : fn,
                                    ni[i] >= 0 ? n.get(ni[i]) : fn,
                                    ni[i + 1] >= 0 ? n.get(ni[i + 1]) : fn,
                                    ti[0] >= 0 ? vt.get(ti[0]) : NO_UV,
                                    ti[i] >= 0 ? vt.get(ti[i]) : NO_UV,
                                    ti[i + 1] >= 0 ? vt.get(ti[i + 1]) : NO_UV});
                        }
                    }
                }
                default -> { }
            }
        }
        if (tris.isEmpty()) throw new IOException("No faces found in " + file.getFileName());
        return build(tris);
    }

    /** Centers the triangles and scales them to fit half a block. Each entry is {a, b, c, na, nb, nc, uva, uvb, uvc}. */
    private static ObjModel build(List<float[][]> tris) {
        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (float[][] tr : tris) for (int i = 0; i < 3; i++) for (int a = 0; a < 3; a++) {
            min[a] = Math.min(min[a], tr[i][a]);
            max[a] = Math.max(max[a], tr[i][a]);
        }
        float size = Math.max(max[0] - min[0], Math.max(max[1] - min[1], max[2] - min[2]));
        float k = size > 1e-6f ? 0.5f / size : 1f;

        float[] pos = new float[tris.size() * 9], nrm = new float[tris.size() * 9], uv = new float[tris.size() * 6];
        for (int t = 0; t < tris.size(); t++) {
            for (int i = 0; i < 3; i++) {
                for (int a = 0; a < 3; a++) {
                    pos[t * 9 + i * 3 + a] = (tris.get(t)[i][a] - (min[a] + max[a]) / 2f) * k;
                    nrm[t * 9 + i * 3 + a] = tris.get(t)[3 + i][a];
                }
                uv[t * 6 + i * 2] = tris.get(t)[6 + i][0];
                uv[t * 6 + i * 2 + 1] = tris.get(t)[6 + i][1];
            }
        }
        return new ObjModel(pos, nrm, uv);
    }

    private static float[] faceNormal(float[] a, float[] b, float[] c) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float x = uy * vz - uz * vy, y = uz * vx - ux * vz, z = ux * vy - uy * vx;
        float l = (float) Math.sqrt(x * x + y * y + z * z);
        return l < 1e-9f ? new float[]{0, 1, 0} : new float[]{x / l, y / l, z / l};
    }

    // ---- built-in shapes --------------------------------------------------

    private static float[] p(float x, float y, float z) { return new float[]{x, y, z}; }
    private static float[] uv(float u, float v) { return new float[]{u, v}; }

    private static float[] unit(float[] a) {
        float l = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1e-9f ? new float[]{0, 1, 0} : new float[]{a[0] / l, a[1] / l, a[2] / l};
    }

    /** One flat triangle. With outward = true the normal is flipped to point away from the shape's center. */
    private static void tri(List<float[][]> out, float[] a, float[] b, float[] c,
                            float[] ua, float[] ub, float[] uc, boolean outward) {
        float[] n = faceNormal(a, b, c);
        if (outward) {
            float d = n[0] * (a[0] + b[0] + c[0]) + n[1] * (a[1] + b[1] + c[1]) + n[2] * (a[2] + b[2] + c[2]);
            if (d < 0) n = new float[]{-n[0], -n[1], -n[2]};
        }
        out.add(new float[][]{a, b, c, n, n, n, ua, ub, uc});
    }

    /** A flat quad (two triangles) showing the whole texture. */
    private static void quad(List<float[][]> out, float[] a, float[] b, float[] c, float[] d, boolean outward) {
        float[] ua = uv(0, 1), ub = uv(1, 1), uc = uv(1, 0), ud = uv(0, 0);
        tri(out, a, b, c, ua, ub, uc, outward);
        tri(out, a, c, d, ua, uc, ud, outward);
    }

    private static float[] spherePoint(int i, int j, int rings, int segs) {
        double th = Math.PI * i / rings, ph = 2 * Math.PI * j / segs;
        return p((float) (0.5 * Math.sin(th) * Math.cos(ph)), (float) (0.5 * Math.cos(th)), (float) (0.5 * Math.sin(th) * Math.sin(ph)));
    }

    private static void sphere(List<float[][]> out) {
        int rings = 8, segs = 16;
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segs; j++) {
                float[] p00 = spherePoint(i, j, rings, segs), p01 = spherePoint(i, j + 1, rings, segs);
                float[] p10 = spherePoint(i + 1, j, rings, segs), p11 = spherePoint(i + 1, j + 1, rings, segs);
                float[] u00 = uv((float) j / segs, (float) i / rings), u01 = uv((float) (j + 1) / segs, (float) i / rings);
                float[] u10 = uv((float) j / segs, (float) (i + 1) / rings), u11 = uv((float) (j + 1) / segs, (float) (i + 1) / rings);
                // smooth shading: the normal of a point on a sphere is the point itself
                if (i != 0) out.add(new float[][]{p00, p01, p11, unit(p00), unit(p01), unit(p11), u00, u01, u11});
                if (i != rings - 1) out.add(new float[][]{p00, p11, p10, unit(p00), unit(p11), unit(p10), u00, u11, u10});
            }
        }
    }

    /** One of SHAPES, or null if the name is unknown. Flat shapes face forward like the Textures item. */
    public static ObjModel shape(String id) {
        if (id == null) return null;
        List<float[][]> t = new ArrayList<>();
        float h = 0.5f;
        switch (id) {
            case "square" -> quad(t, p(-h, -h, 0), p(h, -h, 0), p(h, h, 0), p(-h, h, 0), false);
            case "triangle" -> tri(t, p(-h, -h, 0), p(h, -h, 0), p(0, h, 0), uv(0, 1), uv(1, 1), uv(0.5f, 0), false);
            case "cube" -> {
                quad(t, p(-h, -h, h), p(h, -h, h), p(h, h, h), p(-h, h, h), true);
                quad(t, p(h, -h, -h), p(-h, -h, -h), p(-h, h, -h), p(h, h, -h), true);
                quad(t, p(h, -h, h), p(h, -h, -h), p(h, h, -h), p(h, h, h), true);
                quad(t, p(-h, -h, -h), p(-h, -h, h), p(-h, h, h), p(-h, h, -h), true);
                quad(t, p(-h, h, h), p(h, h, h), p(h, h, -h), p(-h, h, -h), true);
                quad(t, p(-h, -h, -h), p(h, -h, -h), p(h, -h, h), p(-h, -h, h), true);
            }
            case "pyramid" -> {
                float[] top = p(0, h, 0);
                float[] b0 = p(-h, -h, h), b1 = p(h, -h, h), b2 = p(h, -h, -h), b3 = p(-h, -h, -h);
                quad(t, b0, b1, b2, b3, true);
                tri(t, b0, b1, top, uv(0, 1), uv(1, 1), uv(0.5f, 0), true);
                tri(t, b1, b2, top, uv(0, 1), uv(1, 1), uv(0.5f, 0), true);
                tri(t, b2, b3, top, uv(0, 1), uv(1, 1), uv(0.5f, 0), true);
                tri(t, b3, b0, top, uv(0, 1), uv(1, 1), uv(0.5f, 0), true);
            }
            case "sphere" -> sphere(t);
            default -> { return null; }
        }
        return build(t);
    }

    /** Draws every triangle as a degenerate quad (entity render types use QUADS). */
    public void emit(PoseStack.Pose pose, VertexConsumer buf, int light, int argb) {
        int a = argb >>> 24 & 255, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        for (int tri = 0; tri < pos.length / 9; tri++) {
            for (int k = 0; k < 4; k++) {
                int corner = Math.min(k, 2);
                int c = tri * 9 + corner * 3;
                int u = tri * 6 + corner * 2;
                buf.addVertex(pose, pos[c], pos[c + 1], pos[c + 2])
                        .setColor(r, g, b, a)
                        .setUv(uv[u], uv[u + 1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(pose, nrm[c], nrm[c + 1], nrm[c + 2]);
            }
        }
    }
}
