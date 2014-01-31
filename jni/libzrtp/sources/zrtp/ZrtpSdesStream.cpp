#include <stdio.h>
#include <stdint.h>
#include <string.h>

#include <libzrtpcpp/ZrtpSdesStream.h>
#include <libzrtpcpp/ZrtpTextData.h>
#include <libzrtpcpp/ZrtpConfigure.h>
#include <libzrtpcpp/zrtpB64Decode.h>
#include <libzrtpcpp/zrtpB64Encode.h>
#include <srtp/SrtpHandler.h>
#include <srtp/CryptoContext.h>
#include <srtp/CryptoContextCtrl.h>
#include <cryptcommon/ZrtpRandom.h>

#if defined(_WIN32) || defined(_WIN64)
# define snprintf _snprintf
#endif

/*
 * These functions support 256 bit encryption algorithms.
 */
#define MAX_KEY_LEN           32
#define MAX_SALT_LEN          14

/*
 * The ABNF grammar for the crypto attribute is defined below (from RFC 4568):
 *
 *  "a=crypto:" tag 1*WSP crypto-suite 1*WSP key-params *(1*WSP session-param)
 *
 *  tag              = 1*9DIGIT
 */

/*
 * Buffer size for names and other strings inside the crypto string. The parse
 * format below restricts parsing to 99 char to provide space for the @c nul byte.
 */
#define MAX_INNER_LEN 100

/*
 * This format scans a received SDES crypto attribute string according to the
 * grammer shown above but without a "a=crypto:" prefix.
 *
 * The format string parses:
 * - %d - the tag as decimal value
 * - %s - the crypto suite name, limited to 99 chars (see MAX_INNER_LEN)
 * - %s - the key parameters, limited to 99 chars
 * - %n - the number of parsed characters to far. The pointer to the session
 *   parameters is: cryptoString + numParsedChars.
 */
static const char parseCrypto[] = "%d %99s %99s %n";

static const int64_t maxTagValue = 999999999;

static const int minElementsCrypto = 3;

/*
 * The ABNF grammar for the key-param (from RFC 4568):
 *
 *  key-param        = key-method ":" key-info
 *
 * The SRTP specific definitions:
 *
 *  key-method          = srtp-key-method
 *  key-info            = srtp-key-info
 *
 *  srtp-key-method     = "inline"
 *  srtp-key-info       = key-salt ["|" lifetime] ["|" mki]
 *
 */

/*
 * This format parses the key parameter string which is never longer than
 * 99 chars (see parse string above):
 * - the fixed string "inline:"
 * - %[A-Za-z0-9+/=] - base 64 characters of master key||master salt
 * - the fixed separator character '|'
 * - %[0-9^] - the lifetime infomration as string that contains digits and ^
 * - the fixed separator character '|'
 * - %[0-9]:%d - parses and strore MKI value and MKI length, separated by ':'
 *
 * If the key parameter string does not contain the operional fields lifetime
 * and MKI information the respective parameters are not filled.
 */
static const char parseKeyParam[] = " inline:%[A-Za-z0-9+/=]|%[0-9^]|%[0-9]:%d";

static const int minElementsKeyParam = 1;

typedef struct _suite {
    ZrtpSdesStream::sdesSuites suite;
    const char *name;
    int32_t    keyLength;             // key length in bits
    int32_t    saltLength;            // salt lenght in bits
    int32_t    authKeyLength;         // authentication key length in bits
    const char *tagLength;            // tag type hs80 or hs32
    const char *cipher;               // aes1 or aes3
    uint32_t   b64length;             // length of b64 encoded key/saltstring
    uint64_t   defaultSrtpLifetime;   // key lifetimes in number of packets
    uint64_t   defaultSrtcpLifetime;
} suiteParam;

/* NOTE: the b64len of a 128 bit suite is 40, a 256bit suite uses 64 characters */
static suiteParam knownSuites[] = {
    {ZrtpSdesStream::AES_CM_128_HMAC_SHA1_32, "AES_CM_128_HMAC_SHA1_32", 128, 112, 160,
     hs32, aes1, 40, (uint64_t)1<<48, 1<<31
    },
    {ZrtpSdesStream::AES_CM_128_HMAC_SHA1_80, "AES_CM_128_HMAC_SHA1_80", 128, 112, 160,
     hs80, aes1, 40, (uint64_t)1<<48, 1<<31
    },
    {(ZrtpSdesStream::sdesSuites)0, NULL, 0, 0, 0, 0, 0, 0, 0, 0}
};

