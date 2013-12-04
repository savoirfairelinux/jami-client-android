/*
 * Tivi client glue code for ZRTP.
 * Copyright (c) 2012 Slient Circle LLC.  All rights reserved.
 *
 *
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 */
#ifndef _CTZRTPSESSION_H_
#define _CTZRTPSESSION_H_

#include <stdio.h>
#include <stdint.h>
#include <string>
#include <string.h>

#ifndef __EXPORT
  #if (defined _WIN32 || defined __CYGWIN__) && defined(_DLL)
    #define __EXPORT    __declspec(dllimport)
    #define __LOCAL
  #elif __GNUC__ >= 4
    #define __EXPORT    __attribute__ ((visibility("default")))
    #define __LOCAL     __attribute__ ((visibility("hidden")))
  #else
    #define __EXPORT
    #define __LOCAL
  #endif
#endif


class CtZrtpStream;
class CtZrtpCb;
class CtZrtpSendCb;
class ZrtpConfigure;
class CMutexClass;

extern "C" __EXPORT const char *getZrtpBuildInfo();

class __EXPORT CtZrtpSession {

public:
    typedef enum _streamName {
        AudioStream = 0,
        VideoStream = 1,
        AllStreams  = 2             //!< AllStreams is max number of streams
    } streamName;

    typedef enum _streamType {
        NoStream = 0,
        Master,
        Slave
    } streamType;

    typedef enum _tiviStatus {
        eLookingPeer,
        eNoPeer,
        eGoingSecure,
        eError,
        eSecure,
        eSecureMitm,
        eSecureMitmVia,
        eSecureSdes,
        eSecurityDisabled,
        eWrongStream = -1
    } tiviStatus;

    /**
     * Supported SDES crypto suites. Included here again to avoid #include of ZrtpSdesStream.h
     * Keep in sync with same enum in ZrtpSdesStream.
     */
    typedef enum {
        AES_CM_128_HMAC_SHA1_32 = 0,
        AES_CM_128_HMAC_SHA1_80
    } sdesSuites;


    typedef enum _retCodes {
        ok           = 0,       /** OK status */
        fail         = 1       /** General, unspecified failure */
    } returnCodes;

    CtZrtpSession();

    ~CtZrtpSession();

    /**
     * @brief Intialize the cache file singleton.
     *
     * Opens and initializes the cache file instance.
     *
     * @param zidFilename
     *     The name of the ZID file, can be a relative or absolut
     *     filename.
     *
     * @return
     *     1 on success, -1 on failure
     */
    static int initCache(const char *zidFilename);

    /** @brief Initialize CtZrtpSession.
     *
     * Before an application can use ZRTP it has to initialize the
     * ZRTP implementation. This method initializes the timeout
     * thread and opens a file that contains ZRTP specific
     * information such as the applications ZID (ZRTP id) and its
     * retained shared secrets.
     *
     * If one application requires several ZRTP sessions all
     * sessions use the same timeout thread and use the same ZID
     * file. Therefore an application does not need to do any
     * synchronisation regading ZID files or timeouts. This is
     * managed by the ZRTP implementation.
     *
     * The application may specify its own ZID file name. If no
     * ZID file name is specified it defaults to
     * <code>$HOME/.GNUccRTP.zid</code> if the <code>HOME</code>
     * environment variable is set. If it is not set the current
     * directory is used.
     *
     * @param audio
     *     set to @c true if audio stream shout be initialized
     *
     * @param video
     *     set to @c true if video stream shoud be initialized.
     *
     * @param config
     *     this parameter points to ZRTP configuration data. If it is
     *     NULL then ZrtpQueue uses a default setting. Default is NULL.
     *
     * @return
     *     1 on success, ZRTP processing enabled, -1 on failure,
     *     ZRTP processing disabled.
     *
     */
    int init(bool audio, bool video, ZrtpConfigure* config = NULL);

    /**
     * @brief Fills a ZrtpConfiguration based on selected algorithms.
     *
     * The method looks up some global keys and enables or disables various
     * algorithms. The method creates the configuration for the publik key
     * algorithms in a way that follows RFC 6189, chapter 4.1.2
     */
    void setupConfiguration(ZrtpConfigure *conf);

    /**
     * @brief Set the application's callback class.
     *
     * @param ucb
     *     Implementation of the application's callback class
     */
    void setUserCallback(CtZrtpCb* ucb, streamName streamNm);

