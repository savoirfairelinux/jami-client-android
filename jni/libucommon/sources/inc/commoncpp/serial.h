// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// As a special exception, you may use this file as part of a free software
// library without restriction.  Specifically, if other files instantiate
// templates or use macros or inline functions from this file, or you compile
// this file and link it with other files to produce an executable, this
// file does not by itself cause the resulting executable to be covered by
// the GNU General Public License.  This exception does not however
// invalidate any other reasons why the executable file might be covered by
// the GNU General Public License.
//
// This exception applies only to the code released under the name GNU
// Common C++.  If you copy code from other releases into a copy of GNU
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for GNU Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.
//

/**
 * @file serial.h
 * @short Serial I/O services.
 **/

#ifndef COMMONCPP_SERIAL_H_
#define COMMONCPP_SERIAL_H_

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

#ifndef COMMONCPP_THREAD_H_
#include <commoncpp/thread.h>
#endif

#ifndef COMMMONCPP_EXCEPTION_H_
#include <commoncpp/exception.h>
#endif

NAMESPACE_COMMONCPP

/**
 * The Serial class is used as the base for all serial I/O services
 * under APE.  A serial is a system serial port that is used either
 * for line or packet based data input.  Serial ports may also be
 * "streamable" in a derived form.
 *
 *  Common C++ serial I/O classes are used to manage serial devices and
 *  implement serial device protocols.  From the point of view of Common C++,
 *  serial devices are supported by the underlying Posix specified "termios"
 *  call interface.
 *
 *  The serial I/O base class is used to hold a descriptor to a serial device
 *  and to provide an exception handling interface for all serial I/O classes.
 *  The base class is also used to specify serial I/O properties such as
 *  communication speed, flow control, data size, and parity.  The "Serial"
 *  base class is not itself directly used in application development,
 *  however.
 *
 *  Common C++ Serial I/O is itself divided into two conceptual modes; frame
 *  oriented and line oriented I/O.  Both frame and line oriented I/O makes
 *  use of the ability of the underlying tty driver to buffer data and return
 *  "ready" status from when select either a specified number of bytes or
 *  newline record has been reached by manipulating termios c_cc fields
 *  appropriately.  This provides some advantage in that a given thread
 *  servicing a serial port can block and wait rather than have to continually
 *  poll or read each and every byte as soon as it appears at the serial port.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short base class for all serial I/O services.
 */
class __EXPORT Serial
{
public:
    enum Error {
        errSuccess = 0,
        errOpenNoTty,
        errOpenFailed,
        errSpeedInvalid,
        errFlowInvalid,
        errParityInvalid,
        errCharsizeInvalid,
        errStopbitsInvalid,
        errOptionInvalid,
        errResourceFailure,
        errOutput,
        errInput,
        errTimeout,
        errExtended
    };
    typedef enum Error Error;

    enum Flow {
        flowNone,
        flowSoft,
        flowHard,
        flowBoth
    };
    typedef enum Flow Flow;

    enum Parity {
        parityNone,
        parityOdd,
        parityEven
    };
    typedef enum Parity Parity;

    enum Pending {
        pendingInput,
        pendingOutput,
        pendingError
    };
    typedef enum Pending Pending;

private:
    Error errid;
    char *errstr;

    struct {
        bool thrown: 1;
        bool linebuf: 1;
    } flags;

    void    *   original;
    void    *   current;

    /**
     * Used to properly initialize serial object.
     */
    void initSerial(void);

protected:

    fd_t    dev;

    int bufsize;

    /**
     * Opens the serial device.
     *
     * @param fname Pathname of device to open
     */
    void        open(const char *fname);

    /**
     * Closes the serial device.
     *
     */
    void        close(void);

    /**
     * Reads from serial device.
     *
     * @param Data  Point to character buffer to receive data.  Buffers MUST
     *              be at least Length + 1 bytes in size.
     * @param Length Number of bytes to read.
     */
    virtual int aRead(char * Data, const int Length);

    /**
     * Writes to serial device.
     *
     * @param Data  Point to character buffer containing data to write.  Buffers MUST
     * @param Length Number of bytes to write.
     */
    virtual int aWrite(const char * Data, const int Length);

