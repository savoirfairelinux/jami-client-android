
class CryptoContext;
class CryptoContextCtrl;

/**
 * Maximum length of a raw crypto string.
 */
#define MAX_CRYPT_STRING_LEN 200

class ZrtpSdesStream {

public:

    /**
     * Supported SDES crypto suites.
     */
    typedef enum {
        AES_CM_128_HMAC_SHA1_32 = 1,
        AES_CM_128_HMAC_SHA1_80
    } sdesSuites;

    /**
     * SDES stream stated
     */
    typedef enum {
        STREAM_INITALIZED = 1,
        OUT_PROFILE_READY,
        IN_PROFILE_READY,
        SDES_SRTP_ACTIVE
    } sdesZrtpStates;

    /**
     * @brief Create and SDES/ZRTP stream.
     *
     * This method creates an SDES stream with capabilities to handle RTP,
     * RTCP, SRTP, and SRTCP packets.
     *
     * It is not necessary to explicitly start the SDES stream. The method initiates
     * the SRTP after it created and parsed all necessary SDES crypto strings.
     *
     * @param suite defines which crypto suite to use for this stream. The values are
     *              @c AES_CM_128_HMAC_SHA1_80 or @c AES_CM_128_HMAC_SHA1_32.
     */
    ZrtpSdesStream(const sdesSuites suite =AES_CM_128_HMAC_SHA1_32);

    ~ZrtpSdesStream();

    /**
     * @brief Close an SDES/ZRTP stream.
     *
     * Close the stream and return allocated memory to the pool.
     */
    void close();

    /**
     * @brief Creates an SDES crypto string for the SDES/ZRTP stream.
     *
     * Creates the crypto string that the application can use in the SDP fields of
     * SIP INVITE or SIP 200 OK.
     *
     * An INVITE-ing application shall call this function at the same point when
     * it calls the functions to get the @c zrtp-hash string and shall insert the
     * created crypto string into the SDP.
     *
     * An answering application shall call this function directly @b after it called
     * @c sdesZrtpStreamParseSdes. This usually at the same point when it gets the
     * @c zrtp-hash from the SDP parameters and forwards it to @c libzrtp. The
     * answering application's SRTP environment is now ready.
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
     * @param sipInvite if this is set to @c true (not zero) then the method
     *                  takes the necessary actions to create the crypto eonvironment
     *                  for the inviting SIP application. It it is zero then it handles
     *                  the invited case (answerer).
     *
     * @return @c true if data could be created, @c false otherwise.
     */
    bool createSdes(char *cryptoString, size_t *maxLen, bool sipInvite);

    /**
     * @brief Parses an SDES crypto string for the SDES/ZRTP stream.
     *
     * Parses a received crypto string that the application received in a SIP INVITE
     * or SIP 200 OK.
     *
     * An INVITE-ing application shall call this function right after it received
     * the 200 OK from the answering application and must call this function with the
     * @c sipInvite parameter set to @c true. This usually at the same point when
     * it gets the  @c zrtp-hash from the SDP parameters.
     * This application's SRTP environment is now ready.
     *
     * The answering application calls this function after it received the INVITE and
     * extracted the crypto string from the SDP and must call this function with the
     * @c sipInvite parameter set to @c false. This is usually the same point when
     * it gets the @c zrtp-hash from the SDP parameters.
     *
     * @param cryptoString points to the crypto sting in raw format,
     *                     without any signaling prefix, for example @c
     *                     a=crypto: in case of SDP signaling.
     *
     * @param length length of the crypto string to parse. If the length is
     *               @c zero then the function uses @c strlen to compute
     *               the length.
     *
     * @param sipInvite if this is set to @c true then the method
     *                  takes the necessary actions to create the crypto eonvironment
     *                  for the inviting SIP application. It it is zero then it handles
     *                  the invited case (answerer).
     *
     * @return @c true if data could be created, @c false otherwise.
     */
    bool parseSdes(char *cryptoString, size_t length, bool sipInvite);

