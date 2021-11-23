$input a_position, a_color0, a_texcoord0
$output v_texcoord0, v_color0

/*
 * Copyright 2011-2019 Branimir Karadzic. All rights reserved.
 * License: https://github.com/bkaradzic/bgfx#license-bsd-2-clause
 */

#include "../common/common.sh"

uniform vec4 u_dimensions;

#define u_screenWidth u_dimensions.x
#define u_screenHeight u_dimensions.y
#define u_aspectRatio u_dimensions.z
#define u_pointScale u_dimensions.w

void main()
{
  gl_Position =
    mul(u_modelViewProj, vec4(a_position, 1.0)) +
    mul(u_pointScale, vec4(vec2(1.0, u_aspectRatio) * (a_texcoord0 - vec2(0.5, 0.5)), 0.0, 0.0));
  
  v_texcoord0 = a_texcoord0;
  v_color0 = a_color0;
}