    /**
     * @brief Set the application's send data callback class.
     *
     *
     * @param ucb
     *     Implementation of the application's send data callback class
     */
    void setSendCallback(CtZrtpSendCb* scb, streamName streamNm);

    /**
     * @brief Start a stream if it is not already started.
     *
     * The method starts a stream if it is not already started and it starts
     * a video stream (Slave) only if the audio stream (Master) is already secure.
     */
    int startIfNotStarted(unsigned int uiSSRC, int streamNm);

    /**
     * @brief Start a stream.
     *
     * If this start command specifies the @c Master stream the method starts it
     * immediately. The ZRTP engine immediatley send the first Hello packet.
     *
     * The functions my delay the start of a @c Slave stream until the @c Master
     * stream enters secure mode. The functions then gets the multi-stream data
     * from the master stream and copies it into the @c Slave streams and starts
     * them.
     *
     * If the @c Master stream is already in secure mode then the function copies
     * the multi-stream parameters to the @c slave and starts it immediately.
     *
     * @param uiSSRC the local SSRC for the stream
     *
     * @param streamNm which stream to start.
     */
    void start(unsigned int uiSSRC, streamName streamNm);

    /**
     * @brief Stop a stream.
     *
     * Stop a stream and remove it from the session. To create a new stream
     * see @c newStream
     * 
     * @param streamNm which stream to stop.
     */
    void stop(streamName streamNm);

    /**
     * @brief Release all streams in this session.
     *
     * All streams are reset to their initiali values. The application may call
     * @c init to initialize stream(s) again. A stream can be started only if it
     * was initialized.
     */
    void release();

    /**
     * @brief Release all resources for the stream.
     *
     * @param streamNm which stream to release.
     */
    void release(streamName streamNm);

    /**
     * @brief Set peer name of current call's peer.
     *
     * Setting the peer name will always use the AudioStream to determine
     * the ZID and set the name into the name cache.
     */
    void setLastPeerNameVerify(const char *name, int iIsMitm);

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
     * @param streamNm specifies which stream to use
     *
     * @return
     *  - @c true application shall send packet to the recipient.
     *  - @c false don't send the packet.
     */
    bool processOutoingRtp(uint8_t *buffer, size_t length, size_t *newLength, streamName streamNm);

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
     * @param streamNm specifies which stream to use
     * 
     * @return
     *       - 1: success,
     *       - 0: drop packet, not an error
     *       - -1: SRTP authentication failed,
     *       - -2: SRTP replay check failed
     */
    int32_t processIncomingRtp(uint8_t *buffer, size_t length, size_t *newLength, streamName streamNm);

    /**
     * @brief Check if a stream was started.
     *
     * @return @c true is started, @c false otherwise.
     */
    bool isStarted(streamName streamNm);

    /**
     * @brief Check if a stream is enabled for ZRTP.
     *
     * For slave streams this flag is @c true if the application called @c start() for
     * this stream but the master stream is not yet in secure state.
     *
     * @return @c true is enabled, @c false otherwise.
     */
    bool isEnabled(streamName streamNm);

    tiviStatus getCurrentState(streamName streamNm);

    tiviStatus getPreviousState(streamName streamNm);

    /**
     * @brief Is ZRTP enabled for this session.
     *
     * @return @c true if ZRTP is enabled, @c false otherwise
     */
    bool isZrtpEnabled();

    /**
     * @brief Is SDES enabled for this session.
     *
     * @return @c true if SDES is enabled, @c false otherwise
     */
    bool isSdesEnabled();

    /**
     * @brief Enable or disable ZRTP processing for this session.
     *
     * If the application enabled ZRTP processing it should also call @c start
     * to really start the ZRTP engines. An application can enable and start ZRTP
     * processing any time during a RTP session.
     *
     * @param yesNo if @c true ZRTP processing is enabled.
     */
    void setZrtpEnabled(bool yesNo);

    /**
     * @brief Enable or disable SDES processing for this session.
     *
     * If SDES processing is not enabled the functions @c createSdes and @c parseSdes
     * always return false.
     * 
     * Enabling SDES processing after SIP signaling ended does not make sense.
     *
     * @param yesNo if @c true SDES processing is enabled.
     */
    void setSdesEnabled(bool yesNo);