    /*
     * ******** Outgoing RTP/RTCP packet handling
     */
    /**
     * @brief Process an outgoing RTP packet
     *
     * This function processes an outgoing RTP packet. Depending on the state
     * the packet is either:
     *  - not encrypted if neither SDES nor ZRTP are active or supported by the
     *    other client. This is the standard case if the stream was just initialized.
     *  - encrypted with SDES provided key data. This is the case if the application
     *    called both @c sdesZrtpStreamCreateSdes and @c sdesZrtpStreamParseSdes
     *    functions to properly setup the SDES key data.
     *
     * @param packet the buffer that contains the RTP packet. After processing, the
     *               encrypted packet is stored in the same buffer. The buffer must
     *               big enough to hold the additional SRTP data, depending on the
     *               SRTP profile these are usually 4 - 20 bytes.
     *
     * @param length length of the RTP packet
     *
     * @param newLength to an integer that get the new length of the packet including SRTP data.
     *
     * @return
     *  - @c true if encryption is successful, app shall send packet to the recipient.
     *  - @c false if there was an error during encryption, don't send the packet.
     */
    bool outgoingRtp(uint8_t *packet, size_t length, size_t *newLength);

    /**
     * @brief Process an outgoing RTCP packet
     *
     * This function works in the same way as @c sdesZrtpProcessRtp.
     *
     * @param packet the buffer that contains the RTCP packet. After processing, the
     *               encrypted packet is stored in the same buffer. The buffer must
     *               big enough to hold the additional SRTP data, depending on the
     *               SRTP profile these are usually 8 - 20 bytes.
     *
     * @param length length of the RTP packet
     *
     * @param newLength to an integer that get the new length of the packet including SRTP data.
     *
     * @return
     *  - @c true if encryption is successful, app shall send packet to the recipient.
     *  - @c false if there was an error during encryption, don't send the packet.
     */
    bool outgoingRtcp(uint8_t *packet, size_t length, size_t *newLength);

    /*
     * ******** Incoming SRTP/SRTCP packet handling
     */
    /**
     * @brief Process an incoming RTP or SRTP packet
     *
     * This function processes an incoming RTP/SRTP packet. Depending on the state
     * the packet is either:
     *  - not decrypted if SDES is not active or supported by the
     *    other client. This is the standard case if the stream was just initialized.
     *  - decrypted with SDES provided key data. This is the case if the application
     *    called both @c sdesZrtpStreamCreateSdes and @c sdesZrtpStreamParseSdes
     *    functions to properly setup the SDES key data.
     *
     * @param packet the buffer that contains the RTP/SRTP packet. After processing,
     *               the decrypted packet is stored in the same buffer.
     *
     * @param length length of the RTCP packet
     *
     * @param newLength to an integer that get the new length of the packet excluding SRTCP data.
     *
     * @return
     *       - 1: success,
     *       - -1: SRTP authentication failed,
     *       - -2: SRTP replay check failed
     */
    int incomingRtp(uint8_t *packet, size_t length, size_t *newLength);

    /**
     * @brief Process an incoming RTCP or SRTCP packet
     *
     * This function works in the same way as @c sdesZrtpProcessSrtp.
     *
     * @param packet the buffer that contains the RTCP/SRTCP packet. After processing,
     *               the decrypted packet is stored in the same buffer.
     *
     * @param length length of the RTCP packet
     *
     * @param newLength to an integer that get the new length of the packet excluding SRTCP data.
     *
     * @return
     *       - 1: success,
     *       - -1: SRTCP authentication failed,
     *       - -2: SRTCP replay check failed
     */
    int incomingSrtcp(uint8_t *packet, size_t length, size_t *newLength);

    /**
     * Return state of SDES stream.
     *
     * @return state of stream.
     */
    sdesZrtpStates getState() {return state;}

    /**
     * Return name of active cipher algorithm.
     *
     * @return point to name of cipher algorithm.
     */
    const char* getCipher();

    /**
     * Return name of active SRTP authentication algorithm.
     *
     * @return point to name of authentication algorithm.
     */
    const char* getAuthAlgo();


