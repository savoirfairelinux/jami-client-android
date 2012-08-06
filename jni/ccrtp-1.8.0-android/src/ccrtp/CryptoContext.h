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



#ifndef CRYPTOCONTEXT_H
#define CRYPTOCONTEXT_H

#include <cc++/config.h>

#include <ccrtp/rtppkt.h>


#define REPLAY_WINDOW_SIZE 64


const int SrtpAuthenticationNull     =  0;
const int SrtpAuthenticationSha1Hmac =  1;
const int SrtpAuthenticationSkeinHmac = 2;

const int SrtpEncryptionNull  = 0;
const int SrtpEncryptionAESCM = 1;
const int SrtpEncryptionAESF8 = 2;
const int SrtpEncryptionTWOCM = 3;
const int SrtpEncryptionTWOF8 = 4;

#ifdef SRTP_SUPPORT
#include <ccrtp/crypto/AesSrtp.h>
#endif

#ifdef CCXX_NAMESPACES
namespace ost {
#endif

    class RTPPacket;

    /**
     * The implementation for a SRTP cryptographic context.
     *
     * This class holds data and provides functions that implement a
     * cryptographic context for SRTP, Refer to RFC 3711, chapter 3.2 for some
     * more detailed information about the SRTP cryptographic context.
     *
     * Each SRTP cryptographic context maintains a RTP source identified by
     * its SSRC. Thus you can independently protect each source inside a RTP
     * session.
     *
     * Key management mechanisms negotiate the parameters for the SRTP
     * cryptographic context, such as master key, key length, authentication
     * length and so on. The key management mechanisms are not part of
     * SRTP. Refer to MIKEY (RFC 3880) or to Phil Zimmermann's ZRTP protocol
     * (draft-zimmermann-avt-zrtp-01). After key management negotiated the
     * data the application can setup the SRTP cryptographic context and
     * enable SRTP processing.
     *
     * Currently this implementation supports RTP only, not RTCP.
     *
     * @author Israel Abad <i_abad@terra.es>
     * @author Erik Eliasson <eliasson@it.kth.se>
     * @author Johan Bilien <jobi@via.ecp.fr>
     * @author Joachim Orrblad <joachim@orrblad.com>
     * @author Werner Dittmann <Werner.Dittmann@t-online.de>
     */

    class __EXPORT CryptoContext {
	public:
	/**
	 * Constructor for empty SRTP cryptographic context.
	 *
	 * This constructor creates an empty SRTP cryptographic context were
	 * all algorithms are set to the null algorithm, that is no SRTP processing
	 * is performed.
	 *
	 * @param ssrc The RTP SSRC that this SRTP cryptographic context protects.
	 */
	    CryptoContext( uint32 ssrc );