    /**
     * @brief Get the ZRTP Hello hash to be used for signaling.
     *
     * Refer to RFC 6189 chapter 8 to get the full documentation on the intercation
     * between ZRTP and a signaling layer.
     *
     * @param helloHash points to a character buffer with a length of at least 65 characters.
     *                  The method fills it with the hex string part of the ZRTP hello hash and
     *                  terminates it with a @c nul byte.
     *
     * @param streamNm specifies which stream for this hello hash.
     *
     * @param index  Hello hash of the Hello packet identfied by index. Index must 
     *               be 0 <= index < getNumberSupportedVersions().
     *
     * @return the number of characters in the @c helloHash buffer.
     */
    int getSignalingHelloHash(char *helloHash, streamName streamNm, int32_t index);

    /**
     * @brief Set the ZRTP Hello hash from signaling.
     *
     * Refer to RFC 6189 chapter 8 to get the full documentation on the intercation
     * between ZRTP and a signaling layer.
     *
     * @param helloHash is the ZRTP hello hash string from the signaling layer
     *
     * @param streamNm specifies the stream
     */
    void setSignalingHelloHash(const char *helloHash, streamName streamNm);

    /**
     * @brief Set verification flag.
     *
     * If the user verified the SAS he/she should press a @c verify button and
     * this button calls this method to set the verified flag in the cache. This
     * always uses the @c Master stream (AudioStream).
     *
     * @param iVerified if not zero it sets the verified flag, otherwise the flag
     *                  is reset.
     */
    void setVerify(int iVerified);

    /**
     * @brief Checks the security state of the stream.
     *
     *
     * @param streamNm specifies which stream to check
     *
     * @return non null if either @c eSecure, @c eSecureMitm , @c eSecureMitmVia
     *         or @c eSecureSdes is set.
     */
    int isSecure(streamName streamNm);

    /**
     * @brief Return information to tivi client.
     *
     * @param key which information to return
     *
     * @param buffer points to buffer that gets the information
     *
     * @param length length of the buffer
     *
     * @param streamNm stream, if not specified the default is @c AudioStream
     */
    int getInfo(const char *key, uint8_t *buffer, size_t length, streamName streamNm =AudioStream);

    /**
     * @brief Accept enrollment for the active peer.
     *
     * The method checks if a name is already set in the name cache. If no name
     * is found then set the name for this peer in the name cache.
     *
     * The Stream is always the master stream.
     *
     * @param p this is the human readable name for this peer.
     */
    int enrollAccepted(char *p);

    /**
     * @brief Denies enrollment for the active peer.
     *
     * The methods resets the stored PBX secret to @c invalid and resets the peer's
     * name in the name cache to an empty string.
     */
    int enrollDenied();

    /**
     * @brief Set the client ID for ZRTP Hello message.
     *
     * The client may set its id to identify itself in the
     * ZRTP Hello message. The maximum length is 16 characters. A
     * shorter id string is possible, it will be filled with blanks. A
     * longer id string will be truncated to 16 characters.
     * 
     * Setting the client's id must be done before calling
     * CtZrtpSession#init().
     *
     * @param id
     *     The client's id string
     */
    void setClientId(std::string id);

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
     * @param streamNm stream identifier.
     *
     * @param suite defines which crypto suite to use for this stream. The values are
     *              @c AES_CM_128_HMAC_SHA1_80 or @c AES_CM_128_HMAC_SHA1_32. Default
     *              if @c AES_CM_128_HMAC_SHA1_32.
     *
     * @return @c true if data could be created, @c false otherwise.
     */
    bool createSdes(char *cryptoString, size_t *maxLen, streamName streamNm, const sdesSuites suite =AES_CM_128_HMAC_SHA1_32);

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
     * @param streamNm stream identifier.
     *
     * @return @c true if data could be created, @c false otherwise.
     */
    bool parseSdes(char *recvCryptoStr, size_t recvLength, char *sendCryptoStr, size_t *sendLength, bool sipInvite, streamName streamNm);

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
     * @param streamNm stream identifier.
     *
     * @return @c true if data could be copied, @c false otherwise, i.e buffer length too short.
     */
    bool getSavedSdes(char *sendCryptoStr, size_t *sendLength, streamName streamNm);

    /**
     * @brief Check if SDES is active.
     *
     * @param streamNm stream identifier.
     *
     * @return @c true if SDES context is available, @c false otherwise.
     */
    bool isSdesActive(streamName streamNm);

