$input v_color0, v_texcoord0
  
/*
 * Copyright 2011-2019 Branimir Karadzic. All rights reserved.
 * License: https://github.com/bkaradzic/bgfx#license-bsd-2-clause
 */

#include "../common/common.sh"

SAMPLER2D(s_texColor, 0);

void main()
{  
  gl_FragColor = v_color0.bgra * texture2D(s_texColor, v_texcoord0);
  
  // Check alpha ref after masking by texture alpha
  if (gl_FragColor.a <= u_alphaRef) {
    discard;
  }
}
