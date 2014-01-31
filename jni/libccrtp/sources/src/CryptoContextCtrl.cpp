/*
  Copyright (C) 2004-2006 the Minisip Team

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
*/

/* Copyright (C) 2004-2012
 *
 * Authors: Israel Abad <i_abad@terra.es>
 *          Erik Eliasson <eliasson@it.kth.se>
 *          Johan Bilien <jobi@via.ecp.fr>
 *          Joachim Orrblad <joachim@orrblad.com>
 *          Werner Dittmann <Werner.Dittmann@t-online.de>
 */

#include <iostream>

#include <ccrtp-config.h>

#ifdef SRTP_SUPPORT
#include <ccrtp/crypto/hmac.h>
#include <ccrtp/crypto/macSkein.h>
#endif

#include <commoncpp/config.h>
#include <commoncpp/export.h>
#include <ccrtp/CryptoContextCtrl.h>
#include <ccrtp/base.h>

NAMESPACE_COMMONCPP

CryptoContextCtrl::CryptoContextCtrl(uint32 ssrc ):
ssrcCtx(ssrc),
using_mki(false),mkiLength(0),mki(NULL), s_l(0),
replay_window(0),
master_key(NULL), master_key_length(0),
master_salt(NULL), master_salt_length(0),
n_e(0),k_e(NULL),n_a(0),k_a(NULL),n_s(0),k_s(NULL),
ealg(SrtpEncryptionNull), aalg(SrtpAuthenticationNull),
ekeyl(0), akeyl(0), skeyl(0),
macCtx(NULL), cipher(NULL), f8Cipher(NULL)
{}

#ifdef SRTP_SUPPORT
CryptoContextCtrl::CryptoContextCtrl(uint32 ssrc,
                                const int32 ealg,
                                const int32 aalg,
                                uint8* master_key,
                                int32 master_key_length,
                                uint8* master_salt,
                                int32 master_salt_length,
                                int32 ekeyl,
                                int32 akeyl,
                                int32 skeyl,
                                int32 tagLength):

ssrcCtx(ssrc),using_mki(false),mkiLength(0),mki(NULL),
replay_window(0), macCtx(NULL), cipher(NULL), f8Cipher(NULL)
{
    this->ealg = ealg;
    this->aalg = aalg;
    this->ekeyl = ekeyl;
    this->akeyl = akeyl;
    this->skeyl = skeyl;

    this->master_key_length = master_key_length;
    this->master_key = new uint8[master_key_length];
    memcpy(this->master_key, master_key, master_key_length);

    this->master_salt_length = master_salt_length;
    this->master_salt = new uint8[master_salt_length];
    memcpy(this->master_salt, master_salt, master_salt_length);

    switch( ealg ) {
        case SrtpEncryptionNull:
            n_e = 0;
            k_e = NULL;
            n_s = 0;
            k_s = NULL;
            break;

        case SrtpEncryptionTWOF8:
            f8Cipher = new SrtpSymCrypto(SrtpEncryptionTWOF8);
            // fall through

        case SrtpEncryptionTWOCM:
            n_e = ekeyl;
            k_e = new uint8[n_e];
            n_s = skeyl;
            k_s = new uint8[n_s];
            cipher = new SrtpSymCrypto(SrtpEncryptionTWOCM);
            break;

        case SrtpEncryptionAESF8:
            f8Cipher = new SrtpSymCrypto(SrtpEncryptionAESF8);
            // fall through

        case SrtpEncryptionAESCM:
            n_e = ekeyl;
            k_e = new uint8[n_e];
            n_s = skeyl;
            k_s = new uint8[n_s];
            cipher = new SrtpSymCrypto(SrtpEncryptionAESCM);
            break;
    }

    switch( aalg ) {
        case SrtpAuthenticationNull:
            n_a = 0;
            k_a = NULL;
            this->tagLength = 0;
            break;

        case SrtpAuthenticationSha1Hmac:
        case SrtpAuthenticationSkeinHmac:
            n_a = akeyl;
            k_a = new uint8[n_a];
            this->tagLength = tagLength;
            break;
    }
}

#endif

