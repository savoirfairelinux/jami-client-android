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

/* Copyright (C) 2004-2006
 *
 * Authors: Israel Abad <i_abad@terra.es>
 *          Erik Eliasson <eliasson@it.kth.se>
 *          Johan Bilien <jobi@via.ecp.fr>
 *      Joachim Orrblad <joachim@orrblad.com>
 *          Werner Dittmann <Werner.Dittmann@t-online.de>
 */

#include <iostream>

#include <config.h>

#ifdef SRTP_SUPPORT
#include <ccrtp/crypto/hmac.h>
#include <ccrtp/crypto/macSkein.h>
#endif

#include <ccrtp/CryptoContext.h>

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

CryptoContext::CryptoContext( uint32 ssrc ):
ssrc(ssrc),
using_mki(false),mkiLength(0),mki(NULL),
roc(0),guessed_roc(0),s_l(0),key_deriv_rate(0),
replay_window(0),
master_key(NULL), master_key_length(0),
master_key_srtp_use_nb(0), master_key_srtcp_use_nb(0),
master_salt(NULL), master_salt_length(0),
n_e(0),k_e(NULL),n_a(0),k_a(NULL),n_s(0),k_s(NULL),
ealg(SrtpEncryptionNull), aalg(SrtpAuthenticationNull),
ekeyl(0), akeyl(0), skeyl(0),
seqNumSet(false), cipher(NULL), f8Cipher(NULL)
{}

