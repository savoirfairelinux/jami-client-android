/*
  Copyright (C) 2006-2009 Werner Dittmann

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

#ifndef _ZRTPQUEUE_H_
#define _ZRTPQUEUE_H_

#include <ccrtp/cqueue.h>
#include <ccrtp/rtppkt.h>
#include <libzrtpcpp/ZrtpCallback.h>
#include <libzrtpcpp/ZrtpConfigure.h>
#include <CcrtpTimeoutProvider.h>

class __EXPORT ZrtpUserCallback;
class __EXPORT ZRtp;

NAMESPACE_COMMONCPP

/**
 * GNU ccRTP extension to support GNU ZRTP.
 *
 * ZRTP was developed by Phil Zimmermann and provides functions to
 * negotiate keys and other necessary data (crypto data) to set-up
 * the Secure RTP (SRTP) crypto context. Refer to Phil's ZRTP
 * specification at his <a href="http://zfoneproject.com/">Zfone
 * project</a> site to get more detailed imformation about the
 * capabilities of ZRTP.
 *
 * <b>Short overview of the ZRTP implementation</b>
 *
 * ZRTP is a specific protocol to negotiate encryption algorithms
 * and the required key material. ZRTP uses a RTP session to
 * exchange its protocol messages.
 *
 * A complete GNU ZRTP implementation consists of two parts, the
 * GNU ZRTP core and specific code that binds the GNU ZRTP core to
 * the underlying RTP/SRTP stack and the operating system:
 * <ul>
 * <li>
 *      The GNU ZRTP core is independent of a specific RTP/SRTP
 *      stack and the operationg system and consists of the ZRTP
 *      protocol state engine, the ZRTP protocol messages, and the
 *      GNU ZRTP engine. The GNU ZRTP engine provides methods to
 *      setup ZRTP message and to analyze received ZRTP messages,
 *      to compute the crypto data required for SRTP, and to
 *      maintain the required hashes and HMAC.
 * </li>
 * <li>
 *      The second part of an implementation is specific
 *      <em>glue</em> code the binds the GNU ZRTP core to the
 *      actual RTP/SRTP implementation and other operating system
 *      specific services  such as timers.
 * </li>
 * </ul>
 *
 * The GNU ZRTP core uses a callback interface class (refer to
 * ZrtpCallback) to access RTP/SRTP or operating specific methods,
 * for example to send data via the RTP/SRTP stack, to access
 * timers, provide mutex handling, and to report events to the
 * application.
 *
 * <b>The ZrtpQueue</b>
 *
 * ZrtpQueue implements code that is specific to the GNU ccRTP
 * implementation. ZrtpQueue also implements the specific code to
 * provide the mutex and timeout handling to the GNU ZRTP
 * core. Both, the mutex and the timeout handling, use the GNU
 * Common C++ library to stay independent of the operating
 * seystem. For more information refer to the <a
 * href="http://www.gnutelephony.org/index.php/GNU_Common_C%2B%2B">GNU
 * Common C++</a> web site.
 *
 * To perform its tasks ZrtpQueue
 * <ul>
 * <li> extends GNU ccRTP classes to use the underlying
 *      ccRTP methods and the RTP/SRTP send and receive queues
 * </li>
 * <li> implements the ZrtpCallback interface to provide ccRTP
 *      access and other specific services (timer, mutex) to GNU
 *      ZRTP
 * </li>
 * <li> provides ZRTP specific methods that applications may use
 *      to control and setup GNU ZRTP
 * </li>
 * <li> can register and use an application specific callback
 *      class (refer to ZrtpUserCallback)
 * </li>
 * </ul>
 *
 * After instantiating a GNU ZRTP session (see below for a short
 * example) applications may use the ZRTP specific methods of
 * ZrtpQueue to control and setup GNU ZRTP, for example enable or
 * disable ZRTP processing or getting ZRTP status information.
 *
 * GNU ZRTP provides a ZrtpUserCallback class that an application
 * may extend and register with ZrtpQueue. GNU ZRTP and ZrtpQueue
 * use the ZrtpUserCallback methods to report ZRTP events to the
 * application. The application may display this information to
 * the user or act otherwise.
 *
 * The following figure depicts the relationships between
 * ZrtpQueue, ccRTP RTP/SRTP implementation, the GNU ZRTP core,
 * and an application that provides an ZrtpUserCallback class.
 *
@verbatim

                      +----------+
                      |  ccRTP   |
                      | RTP/SRTP |
                      |          |
                      +----------+
                           ^
                           | extends
                           |
+----------------+      +-----+------+
|  Application   |      |            |      +-----------------+
|  instantiates  | uses | ZrtpQueue  | uses |                 |
| a ZRTP Session +------+ implements +------+    GNU ZRTP     |
|  and provides  |      |ZrtpCallback|      |      core       |
|ZrtpUserCallback|      |            |      | implementation  |
+----------------+      +------------+      |  (ZRtp et al)   |
                                         |                 |
                                         +-----------------+
@endverbatim
 *
 * Because ZrtpQueue extends the ccRTP RTP/SRTP implementation
 * (AVPQueue) all public methods defined by ccRTP are also
 * available for a ZRTP session. ZrtpQueue overwrites some of the
 * public methods of ccRTP (AVPQueue) to implement ZRTP specific
 * code.
 *
 * GNU ZRTP provides a <em>SymmetricZRTPSession</em> type to
 * simplify its use. An application uses this type in the same way
 * as it would use the normal ccRTP <em>SymmetricRTPSession</em>
 * type. The following short code snippets show how an application
 * could instantiate ccRTP and GNU ZRTP sessions. The first
 * snippet shows how to instantiate a ccRTP session:
 *
 * @code
 * ...
 * #include <ccrtp/rtp.h>
 * ...
 *     SymmetricRTPSession tx(pattern.getSsrc(),
 *                            InetHostAddress("localhost"));
 * ...
 *
 * @endcode
 *
 * The same code as above but using a GNU ZRTP session this time:
 * @code
 * ...
 * #include <libzrtpcpp/zrtpccrtp.h>
 * ...
 *     SymmetricZRTPSession tx(pattern.getSsrc(),
 *                             InetHostAddress("localhost"));
 * ...
 *
 * @endcode
 *
 * The only differences are the different include statements and
 * the different session types.
 *
 * The <em>demo</em> folder contains a small example that shows
 * how to use GNU ZRTP.
 *
 * Please refer to the GNU ccRTP documentation for a description
 * of ccRTP methods and functions. This ZrtpQueue documentation
 * shows the ZRTP specific extensions and describes overloaded
 * methods and a possible different behaviour.
 *
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 */