    /**
     * This service is used to throw all serial errors which usually
     * occur during the serial constructor.
     *
     * @param error defined serial error id.
     * @param errstr string or message to optionally pass.
     */
    Error error(Error error, char *errstr = NULL);

    /**
     * This service is used to thow application defined serial
     * errors where the application specific error code is a string.
     *
     * @param err string or message to pass.
     */
    inline void error(char *err)
        {error(errExtended, err);};


    /**
     * This method is used to turn the error handler on or off for
     * "throwing" execptions by manipulating the thrown flag.
     *
     * @param enable true to enable handler.
     */
    inline void setError(bool enable)
        {flags.thrown = !enable;};

    /**
     * Set packet read mode and "size" of packet read buffer.
     * This sets VMIN to x.  VTIM is normally set to "0" so that
     * "isPending()" can wait for an entire packet rather than just
     * the first byte.
     *
     * @return actual buffer size set.
     * @param size of packet read request.
     * @param btimer optional inter-byte data packet timeout.
     */
    int setPacketInput(int size, unsigned char btimer = 0);

    /**
     * Set "line buffering" read mode and specifies the newline
     * character to be used in seperating line records.  isPending
     * can then be used to wait for an entire line of input.
     *
     * @param newline newline character.
     * @param nl1 EOL2 control character.
     * @return size of conical input buffer.
     */
    int setLineInput(char newline = 13, char nl1 = 0);

    /**
     * Restore serial device to the original settings at time of open.
     */
    void restore(void);

    /**
     * Used to flush the input waiting queue.
     */
    void flushInput(void);

    /**
     * Used to flush any pending output data.
     */
    void flushOutput(void);

    /**
     * Used to wait until all output has been sent.
     */
    void waitOutput(void);

    /**
     * Used as the default destructor for ending serial I/O
     * services.  It will restore the port to it's original state.
     */
    void endSerial(void);

    /**
     * Used to initialize a newly opened serial file handle.  You
     * should set serial properties and DTR manually before first
     * use.
     */
    void initConfig(void);

    /**
     * This allows later ttystream class to open and close a serial
     * device.
     */
    Serial()
        {initSerial();};

    /**
     * A serial object may be constructed from a named file on the
     * file system.  This named device must be "isatty()".
     *
     * @param name of file.
     */
    Serial(const char *name);


public:

    /**
     * The serial base class may be "thrown" as a result on an error,
     * and the "catcher" may then choose to destory the object.  By
     * assuring the socket base class is a virtual destructor, we
     * can assure the full object is properly terminated.
     */
    virtual ~Serial();

    /**
     * Serial ports may also be duplecated by the assignment
     * operator.
     */
    Serial &operator=(const Serial &from);

    /**
     * Set serial port speed for both input and output.
     *
     * @return 0 on success.
     * @param speed to select. 0 signifies modem "hang up".
     */
    Error setSpeed(unsigned long speed);

    /**
     * Set character size.
     *
     * @return 0 on success.
     * @param bits character size to use (usually 7 or 8).
     */
    Error setCharBits(int bits);

    /**
     * Set parity mode.
     *
     * @return 0 on success.
     * @param parity mode.
     */
    Error setParity(Parity parity);

    /**
     * Set number of stop bits.
     *
     * @return 0 on success.
     * @param bits stop bits.
     */
    Error setStopBits(int bits);

    /**
     * Set flow control.
     *
     * @return 0 on success.
     * @param flow control mode.
     */
    Error setFlowControl(Flow flow);

    /**
     * Set the DTR mode off momentarily.
     *
     * @param millisec number of milliseconds.
     */
    void toggleDTR(timeout_t millisec);

    /**
     * Send the "break" signal.
     */
    void sendBreak(void);

    /**
     * Often used by a "catch" to fetch the last error of a thrown
     * serial.
     *
     * @return error numbr of last Error.
     */
    inline Error getErrorNumber(void)
        {return errid;};

    /**
     * Often used by a "catch" to fetch the user set error string
     * of a thrown serial.
     *
     * @return string for error message.
     */
    inline char *getErrorString(void)
        {return errstr;};

