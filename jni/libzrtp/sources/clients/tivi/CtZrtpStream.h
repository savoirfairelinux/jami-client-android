/*
 * Tivi client glue code for ZRTP.
 * Copyright (c) 2012 Slient Circle LLC.  All rights reserved.
 *
 *
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 */

#ifndef _CTZRTPSTREAM_H_
#define _CTZRTPSTREAM_H_

#include <map>
#include <vector>

#include <libzrtpcpp/ZrtpCallback.h>
#include <libzrtpcpp/ZrtpSdesStream.h>

#include <CtZrtpSession.h>
#include <TiviTimeoutProvider.h>

// Define sizer of internal buffers.
// NOTE: ZRTP buffer is large. An application shall never use ZRTP protocol
// options that fully use it, otherwise IP packet fragmentation may happen.
static const int maxZrtpSize = 3072;
static const int maxSdesString = 256;

static const uint32_t supressWarn = 200;
static const uint32_t srtpErrorBurstThreshold = 20;

class CryptoContext;
class CryptoContextCtrl;
class ZRtp;
class CtZrtpCb;
class CtZrtpSendCb;
class CtZrtpSession;
class ZrtpSdesStream;
class CMutexClass;

class __EXPORT CtZrtpStream: public ZrtpCallback  {

public:

    CtZrtpSession::tiviStatus getCurrentState() {return tiviState;}

    CtZrtpSession::tiviStatus  getPreviousState() {return prevTiviState;}

protected:

    CtZrtpSession::streamName  index;      //!< either audio or video. Index in stream array
    CtZrtpSession::streamType  type;       //!< Master or slave stream. Necessary to handle multi-stream
    ZRtp              *zrtpEngine;         //!< The ZRTP core class of this stream
    uint32_t          ownSSRC;             //!< Our own SSRC, in host order

    uint64_t          zrtpProtect;
    uint64_t          sdesProtect;

    uint64_t          zrtpUnprotect;
    uint64_t          sdesUnprotect;
    uint64_t          unprotectFailed;

    bool              enableZrtp;          //!< Enable the streams ZRTP engine
    bool              started;             //!< This stream's ZRTP engine is started
    bool              isStopped;           //!< Stream stopped by Tivi
    CtZrtpSession     *session;

    CtZrtpStream();
    friend class CtZrtpSession;
    friend class TimeoutProvider<std::string, CtZrtpStream*>;


    virtual ~CtZrtpStream();
    /**
     * Handle timeout event forwarded by the TimeoutProvider.
     *
     * Just call the ZRTP engine for further processing.
     */
    void handleTimeout(const std::string &c);

    /**
     * Set the application's callback class.
     *
     * @param ucb
     *     Implementation of the application's callback class
     */
    void setUserCallback(CtZrtpCb* ucb);

    /**
     * Set the application's send data callback class.
     *
     *
     * @param ucb
     *     Implementation of the application's send data callback class
     */
    void setSendCallback(CtZrtpSendCb* scb);

    /**
     * Stop this stream and reset internal variables to initial state.
     *
     */
    void stopStream();

    /**
     * @brief Process outgoing data.
     *
     * Depending on the state of the buffer the functions either returns the buffer
     * umodified or encrypted.
     * 
     * The function takes a uint8_t buffer that must contain RTP packet data. The
     * function also assumes that the RTP packet contains all protocol relevant fields
     * (SSRC, sequence number etc.) in network order.
     *
     * When encrypting the buffer must big enough to store additional data, usually
     * 10 bytes if the application set the full authentication length (80 bit).
     *
     * @param buffer contains data in RTP packet format
     * 
     * @param length length of the RTP packet data in buffer.
     *
     * @param newLength returns the new length of the RTP data. When encrypting
     *                  @c newLength covers the additional SRTP authentication data.
     *
     * @return
     *  - @c true application shall send packet to the recipient.
     *  - @c false don't send the packet.
     */
    bool processOutgoingRtp(uint8_t *buffer, size_t length, size_t *newLength);

