/****************************************************************
 * frex:shaders/api/vertex.glsl - Canvas Implementation
 ***************************************************************/

out vec4 frx_vertex;
out vec2 frx_texcoord;
out vec4 frx_vertexColor;

#ifdef DEPTH_PASS
vec3 frx_vertexNormal;
#else
out vec3 frx_vertexNormal;
out vec4 frx_vertexTangent;
out vec3 frx_vertexLight;
out vec4 frx_var0;
out vec4 frx_var1;
out vec4 frx_var2;
out vec4 frx_var3;
out float frx_distance;
#endif