    /**
     * @brief Get Crypto Mix attribute string
     *
     * The @b offerer shall call this method to get a string of @b all supported crypto mix algorithms
     * and shall send this list to the answerer.
     *
     * The @b answerer must call this function after it received the crypto mix string and @b after it
     * called @c setCryptoMixAttribute(...). In this case the method returns only one (the selected)
     * crypto mix algorithm and the answerer must send this to the offerer in 200 OK for example. Must
     * be called before @c parseSdes
     *
     * @param algoNames points to a buffer that will filled with the crypto mix algorithm names.
     *                  The buffer must be long enough to hold at least the name of the mandatory
     *                  algorithm HMAC-SHA-384.
     *
     * @param streamNm stream identifier.
     *
     * @param length length buffer
     *
     * @return Length of algorithm names (excluding zero byte) or zero if crypto mix not supported or
     *         enabled.
     */
    int getCryptoMixAttribute(char *algoNames, size_t length, streamName streamNm);

    /**
     * @brief Set Crypto Mix attribute string
     *
     * The method splits the string into algorithm names and checks if it contains an
     * supported algorithm.
     *
     * The answerer must call this method @b before it calls the @c getCryptoMixAttribute() method and
     * @b before it calls the @c parseSdes method.
     *
     * The offerer call this method @b before it calls @c parseSdes
     *
     * @param algoNames points to a buffer that holds the received crypto mix algorithm names.
     *                  The buffer must be zero terminated.
     *
     * @param streamNm stream identifier.
     *
     * @return @c false if algorithm is not supported.
     */
    bool setCryptoMixAttribute(const char *algoNames, streamName streamNm);

    /**
     * @brief Reset SDES
     * 
     * This method deletes an existing SDES context unconditionally. The application must make
     * sure that it does not use the SDES context in any way, for example feeding RTP or SRTP packets
     * to this stream.
     *
     * @param streamNm stream identifier.
     *
     * @param force if set to true then it resets the context unconditionally, otherwise only if
     *              SDES is not in active state.
     */
    void resetSdesContext(streamName streamNm, bool force =false);

    /**
     * @brief Clean Cache
     *
     * This method does not work for file based cache implementation. An application
     * shall use this functions with great care because it drops all stored retained
     * secrets.
     *
     * The cache must be initialized and open.
     */
    static void cleanCache();

    /**
     * @brief Get number of supported ZRTP protocol versions.
     *
     * @param streamNm stream identifier.
     *
     * @return the number of supported ZRTP protocol versions.
     */
    int32_t getNumberSupportedVersions(streamName streamNm);

    /**
     * @brief Get the supported ZRTP encapsulation attribute.
     * 
     * Get this attribute value and set it as a SDP parameter to signal support of ZRTP encapsulation.
     *
     * @param streamNm stream identifier.
     *
     * @return the pointer to the attribute cC-string or @c NULL if encapsulation is not supported.
     */
    const char* getZrtpEncapAttribute(streamName streamNm);

    /**
     * @brief Set the ZRTP encapsulation attribute.
     * 
     * If an application receives the ZRTP encapsulation SDP attribute then it should set the
     * attribute value. The stream uses ZRTP encapsulation only if this SDP parameter is set
     * @b and SDES is available and active.
     * 
     * @param attribute pointer to a C-string that defines the ZRTP encapsulation method.
     * @param streamNm stream identifier.
     *
     * @see getZrtpEncapAttribute
     */
    void setZrtpEncapAttribute(const char *attribute, streamName streamNm);

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

protected:
    friend class CtZrtpStream;

    /**
     * @brief Session master stream entered secure state.
     *
     * The session's master stream entered secure state and computed all
     * necessary information to kick of slave streams. The session checks
     * if slave streams are available and if they are ready to start.
     *
     * @param stream is the stream that enters secure mode. This must be a
     *               @c Master stream
     */
    void masterStreamSecure(CtZrtpStream *stream);


private:
    void synchEnter();
    void synchLeave();

    CtZrtpStream *streams[AllStreams];
    std::string  clientIdString;
    std::string  multiStreamParameter;
    const uint8_t* ownZid;

    bool mitmMode;
    bool signSas;
    bool enableParanoidMode;
    bool isReady;
    bool zrtpEnabled;
    bool sdesEnabled;
};

#endif /* _CTZRTPSESSION_H_ */