#ifdef SRTP_SUPPORT
CryptoContext::CryptoContext(   uint32 ssrc,
                                int32 roc,
                                int64 key_deriv_rate,
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

ssrc(ssrc),using_mki(false),mkiLength(0),mki(NULL),
roc(roc),guessed_roc(0),s_l(0),key_deriv_rate(key_deriv_rate),
replay_window(0),
master_key_srtp_use_nb(0), master_key_srtcp_use_nb(0), seqNumSet(false),
cipher(NULL), f8Cipher(NULL)
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
        f8Cipher = new AesSrtp(SrtpEncryptionTWOCM);

        case SrtpEncryptionTWOCM:
        n_e = ekeyl;
        k_e = new uint8[n_e];
        n_s = skeyl;
        k_s = new uint8[n_s];
        cipher = new AesSrtp(SrtpEncryptionTWOCM);
        break;

        case SrtpEncryptionAESF8:
        f8Cipher = new AesSrtp(SrtpEncryptionAESCM);

        case SrtpEncryptionAESCM:
        n_e = ekeyl;
        k_e = new uint8[n_e];
        n_s = skeyl;
        k_s = new uint8[n_s];
        cipher = new AesSrtp(SrtpEncryptionAESCM);
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

CryptoContext::~CryptoContext(){

    ealg = SrtpEncryptionNull;
    aalg = SrtpAuthenticationNull;

#ifdef SRTP_SUPPORT
    if (mki)
        delete [] mki;

    if (master_key_length > 0) {
        master_key_length = 0;
        delete [] master_key;
    }
    if (master_salt_length > 0) {
        master_salt_length = 0;
        delete [] master_salt;
    }
    if (n_e > 0) {
        n_e = 0;
        delete [] k_e;
    }
    if (n_s > 0) {
        n_s = 0;
        delete [] k_s;
    }
    if (n_a > 0) {
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
}

void CryptoContext::srtpEncrypt( RTPPacket* rtp, uint64 index, uint32 ssrc )
{
    if (ealg == SrtpEncryptionNull) {
        return;
    }
#ifdef SRTP_SUPPORT
    if (ealg == SrtpEncryptionAESCM) {

        /* Compute the CM IV (refer to chapter 4.1.1 in RFC 3711):
         *
         * k_s   XX XX XX XX XX XX XX XX XX XX XX XX XX XX
         * SSRC              XX XX XX XX
         * index                         XX XX XX XX XX XX
         * ------------------------------------------------------XOR
         * IV    XX XX XX XX XX XX XX XX XX XX XX XX XX XX 00 00
         */

        unsigned char iv[16];
        memcpy( iv, k_s, 4 );

        int i;
        for(i = 4; i < 8; i++ ){
        iv[i] = ( 0xFF & ( ssrc >> ((7-i)*8) ) ) ^ k_s[i];
        }
        for(i = 8; i < 14; i++ ){
        iv[i] = ( 0xFF & (unsigned char)( index >> ((13-i)*8) ) ) ^ k_s[i];
        }
        iv[14] = iv[15] = 0;

        int32 pad = rtp->isPadded() ? rtp->getPaddingSize() : 0;
        cipher->ctr_encrypt( const_cast<uint8*>(rtp->getPayload()),
                  rtp->getPayloadSize()+pad, iv);
    }

    if (ealg == SrtpEncryptionAESF8) {

        /* Create the F8 IV (refer to chapter 4.1.2.2 in RFC 3711):
         *
         * IV = 0x00 || M || PT || SEQ  ||      TS    ||    SSRC   ||    ROC
         *      8Bit  1bit  7bit  16bit       32bit        32bit        32bit
         * ------------\     /--------------------------------------------------
         *       XX       XX      XX XX   XX XX XX XX   XX XX XX XX  XX XX XX XX
         */

        unsigned char iv[16];
        uint32 *ui32p = (uint32 *)iv;

        memcpy(iv, rtp->getRawPacket(), 12);
        iv[0] = 0;

        // set ROC in network order into IV
        ui32p[3] = htonl(roc);

        int32 pad = rtp->isPadded() ? rtp->getPaddingSize() : 0;
        cipher->f8_encrypt(rtp->getPayload(),
                  rtp->getPayloadSize()+pad,
                  iv, k_e, n_e, k_s, n_s, f8Cipher);
    }
#endif
}

/* Warning: tag must have been initialized */
void CryptoContext::srtpAuthenticate(RTPPacket* rtp, uint32 roc, uint8* tag )
{
    if (aalg == SrtpAuthenticationNull) {
        return;
    }
#ifdef SRTP_SUPPORT
    int32_t macL;

    unsigned char temp[20];
    const unsigned char* chunks[3];
    unsigned int chunkLength[3];
    uint32_t beRoc = htonl(roc);

    chunks[0] = rtp->getRawPacket();
    chunkLength[0] = rtp->getRawPacketSize();

    chunks[1] = (unsigned char *)&beRoc;
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
static void computeIv(unsigned char* iv, uint64 label, uint64 index,
                      int64 kdv, unsigned char* master_salt)
{

    uint64 key_id;

    if (kdv == 0) {
        key_id = label << 48;
    }
    else {
        key_id = ((label << 48) | (index / kdv));
    }

    //printf( "Key_ID: %llx\n", key_id );

    /* compute the IV
       key_id:                           XX XX XX XX XX XX XX
       master_salt: XX XX XX XX XX XX XX XX XX XX XX XX XX XX
       ------------------------------------------------------------ XOR
       IV:          XX XX XX XX XX XX XX XX XX XX XX XX XX XX 00 00
    */

    int i;
    for(i = 0; i < 7 ; i++ ) {
        iv[i] = master_salt[i];
    }

    for(i = 7; i < 14 ; i++ ) {
        iv[i] = (unsigned char)(0xFF & (key_id >> (8*(13-i)))) ^
        master_salt[i];
    }

    iv[14] = iv[15] = 0;
}
#endif

/* Derives the srtp session keys from the master key */
void CryptoContext::deriveSrtpKeys(uint64 index)
{
#ifdef SRTP_SUPPORT
    uint8 iv[16];

    // prepare AES cipher to compute derived keys.
    cipher->setNewKey(master_key, master_key_length);

    // compute the session encryption key
    uint64 label = 0;
    computeIv(iv, label, index, key_deriv_rate, master_salt);
    cipher->get_ctr_cipher_stream(k_e, n_e, iv);

    // compute the session authentication key
    label = 0x01;
    computeIv(iv, label, index, key_deriv_rate, master_salt);
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
    // compute the session salt
    label = 0x02;
    computeIv(iv, label, index, key_deriv_rate, master_salt);
    cipher->get_ctr_cipher_stream(k_s, n_s, iv);

    // as last step prepare AES cipher with derived key.
    cipher->setNewKey(k_e, n_e);
#endif
}

/* Based on the algorithm provided in Appendix A - draft-ietf-srtp-05.txt */
uint64_t CryptoContext::guessIndex(uint16 new_seq_nb )
{
    /*
     * Initialize the sequences number on first call that uses the
     * sequence number. Either GuessIndex() or checkReplay().
     */
    if (!seqNumSet) {
        seqNumSet = true;
        s_l = new_seq_nb;
    }
    if (s_l < 32768){
        if (new_seq_nb - s_l > 32768) {
            guessed_roc = roc - 1;
        }
        else {
            guessed_roc = roc;
        }
    }
    else {
        if (s_l - 32768 > new_seq_nb) {
            guessed_roc = roc + 1;
        }
        else {
            guessed_roc = roc;
        }
    }

    return ((uint64)guessed_roc) << 16 | new_seq_nb;
}

bool CryptoContext::checkReplay( uint16 new_seq_nb )
{
#ifdef SRTP_SUPPORT
    if ( aalg == SrtpAuthenticationNull && ealg == SrtpEncryptionNull ) {
        /* No security policy, don't use the replay protection */
        return true;
    }

    /*
     * Initialize the sequences number on first call that uses the
     * sequence number. Either guessIndex() or checkReplay().
     */
    if (!seqNumSet) {
        seqNumSet = true;
        s_l = new_seq_nb;
    }
    uint64 guessed_index = guessIndex( new_seq_nb );
    uint64 local_index = (((uint64_t)roc) << 16) | s_l;

    int64 delta = guessed_index - local_index;
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

void CryptoContext::update(uint16 new_seq_nb)
{
#ifdef SRTP_SUPPORT
    int64 delta = guessIndex(new_seq_nb) - (((uint64)roc) << 16 | s_l );

    /* update the replay bitmask */
    if( delta > 0 ){
        replay_window = replay_window << delta;
        replay_window |= 1;
    }
    else {
        replay_window |= ( 1 << delta );
    }

    /* update the locally stored ROC and highest sequence number */
    if( new_seq_nb > s_l ) {
        s_l = new_seq_nb;
    }
    if( guessed_roc > roc ) {
        roc = guessed_roc;
        s_l = new_seq_nb;
    }
#endif
}

CryptoContext* CryptoContext::newCryptoContextForSSRC(uint32 ssrc, int roc, int64 keyDerivRate)
{
#ifdef SRTP_SUPPORT
    CryptoContext* pcc = new CryptoContext(
            ssrc,
            roc,                                     // Roll over Counter,
            keyDerivRate,                            // keyderivation << 48,
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

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-default-style: ellemtel
 * c-basic-offset: 4
 * End:
 */

