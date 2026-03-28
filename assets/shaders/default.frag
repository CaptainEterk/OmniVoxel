#version 330 core

// TODO: Make these uniforms
#define TEXTURE_SIZE 16u
#define FAR_FRESNEL_COLOR vec4(0.0, 0.5, 0.5, 0.4)
#define CLOSE_FRESNEL_COLOR vec4(0.0, 0.25, 0.3, 0.95)
in vec2 TexCoord;
in float shadow;
smooth in vec3 position;
in vec4 lighting;
in vec3 vNormal;
in vec3 faceNormal;
flat in uint blockType;

out vec4 FragColor;

// TODO: Make this do text as well (2)
uniform uint meshType;

uniform vec3 cameraPosition;

uniform vec4 fogColor;
uniform float fogFar;
uniform float fogNear;
uniform float time;

uniform sampler2D blockTexture;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float cubicWeight(float t) {
    t = abs(t);
    return (t <= 1.0) ? 1.0 - 2.0*t*t + t*t*t : ((t < 2.0) ? 4.0 - 8.0*t + 5.0*t*t - t*t*t : 0.0);
}

float smoothNoiseCubic(vec2 uv) {
    vec2 i = floor(uv);
    vec2 f = fract(uv);
    float result = 0.0;
    float totalWeight = 0.0;

    for(int dx = -1; dx <= 2; dx++) {
        for(int dz = -1; dz <= 2; dz++) {
            vec2 neighbor = i + vec2(float(dx), float(dz));
            float weight = cubicWeight(f.x - float(dx)) * cubicWeight(f.y - float(dz));
            result += hash(neighbor) * weight;
            totalWeight += weight;
        }
    }

    return result / totalWeight;
}

float simpleNoise(vec2 pos) {
    return smoothNoiseCubic(pos);
}

void main() {
    if (meshType == 0u) {
        FragColor = texture(blockTexture, TexCoord / TEXTURE_SIZE);
        if (FragColor.a == 0) discard;

        float distance = length(position-cameraPosition);
        float fogFactor = (fogFar - distance) / (fogFar - fogNear);
        fogFactor = clamp(fogFactor, 0.0, 1.0);
        if (fogFactor == 0) {
            discard;
        }

        if (blockType == 1u) {
            float fresnel = 1.0 - abs(dot(vNormal, normalize(faceNormal)));
            FragColor *= vec4(vec3(0.5), fresnel);
            FragColor += CLOSE_FRESNEL_COLOR*fresnel+FAR_FRESNEL_COLOR*(1-fresnel);
        }

        float light = clamp(lighting.a, 0.0, 1.0);

        float ambient = 0.05;

        float finalLight = max(light, ambient);

        // apply lighting
        FragColor.rgb *= finalLight * shadow;
        FragColor = mix(fogColor, FragColor, fogFactor);
        // TODO: Mix with filter color too for water and things.
    } else if (meshType == 1u) {
        FragColor = texture(blockTexture, TexCoord);
    } else if (meshType == 2u) {

    }
}