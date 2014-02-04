#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <crypto/hmac256.h>

/*
HKDF-Expand(PRK, info, L)

Description in RFC 5869

   HKDF-Expand(PRK, info, L) -> OKM

   Options:
      Hash     a hash function; HashLen denotes the length of the
               hash function output in octets
   Inputs:
      PRK      a pseudorandom key of at least HashLen octets
               (usually, the output from the extract step)
      info     optional context and application specific information
               (can be a zero-length string)
      L        length of output keying material in octets
               (<= 255*HashLen)

   Output:
      OKM      output keying material (of L octets)

   The output OKM is calculated as follows:

   N = ceil(L/HashLen)
   T = T(1) | T(2) | T(3) | ... | T(N)
   OKM = first L octets of T

   where:
   T(0) = empty string (zero length)
   T(1) = HMAC-Hash(PRK, T(0) | info | 0x01)
   T(2) = HMAC-Hash(PRK, T(1) | info | 0x02)
   T(3) = HMAC-Hash(PRK, T(2) | info | 0x03)
   ...

   (where the constant concatenated to the end of each T(n) is a
   single octet.)


   A.1.  Test Case 1

   Basic test case with SHA-256

   Hash = SHA-256
   IKM  = 0x0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b (22 octets)
   salt = 0x000102030405060708090a0b0c (13 octets)
   info = 0xf0f1f2f3f4f5f6f7f8f9 (10 octets)
   L    = 42

   PRK  = 0x077709362c2e32df0ddc3f0dc47bba63
          90b6c73bb50f9c3122ec844ad7c2b3e5 (32 octets)
   OKM  = 0x3cb25f25faacd57a90434f64d0362f2a
          2d2d0a90cf1a5a4c5db02d56ecc4c5bf
          34007208d5b887185865 (42 octets)

A.2.  Test Case 2

   Test with SHA-256 and longer inputs/outputs

   Hash = SHA-256
   IKM  = 0x000102030405060708090a0b0c0d0e0f
          101112131415161718191a1b1c1d1e1f
          202122232425262728292a2b2c2d2e2f
          303132333435363738393a3b3c3d3e3f
          404142434445464748494a4b4c4d4e4f (80 octets)
   salt = 0x606162636465666768696a6b6c6d6e6f
          707172737475767778797a7b7c7d7e7f
          808182838485868788898a8b8c8d8e8f
          909192939495969798999a9b9c9d9e9f
          a0a1a2a3a4a5a6a7a8a9aaabacadaeaf (80 octets)
   info = 0xb0b1b2b3b4b5b6b7b8b9babbbcbdbebf
          c0c1c2c3c4c5c6c7c8c9cacbcccdcecf
          d0d1d2d3d4d5d6d7d8d9dadbdcdddedf
          e0e1e2e3e4e5e6e7e8e9eaebecedeeef
          f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff (80 octets)
   L    = 82

   PRK  = 0x06a6b88c5853361a06104c9ceb35b45c
          ef760014904671014a193f40c15fc244 (32 octets)
   OKM  = 0xb11e398dc80327a1c8e7f78c596a4934
          4f012eda2d4efad8a050cc4c19afa97c
          59045a99cac7827271cb41c65e590e09
          da3275600c2f09b8367793a9aca3db71
          cc30c58179ec3e87c14c01d5c1f3434f
          1d87 (82 octets)

A.3.  Test Case 3

   Test with SHA-256 and zero-length salt/info

   Hash = SHA-256
   IKM  = 0x0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b (22 octets)
   salt = (0 octets)
   info = (0 octets)
   L    = 42

   PRK  = 0x19ef24a32c717b167f33a91d6f648bdf
          96596776afdb6377ac434c1c293ccb04 (32 octets)
   OKM  = 0x8da4e775a563c18f715f802a063c5a31
          b8a11f5c5ee1879ec3454e5f3c738d2d
          9d201395faa4b61a96c8 (42 octets)

*/

static void hexdump(const char* title, const unsigned char *s, int l)
{
    int n=0;

    if (s == NULL) return;

    fprintf(stderr, "%s",title);
    for( ; n < l ; ++n) {
        if((n%16) == 0)
            fprintf(stderr, "\n%04x",n);
        fprintf(stderr, " %02x",s[n]);
    }
    fprintf(stderr, "\n");
}


static uint8_t info_A1[] = {
    0xf0, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9};

static int32_t L_A1 = 42;

static uint8_t PRK_A1[] = {
    0x07, 0x77, 0x09, 0x36, 0x2c, 0x2e, 0x32, 0xdf, 0x0d, 0xdc, 0x3f, 0x0d, 0xc4, 0x7b, 0xba, 0x63,
    0x90, 0xb6, 0xc7, 0x3b, 0xb5, 0x0f, 0x9c, 0x31, 0x22, 0xec, 0x84, 0x4a, 0xd7, 0xc2, 0xb3, 0xe5};

