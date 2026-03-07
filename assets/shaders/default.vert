#version 330 core

#define BITMASK_8 255u
#define BITMASK_4 15u
#define BITMASK_3 7u
#define BITMASK_10 1023u
#define BITMASK_2 3u
#define CHUNK_SIZE vec3(32.0, 32.0, 32.0)

// TODO: Make this a uniform that can change over time
#define SHADOWS float[6](1.2, 0.3, 0.4, 0.6, 0.8, 1.0)

layout(location = 0) in uint data1;
layout(location = 1) in uint data2;
layout(location = 2) in uint data3;
layout(location = 3) in vec3 vPosition;
layout(location = 4) in vec2 vUV;

out vec2 TexCoord;
out float shadow;
smooth out vec3 position;
out vec3 lighting;
out float ao;
out vec3 vNormal;
flat out uint blockType;

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

void main() {
    if (meshType == 0u) {
        uint x = (data1 >> 22u) & BITMASK_10;
        uint y = (data1 >> 12u) & BITMASK_10;
        uint z = (data1 >> 2u)  & BITMASK_10;

        ao = float(data1 & BITMASK_2);

        vec3 xyz = vec3(x, y, z);

        float r = float((data2 >> 28) & BITMASK_4) / float(BITMASK_4);
        float g = float((data2 >> 24) & BITMASK_4) / float(BITMASK_4);
        float b = float((data2 >> 20) & BITMASK_4) / float(BITMASK_4);

        uint normal = (data2 >> 17) & BITMASK_3;

        float u = float((data2 >> 9) & BITMASK_8);
        float v = float((data2 >> 1) & BITMASK_8);
        TexCoord = vec2(u, v);

        xyz *= 0.0625;
        xyz += chunkPosition*CHUNK_SIZE;

        shadow = (normal < 6u) ? SHADOWS[normal] : 1.0;

        lighting = vec3(r, g, b);

        vec3 toCameraVector = cameraPosition-xyz;
        vec3 viewVector = normalize(toCameraVector);
        vNormal = viewVector;

        blockType = data3;

        if (blockType == 1u) {
            xyz.y += simpleNoise(fract(xyz.xz/100)*100 + time / 1000)/5/length(toCameraVector);
        }

        position = xyz;

        gl_Position = projection * view * vec4(position, 1.0);
    } else if (meshType == 1u) {
        position = vPosition;
        TexCoord = vUV;

        gl_Position = projection * cameraView * model * vec4(position, 1.0);
    }
}