class __EXPORT ZrtpQueue : public AVPQueue, ZrtpCallback {

public:

    /**
     * Initialize the ZrtpQueue.
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
     * The current implementation of ZrtpQueue does not support
     * different ZID files for one application instance. This
     * restriction may be removed in later versions.
     *
     * The application may specify its own ZID file name. If no
     * ZID file name is specified it defaults to
     * <code>$HOME/.GNUccRTP.zid</code> if the <code>HOME</code>
     * environment variable is set. If it is not set the current
     * directory is used.
     *
     * If the method could set up the timeout thread and open the ZID
     * file then it enables ZRTP processing and returns.
     *
     * @param zidFilename
     *     The name of the ZID file, can be a relative or absolut
     *     filename.
     *
     * @param autoEnable
     *     if set to true the method automatically sets enableZrtp to
     *     true. This enables the ZRTP auto-sense mode. Default is true.
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
    int32_t initialize(const char *zidFilename, bool autoEnable = true,
                       ZrtpConfigure* config = NULL);

    /*
     * Applications use the following methods to control ZRTP, for example
     * to enable ZRTP, set flags etc.
     */

    /**
     * Enable or disable ZRTP processing.
     *
     * Call this method to enable or disable ZRTP processing after
     * calling <code>initialize()</code>. This can be done before
     * using a RTP session or at any time during a RTP session.
     *
     * Existing SRTP sessions or currently active ZRTP processing will
     * not be stopped or disconnected.
     *
     * If the application enables ZRTP then:
     * <ul>
     * <li>ZrtpQueue starts to send ZRTP Hello packets after at least
     * one RTP packet was sent and received on the associated RTP
     * session. Thus if an application enables ZRTP and ZrtpQueue
     * detects traffic on the RTP session then ZrtpQueue automatically
     * starts the ZRTP protocol. This automatic start is convenient
     * for applications that negotiate RTP parameters and set up RTP
     * sessions but the actual RTP traffic starts some time later.
     * </li>
     * <li>ZrtpQueue analyses incoming packets to detect ZRTP
     * messages. If ZRTP was started, either via automatic start (see
     * above) or explicitly via startZrtp(), then ZrtpQueue
     * forwards ZRTP packets to the GNU ZRTP core.
     * </ul>
     *
     * @param onOff
     *     @c true to enable ZRTP, @c false to disable ZRTP
     */
    void setEnableZrtp(bool onOff);