CryptoContextCtrl::~CryptoContextCtrl(){

#ifdef SRTP_SUPPORT
    if (mki)
        delete [] mki;

    if (master_key_length > 0) {
        memset(master_key, 0, master_key_length);
        master_key_length = 0;
        delete [] master_key;
    }
    if (master_salt_length > 0) {
        memset(master_salt, 0, master_salt_length);
        master_salt_length = 0;
        delete [] master_salt;
    }
    if (n_e > 0) {
        memset(k_e, 0, n_e);
        n_e = 0;
        delete [] k_e;
    }
    if (n_s > 0) {
        memset(k_s, 0, n_s);
        n_s = 0;
        delete [] k_s;
    }
    if (n_a > 0) {
        memset(k_a, 0, n_a);
        n_a = 0;
        delete [] k_a;
    }
    if (cipher != NULL) {
        delete cipher;
        cipher = NULL;
    }
    if (f8Cipher != NULL) {
        delete f8Cipher;
        f8Cipher = NULL;
    }
    if (macCtx != NULL) {
        switch(aalg) {
            case SrtpAuthenticationSha1Hmac:
                freeSha1HmacContext(macCtx);
                break;

            case SrtpAuthenticationSkeinHmac:
                freeSkeinMacContext(macCtx);
                break;
        }
    }
#endif

    ealg = SrtpEncryptionNull;
    aalg = SrtpAuthenticationNull;
}

void CryptoContextCtrl::srtcpEncrypt( uint8* rtp, size_t len, uint64 index, uint32 ssrc )
{
    if (ealg == SrtpEncryptionNull) {
        return;
    }
#ifdef SRTP_SUPPORT
    if (ealg == SrtpEncryptionAESCM || ealg == SrtpEncryptionTWOCM) {

        /* Compute the CM IV (refer to chapter 4.1.1 in RFC 3711):
        *
        * k_s   XX XX XX XX XX XX XX XX XX XX XX XX XX XX
        * SSRC              XX XX XX XX
        * index                               XX XX XX XX
        * ------------------------------------------------------XOR
        * IV    XX XX XX XX XX XX XX XX XX XX XX XX XX XX 00 00
        *        0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15
        */
        unsigned char iv[16];

        iv[0] = k_s[0];
        iv[1] = k_s[1];
        iv[2] = k_s[2];
        iv[3] = k_s[3];

        // The shifts transform the ssrc and index into network order
        iv[4] = ((ssrc >> 24) & 0xff) ^ k_s[4];
        iv[5] = ((ssrc >> 16) & 0xff) ^ k_s[5];
        iv[6] = ((ssrc >> 8) & 0xff) ^ k_s[6];
        iv[7] = (ssrc & 0xff) ^ k_s[7];

        iv[8] = k_s[8];
        iv[9] = k_s[9];

        iv[10] = ((index >> 24) & 0xff) ^ k_s[10];
        iv[11] = ((index >> 16) & 0xff) ^ k_s[11];
        iv[12] = ((index >> 8) & 0xff) ^ k_s[12];
        iv[13] = (index & 0xff) ^ k_s[13];

        iv[14] = iv[15] = 0;

        cipher->ctr_encrypt(rtp, len, iv);
    }

    if (ealg == SrtpEncryptionAESF8 || ealg == SrtpEncryptionTWOF8) {

        unsigned char iv[16];

        // 4 bytes of the iv are zero
        // the first byte of the RTP header is not used.
        iv[0] = 0;
        iv[1] = 0;
        iv[2] = 0;
        iv[3] = 0;

        // Need the encryption flag
        index = index | 0x80000000;

        // set the index and the encrypt flag in network order into IV
        iv[4] = index >> 24;
        iv[5] = index >> 16;
        iv[6] = index >> 8;
        iv[7] = index;

        // The fixed header follows and fills the rest of the IV
        memcpy(iv+8, rtp, 8);

        cipher->f8_encrypt(rtp, len, iv, f8Cipher);
    }
#endif
}

/* Warning: tag must have been initialized */
void CryptoContextCtrl::srtcpAuthenticate(uint8* rtp, size_t len, uint32 index, uint8* tag )
{
    if (aalg == SrtpAuthenticationNull) {
        return;
    }
#ifdef SRTP_SUPPORT
    int32_t macL;

    unsigned char temp[20];
    const unsigned char* chunks[3];
    unsigned int chunkLength[3];
    uint32_t beIndex = htonl(index);

    chunks[0] = rtp;
    chunkLength[0] = len;

    chunks[1] = (unsigned char *)&beIndex;
    chunkLength[1] = 4;
    chunks[2] = NULL;

    switch (aalg) {
    case SrtpAuthenticationSha1Hmac:
        hmacSha1Ctx(macCtx,
                    chunks,           // data chunks to hash
                    chunkLength,      // length of the data to hash
                    temp, &macL);
        /* truncate the result */
        memcpy(tag, temp, getTagLength());
        break;
    case SrtpAuthenticationSkeinHmac:
        macSkeinCtx(macCtx,
                    chunks,           // data chunks to hash
                    chunkLength,      // length of the data to hash
                    temp);
        /* truncate the result */
        memcpy(tag, temp, getTagLength());
        break;
    }
#endif
}

