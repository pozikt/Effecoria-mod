#include veil:space_helper

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;

uniform float Intensity;
uniform float Progress;
uniform float Time;
uniform float SlashSeed;
uniform float CutMode; // 0 = line segment, 1 = around point
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

// Aspect-corrected distance from point p to segment a–b.
float segDist(vec2 p, vec2 a, vec2 b, out float tAlong, out vec2 normal) {
    vec2 ba = b - a;
    float len2 = max(dot(ba, ba), 1e-8);
    tAlong = clamp(dot(p - a, ba) / len2, 0.0, 1.0);
    vec2 closest = a + ba * tAlong;
    vec2 d = p - closest;
    float dist = length(d);
    normal = dist > 1e-6 ? d / dist : normalize(vec2(-ba.y, ba.x));
    return dist;
}

vec2 aspectUv(vec2 uv) {
    float aspect = getAspectRatio();
    return vec2(uv.x * aspect, uv.y);
}

vec2 deaspect(vec2 uvA) {
    float aspect = getAspectRatio();
    return vec2(uvA.x / aspect, uvA.y);
}

void applySlash(
        inout vec3 color,
        vec2 uv,
        vec2 a,
        vec2 b,
        float depthA,
        float depthB,
        float strength,
        float seed) {
    float tAlong;
    vec2 normal;
    vec2 uvA = aspectUv(uv);
    float d = segDist(uvA, aspectUv(a), aspectUv(b), tAlong, normal);

    float width = 0.0018 + 0.0022 * strength;
    float flash = exp(-d * d / (width * width));
    // Soft ribbon so the seam reads even when not looking dead-on
    float ribbon = exp(-d * d / ((width * 4.5) * (width * 4.5)));

    float cutDepth = mix(depthA, depthB, tAlong);
    float depthSample = texture(DiffuseDepthSampler, texCoord).r;
    float depthGate = 1.0;
    if (cutDepth > 0.0 && cutDepth < 1.0) {
        depthGate = smoothstep(cutDepth - 0.12, cutDepth + 0.04, depthSample);
        depthGate = mix(0.35, 1.0, depthGate);
    }

    // Hard half-space shear: space on one side jumps across the cut
    float side = sign(dot(uvA - aspectUv(a), normal) + 1e-6);
    float shearAmt = (0.035 + 0.04 * strength) * flash * depthGate * (1.0 - Progress * 0.25);
    vec2 shear = deaspect(normal * side * shearAmt * (0.55 + 0.45 * hash(seed)));

    vec2 sampleUv = clamp(uv + shear, vec2(0.001), vec2(0.999));
    vec3 sliced = texture(DiffuseSampler0, sampleUv).rgb;

    // Chromatic edge along the seam
    vec2 tangent = deaspect(normalize(vec2(-normal.y, normal.x)));
    float chroma = 0.006 * strength * flash * depthGate;
    float rC = texture(DiffuseSampler0, clamp(sampleUv + tangent * chroma, vec2(0.001), vec2(0.999))).r;
    float gC = sliced.g;
    float bC = texture(DiffuseSampler0, clamp(sampleUv - tangent * chroma, vec2(0.001), vec2(0.999))).b;
    vec3 edged = vec3(rC, gC, bC);

    float mixW = clamp(flash * 0.95 + ribbon * 0.25, 0.0, 1.0) * depthGate;
    color = mix(color, edged, mixW);
    color += vec3(0.75, 0.92, 1.2) * flash * 0.85 * strength * depthGate;
    color += vec3(1.0, 1.0, 1.0) * flash * flash * 0.35 * strength;
}

void main() {
    vec4 base = texture(DiffuseSampler0, texCoord);
    if (Progress < 0.001 || BehindCamera > 0.5) {
        fragColor = base;
        return;
    }

    float strength = clamp(Intensity * Progress, 0.0, 1.6);
    vec3 color = base.rgb;
    vec2 uv = texCoord;

    if (CutMode < 0.5) {
        // Primary world-anchored cut caster → target
        applySlash(color, uv, FromUV, ToUV, FromDepth, ToDepth, strength, SlashSeed);

        // Parallel micro-cuts (Judgement stack) slightly offset in UV
        vec2 dir = ToUV - FromUV;
        vec2 perp = normalize(vec2(-dir.y, dir.x) + vec2(1e-5));
        for (int i = 1; i <= 3; i++) {
            float fi = float(i);
            float off = (hash(SlashSeed + fi * 5.1) - 0.5) * 0.035 * strength;
            vec2 o = perp * off;
            applySlash(
                    color,
                    uv,
                    FromUV + o,
                    ToUV + o,
                    FromDepth,
                    ToDepth,
                    strength * (0.7 - 0.12 * fi),
                    SlashSeed + fi * 13.0);
        }
    } else {
        // Radial cuts through the focus (around target)
        vec2 center = ToUV;
        float depth = ToDepth;
        float radius = 0.08 + 0.06 * strength;
        int cuts = 4;
        for (int i = 0; i < cuts; i++) {
            float fi = float(i);
            float ang = (6.2831853 * fi / float(cuts)) + SlashSeed * 0.01 + Time * 0.15;
            ang += (hash(SlashSeed + fi * 7.3) - 0.5) * 0.35;
            vec2 dir = vec2(cos(ang), sin(ang));
            dir.x /= getAspectRatio();
            vec2 a = center - dir * radius;
            vec2 b = center + dir * radius;
            applySlash(color, uv, a, b, depth, depth, strength, SlashSeed + fi * 19.0);
        }
    }

    color = mix(base.rgb, color, clamp(strength * 1.15, 0.0, 1.0));
    fragColor = vec4(color, 1.0);
}