    /*
     * ******** Lower layer functions
     */
private:
    /**
     * @brief Create an SRTP crypto context and the according SDES crypto string.
     *
     * This lower layer method creates an SRTP profile an the according SDES
     * crypto string. It selects a valid crypto suite, generates the key and salt
     * data, converts these into base 64 and returns the crypto string in raw format
     * without any signaling prefixes. The method also creates the internal
     * SRTP/SRTCP crypto contexts for outgoing data.
     *
     * The output string has the following format:
     * @verbatim
     * 1 AES_CM_128_HMAC_SHA1_32 inline:NzB4d1BINUAvLEw6UzF3WSJ+PSdFcGdUJShpX1Zj
     * @endverbatim
     *
     * Applications usually don't use this method directly. Applications shall
     * use the SDES stream functions.
     *
     * Depending on the crypto suite the overall length of the crypto string
     * is variable. For a normal AES_128_CM suite the minumum lenth is 73
     * characters, a AES_256_CM suite results in 97 characters (not counting
     * any signaling prefixes).
     *
     * @param cryptoString points to a char output buffer that receives the
     *                     crypto  string in the raw format, without the any
     *                     signaling prefix, for example @c a=crypto: in case
     *                     of SDP signaling. The function terminates the
     *                     crypto string with a @c nul byte
     *
     * @param maxLen points to an integer. On input this integer specifies the
     *               length of the output buffer. If @c maxLen is smaller than
     *               the resulting crypto string the function returns an error
     *               conde. On return the functions sets @c maxLen to the
     *               actual length of the resultig crypto string.
     *
     * @param tag the value of the @c tag field in the crypto string. The
     *            answerer must use this input to make sure that the tag value
     *            in the answer matches the value in the offer. See RFC 4568,
     *            section 5.1.2.
     *            If the tag value is @c -1 the function sets the tag to @c 1.
     *
     * @return @c true if data could be created, @c false
     *          otherwise.
     */
    bool createSdesProfile(char *cryptoString, size_t *maxLen);

    /**
     * @brief Parse and check an offered SDES crypto string and create SRTP crypto context.
     *
     * The method parses an offered SDES crypto string and checks if it is
     * valid. Next it checks if the string contains a supported crypto suite
     * and if the key and salt lengths match the selected crypto suite. The method
     * also creates the internal SRTP/SRTCP crypto contexts for incoming data.
     *
     * Applications usually don't use this method directly. Applications shall
     * use the SDES stream functions.
     *
     * @b NOTE: This function does not support the optional parameters lifetime,
     * MKI, and session parameters. While it can parse liftime and MKI theiy are
     * not evaluated and used. If these parameters are used in the input crypto
     * string the function return @c false.
     *
     * @param cryptoString points to the crypto sting in raw format,
     *        without any signaling prefix, for example @c a=crypto: in case of
     *        SDP signaling.
     *
     * @param length length of the crypto string to parse. If the length is
     *        @c zero then the function uses @c strlen to compute the length.
     *
     * @param parsedSuite the function sets this to the @c sdesSuites enumerator of
     *        the parsed crypto suite. The answerer shall use this as input to
     *        @c createSdesProfile to make sure that it creates the same crypto suite.
     *        See RFC 4568, section 5.1.2
     *
     * @param tag the function sets this to the @c tag value of the parsed crypto
     *        string. The answerer must use this as input to @c createSdesProfile
     *        to make sure that it creates the correct tag in the crypto string.
     *        See RFC 4568, section 5.1.2
     *
     * @return @c true if checks were ok, @c false
     *          otherwise.
     */
    bool parseCreateSdesProfile(const char *cryptoString, size_t length, sdesSuites *parsedSuite, int32_t *tag);

    sdesZrtpStates state;
    sdesSuites     suite;
    int32_t        tag;
    CryptoContext     *recvSrtp;           //!< The SRTP context for this stream
    CryptoContextCtrl *recvSrtcp;          //!< The SRTCP context for this stream
    CryptoContext     *sendSrtp;           //!< The SRTP context for this stream
    CryptoContextCtrl *sendSrtcp;          //!< The SRTCP context for this stream
    uint32_t srtcpIndex;                   //!< the local SRTCP index

};