    /**
     * Get the "buffer" size for buffered operations.  This can
     * be used when setting packet or line read modes to determine
     * how many bytes to wait for in a given read call.
     *
     * @return number of bytes used for buffering.
     */
    inline int getBufferSize(void)
        {return bufsize;};

    /**
     * Get the status of pending operations.  This can be used to
     * examine if input or output is waiting, or if an error has
     * occured on the serial device.
     *
     * @return true if ready, false if timeout.
     * @param pend ready check to perform.
     * @param timeout in milliseconds.
     */
    virtual bool isPending(Pending pend, timeout_t timeout = TIMEOUT_INF);
};

/**
 * TTY streams are used to represent serial connections that are fully
 * "streamable" objects using C++ stream classes and friends.
 *
 * The first application relevant serial I/O class is the TTYStream class.
 * TTYStream offers a linearly buffered "streaming" I/O session with the
 * serial device.  Furthermore, traditional C++ "stream" operators (<< and
 * >>) may be used with the serial device.  A more "true" to ANSI C++ library
 * format "ttystream" is also available, and this supports an "open" method
 * in which one can pass initial serial device parameters immediately
 * following the device name in a single string, as in
 * "/dev/tty3a:9600,7,e,1", as an example.
 *
 * The TTYSession aggragates a TTYStream and a Common C++ Thread which is
 * assumed to be the execution context that will be used to perform actual
 * I/O operations.  This class is very anagolous to TCPSession.
 *
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short streamable tty serial I/O class.
 */
class __EXPORT TTYStream : protected std::streambuf, public Serial, public std::iostream
{
private:
    int doallocate();

    friend TTYStream& crlf(TTYStream&);
    friend TTYStream& lfcr(TTYStream&);

protected:
    char *gbuf, *pbuf;
    timeout_t timeout;

    /**
     * This constructor is used to derive "ttystream", a more
     * C++ style version of the TTYStream class.
     */
    TTYStream();

    /**
     * Used to allocate the buffer space needed for iostream
     * operations.  This is based on MAX_INPUT.
     */
    void allocate(void);

    /**
     * Used to terminate the buffer space and clean up the tty
     * connection.  This function is called by the destructor.
     */
    void endStream(void);

    /**
     * This streambuf method is used to load the input buffer
     * through the established tty serial port.
     *
     * @return char from get buffer, EOF also possible.
     */
    int underflow(void);

    /**
     * This streambuf method is used for doing unbuffered reads
     * through the establish tty serial port when in interactive mode.
     * Also this method will handle proper use of buffers if not in
     * interative mode.
     *
     * @return char from tty serial port, EOF also possible.
     */
    int uflow(void);

    /**
     * This streambuf method is used to write the output
     * buffer through the established tty port.
     *
     * @param ch char to push through.
     * @return char pushed through.
     */
    int overflow(int ch);

public:
    /**
     * Create and open a tty serial port.
     *
     * @param filename char name of device to open.
     * @param to default timeout.
     */
    TTYStream(const char *filename, timeout_t to = 0);

    /**
     * End the tty stream and cleanup.
     */
    virtual ~TTYStream();

    /**
     * Set the timeout control.
     *
     * @param to timeout to use.
     */
    inline void setTimeout(timeout_t to)
        {timeout = to;};

    /**
     * Set tty mode to buffered or "interactive".  When interactive,
     * all streamed I/O is directly sent to the serial port
     * immediately.
     *
     * @param flag bool set to true to make interactive.
     */
    void interactive(bool flag);

    /**
     * Flushes the stream input and out buffers, writes
     * pending output.
     *
     * @return 0 on success.
     */
    int sync(void);

    /**
     * Get the status of pending operations.  This can be used to
     * examine if input or output is waiting, or if an error has
     * occured on the serial device.  If read buffer contains data
     * then input is ready and if write buffer contains data it is
     * first flushed then checked.
     *
     * @return true if ready, false if timeout.
     * @param pend ready check to perform.
     * @param timeout in milliseconds.
     */
    bool isPending(Pending pend, timeout_t timeout = TIMEOUT_INF);
};

