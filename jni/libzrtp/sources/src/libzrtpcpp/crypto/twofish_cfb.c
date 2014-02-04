#include <stdint.h>
#include <stdlib.h>

#include "twofish.h"

void Twofish_cfb128_encrypt(Twofish_key* keyCtx, Twofish_Byte* in, 
			    Twofish_Byte* out, size_t len,
			    Twofish_Byte* ivec, int32_t *num)
{
  uint32_t n;

  n = *num;

  do {
    while (n && len) {
      *(out++) = ivec[n] ^= *(in++);
      --len;
      n = (n+1) % 16;
    }
    while (len>=16) {
      Twofish_encrypt(keyCtx, ivec, ivec);
      for (n=0; n<16; n+=sizeof(size_t)) {
	*(size_t*)(out+n) =
	  *(size_t*)(ivec+n) ^= *(size_t*)(in+n);
      }
      len -= 16;
      out += 16;
      in  += 16;
    }
    n = 0;
    if (len) {
      Twofish_encrypt(keyCtx, ivec, ivec);
      while (len--) {
	out[n] = ivec[n] ^= in[n];
	++n;
      }
    }
    *num = n;
    return;
  } while (0);
}


void Twofish_cfb128_decrypt(Twofish_key* keyCtx, Twofish_Byte* in, 
			    Twofish_Byte* out, size_t len,
			    Twofish_Byte* ivec, int32_t *num)
{
  uint32_t n;

  n = *num;

  do {
    while (n && len) {
      unsigned char c;
      *(out++) = ivec[n] ^ (c = *(in++)); ivec[n] = c;
      --len;
      n = (n+1) % 16;
    }
    while (len>=16) {
      Twofish_encrypt(keyCtx, ivec, ivec);
      for (n=0; n<16; n+=sizeof(size_t)) {
	size_t t = *(size_t*)(in+n);
	*(size_t*)(out+n) = *(size_t*)(ivec+n) ^ t;
	*(size_t*)(ivec+n) = t;
      }
      len -= 16;
      out += 16;
      in  += 16;
    }
    n = 0;
    if (len) {
      Twofish_encrypt(keyCtx, ivec, ivec);
      while (len--) {
	unsigned char c;
	out[n] = ivec[n] ^ (c = in[n]); ivec[n] = c;
	++n;
      }
    }
    *num = n;
    return;
  } while (0);
}