	/**
	 * Constructor for an active SRTP cryptographic context.
	 *
	 * This constructor creates an active SRTP cryptographic context were
	 * algorithms are enabled, keys are computed and so on. This SRTP
	 * cryptographic context can protect a RTP SSRC stream.
	 *
	 * @param ssrc
	 *    The RTP SSRC that this SRTP cryptographic context protects.
	 *
	 * @param roc
	 *    The initial Roll-Over-Counter according to RFC 3711. These are the
	 *    upper 32 bit of the overall 48 bit SRTP packet index. Refer to
	 *    chapter 3.2.1 of the RFC.
	 *
	 * @param keyDerivRate
	 *    The key derivation rate defines when to recompute the SRTP session
	 *    keys. Refer to chapter 4.3.1 in the RFC.
	 *
	 * @param ealg
	 *    The encryption algorithm to use. Possible values are <code>
	 *    SrtpEncryptionNull, SrtpEncryptionAESCM, SrtpEncryptionAESF8
	 *    </code>. See chapter 4.1.1 for AESCM (Counter mode) and 4.1.2
	 *    for AES F8 mode.
	 *
	 * @param aalg
	 *    The authentication algorithm to use. Possible values are <code>
	 *    SrtpEncryptionNull, SrtpAuthenticationSha1Hmac</code>. The only
	 *    active algorithm here is SHA1 HMAC, a SHA1 based hashed message
	 *    authentication code as defined in RFC 2104.
	 *
	 * @param masterKey
	 *    Pointer to the master key for this SRTP cryptographic context.
	 *    Must point to <code>masterKeyLength</code> bytes. Refer to chapter
	 *    3.2.1 of the RFC about the role of the master key.
	 *
	 * @param masterKeyLength
	 *    The length in bytes of the master key in bytes. The length must
	 *    match the selected encryption algorithm. Because SRTP uses AES
	 *    based  encryption only, then master key length may be 16 or 32
	 *    bytes (128 or 256 bit master key)
	 *
	 * @param masterSalt
	 *    SRTP uses the master salt to computer the initialization vector
	 *    that in turn is input to compute the session key, session
	 *    authentication key and the session salt.
	 *
	 * @param masterSaltLength
	 *    The length in bytes of the master salt data in bytes. SRTP uses
	 *    AES as encryption algorithm. AES encrypts 16 byte blocks
	 *    (independent of the key length). According to RFC3711 the standard
	 *    value for the master salt length should be 112 bit (14 bytes).
	 *
	 * @param ekeyl
	 *    The length in bytes of the session encryption key that SRTP shall
	 *    compute and use. Usually the same length as for the master key
	 *    length. But you may use a different length as well. Be carefull
	 *    that the key management mechanisms supports different key lengths.
	 *
	 * @param akeyl
	 *    The length in bytes of the session authentication key. SRTP
	 *    computes this key and uses it as input to the authentication
	 *    algorithm.
	 *    The standard value is 160 bits (20 bytes).
	 *
	 * @param skeyl
	 *    The length in bytes of the session salt. SRTP computes this salt
	 *    key and uses it as input during encryption. The length usually
	 *    is the same as the master salt length.
	 *
	 * @param tagLength
	 *    The length is bytes of the authentication tag that SRTP appends
	 *    to the RTP packet. Refer to chapter 4.2. in the RFC 3711.
	 */
	    CryptoContext( uint32 ssrc, int32 roc,
			   int64  keyDerivRate,
			   const  int32 ealg,
			   const  int32 aalg,
			   uint8* masterKey,
			   int32  masterKeyLength,
			   uint8* masterSalt,
			   int32  masterSaltLength,
			   int32  ekeyl,
			   int32  akeyl,
			   int32  skeyl,
			   int32  tagLength );
	/**
	 * Destructor.
	 *
	 * Cleans the SRTP cryptographic context.
	 */
	    ~CryptoContext();

	/**
	 * Set the Roll-Over-Counter.
	 *
	 * Ths method sets the upper 32 bit of the 48 bit SRTP packet index
	 * (the roll-over-part)
	 *
	 * @param r
	 *   The roll-over-counter
	 */
	    inline void
	    setRoc(uint32 r)
	    {roc = r;}

	/**
	 * Get the Roll-Over-Counter.
	 *
	 * Ths method get the upper 32 bit of the 48 bit SRTP packet index
	 * (the roll-over-part)
	 *
	 * @return The roll-over-counter
	 */
	    inline uint32
	    getRoc() const
	    {return roc;}

	/**
	 * Perform SRTP encryption.
	 *
	 * This method encrypts <em>and</em> decrypts SRTP payload data. Plain
	 * data gets encrypted, encrypted data get decrypted.
	 *
	 * @param rtp
	 *    The RTP packet that contains the data to encrypt.
	 *
	 * @param index
	 *    The 48 bit SRTP packet index. See the <code>guessIndex</code>
	 *    method.
	 *
	 * @param ssrc
	 *    The RTP SSRC data in <em>host</em> order.
	 */
	    void srtpEncrypt( RTPPacket* rtp, uint64 index, uint32 ssrc );

	/**
	 * Compute the authentication tag.
	 *
	 * Compute the authentication tag according the the paramters in the
	 * SRTP Cryptograhic context.
	 *
	 * @param rtp
	 *    The RTP packet that contains the data to authenticate.
	 *
	 * @param roc
	 *    The 32 bit SRTP roll-over-counter.
	 *
	 * @param tag
	 *    Points to a buffer that hold the computed tag. This buffer must
	 *    be able to hold <code>tagLength</code> bytes.
	 */
	    void srtpAuthenticate(RTPPacket* rtp, uint32 roc, uint8* tag );

