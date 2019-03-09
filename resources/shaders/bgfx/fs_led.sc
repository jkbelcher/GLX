$input v_color0, v_texcoord0
  
/*
 * Copyright 2011-2019 Branimir Karadzic. All rights reserved.
 * License: https://github.com/bkaradzic/bgfx#license-bsd-2-clause
 */

#include "../common/common.sh"

SAMPLER2D(s_texColor, 0);

void main()
{
	gl_FragColor = 
    vec4(v_color0.b, v_color0.g, v_color0.r, v_color0.a) *
    texture2D(s_texColor, v_texcoord0);
  if (gl_FragColor.a <= u_alphaRef) {
    discard;
  }
}