/**
 * A more natural C++ "ttystream" class for use by non-threaded
 * applications.  This class behaves a lot more like fstream and
 * similar classes.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short C++ "fstream" style ttystream class.
 */

class __EXPORT ttystream : public TTYStream
{
public:
    /**
     * Construct an unopened "ttystream" object.
     */
    ttystream();

    /**
     * Construct and "open" a tty stream object.  A filename in
     * the form "device:options[,options]" may be used to pass
     * device options as part of the open.
     *
     * @param name of file and serial options.
     */
    ttystream(const char *name);

    /**
     * Open method for a tty stream.
     *
     * @param name filename to open.
     */
    void open(const char *name);

    /**
     * Close method for a tty stream.
     */
    void close(void);

    /**
     * Test to see if stream is opened.
     */
    inline bool operator!()
        {return (dev < 0);};
};

/**
 *
 * The TTYSession aggragates a TTYStream and a Common C++ Thread which is
 * assumed to be the execution context that will be used to perform actual
 * I/O operations.  This class is very anagolous to TCPSession.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short This class is very anagolous to TCPSession.
 */

class __EXPORT TTYSession : public Thread, public TTYStream
{
public:
    /**
     * Create TTY stream that will be managed by it's own thread.
     *
     * @param name of tty device to open.
     * @param pri execution priority.
     * @param stack allocation needed on some platforms.
     */
    TTYSession(const char *name, int pri = 0, int stack = 0);

    virtual ~TTYSession();
};

#ifndef _MSWINDOWS_

//  Not support this right now.......
//
class SerialPort;
class SerialService;

/**
 * The serial port is an internal class which is attached to and then
 * serviced by a specified SerialService thread.  Derived versions of
 * this class offer specific functionality such as serial integration
 * protocols.
 *
 * The TTYPort and TTYService classes are used to form thread-pool serviced
 * serial I/O protocol sets.  These can be used when one has a large number
 * of serial devices to manage, and a single (or limited number of) thread(s)
 * can then be used to service the tty port objects present.  Each tty port
 * supports a timer control and several virtual methods that the service
 * thread can call when events occur.  This model provides for "callback"
 * event management, whereby the service thread performs a "callback" into
 * the port object when events occur.  Specific events supported include the
 * expiration of a TTYPort timer, pending input data waiting to be read, and
 * "sighup" connection breaks.
 *
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short base class for thread pool serviced serial I/O.
 */
class __EXPORT SerialPort: public Serial, public TimerPort
{
private:
    SerialPort *next, *prev;
    SerialService *service;
#ifdef  USE_POLL
    struct pollfd *ufd;
#endif
    bool detect_pending;
    bool detect_output;
    bool detect_disconnect;

    friend class SerialService;

protected:
    /**
     * Construct a tty serial port for a named serial device.
     *
     * @param svc pool thread object.
     * @param name of tty port.
     */
    SerialPort(SerialService *svc, const char *name);

    /**
     * Disconnect the Serial Port from the service pool thread
     * and shutdown the port.
     */
    virtual ~SerialPort();

    /**
     * Used to indicate if the service thread should monitor pending
     * data for us.
     */
    void setDetectPending( bool );

    /**
     * Get the current state of the DetectPending flag.
     */
    inline bool getDetectPending( void ) const
        { return detect_pending; }

    /**
     * Used to indicate if output ready monitoring should be performed
     * by the service thread.
     */
    void setDetectOutput( bool );

    /**
     * Get the current state of the DetectOutput flag.
     */
    inline bool getDetectOutput( void ) const
        { return detect_output; }

    /**
     * Called by the service thread when the objects timer
     * has expired.
     */
    virtual void expired(void);

    /**
     * Called by the service thread when input data is pending
     * for this tty port.  Effected by setPacketInput and by
     * setLineInput.
     */
    virtual void pending(void);

    /**
     * Called by the service thread when an exception has occured
     * such as a hangup.
     */
    virtual void disconnect(void);

    /**
     * Transmit "send" data to the serial port.  This is not public
     * since it's meant to support internal protocols rather than
     * direct public access to the device.
     *
     * @return number of bytes send.
     * @param buf address of buffer to send.
     * @param len of bytes to send.
     */
    inline int output(void *buf, int len)
        {return aWrite((char *)buf, len);};