    /**
     * @brief Process incoming data.
     *
     * Depending on the state of the buffer the functions either returns the RTP data
     * in the buffer either umodified or decrypted. An additional status is @c drop.
     * The functions returns this status if the application must not process this
     * RTP data. The function handled these packets as ZRTP packets.
     *
     * The function takes a uint8_t buffer that must contain RTP or ZRTP packet data.
     * The function also assumes that the RTP/ZRTP packet contains all protocol relevant
     * fields (SSRC, sequence number etc.) in network order or in the order defined
     * for the protocol.
     *
     * @param buffer contains data in RTP/ZRTP packet format
     *
     * @param length length of the RTP/ZRTP packet data in buffer.
     *
     * @param newLength returns the new length of the RTP data. When encrypting
     *                  @c newLength covers the additional SRTP authentication data.
     *
     * @return 1: success, 0: not an error but drop packet, -1: SRTP authentication failed,
     *            -2: SRTP replay check failed
     */
    int32_t processIncomingRtp(uint8_t* buffer, const size_t length, size_t* newLength);

    /**
     * @brief Get the ZRTP Hello hash to be used for signaling
     *
     * Refer to RFC 6189 chapter 8 to get the full documentation on the intercation
     * between ZRTP and a signaling layer.
     *
     * @param helloHash points to a character buffer with a length of at least 65 characters.
     *                  The method fills it with the hex string part of the ZRTP hello hash and
     *                  terminates it with a @c nul byte.
     *
     * @param index  Hello hash of the Hello packet identfied by index. Index must 
     *               be 0 <= index < getNumberSupportedVersions().
     *
     * @return the number of characters in the @c helloHash buffer.
     */
    int getSignalingHelloHash(char *helloHash, int32_t index);

    /**
     * @brief Set the ZRTP Hello hash from signaling
     *
     * Refer to RFC 6189 chapter 8 to get the full documentation on the intercation
     * between ZRTP and a signaling layer.
     *
     * @param helloHash is the ZRTP hello hash string from the signaling layer
     */
    void setSignalingHelloHash(const char *helloHash);

    /**
     * @brief Checks the security state of the stream.
     *
     * @return non null if either @c eSecure, @c eSecureMitm , @c eSecureMitmVia
     *         or @c eSecureSdes is set.
     */
    int isSecure();

    /**
     * Return information to tivi client.
     *
     * @param key which information to return
     *
     * @param buffer points to buffer that gets the information
     *
     * @param maxLen length of the buffer
     */
    int getInfo(const char *key, char *buffer, int maxLen);

    bool isStarted() {return started;}

    bool isEnabled() {return enableZrtp;}

    /**
     * Accept enrollment for the active peer.
     *
     * The method checks if a name is already set in the name cache. If no name
     * is found then set the name for this peer in the name cache.
     * 
     * @param p this is the human readable name for this peer.
     */
    int enrollAccepted(char *p);

    /**
     * Denies enrollment for the active peer.
     *
     * The methods resets the stored PBX secret to @c invalid and resets the peer's
     * name in the name cahce to an empty string.
     */
    int enrollDenied();

    /**
     * @brief Creates an SDES crypto string for the SDES/ZRTP stream.
     *
     * Creates and returns a SDES crypto string for the client that sends
     * the SIP INVITE.
     *
     * @param cryptoString points to a char output buffer that receives the
     *                     crypto  string in the raw format, without the any
     *                     signaling prefix, for example @c a=crypto: in case
     *                     of SDP signaling. The function terminates the
     *                     crypto string with a @c nul byte
     *
     * @param maxLen length of the crypto string buffer. On return it contains the
     *               actual length of the crypto string.
     *
     * @param suite defines which crypto suite to use for this stream. The values are
     *              @c AES_CM_128_HMAC_SHA1_80 or @c AES_CM_128_HMAC_SHA1_32.
     *
     * @return @c true if data could be created, @c false otherwise.
     */
    bool createSdes(char *cryptoString, size_t *maxLen, const ZrtpSdesStream::sdesSuites suite =ZrtpSdesStream::AES_CM_128_HMAC_SHA1_32);