ZrtpSdesStream::ZrtpSdesStream(const sdesSuites s) :
    state(STREAM_INITALIZED), suite(s), recvSrtp(NULL), recvSrtcp(NULL), sendSrtp(NULL),
    sendSrtcp(NULL), srtcpIndex(0) {
}

ZrtpSdesStream::~ZrtpSdesStream() {
    close();
}

void ZrtpSdesStream::close() {
    delete sendSrtp;
    sendSrtp = NULL;

    delete recvSrtp;
    recvSrtp = NULL;

    delete sendSrtcp;
    sendSrtp = NULL;

    delete recvSrtcp;
    recvSrtp = NULL;
}

bool ZrtpSdesStream::createSdes(char *cryptoString, size_t *maxLen, bool sipInvite) {

    if (sipInvite) {
        if (state != STREAM_INITALIZED)
            return false;
        tag = 1;
    }
    else {
        if (state != IN_PROFILE_READY)
            return false;
    }

    bool s = createSdesProfile(cryptoString, maxLen);
    if (!s)
        return s;

    if (sipInvite) {
        state = OUT_PROFILE_READY;
    }
    else {
        state = SDES_SRTP_ACTIVE;
    }
    return s;

}

bool ZrtpSdesStream::parseSdes(char *cryptoString, size_t length, bool sipInvite) {

    if (sipInvite) {
        if (state != OUT_PROFILE_READY)
            return false;
    }
    else {
        if (state != STREAM_INITALIZED)
            return false;
    }
    sdesSuites tmpSuite;
    int32_t tmpTag;

    bool s = parseCreateSdesProfile(cryptoString, length, &tmpSuite, &tmpTag);
    if (!s)
        return s;

    if (sipInvite) {
        // Check if answerer used same tag and suite as the offerer
        if (tmpTag != tag || suite != tmpSuite)
            return false;
        state = SDES_SRTP_ACTIVE;
    }
    else {
        // Answerer stores tag and suite and uses it in createSdesProfile
        suite = tmpSuite;
        tag = tmpTag;
        state = IN_PROFILE_READY;
    }
    return s;
}

bool ZrtpSdesStream::outgoingRtp(uint8_t *packet, size_t length, size_t *newLength) {

    if (state != SDES_SRTP_ACTIVE || sendSrtp == NULL) {
        *newLength = length;
        return true;
    }
    bool rc = SrtpHandler::protect(sendSrtp, packet, length, newLength);
    if (rc)
        ;//protect++;
    return rc;
}

int ZrtpSdesStream::incomingRtp(uint8_t *packet, size_t length, size_t *newLength) {
    if (state != SDES_SRTP_ACTIVE || recvSrtp == NULL) {    // SRTP inactive, just return with newLength set
        *newLength = length;
        return 1;
    }
    int32_t rc = SrtpHandler::unprotect(recvSrtp, packet, length, newLength);
    if (rc == 1) {
//            unprotect++
    }
    else {
//            unprotectFailed++;
    }
    return rc;
}

bool ZrtpSdesStream::outgoingRtcp(uint8_t *packet, size_t length, size_t *newLength) {
#if 0
SrtpHandler::protectCtrl(CryptoContextCtrl* pcc, uint8_t* buffer, size_t length, size_t* newLength, uint32_t *srtcpIndex)
#endif
    return false;
}

int ZrtpSdesStream::incomingSrtcp(uint8_t *packet, size_t length, size_t *newLength) {
#if 0
int32_t SrtpHandler::unprotectCtrl(CryptoContextCtrl* pcc, uint8_t* buffer, size_t length, size_t* newLength)
#endif
    return 0;
}

const char* ZrtpSdesStream::getCipher() {
    return knownSuites[suite].cipher;
}

const char* ZrtpSdesStream::getAuthAlgo(){
    return knownSuites[suite].tagLength;
}

#ifdef WEAKRANDOM
/*
 * A standard random number generator that uses the portable random() system function.
 *
 * This should be enhanced to use a better random generator
 */
