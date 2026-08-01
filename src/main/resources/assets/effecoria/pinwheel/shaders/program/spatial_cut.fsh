#include veil:space_helper

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;

uniform float Intensity;
uniform float Progress; // 0..1 split amount (open→hold→heal)
uniform float Time;
uniform float SlashSeed;
uniform float CutMode; // 0 = line, 1 = around (two randomized seams)
uniform vec2 FromUV;
uniform vec2 ToUV;
uniform float FromDepth;
uniform float ToDepth;
uniform float BehindCamera;

in vec2 texCoord;

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

// Signed distance to segment a–b in aspect space.
float signedSegDist(vec2 p, vec2 a, vec2 b, out float tAlong, out vec2 normal, out vec2 tangent) {
    vec2 ba = b - a;
    float len2 = max(dot(ba, ba), 1e-8);
    tAlong = clamp(dot(p - a, ba) / len2, 0.0, 1.0);
    vec2 closest = a + ba * tAlong;
    vec2 d = p - closest;
    tangent = normalize(ba);
    normal = normalize(vec2(-ba.y, ba.x));
    return dot(d, normal);
}

vec2 aspectUv(vec2 uv) {
    return vec2(uv.x * getAspectRatio(), uv.y);
}

vec2 deaspect(vec2 uvA) {
    return vec2(uvA.x / getAspectRatio(), uvA.y);
}

void applySeam(
        inout vec3 color,
        vec2 uv,
        vec2 a,
        vec2 b,
        float depthA,
        float depthB,
        float strength,
        float split,
        float seed) {
    if (split < 0.001) {
        return;
    }

    float tAlong;
    vec2 normal;
    vec2 tangent;
    vec2 uvA = aspectUv(uv);
    float sd = signedSegDist(uvA, aspectUv(a), aspectUv(b), tAlong, normal, tangent);
    float absD = abs(sd);

    float endFade = smoothstep(0.0, 0.06, tAlong) * smoothstep(0.0, 0.06, 1.0 - tAlong);

    float cutDepth = mix(depthA, depthB, tAlong);
    float depthSample = texture(DiffuseDepthSampler, texCoord).r;
    float depthGate = 1.0;
    if (cutDepth > 0.0 && cutDepth < 1.0) {
        depthGate = smoothstep(cutDepth - 0.12, cutDepth + 0.04, depthSample);
        depthGate = mix(0.45, 1.0, depthGate);
    }

    float gate = depthGate * endFade;
    // Soft band where the opposing slide is visible
    float bandW = 0.045 + 0.025 * strength;
    float band = exp(-absD * absD / (bandW * bandW)) * gate;
    if (band < 0.02) {
        return;
    }

    // Mega-thin luminous cut edge
    float edgeW = 0.00022 + 0.00012 * strength;
    float edge = exp(-absD * absD / (edgeW * edgeW));

    // Opposite halves slide along the cut (left↔right relative to the seam)
    float side = sd >= 0.0 ? 1.0 : -1.0;
    float slide = (0.018 + 0.022 * strength) * split;
    // Which way "above" goes is seeded so cuts don't all look identical
    float dirFlip = hash(seed) > 0.5 ? 1.0 : -1.0;
    vec2 offset = deaspect(tangent * side * dirFlip * slide);

    vec2 sampleUv = clamp(uv + offset, vec2(0.001), vec2(0.999));

    // Mild chroma only near the seam
    vec2 chromaOff = deaspect(tangent * 0.0022 * strength * split * band);
    float rC = texture(DiffuseSampler0, clamp(sampleUv + chromaOff, vec2(0.001), vec2(0.999))).r;
    float gC = texture(DiffuseSampler0, sampleUv).g;
    float bC = texture(DiffuseSampler0, clamp(sampleUv - chromaOff, vec2(0.001), vec2(0.999))).b;

    color = mix(color, vec3(rC, gC, bC), band * 0.98);

    // Hairline cyan/white cut
    color += vec3(0.85, 0.95, 1.2) * edge * 1.15 * strength * split * gate;
    color += vec3(1.0) * edge * edge * 0.35 * split * gate;
}

void main() {
    vec4 base = texture(DiffuseSampler0, texCoord);
    if (Progress < 0.001 || BehindCamera > 0.5) {
        fragColor = base;
        return;
    }

    float split = clamp(Progress, 0.0, 1.0);
    float strength = clamp(Intensity, 0.35, 1.5);
    vec3 color = base.rgb;
    vec2 uv = texCoord;

    if (CutMode < 0.5) {
        applySeam(color, uv, FromUV, ToUV, FromDepth, ToDepth, strength, split, SlashSeed);
    } else {
        // Two seams through the focus — randomized angles (not a fixed cross)
        vec2 center = ToUV;
        float depth = ToDepth;
        float radius = 0.10 + 0.05 * strength;

        float ang0 = hash(SlashSeed) * 6.2831853;
        // Second cut: avoid near-parallel; bias toward ~50–130° but keep jitter
        float spread = mix(0.85, 2.35, hash(SlashSeed + 17.3));
        if (hash(SlashSeed + 3.1) > 0.5) {
            spread = -spread;
        }
        float ang1 = ang0 + spread;

        for (int i = 0; i < 2; i++) {
            float ang = (i == 0) ? ang0 : ang1;
            vec2 dir = vec2(cos(ang), sin(ang));
            dir.x /= getAspectRatio();
            // Slight length / center jitter so the pair never looks stamped
            float r = radius * mix(0.85, 1.15, hash(SlashSeed + float(i) * 5.7));
            vec2 jitter = vec2(
                    (hash(SlashSeed + float(i) * 11.0) - 0.5) * 0.012,
                    (hash(SlashSeed + float(i) * 13.0) - 0.5) * 0.012);
            vec2 c = center + jitter;
            vec2 a = c - dir * r;
            vec2 b = c + dir * r;
            applySeam(color, uv, a, b, depth, depth, strength, split, SlashSeed + float(i) * 19.0);
        }
    }

    fragColor = vec4(color, 1.0);
}