    /**
     * Return the state of ZRTP enable state.
     *
     * @return @c true if ZRTP processing is enabled, @c false
     * otherwise.
     */
    bool isEnableZrtp();

    /**
     * Set SAS as verified.
     *
     * The application may call this method if the user confirmed
     * (verfied) the Short Authentication String (SAS) with the peer.
     *
     * ZRTP calls ZrtpUserCallback#showSAS after it computed the SAS
     * and the application registered a user callback class. The
     * application should display the SAS and provide a mechanism at
     * the user interface that enables the user to confirm the SAS.
     *
     * ZRTP remembers the SAS confirmation status together with the
     * retained secrets data. If both parties confirmed the SAS then
     * ZRTP informs the application about this status on the next ZRTP
     * session.
     *
     * For more detailed information regarding SAS please refer to the
     * ZRTP specification, chapter 8.
     */
    void SASVerified();

    /**
     * Reset the SAS verfied flag for the current user's retained secrets.
     *
     */
    void resetSASVerified();

    /**
     * To confirm a go clear request.
     *
     * Call this method if the user confirmed a go clear (secure mode off).
     */
    void goClearOk();

    /**
     * Request to switch off secure mode.
     *
     * Call this method is the user itself wants to switch off secure
     * mode (go clear). After sending the "go clear" request to the peer
     * ZRTP immediatly switch off SRTP processing. Every RTP data is sent
     * in clear after the go clear request.
     */
    void requestGoClear();

    /**
     * Set the auxilliary secret.
     *
     * Use this method to set the srtps secret data. Refer to ZRTP
     * specification, chapter 5.3 ff
     *
     * @param data
     *     Points to the auxilliary secret data.
     * @param length
     *     Length of the auxilliary secrect in bytes
     */
    void setAuxSecret(uint8_t* data, int32_t length);

    /**
     * Set the application's callback class.
     *
     * The destructor of ZrtpQueue also destorys the user callback
     * class if it was set. The application must not delete the
     * callback object or use/reference the callback object after
     * ZrtpQueue was destroyed.
     *
     * @param ucb
     *     Implementation of the application's ZrtpUserCallback class
     */
    void setUserCallback(ZrtpUserCallback* ucb);

    /**
     * Set the client ID for ZRTP Hello message.
     *
     * The GNU ccRTP client may set its id to identify itself in the
     * ZRTP Hello message. The maximum length is 16 characters. A
     * shorter id string is possible, it will be filled with blanks. A
     * longer id string will be truncated to 16 characters. The
     * standard client id is <code>'GNU ccRTP ZRTP '</code> (without
     * the quotes).
     *
     * Setting the client's id must be done before calling
     * ZrtpQueue#initialize() or ZrtpQueue#startZrtp() .
     *
     * @param id
     *     The client's id string
     */
    void setClientId(std::string id);