static int _random(unsigned char *output, size_t len)
{
    size_t i;

    for(i = 0; i < len; ++i )
        output[i] = random();

    return( 0 );
}
#else
#include <cryptcommon/ZrtpRandom.h>
static int _random(unsigned char *output, size_t len)
{
    return ZrtpRandom::getRandomData(output, len);
}
#endif

static int b64Encode(const uint8_t *binData, int32_t binLength, char *b64Data, int32_t b64Length)
{
    base64_encodestate _state;
    int codelength;

    base64_init_encodestate(&_state, 0);
    codelength = base64_encode_block(binData, binLength, b64Data, &_state);
    codelength += base64_encode_blockend(b64Data+codelength, &_state);

    return codelength;
}

static int b64Decode(const char *b64Data, int32_t b64length, uint8_t *binData, int32_t binLength)
{
    base64_decodestate _state;
    int codelength;

    base64_init_decodestate(&_state);
    codelength = base64_decode_block(b64Data, b64length, binData, &_state);
    return codelength;
}

bool ZrtpSdesStream::createSdesProfile(char *cryptoString, size_t *maxLen) {

    uint8_t keySalt[((MAX_KEY_LEN + MAX_SALT_LEN + 3)/4)*4] = {0};  /* Some buffer for random data, multiple of 4 */
    char b64keySalt[(MAX_KEY_LEN + MAX_SALT_LEN) * 2] = {'\0'};
    uint32_t sidx;
    int32_t b64Len;

    /* Lookup crypto suite parameters */
    for (sidx = 0; knownSuites[sidx].name != NULL; sidx++) {
        if (knownSuites[sidx].suite == suite)
            break;
    }
    if (sidx >= sizeof(knownSuites)/sizeof(struct _suite)) {
        return false;
    }
    suiteParam *pSuite = &knownSuites[sidx];
    _random(keySalt, sizeof(keySalt));

    AlgorithmEnum& auth = zrtpAuthLengths.getByName(pSuite->tagLength);
    int authn = SrtpAuthenticationSha1Hmac;
    int authKeyLen = pSuite->authKeyLength / 8;
    int tagLength = auth.getKeylen() / 8;

    // If SDES will support other encryption algos - get it here based on
    // the algorithm name in suite
    int cipher = SrtpEncryptionAESCM;

    int keyLenBytes = pSuite->keyLength / 8;
    int saltLenBytes = pSuite->saltLength / 8;

    sendSrtp = new CryptoContext(0,                     // SSRC (used for lookup)
                                 0,                     // Roll-Over-Counter (ROC)
                                 0L,                    // keyderivation << 48,
                                 cipher,                // encryption algo
                                 authn,                 // authtentication algo
                                 keySalt,               // Master Key
                                 keyLenBytes,           // Master Key length
                                 &keySalt[keyLenBytes], // Master Salt
                                 saltLenBytes,          // Master Salt length
                                 keyLenBytes,           // encryption keylen
                                 authKeyLen,            // authentication key len (HMAC key lenght)
                                 saltLenBytes,          // session salt len
                                 tagLength);            // authentication tag len
    sendSrtp->deriveSrtpKeys(0L);

    if (tag == -1)
        tag = 1;

    /* Get B64 code for master key and master salt */
    b64Len = b64Encode(keySalt, keyLenBytes + saltLenBytes, b64keySalt, sizeof(b64keySalt));
    b64keySalt[b64Len] = '\0';
    memset(cryptoString, 0, *maxLen);
    *maxLen = snprintf(cryptoString, *maxLen-1, "%d %s inline:%s", tag, pSuite->name, b64keySalt);

    memset(keySalt, 0, sizeof(keySalt));
    return true;
}