    /**
     * Perform when output is available for sending data.
     */
    virtual void output(void);

    /**
     * Receive "input" for pending data from the serial port.  This
     * is not a public member since it's meant to support internal
     * protocols rather than direct external access to the device.
     *
     * @return number of bytes received.
     * @param buf address of buffer to input.
     * @param len of input buffer used.
     */
    inline int input(void *buf, int len)
        {return aRead((char *)buf, len);};
public:
    /**
     * Derived setTimer to notify the service thread pool of changes
     * in expected timeout.  This allows SerialService to
     * reschedule all timers.
     *
     * @param timeout in milliseconds.
     */
    void setTimer(timeout_t timeout = 0);

    /**
     * Derived incTimer to notify the service thread pool of a
     * change in expected timeout.  This allows SerialService to
     * reschedule all timers.
     */
    void incTimer(timeout_t timeout);
};

/**
 * The SerialService is a thead service object that is meant to service
 * attached serial ports.  Multiple pool objects may be created and
 * multiple serial ports may be attached to the same thread of
 * of execution.  This allows one to balance threads and the serial ports
 * they service.
 *
 *  The TTYPort and TTYService classes are used to form thread-pool serviced
 *  serial I/O protocol sets.  These can be used when one has a large number
 *  of serial devices to manage, and a single (or limited number of) thread(s)
 *  can then be used to service the tty port objects present.  Each tty port
 *  supports a timer control and several virtual methods that the service
 *  thread can call when events occur.  This model provides for "callback"
 *  event management, whereby the service thread performs a "callback" into
 *  the port object when events occur.  Specific events supported include the
 *  expiration of a TTYPort timer, pending input data waiting to be read, and
 *  "sighup" connection breaks.
 *
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Thread pool service for serial ports.
 */
class __EXPORT SerialService : public Thread, private Mutex
{
private:
    fd_set connect;
    int iosync[2];
    int hiwater;
    int count;
    SerialPort *first, *last;

    /**
     * Attach a new serial port to this service thread.
     *
     * @param port of SerialPort derived object to attach.
     */
    void attach(SerialPort *port);

    /**
     * Detach a serial port from this service thread.
     *
     * @param port of SerialPort derived object to remove.
     */
    void detach(SerialPort *port);

    /**
     * The service thread itself.
     */
    void run(void);

    friend class SerialPort;

protected:
    /**
     * A virtual handler for processing user defined update
     * requests (1-254) which have been posted through Update.
     *
     * @param flag of update request.
     */
    virtual void onUpdate(unsigned char flag);

    /**
     * A virtual handler for event loop calls.  This can be
     * used to extend event loop processing.
     */
    virtual void onEvent(void);

    /**
     * A virtual handler for adding support for additional
     * callback events into SerialPort.
     *
     * @param port serial port currently being evaluated.
     */
    virtual void onCallback(SerialPort *port);

public:
    /**
     * Notify service thread that a port has been added or
     * removed, or a timer changed, so that a new schedule
     * can be computed for expiring attached ports.  This
     * can also be used to pass requests to the OnUpdate()
     * event handler.
     *
     * @param flag event for OnUpdate, termination, or reschedule.
     */
    void update(unsigned char flag = 0xff);

    /**
     * Create a service thread for attaching serial ports.  The
     * thread begins execution with the first attached port.
     *
     * @param pri of this thread to run under.
     * @param stack stack size.
     * @param id stack ID.
     */
    SerialService(int pri = 0, size_t stack = 0, const char *id = NULL);

    /**
     * Terminate the service thread and update attached objects.
     */
    virtual ~SerialService();

    /**
     * Get current reference count.  This can be used when selecting
     * the lead used service handler from a pool.
     *
     * @return count of active ports.
     */
    inline int getCount(void)
        {return count;};
};

#endif

#ifdef  CCXX_EXCEPTIONS
class __EXPORT SerException : public IOException
{
public:
    SerException(const String &str) : IOException(str) {};
};
#endif

END_NAMESPACE

#endif
/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