    /**
     * @brief Parses an SDES crypto string for the SDES/ZRTP stream.
     *
     * Parses a received crypto string that the application received in a SIP INVITE
     * or SIP 200 OK.
     *
     * An INVITE-ing application shall call this function right after it received
     * the 200 OK from the answering application and must call this function with the
     * @c sipInvite parameter set to @c true. This usually at the same point when
     * it gets the  @c zrtp-hash from the SDP parameters. This application's SRTP
     * environment is now ready. The method ignores the @c sendCryptoStr parameter
     * and its length if @c sipInvite is true.
     *
     * The answering application calls this function after it received the INVITE and
     * extracted the crypto string from the SDP and must call this function with the
     * @c sipInvite parameter set to @c false. This is usually the same point when
     * it gets the @c zrtp-hash from the SDP parameters. The answering client must
     * provide a @c sendCryptoStr buffer. The method fills this buffer with the crypto
     * string that the answering client sends with 200 OK.
     *
     * @param recvCryptoStr points to the received crypto string in raw format,
     *                     without any signaling prefix, for example @c
     *                     a=crypto: in case of SDP signaling.
     *
     * @param recvLenght length of the received crypto string. If the length is
     *               @c zero then the method uses @c strlen to compute
     *               the length.
     *
     * @param sendCryptoStr points to a buffer. The method stores a crypto string
     *                     in raw format in this buffer (without any signaling prefix, for
     *                     example @c a=crypto: in case of SDP signaling. If the answering client
     *                     does not provide a buffer (sendCryptoStr == NULL) then the method
     *                     stores the string in a temporary buffer and the client can get the
     *                     string at a later time using getSavedSdes().
     *
     * @param sendLenght length of the send crypto string buffer. On return it contains the
     *                   actual length of the crypto string.
     *
     * @param sipInvite the client that sent the SIP INVITE must set this to  @c true.
     *
     * @return @c true if data could be created, @c false otherwise.
     */
    bool parseSdes(char *recvCryptoStr, size_t recvLength, char *sendCryptoStr, size_t *sendLength, bool sipInvite);

    /**
     * @brief Get the saved SDES crypto string.
     *
     * Refer to parseSdes() documentation.
     * 
     * @param sendCryptoStr points to a buffer. The method stores the saved crypto string
     *                     in this buffer.
     *
     * @param sendLenght length of the send crypto string buffer. On return it contains the
     *                   actual length of the crypto string.
     * 
     * @return @c true if data could be copied, @c false otherwise, i.e buffer length too short.
     */
    bool getSavedSdes(char *sendCryptoStr, size_t *sendLength);

    /**
     * @brief Check if SDES is active and is in SDES secure state.
     *
     * @return @c true if SDES is in secure state, @c false otherwise.
     */
    bool isSdesActive();

    /**
     * @brief Get Crypto Mix attribute string
     *
     * The offerer shall call this method to get a string of @b all supported crypto mix algorithms
     * and shall send this list to the answerer.
     *
     * The answerer shall call this function only @b after it received the crypto mix string and
     * called @c setCryptoMixAttribute(...). In this case the method returns only one (the selected)
     * crypto mix algorithm and the answerer must send this to the offerer in 200 OK for example.
     *
     * @param algoNames points to a buffer that will filled with the crypto mix algorithm names.
     *                  The buffer must be long enough to hold at least the name of the mandatory
     *                  algorithm HMAC-SHA-384.
     *
     * @param length length buffer
     *
     * @return Length of algorithm names (excluding zero byte) or zero if crypto mix not supported or
     *         enabled.
     */
    int getCryptoMixAttribute(char *algoNames, size_t length);

    /**
     * @brief Set Crypto Mix attribute string
     *
     * The method splits the string into algorithm names and checks if it contains an
     * supported algorithm.
     *
     * The answerer must call this method @b before it calls the @c getCryptoMixAttribute() method.
     *
     * The offerer call this method only @b after it received the selected algorithm in the answer.
     *
     * @param algoNames points to a buffer that holds the received crypto mix algorithm names.
     *                  The buffer must be zero terminated.
     *
     * @return @c false if algorithm is not supported.
     */
    bool setCryptoMixAttribute(const char *algoNames);

    /**
     * @brief Reset SDES
     * 
     * This method deletes an existing SDES context unconditionally. The application must make
     * sure that it does not use the SDES context in any way, for example feeding RTP or SRTP packets
     * to this stream.
     * 
     * @param force if set to true then it resets the context unconditionally, otherwise only if
     *              SDES is not in active state.
     */
    void resetSdesContext(bool force =false);