   /**
     * Get the ZRTP Hello Hash data.
     *
     * Use this method to get the ZRTP Hello hash data. The method
     * returns the data as a string containing the ZRTP protocol version and
     * hex-digits.
     * 
     * The index defines which Hello packet to use. Each supported ZRTP procol version
     * uses a different Hello packet and thus computes different hashes.
     *
     * Refer to ZRTP specification, chapter 8.
     *
     * @param index
     *     Hello hash of the Hello packet identfied by index. Index must be 0 <= index < getNumberSupportedVersions().
     *
     * @return
     *    a std::string formatted according to RFC6189 section 8 without the leading 'a=zrtp-hash:'
     *    SDP attribute identifier. The hello hash is available immediatly after class instantiation.
     * 
     * @see getNumberSupportedVersions()
     */
    std::string getHelloHash(int32_t index);

    /**
     * Get the peer's ZRTP Hello Hash data.
     *
     * Use this method to get the peer's ZRTP Hello Hash data. The method
     * returns the data as a string containing the ZRTP protocol version and
     * hex-digits.
     *
     * The peer's hello hash is available only after ZRTP received a hello. If
     * no data is available the function returns an empty string.
     *
     * Refer to ZRTP specification, chapter 8.
     *
     * @return
     *    a std:string containing the Hello version and the hello hash as hex digits.
     */
    std::string getPeerHelloHash();

    /**
     * Get Multi-stream parameters.
     *
     * Use this method to get the Multi-stream that were computed during
     * the ZRTP handshake. An application may use these parameters to
     * enable multi-stream processing for an associated SRTP session.
     *
     * Refer to chapter 5.4.2 in the ZRTP specification for further details
     * and restriction how and when to use multi-stream mode.
     *
     * @return
     *    a string that contains the multi-stream parameters. The application
     *    must not modify the contents of this string, it is opaque data. The
     *    application may hand over this string to a new ZrtpQueue instance
     *    to enable multi-stream processing for this ZrtpQueue. If ZRTP was
     *    not started or ZRTP is not yet in secure state the method returns an
     *    empty string.
     *
     * @see setMultiStrParams()
     */
    std::string getMultiStrParams();

    /**
     * Set Multi-stream parameters.
     *
     * Use this method to set the parameters required to enable Multi-stream
     * processing of ZRTP. The multi-stream parameters must be set before the
     * application starts the ZRTP protocol engine.
     *
     * Refer to chapter 5.4.2 in the ZRTP specification for further details
     * of multi-stream mode.
     *
     * @param parameters
     *     A string that contains the multi-stream parameters that this
     *     new ZrtpQueue instanace shall use.
     *
     * @see getMultiStrParams()
     */
    void setMultiStrParams(std::string parameters);

    /**
     * Check if this ZRTP use Multi-stream.
     *
     * Use this method to check if this ZRTP instance uses multi-stream. Even
     * if the application provided multi-stram parameters it may happen that
     * full DH mode was used. Refer to chapters 5.2 and 5.4.2 in the ZRTP #
     * when this may happen.
     *
     * @return
     *     True if multi-stream is used, false otherwise.
     */
    bool isMultiStream();

    /**
     * Check if the other ZRTP client supports Multi-stream.
     *
     * Use this method to check if the other ZRTP client supports
     * Multi-stream mode.
     *
     * @return
     *     True if multi-stream is available, false otherwise.
     */
    bool isMultiStreamAvailable();

    /**
     * Accept a PBX enrollment request.
     *
     * If a PBX service asks to enroll the MiTM key and the user accepts this
     * requtes, for example by pressing an OK button, the client application
     * shall call this method and set the parameter <code>accepted</code> to
     * true. If the user does not accept the request set the parameter to
     * false.
     *
     * @param accepted
     *     True if the enrollment request is accepted, false otherwise.
     */
    void acceptEnrollment(bool accepted);

    /**
     * Get the commited SAS rendering algorithm for this ZRTP session.
     *
     * @return the commited SAS rendering algorithm
     */
    std::string getSasType();

    /**
     * Get the computed SAS hash for this ZRTP session.
     *
     * A PBX ZRTP back-to-Back function uses this function to get the SAS
     * hash of an enrolled client to construct the SAS relay packet for
     * the other client.
     *
     * @return a refernce to the byte array that contains the full
     *         SAS hash.
     */
    uint8_t* getSasHash();

