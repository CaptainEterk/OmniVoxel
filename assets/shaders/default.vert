#version 330 core

#define BITMASK_8 255u
#define BITMASK_4 15u
#define BITMASK_3 7u
#define BITMASK_10 1023u
#define BITMASK_2 3u
#define BITMASK_13 8191u
#define BITMASK_6 0x3Fu
#define CHUNK_SIZE vec3(32.0, 32.0, 32.0)

// TODO: Make this a uniform that can change over time
const float SHADOWS[6] = float[6](1.2, 0.3, 0.4, 0.6, 0.8, 1.0);

const vec3 NORMALS[6] = vec3[6](
vec3(0, 1, 0),
vec3(0, -1, 0),
vec3(0, 0, -1),
vec3(0, 0, 1),
vec3(1, 0, 0),
vec3(-1, 0, 0)
);

layout(location = 0) in uint data1;
layout(location = 1) in uint data2;
layout(location = 2) in uint data3;
layout(location = 3) in vec3 vPosition;
layout(location = 4) in vec2 vUV;
layout (location = 5) in vec2 aPos;

out vec2 TexCoord;
out float shadow;
smooth out vec3 position;
out vec4 lighting;
out float ao;
out vec3 vNormal;
out vec3 faceNormal;
flat out uint blockType;
out vec2 skyUV;

uniform uint meshType;

uniform ivec3 chunkPosition;

uniform vec3 cameraPosition;

uniform mat4 cameraView;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 model;

uniform float time;

float simpleNoise(vec2 pos) {
    float n = dot(pos, vec2(127.1, 311.7));
    return cos(sin(n) * 13.71632);
}

void decodeData(uint data1, uint data2, uint data3, out uint x, out uint y, out uint z, out uint normal, out uint u, out uint v, out uint blockType, out uint r, out uint g, out uint b, out uint s) {
    // Data 1
    x = (data1 >> 22u) & BITMASK_10;
    y = (data1 >> 12u) & BITMASK_10;
    z = (data1 >> 2u)  & BITMASK_10;

    // Data 2
    normal = (data2 >> 29u) & BITMASK_3;
    u = (data2 >> 21u) & BITMASK_8;
    v = (data2 >> 13u) & BITMASK_8;
    blockType = (data2 >> 0u) & BITMASK_13;

    // Data 3
    r = (data3 >> 28u) & BITMASK_4;
    g = (data3 >> 24u) & BITMASK_4;
    b = (data3 >> 20u) & BITMASK_4;
    s = (data3 >> 16u) & BITMASK_4;
}

void main() {
    if (meshType == 0u) {
        uint x, y, z, normal, u, v, r, g, b, s;
        decodeData(data1, data2, data3, x, y, z, normal, u, v, blockType, r, g, b, s);

        // Position
        vec3 xyz = vec3(x, y, z);
        xyz *= 0.0625;
        xyz += chunkPosition*CHUNK_SIZE;

        // Texture
        TexCoord = vec2(float(u), float(v));

        shadow = (normal < 6u) ? SHADOWS[normal] : 1.0;

        // Lighting
        float rf = float(r) / float(BITMASK_4);
        float gf = float(g) / float(BITMASK_4);
        float bf = float(b) / float(BITMASK_4);
        float sf = float(s) / float(BITMASK_4);
        lighting = vec4(rf, gf, bf, sf);

        vec3 toCameraVector = cameraPosition-xyz;
        vec3 viewVector = normalize(toCameraVector);
        vNormal = viewVector;
        faceNormal = NORMALS[normal];

        if (blockType == 1u) {
            xyz.y += simpleNoise(fract(xyz.xz/100)*100 + time / 1000)/5/length(toCameraVector);
        }

        position = xyz;

        gl_Position = projection * view * vec4(position, 1.0);
    } else if (meshType == 1u) {
        position = vPosition;
        TexCoord = vUV;

        gl_Position = projection * cameraView * model * vec4(position, 1.0);
    } else if (meshType == 2u) {
        skyUV = aPos * 0.5 + 0.5;
        gl_Position = vec4(aPos, 0.0, 1.0);
    }
}