static uint8_t OKM_A1[] = {
    0x3c, 0xb2, 0x5f, 0x25, 0xfa, 0xac, 0xd5, 0x7a, 0x90, 0x43, 0x4f, 0x64, 0xd0, 0x36, 0x2f, 0x2a,
    0x2d, 0x2d, 0x0a, 0x90, 0xcf, 0x1a, 0x5a, 0x4c, 0x5d, 0xb0, 0x2d, 0x56, 0xec, 0xc4, 0xc5, 0xbf,
    0x34, 0x00, 0x72, 0x08, 0xd5, 0xb8, 0x87, 0x18, 0x58, 0x65}; // (42 octets)


static int32_t L_A3 = 42;

static uint8_t PRK_A3[] = {
    0x19, 0xef, 0x24, 0xa3, 0x2c, 0x71, 0x7b, 0x16, 0x7f, 0x33, 0xa9, 0x1d, 0x6f, 0x64, 0x8b, 0xdf,
    0x96, 0x59, 0x67, 0x76, 0xaf, 0xdb, 0x63, 0x77, 0xac, 0x43, 0x4c, 0x1c, 0x29, 0x3c, 0xcb, 0x04};   // (32 octets)

static uint8_t OKM_A3[] = {
    0x8d, 0xa4, 0xe7, 0x75, 0xa5, 0x63, 0xc1, 0x8f, 0x71, 0x5f, 0x80, 0x2a, 0x06, 0x3c, 0x5a, 0x31,
    0xb8, 0xa1, 0x1f, 0x5c, 0x5e, 0xe1, 0x87, 0x9e, 0xc3, 0x45, 0x4e, 0x5f, 0x3c, 0x73, 0x8d, 0x2d,
    0x9d, 0x20, 0x13, 0x95, 0xfa, 0xa4, 0xb6, 0x1a, 0x96, 0xc8};  // (42 octets)


    
void* createSha256HmacContext(uint8_t* key, int32_t keyLength);
void freeSha256HmacContext(void* ctx);
void hmacSha256Ctx(void* ctx, const uint8_t* data[], uint32_t dataLength[], uint8_t* mac, int32_t* macLength );


static int expand(uint8_t* prk, uint32_t prkLen, uint8_t* info, int32_t infoLen, int32_t L, uint32_t hashLen, uint8_t* outbuffer)
{
    int32_t n;
    uint8_t *T;
    void* hmacCtx;

    const uint8_t* data[4];      // Data pointers for HMAC data, max. 3 plus terminating NULL
    uint32_t dataLen[4];
    int32_t dataIdx = 0;

    uint8_t counter;
    int32_t macLength;

    if (prkLen < hashLen)
        return -1;

    n = (L + (hashLen-1)) / hashLen;

    // T points to buffer that holds concatenated T(1) || T(2) || ... T(N))
    T = reinterpret_cast<uint8_t*>(malloc(n * hashLen));

    // Prepare the HMAC
    hmacCtx = createSha256HmacContext(prk, prkLen);

    // Prepare first HMAC. T(0) has zero length, thus we ignore it in first run.
    // After first run use its output (T(1)) as first data in next HMAC run.
    for (int i = 1; i <= n; i++) {
        if (infoLen > 0 && info != NULL) {
            data[dataIdx] = info;
            dataLen[dataIdx++] = infoLen;
        }
        counter = i & 0xff;
        data[dataIdx] = &counter;
        dataLen[dataIdx++] = 1;

        data[dataIdx] = NULL;
        dataLen[dataIdx++] = 0;

        hmacSha256Ctx(hmacCtx, data, dataLen, T + ((i-1) * hashLen), &macLength);

        dataIdx = 0;
        data[dataIdx] = T + ((i-1) * hashLen);
        dataLen[dataIdx++] = hashLen;
    }
    freeSha256HmacContext(hmacCtx);
    memcpy(outbuffer, T, L);
    free(T);
    return 0;
}

int main(int argc, char *argv[])
{
    uint8_t buffer[500];
    expand(PRK_A1, sizeof(PRK_A1), info_A1, sizeof(info_A1), L_A1, SHA256_DIGEST_LENGTH, buffer);
    if (memcmp(buffer, OKM_A1, L_A1) != 0) {
        fprintf(stderr, "ERROR: Test result A1 mismatch");
        hexdump("Computed result of expand A1", buffer, L_A1);
        hexdump("Expected result of expand A1", OKM_A1, L_A1);
        return 1;
    }

    expand(PRK_A3, sizeof(PRK_A3), NULL, 0, L_A3, SHA256_DIGEST_LENGTH, buffer);
    if (memcmp(buffer, OKM_A3, L_A3) != 0) {
        fprintf(stderr, "ERROR: Test result A3 mismatch");
        hexdump("Computed result of expand A3", buffer, L_A3);
        hexdump("Expected result of expand A3", OKM_A3, L_A3);
        return 1;
    }

    printf("Done\n");
    return 0;
}