    /**
     * Send the SAS relay packet.
     *
     * The method creates and sends a SAS relay packet according to the ZRTP
     * specifications. Usually only a MitM capable user agent (PBX) uses this
     * function.
     *
     * @param sh the full SAS hash value
     * @param render the SAS rendering algorithm
     */
    bool sendSASRelayPacket(uint8_t* sh, std::string render);

    /**
     * Check the state of the MitM mode flag.
     *
     * If true then this ZRTP session acts as MitM, usually enabled by a PBX
     * client (user agent)
     *
     * @return state of mitmMode
     */
    bool isMitmMode();

    /**
     * Set the state of the MitM mode flag.
     *
     * If MitM mode is set to true this ZRTP session acts as MitM, usually
     * enabled by a PBX client (user agent).
     *
     * @param mitmMode defines the new state of the mitmMode flag
     */
    void setMitmMode(bool mitmMode);

    /**
     * Enable or disable paranoid mode.
     *
     * The Paranoid mode controls the behaviour and handling of the SAS verify flag. If
     * Panaoid mode is set to flase then ZRtp applies the normal handling. If Paranoid
     * mode is set to true then the handling is:
     *
     * <ul>
     * <li> always set the SAS verify flag to <code>false</code> at srtpSecretsOn() callback. The
     *      user interface (UI) must show <b>SAS not verified</b>. See implementation note below.</li>
     * <li> don't set the SAS verify flag in the <code>Confirm</code> packets, thus forcing the other
     *      peer to report <b>SAS not verified</b>.</li>
     * <li> ignore the <code>SASVerified()</code> function, thus do not set the SAS verified flag
     *      in the ZRTP cache. </li>
     * <li> Disable the <em>Trusted PBX MitM</em> feature. Just send the <code>SASRelay</code> packet
     *      but do not process the relayed data. This protects the user from a malicious
     *      "trusted PBX".</li>
     * </ul>
     * ZRtp performs alls other steps during the ZRTP negotiations as usual, in particular it
     * computes, compares, uses, and stores the retained secrets. This avoids unnecessary warning
     * messages. The user may enable or disable the Paranoid mode on a call-by-call basis without
     * breaking the key continuity data.
     *
     * <b>Implementation note:</b><br/>
     * An application shall <b>always display the SAS if the SAS verify flag is <code>false</code></b>.
     * The application shall remind the user to compare the SAS code, for example using larger fonts,
     * different colours and other display features.
     */
    void setParanoidMode(bool yesNo);

    /**
     * Check status of paranoid mode.
     *
     * @return
     *    Returns true if paranoid mode is enabled.
     */
    bool isParanoidMode();

    /**
     * Check the state of the enrollment mode.
     *
     * If true then we will set the enrollment flag (E) in the confirm
     * packets and performs the enrollment actions. A MitM (PBX) enrollment service sets this flagstarted this ZRTP
     * session. Can be set to true only if mitmMode is also true.
     * @return status of the enrollmentMode flag.
     */
    bool isEnrollmentMode();

    /**
     * Check the state of the enrollment mode.
     *
     * If true then we will set the enrollment flag (E) in the confirm
     * packets and perform the enrollment actions. A MitM (PBX) enrollment
     * service must sets this mode to true.
     *
     * Can be set to true only if mitmMode is also true.
     *
     * @param enrollmentMode defines the new state of the enrollmentMode flag
     */
    void setEnrollmentMode(bool enrollmentMode);

    /**
     * Check if a peer's cache entry has a vaild MitM key.
     *
     * If true then the other peer ha a valid MtiM key, i.e. the peer has performed
     * the enrollment procedure. A PBX ZRTP Back-2-Back application can use this function
     * to check which of the peers is enrolled.
     *
     * @return True if the other peer has a valid Mitm key (is enrolled).
     */
    bool isPeerEnrolled();

    /**
     * Set the state of the SAS signature mode flag.
     *
     * If SAS signature mode is set to true this ZRTP session support SAS signature
     * callbacks and signature transfer between clients.
     *
     * @param sasSignMode defines the new state of the sasSignMode flag
     */
    void setSignSas(bool sasSignMode);