	/**
	 * Perform key derivation according to SRTP specification
	 *
	 * This method computes the session key, session authentication key and the
	 * session salt key. This method must be called at least once after the
	 * SRTP Cryptograhic context was set up.
	 *
	 * @param index
	 *    The 48 bit SRTP packet index. See the <code>guessIndex</code>
	 *    method.
	 */
	    void deriveSrtpKeys(uint64 index);

	/**
	 * Compute (guess) the new SRTP index based on the sequence number of
	 * a received RTP packet.
	 *
	 * The method uses the algorithm show in RFC3711, Appendix A, to compute
	 * the new index.
	 *
	 * @param newSeqNumber
	 *    The sequence number of the received RTP packet in host order.
	 *
	 * @return The new SRTP packet index
	 */
	    uint64 guessIndex(uint16 newSeqNumber);

	/**
	 * Check for packet replay.
	 *
	 * The method check if a received packet is either to old or was already
	 * received.
	 *
	 * The method supports a 64 packet history relative the the given
	 * sequence number.
	 *
	 * @param newSeqNumber
	 *    The sequence number of the received RTP packet in host order.
	 *
	 * @return <code>true</code> if no replay, <code>false</code> if packet
	 *    is too old ar was already received.
	 */
	    bool checkReplay(uint16 newSeqNumber);

	/**
	 * Update the SRTP packet index.
	 *
	 * Call this method after all checks were successful. See chapter
	 * 3.3.1 in the RFC when to update the ROC and ROC processing.
	 *
	 * @param newSeqNumber
	 *    The sequence number of the received RTP packet in host order.
	 */
	    void update( uint16 newSeqNumber );

	/**
	 * Get the length of the SRTP authentication tag in bytes.
	 *
	 * @return the length of the authentication tag.
	 */
	    inline int32
	    getTagLength() const
	    {return tagLength;}


	/**
	 * Get the length of the MKI in bytes.
	 *
	 * @return the length of the MKI.
	 */
	    inline int32
	    getMkiLength() const
	    {return mkiLength;}

	/**
	 * Get the SSRC of this SRTP Cryptograhic context.
	 *
	 * @return the SSRC.
	 */
	    inline uint32
	    getSsrc() const
	    {return ssrc;}

        /**
         * Derive a new Crypto Context for use with a new SSRC
         *
         * This method returns a new Crypto Context initialized with the data
         * of this crypto context. Replacing the SSRC, Roll-over-Counter, and
         * the key derivation rate the application cab use this Crypto Context
         * to encrypt / decrypt a new stream (Synchronization source) inside
         * one RTP session.
         *
         * Before the application can use this crypto context it must call
         * the <code>deriveSrtpKeys</code> method.
         *
         * @param ssrc
         *     The SSRC for this context
         * @param roc
         *     The Roll-Over-Counter for this context
         * @param keyDerivRate
         *     The key derivation rate for this context
         * @return
         *     a new CryptoContext with all relevant data set.
         */

            CryptoContext* newCryptoContextForSSRC(uint32 ssrc, int roc, int64 keyDerivRate);

	private:

	    uint32 ssrc;
	    bool   using_mki;
	    uint32 mkiLength;
	    uint8* mki;

	    uint32 roc;
	    uint32 guessed_roc;
	    uint16 s_l;
	    int64  key_deriv_rate;

	    /* bitmask for replay check */
	    uint64 replay_window;

	    uint8* master_key;
	    uint32 master_key_length;
	    uint32 master_key_srtp_use_nb;
	    uint32 master_key_srtcp_use_nb;
	    uint8* master_salt;
	    uint32 master_salt_length;

	    /* Session Encryption, Authentication keys, Salt */
	    int32  n_e;
	    uint8* k_e;
	    int32  n_a;
	    uint8* k_a;
	    int32  n_s;
	    uint8* k_s;

	    int32 ealg;
	    int32 aalg;
	    int32 ekeyl;
	    int32 akeyl;
	    int32 skeyl;
	    int32 tagLength;
	    bool  seqNumSet;

        void*   macCtx;

#ifdef SRTP_SUPPORT
	    AesSrtp* cipher;
	    AesSrtp* f8Cipher;
#else
	    void* cipher;
	    void* f8Cipher;
#endif

    };
#ifdef  CCXX_NAMESPACES
}
#endif

#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-default-style: ellemtel
 * c-basic-offset: 4
 * End:
 */