    /**
     * @brief Get number of supported ZRTP protocol versions.
     *
     * @return the number of supported ZRTP protocol versions.
     */
    int32_t getNumberSupportedVersions();

    /**
     * @brief Get the supported ZRTP encapsulation attribute.
     * 
     * Get this attribute value and set it as a SDP parameter to signal support of ZRTP encapsulation.
     *
     * @return the pointer to the attribute cC-string or @c NULL if encapsulation is not supported.
     */
    const char* getZrtpEncapAttribute();

    /**
     * @brief Set the ZRTP encapsulation attribute.
     * 
     * If an application receives the ZRTP encapsulation SDP attribute then it should set the
     * attribute value. The stream uses ZRTP encapsulation only if this SDP parameter is set
     * @b and SDES is available and active.
     * 
     * @param attribute pointer to a C-string that defines the ZRTP encapsulation method.
     *
     * @see getZrtpEncapAttribute
     */
    void setZrtpEncapAttribute(const char *attribute);

    /**
     * @brief Set the auxilliary secret for ZRTP
     * 
     * An application may set an auxilliary secret and the ZRTP stack uses it as
     * additional data to compute the SRTP keys.
     * 
     * Only the master stream (Audio) can use the auxilliary secret because only the
     * master stream performs a Diffie-Hellman negotiation.
     *
     * @param secret the secret data
     * @param length the length of the secret data in bytes
     */
    void setAuxSecret(const unsigned char *secret, int length);

    /*
     * The following methods implement the GNU ZRTP callback interface.
     * For detailed documentation refer to file ZrtpCallback.h
     */
    int32_t sendDataZRTP(const unsigned char* data, int32_t length);

    int32_t activateTimer(int32_t time);

    int32_t cancelTimer();

    void sendInfo(GnuZrtpCodes::MessageSeverity severity, int32_t subCode);

    bool srtpSecretsReady(SrtpSecret_t* secrets, EnableSecurity part);

    void srtpSecretsOff(EnableSecurity part);

    void srtpSecretsOn(std::string c, std::string s, bool verified);

    void handleGoClear();

    void zrtpNegotiationFailed(GnuZrtpCodes::MessageSeverity severity, int32_t subCode);

    void zrtpNotSuppOther();

    void synchEnter();

    void synchLeave();

    void zrtpAskEnrollment(GnuZrtpCodes::InfoEnrollment info);

    void zrtpInformEnrollment(GnuZrtpCodes::InfoEnrollment  info);

    void signSAS(uint8_t* sasHash);

    bool checkSASSignature(uint8_t* sasHash);

    /*
     * End of ZrtpCallback functions.
     */
private:
    CtZrtpSession::tiviStatus  tiviState;  //!< Status reported to Tivi client
    CtZrtpSession::tiviStatus  prevTiviState;  //!< previous status reported to Tivi client

    CryptoContext     *recvSrtp;           //!< The SRTP context for this stream
    CryptoContextCtrl *recvSrtcp;          //!< The SRTCP context for this stream
    CryptoContext     *sendSrtp;           //!< The SRTP context for this stream
    CryptoContextCtrl *sendSrtcp;          //!< The SRTCP context for this stream
    CtZrtpCb          *zrtpUserCallback;
    CtZrtpSendCb      *zrtpSendCallback;

    uint8_t zrtpBuffer[maxZrtpSize];
    char sdesTempBuffer[maxSdesString];
    uint16_t senderZrtpSeqNo;
    uint32_t peerSSRC;
    std::vector<std::string> peerHelloHashes;
    bool     zrtpHashMatch;
    bool     sasVerified;
    bool     helloReceived;
    bool     useSdesForMedia;
    bool     useZrtpTunnel;
    bool     zrtpEncapSignaled;
    ZrtpSdesStream *sdes;

    uint32_t supressCounter;
    uint32_t srtpAuthErrorBurst;
    uint32_t srtpReplayErrorBurst;
    uint32_t srtpDecodeErrorBurst;
    uint32_t zrtpCrcErrors;

    CMutexClass *synchLock;

    char mixAlgoName[20];                   //!< stores name in during getInfo() call

    int role;                               //!< Initiator or Responder role

    void initStrings();
};

#endif /* _CTZRTPSTREAM_H_ */