    /**
     * Set signature data
     *
     * This functions stores signature data and transmitts it during ZRTP
     * processing to the other party as part of the Confirm packets. Refer to
     * chapters 6.7 and 8.2 in the ZRTP specification.
     *
     * @param data
     *    The signature data including the signature type block. The method
     *    copies this data into the Confirm packet at signature type block.
     * @param length
     *    The length of the signature data in bytes. This length must be
     *    multiple of 4.
     * @return
     *    True if the method stored the data, false otherwise.
     */
    bool setSignatureData(uint8* data, int32 length);

    /**
     * Get signature data
     *
     * This functions returns signature data that was receivied during ZRTP
     * processing. Refer to chapters 6.7 and 8.2.
     *
     * @return
     *    Pointer to signature data. This is a pointer to volatile data that is
     *    only valid during the checkSASSignature() callback. The application
     *    shall copy the data if necessary.
     */
    const uint8* getSignatureData();

    /**
     * Get length of signature data
     *
     * This functions returns the length of signature data that was receivied
     * during ZRTP processing. Refer to chapters 6.7 and 8.2.
     *
     * @return
     *    Length in bytes of the received signature data. The method returns
     *    zero if no signature data avilable.
     */
    int32 getSignatureLength();

    /**
     * Put data into the RTP output queue.
     *
     * This is used to create a data packet in the send queue.
     * Sometimes a "NULL" or empty packet will be used instead, and
     * these are known as "silent" packets.  "Silent" packets are
     * used simply to "push" the scheduler along more accurately
     * by giving the appearence that a next packet is waiting to
     * be sent and to provide a valid timestamp for that packet.
     *
     * This method overrides the same method in OutgoingDataQueue class.
     * During ZRTP processing it may be necessary to control the
     * flow of outgoing RTP payload packets (GoClear processing).
     *
     * @param stamp Timestamp for expected send time of packet.
     * @param data Value or NULL if special "silent" packet.
     * @param len May be 0 to indicate a default by payload type.
     **/
    void
    putData(uint32 stamp, const unsigned char* data = NULL, size_t len = 0);

    /**
     * Immediatly send a data packet.
     *
     * This is used to create a data packet and send it immediately.
     * Sometimes a "NULL" or empty packet will be used instead, and
     * these are known as "silent" packets.  "Silent" packets are
     * used simply to "push" the scheduler along more accurately
     * by giving the appearence that a next packet is waiting to
     * be sent and to provide a valid timestamp for that packet.
     *
     * This method overrides the same method in OutgoingDataQueue
     * class.  During ZRTP processing it may be necessary to
     * control the flow of outgoing RTP payload packets (GoClear
     * processing).
     *
     * @param stamp Timestamp immediate send time of packet.
     * @param data Value or NULL if special "silent" packet.
     * @param len May be 0 to indicate a default by payload type.
     **/
    void
    sendImmediate(uint32 stamp, const unsigned char* data = NULL, size_t len = 0);

    /**
     * Starts the ZRTP protocol engine.
     *
     * Applications may call this method to immediatly start the ZRTP protocol
     * engine any time after initializing ZRTP and setting optinal parameters,
     * for example client id or multi-stream parameters.
     *
     * If the application does not call this method but sucessfully initialized
     * the ZRTP engine using <code>initialize()</code> then ZRTP also starts
     * after the application sent and received RTP packets. An application can
     * disable this automatic, delayed start of the ZRTP engine using
     * <code>setEnableZrtp(false)</code> before sending or receiving RTP
     * packets.
     *
     */
    void startZrtp();

    /**
     * Stops the ZRTP protocol engine.
     *
     * Applications call this method to stop the ZRTP protocol
     * engine.
     *
     */
    void stopZrtp();