bool ZrtpSdesStream::parseCreateSdesProfile(const char *cryptoStr, size_t length, sdesSuites *parsedSuite, int32_t *outTag) {
    int elements,  i;
    int charsScanned;
    int mkiLength = 0;
    uint8_t keySalt[((MAX_KEY_LEN + MAX_SALT_LEN + 3)/4)*4] = {0};
    uint32_t sidx;

    char cryptoString[MAX_CRYPT_STRING_LEN+1] = {'\0'};

    /* Parsed strings */
    char suiteName[MAX_INNER_LEN]  = {'\0'};
    char keyParams[MAX_INNER_LEN]  = {'\0'};
    char keySaltB64[MAX_INNER_LEN] = {'\0'};
    char lifetime[MAX_INNER_LEN]   = {'\0'};
    char mkiVal[MAX_INNER_LEN]     = {'\0'};

    if (length == 0)
        length = strlen(cryptoStr);

    if (length > MAX_CRYPT_STRING_LEN) {
//        fprintf(stderr, "parseCreateSdesProfile() crypto string too long: %ld, maximum: %d\n", length, MAX_CRYPT_STRING_LEN);
        return false;
    }
    /* make own copy, null terminated */
    memcpy(cryptoString, cryptoStr, length);

    *outTag = -1;
    elements = sscanf(cryptoString, parseCrypto, outTag, suiteName, keyParams, &charsScanned);

    /* Do we have enough elements in the string */
    if (elements < minElementsCrypto) {
//        fprintf(stderr, "parseCreateSdesProfile() to few elements in crypto string: %d, expected: %d\n", elements, minElementsCrypto);
        return false;
    }

    /* Lookup crypto suite */
    for (sidx = 0; knownSuites[sidx].name != NULL; sidx++) {
        if (!strcmp(knownSuites[sidx].name, suiteName))
            break;
    }
    if (sidx >= sizeof(knownSuites)/sizeof(struct _suite)) {
//        fprintf(stderr, "parseCreateSdesProfile() unsupported crypto suite: %s\n", suiteName);
        return false;
    }
    suiteParam *pSuite = &knownSuites[sidx];
    *parsedSuite = pSuite->suite;

    /* Now scan the key parameters */
    elements = sscanf(keyParams, parseKeyParam, keySaltB64, lifetime, mkiVal, &mkiLength);

    /* Currently only one we only accept key||salt B64 string, no other parameters */
    if (elements != minElementsKeyParam) {
//         fprintf(stderr, "parseCreateSdesProfile() wrong number of parameters in key parameters: %d, expected: %d\n",
//                      elements, minElementsKeyParam);
        return false;
    }

    int keyLenBytes = pSuite->keyLength / 8;
    int saltLenBytes = pSuite->saltLength / 8;

    /* Check if key||salt B64 string hast the correct length */
    if (strlen(keySaltB64) != pSuite->b64length) {
//         fprintf(stderr, "parseCreateSdesProfile() B64 key||salt string length does not match: %ld, expected: %d\n",
//                     strlen(keySaltB64), pSuite->b64length);
        return false;
    }

    i = b64Decode(keySaltB64, pSuite->b64length, keySalt, keyLenBytes + saltLenBytes);

    /* Did the B64 decode deliver enough data for key||salt */
    if (i != (keyLenBytes + saltLenBytes)) {
//         fprintf(stderr, "parseCreateSdesProfile() B64 key||salt binary data length does not match: %d, expected: %d\n",
//                     i, keyLenBytes + saltLenBytes);
        return false;
    }

    AlgorithmEnum& auth = zrtpAuthLengths.getByName(pSuite->tagLength);
    int authn = SrtpAuthenticationSha1Hmac;
    int authKeyLen = pSuite->authKeyLength / 8;
    int tagLength = auth.getKeylen() / 8;

    // If SDES will support other encryption algos - get it here based on
    // the algorithm name in suite
    int cipher = SrtpEncryptionAESCM;

    recvSrtp = new CryptoContext(0,                     // SSRC (used for lookup)
                                 0,                     // Roll-Over-Counter (ROC)
                                 0L,                    // keyderivation << 48,
                                 cipher,                // encryption algo
                                 authn,                 // authtentication algo
                                 keySalt,               // Master Key
                                 keyLenBytes,           // Master Key length
                                 &keySalt[keyLenBytes], // Master Salt
                                 saltLenBytes,          // Master Salt length
                                 keyLenBytes,           // encryption keylen
                                 authKeyLen,            // authentication key len (HMAC key lenght)
                                 saltLenBytes,          // session salt len
                                 tagLength);            // authentication tag len
    recvSrtp->deriveSrtpKeys(0L);

    memset(keySalt, 0, sizeof(keySalt));

    return true;
}