#ifdef SRTP_SUPPORT
/* used by the key derivation method */
static void computeIv(unsigned char* iv, uint8 label, uint8* master_salt)
{
    //printf( "Key_ID: %llx\n", key_id );

    /* compute the IV
       key_id:                           XX XX XX XX XX XX XX
       master_salt: XX XX XX XX XX XX XX XX XX XX XX XX XX XX
       ------------------------------------------------------------ XOR
       IV:          XX XX XX XX XX XX XX XX XX XX XX XX XX XX 00 00
    */

    memcpy(iv, master_salt, 14);
    iv[7] ^= label;

    iv[14] = iv[15] = 0;
}
#endif

/* Derives the srtp session keys from the master key */
void CryptoContextCtrl::deriveSrtcpKeys()
{
#ifdef SRTP_SUPPORT
    uint8 iv[16];

    // prepare AES cipher to compute derived keys.
    cipher->setNewKey(master_key, master_key_length);
    memset(master_key, 0, master_key_length);

    // compute the session encryption key
    uint8 label = 3;
    computeIv(iv, label, master_salt);
    cipher->get_ctr_cipher_stream(k_e, n_e, iv);

    // compute the session authentication key
    label = 4;
    computeIv(iv, label, master_salt);
    cipher->get_ctr_cipher_stream(k_a, n_a, iv);

    // Initialize MAC context with the derived key
    switch (aalg) {
    case SrtpAuthenticationSha1Hmac:
        macCtx = createSha1HmacContext(k_a, n_a);
        break;
    case SrtpAuthenticationSkeinHmac:
        // Skein MAC uses number of bits as MAC size, not just bytes
        macCtx = createSkeinMacContext(k_a, n_a, tagLength*8, Skein512);
        break;
    }
    memset(k_a, 0, n_a);

    // compute the session salt
    label = 5;
    computeIv(iv, label, master_salt);
    cipher->get_ctr_cipher_stream(k_s, n_s, iv);
    memset(master_salt, 0, master_salt_length);

    // as last step prepare ciphers with derived key.
    cipher->setNewKey(k_e, n_e);
    if (f8Cipher != NULL)
        cipher->f8_deriveForIV(f8Cipher, k_e, n_e, k_s, n_s);
    memset(k_e, 0, n_e);
#endif
}

bool CryptoContextCtrl::checkReplay( uint32 index )
{
#ifdef SRTP_SUPPORT
    if ( aalg == SrtpAuthenticationNull && ealg == SrtpEncryptionNull ) {
        /* No security policy, don't use the replay protection */
        return true;
    }

    int64 delta = s_l - index;
    if (delta > 0) {
        /* Packet not yet received*/
        return true;
    }
    else {
        if( -delta > REPLAY_WINDOW_SIZE ) {
            /* Packet too old */
            return false;
        }
        else {
            if((replay_window >> (-delta)) & 0x1) {
                /* Packet already received ! */
                return false;
        }
        else {
            /* Packet not yet received */
            return true;
        }
    }
}
#else
    return true;
#endif
}

void CryptoContextCtrl::update(uint32 index)
{
#ifdef SRTP_SUPPORT
    int64 delta = index - s_l;

    /* update the replay bitmask */
    if( delta > 0 ){
        replay_window = replay_window << delta;
        replay_window |= 1;
    }
    else {
        replay_window |= ( 1 << delta );
    }
    s_l = index;

#endif
}

CryptoContextCtrl* CryptoContextCtrl::newCryptoContextForSSRC(uint32 ssrc)
{
#ifdef SRTP_SUPPORT
    CryptoContextCtrl* pcc = new CryptoContextCtrl(
            ssrc,
            this->ealg,                              // encryption algo
            this->aalg,                              // authentication algo
            this->master_key,                        // Master Key
            this->master_key_length,                 // Master Key length
            this->master_salt,                       // Master Salt
            this->master_salt_length,                // Master Salt length
            this->ekeyl,                             // encryption keyl
            this->akeyl,                             // authentication key len
            this->skeyl,                             // session salt len
            this->tagLength);                        // authentication tag len

    return pcc;
#else
    return NULL;
#endif
}

END_NAMESPACE

/** EMACS **
 * Local variables:
 * mode: c++
 * c-default-style: ellemtel
 * c-basic-offset: 4
 * End:
 */