    /**
     * Get other party's ZID (ZRTP Identifier) data
     *
     * This functions returns the other party's ZID that was receivied
     * during ZRTP processing.
     *
     * The ZID data can be retrieved after ZRTP receive the first Hello
     * packet from the other party. The application may call this method
     * for example during SAS processing in showSAS(...) user callback
     * method.
     *
     * @param data
     *    Pointer to a data buffer. This buffer must have a size of
     *    at least 12 bytes (96 bit) (ZRTP Identifier, see chap. 4.9)
     * @return
     *    Number of bytes copied into the data buffer - must be equivalent
     *    to 96 bit, usually 12 bytes.
     */
    int32 getPeerZid(uint8* data);

    /**
      * Get number of supported ZRTP protocol versions.
      *
      * @return the number of supported ZRTP protocol versions.
      */
     int32_t getNumberSupportedVersions();

     /**
      * Get negotiated ZRTP protocol version.
      *
      * @return the integer representation of the negotiated ZRTP protocol version.
      */
     int32_t getCurrentProtocolVersion();

protected:
    friend class TimeoutProvider<std::string, ost::ZrtpQueue*>;

    /**
     * A hook that gets called if the decoding of an incoming SRTP
     * was erroneous
     *
     * @param pkt
     *     The SRTP packet with error.
     * @param errorCode
     *     The error code: -1 - SRTP authentication failure, -2 - replay
     *     check failed
     * @return
     *     True: put the packet in incoming queue for further processing
     *     by the applications; false: dismiss packet. The default
     *     implementation returns false.
     */
    virtual bool
    onSRTPPacketError(IncomingRTPPkt& pkt, int32 errorCode);

    /**
     * Handle timeout event forwarded by the TimeoutProvider.
     *
     * Just call the ZRTP engine for further processing.
     */
    void handleTimeout(const std::string &c);

    /**
     * This function is used by the service thread to process
     * the next incoming packet and place it in the receive list.
     *
     * This class overloads the function of IncomingDataQueue
     * implementation.
     *
     * @return number of payload bytes received,  <0 if error.
     */
    virtual size_t takeInDataPacket();

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

    ZrtpQueue(uint32 size = RTPDataQueue::defaultMembersHashSize,
              RTPApplication& app = defaultApplication());

    /**
     * Local SSRC is given instead of computed by the queue.
     */
    ZrtpQueue(uint32 ssrc, uint32 size =
                  RTPDataQueue::defaultMembersHashSize,
              RTPApplication& app = defaultApplication());

    virtual ~ZrtpQueue();

private:
    void init();
    size_t rtpDataPacket(unsigned char* packet, int32 rtn,
                         InetHostAddress network_address,
                         tpport_t transport_port);

    ZRtp *zrtpEngine;
    ZrtpUserCallback* zrtpUserCallback;

    std::string clientIdString;

    bool enableZrtp;

    int32 secureParts;

    int16 senderZrtpSeqNo;
    ost::Mutex synchLock;   // Mutex for ZRTP (used by ZrtpStateClass)
    uint32 peerSSRC;
    uint64 zrtpUnprotect;
    bool started;
    bool mitmMode;
    bool signSas;
    bool enableParanoidMode;
};

class IncomingZRTPPkt : public IncomingRTPPkt {

public:
    /**
     * Build a ZRTP packet object from a data buffer.
     *
     * @param block pointer to the buffer the whole packet is stored in.
     * @param len length of the whole packet, expressed in octets.
     *
     **/

    IncomingZRTPPkt(const unsigned char* block, size_t len);

    ~IncomingZRTPPkt()
    { }

    uint32
    getZrtpMagic() const;

    uint32
    getSSRC() const;
};

class OutgoingZRTPPkt : public OutgoingRTPPkt {

public:
    /**
     * Construct a new ZRTP packet to be sent.
     *
     * A new copy in memory (holding all this components
     * along with the fixed header) is created.
     *
     * @param hdrext whole header extension.
     * @param hdrextlen size of whole header extension, in octets.
     **/
    OutgoingZRTPPkt(const unsigned char* const hdrext, uint32 hdrextlen);
    ~OutgoingZRTPPkt()
    { }
};

END_NAMESPACE

#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-default-style: ellemtel
 * c-basic-offset: 4
 * End:
 */

