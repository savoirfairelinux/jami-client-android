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

#include <ucommon-config.h>
#include <commoncpp/config.h>
#include <commoncpp/export.h>
#include <commoncpp/exception.h>
#include <commoncpp/thread.h>
#include <commoncpp/serial.h>
#include <cstdlib>
#include <climits>

#ifdef  _MSWINDOWS_

#define B256000     CBR_256000
#define B128000     CBR_128000
#define B115200     CBR_115200
#define B57600      CBR_57600
#define B56000      CBR_56000
#define B38400      CBR_38400
#define B19200      CBR_19200
#define B14400      CBR_14400
#define B9600       CBR_9600
#define B4800       CBR_4800
#define B2400       CBR_2400
#define B1200       CBR_1200
#define B600        CBR_600
#define B300        CBR_300
#define B110        CBR_110

#include <io.h>
#else
#include <sys/ioctl.h>
#ifdef  HAVE_TERMIOS_H
#include <termios.h>
#endif
#endif

#include <fcntl.h>
#include <cerrno>
#include <iostream>

NAMESPACE_COMMONCPP
using std::streambuf;
using std::iostream;
using std::ios;

#ifndef MAX_INPUT
#define MAX_INPUT 255
#endif

#ifndef MAX_CANON
#define MAX_CANON MAX_INPUT
#endif

#ifdef  __FreeBSD__
#undef  _PC_MAX_INPUT
#undef  _PC_MAX_CANON
#endif

#if defined(__QNX__)
#define CRTSCTS (IHFLOW | OHFLOW)
#endif

#if defined(_THR_UNIXWARE) || defined(__hpux) || defined(_AIX)
#include <sys/termiox.h>
#define CRTSCTS (CTSXON | RTSXOFF)
#endif

// IRIX

#ifndef CRTSCTS
#ifdef  CNEW_RTSCTS
#define CRTSCTS (CNEW_RTSCTS)
#endif
#endif

#if defined(CTSXON) && defined(RTSXOFF) && !defined(CRTSCTS)
#define CRTSCTS (CTSXON | RTSXOFF)
#endif

#ifndef CRTSCTS
#define CRTSCTS 0
#endif

Serial::Serial(const char *fname)
{
    initSerial();

#if defined(_MSWINDOWS_) || defined(HAVE_TERMIOS_H)
    open(fname);
#endif

#ifdef  _MSWINDOWS_
    if(dev == INVALID_HANDLE_VALUE)
#elif defined(HAVE_TERMIOS_H)
    if(dev < 0)
#else
    if(1)
#endif
    {
        error(errOpenFailed);
        return;
    }

#ifdef  _MSWINDOWS_
    COMMTIMEOUTS  CommTimeOuts ;

    GetCommTimeouts(dev, &CommTimeOuts);

//    CommTimeOuts.ReadIntervalTimeout = MAXDWORD;
    CommTimeOuts.ReadIntervalTimeout = 0;

    CommTimeOuts.ReadTotalTimeoutMultiplier = 0 ;
    CommTimeOuts.ReadTotalTimeoutConstant = 0;
    CommTimeOuts.WriteTotalTimeoutMultiplier = 0 ;
    CommTimeOuts.WriteTotalTimeoutConstant = 1000;

    SetCommTimeouts(dev, &CommTimeOuts) ;

#elif defined(HAVE_TERMIOS_H)

    if(!isatty(dev)) {
        Serial::close();
        error(errOpenNoTty);
        return;
    }
#endif
}

Serial::~Serial()
{
    endSerial();
}

void Serial::initConfig(void)
{
#ifdef  _MSWINDOWS_

#define ASCII_XON       0x11
#define ASCII_XOFF      0x13

    DCB * attr = (DCB *)current;
    DCB * orig = (DCB *)original;

    attr->DCBlength = sizeof(DCB);
    orig->DCBlength = sizeof(DCB);

    GetCommState(dev, orig);
    GetCommState(dev, attr);

    attr->DCBlength = sizeof(DCB);
    attr->BaudRate = 1200;
    attr->Parity = NOPARITY;
    attr->ByteSize = 8;

    attr->XonChar = ASCII_XON;
    attr->XoffChar = ASCII_XOFF;
    attr->XonLim = 100;
    attr->XoffLim = 100;
    attr->fOutxDsrFlow = 0;
    attr->fDtrControl = DTR_CONTROL_ENABLE;
    attr->fOutxCtsFlow = 1;
    attr->fRtsControl = RTS_CONTROL_ENABLE;
    attr->fInX = attr->fOutX = 0;

    attr->fBinary = true;
    attr->fParity = true;

    SetCommState(dev, attr);

#elif defined(HAVE_TERMIOS_H)
    struct termios *attr = (struct termios *)current;
    struct termios *orig = (struct termios *)original;
    long ioflags = fcntl(dev, F_GETFL);

    tcgetattr(dev, (struct termios *)original);
    tcgetattr(dev, (struct termios *)current);

    attr->c_oflag = attr->c_lflag = 0;
    attr->c_cflag = CLOCAL | CREAD | HUPCL;
    attr->c_iflag = IGNBRK;

    memset(attr->c_cc, 0, sizeof(attr->c_cc));
    attr->c_cc[VMIN] = 1;

    // inherit original settings, maybe we should keep more??
    cfsetispeed(attr, cfgetispeed(orig));
    cfsetospeed(attr, cfgetospeed(orig));
    attr->c_cflag |= orig->c_cflag & (CRTSCTS | CSIZE | PARENB | PARODD | CSTOPB);
    attr->c_iflag |= orig->c_iflag & (IXON | IXANY | IXOFF);

    tcsetattr(dev, TCSANOW, attr);
    fcntl(dev, F_SETFL, ioflags & ~O_NDELAY);

#if defined(TIOCM_RTS) && defined(TIOCMODG)
    int mcs = 0;
    ioctl(dev, TIOCMODG, &mcs);
    mcs |= TIOCM_RTS;
    ioctl(dev, TIOCMODS, &mcs);
#endif

#ifdef  _COHERENT
    ioctl(dev, TIOCSRTS, 0);
#endif

#endif  // WIN32
    }

void Serial::restore(void)
{
#ifdef  _MSWINDOWS_
    memcpy(current, original, sizeof(DCB));
    SetCommState(dev, (DCB *)current);
#elif defined(HAVE_TERMIOS_H)
    memcpy(current, original, sizeof(struct termios));
    tcsetattr(dev, TCSANOW, (struct termios *)current);
#endif
}

void Serial::initSerial(void)
{
    flags.thrown = false;
    flags.linebuf = false;
    errid = errSuccess;
    errstr = NULL;

    dev = INVALID_HANDLE_VALUE;
#ifdef  _MSWINDOWS_
    current = new DCB;
    original = new DCB;
#elif defined(HAVE_TERMIOS_H)
    current = new struct termios;
    original = new struct termios;
#endif
}

void Serial::endSerial(void)
{
#ifdef  _MSWINDOWS_
    if(dev == INVALID_HANDLE_VALUE && original)
        SetCommState(dev, (DCB *)original);

    if(current)
        delete (DCB *)current;

    if(original)
        delete (DCB *)original;
#elif defined(HAVE_TERMIOS_H)
    if(dev < 0 && original)
        tcsetattr(dev, TCSANOW, (struct termios *)original);

    if(current)
        delete (struct termios *)current;

    if(original)
        delete (struct termios *)original;
#endif
    Serial::close();

    current = NULL;
    original = NULL;
}

Serial::Error Serial::error(Error err, char *errs)
{
    errid = err;
    errstr = errs;
    if(!err)
        return err;

    if(flags.thrown)
        return err;

    // prevents recursive throws

    flags.thrown = true;
#ifdef  CCXX_EXCEPTIONS
    if(Thread::getException() == Thread::throwObject)
            throw((Serial *)this);
#ifdef  COMMON_STD_EXCEPTION
    else if(Thread::getException() == Thread::throwException) {
        if(!errs)
            errs = (char *)"";
        throw SerException(String(errs));
    }
#endif
#endif
    return err;
}

int Serial::setPacketInput(int size, unsigned char btimer)
{
#ifdef  _MSWINDOWS_
    //  Still to be done......
    return 0;
#elif defined(HAVE_TERMIOS_H)

#ifdef  _PC_MAX_INPUT
    int max = fpathconf(dev, _PC_MAX_INPUT);
#else
    int max = MAX_INPUT;
#endif
    struct termios *attr = (struct termios *)current;

    if(size > max)
        size = max;

    attr->c_cc[VEOL] = attr->c_cc[VEOL2] = 0;
    attr->c_cc[VMIN] = (unsigned char)size;
    attr->c_cc[VTIME] = btimer;
    attr->c_lflag &= ~ICANON;
    tcsetattr(dev, TCSANOW, attr);
    bufsize = size;
    return size;
#endif
}

int Serial::setLineInput(char newline, char nl1)
{
#ifdef  _MSWINDOWS_
    //  Still to be done......
    return 0;
#elif defined(HAVE_TERMIOS_H)

    struct termios *attr = (struct termios *)current;
    attr->c_cc[VMIN] = attr->c_cc[VTIME] = 0;
    attr->c_cc[VEOL] = newline;
    attr->c_cc[VEOL2] = nl1;
    attr->c_lflag |= ICANON;
    tcsetattr(dev, TCSANOW, attr);
#ifdef _PC_MAX_CANON
    bufsize = fpathconf(dev, _PC_MAX_CANON);
#else
    bufsize = MAX_CANON;
#endif
    return bufsize;
#endif
}

void Serial::flushInput(void)
{
#ifdef  _MSWINDOWS_
    PurgeComm(dev, PURGE_RXABORT | PURGE_RXCLEAR);
#elif defined(HAVE_TERMIOS_H)
    tcflush(dev, TCIFLUSH);
#endif
}

void Serial::flushOutput(void)
{
#ifdef  _MSWINDOWS_
    PurgeComm(dev, PURGE_TXABORT | PURGE_TXCLEAR);
#elif defined(HAVE_TERMIOS_H)
    tcflush(dev, TCOFLUSH);
#endif
}

void Serial::waitOutput(void)
{
#ifdef  _MSWINDOWS_

#elif defined(HAVE_TERMIOS_H)
    tcdrain(dev);
#endif
}

Serial &Serial::operator=(const Serial &ser)
{
    Serial::close();

    if(ser.dev < 0)
        return *this;

#ifdef  _MSWINDOWS_
    HANDLE process = GetCurrentProcess();

    int result = DuplicateHandle(process, ser.dev, process, &dev, DUPLICATE_SAME_ACCESS, 0, 0);

    if (0 == result) {
        memcpy(current, ser.current, sizeof(DCB));
        memcpy(original, ser.original, sizeof(DCB));
    }
#elif defined(HAVE_TERMIOS_H)
    dev = dup(ser.dev);

    memcpy(current, ser.current, sizeof(struct termios));
    memcpy(original, ser.original, sizeof(struct termios));
#endif
    return *this;
}


void Serial::open(const char * fname)
{

#ifndef _MSWINDOWS_
    int cflags = O_RDWR | O_NDELAY;
    dev = ::open(fname, cflags);
    if(dev > -1)
        initConfig();
#elif defined(HAVE_TERMIOS_H)
    // open COMM device
    dev = CreateFile(fname,
        GENERIC_READ | GENERIC_WRITE,
        0,                    // exclusive access
        NULL,                 // no security attrs
        OPEN_EXISTING,
        FILE_FLAG_OVERLAPPED | FILE_ATTRIBUTE_NORMAL | FILE_FLAG_WRITE_THROUGH | FILE_FLAG_NO_BUFFERING,
        NULL);
    if(dev != INVALID_HANDLE_VALUE)
        initConfig();
#endif
}

#ifdef  _MSWINDOWS_
int Serial::aRead(char * Data, const int Length)
{

    unsigned long   dwLength = 0, dwError, dwReadLength;
    COMSTAT cs;
    OVERLAPPED ol;

    // Return zero if handle is invalid
    if(dev == INVALID_HANDLE_VALUE)
        return 0;

    // Read max length or only what is available
    ClearCommError(dev, &dwError, &cs);

    // If not requiring an exact byte count, get whatever is available
    if(Length > (int)cs.cbInQue)
        dwReadLength = cs.cbInQue;
    else
        dwReadLength = Length;

    memset(&ol, 0, sizeof(OVERLAPPED));
    ol.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

    if(dwReadLength > 0) {
        if(ReadFile(dev, Data, dwReadLength, &dwLength, &ol) == FALSE) {
            if(GetLastError() == ERROR_IO_PENDING) {
                WaitForSingleObject(ol.hEvent, INFINITE);
                GetOverlappedResult(dev, &ol, &dwLength, TRUE);
            }
            else
                ClearCommError(dev, &dwError, &cs);
        }
    }

    if(ol.hEvent != INVALID_HANDLE_VALUE)
        CloseHandle(ol.hEvent);

    return dwLength;
}

int Serial::aWrite(const char * Data, const int Length)
{
    COMSTAT cs;
    unsigned long dwError = 0;
    OVERLAPPED ol;

    // Clear the com port of any error condition prior to read
    ClearCommError(dev, &dwError, &cs);

    unsigned long retSize = 0;

    memset(&ol, 0, sizeof(OVERLAPPED));
    ol.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

    if(WriteFile(dev, Data, Length, &retSize, &ol) == FALSE) {
        if(GetLastError() == ERROR_IO_PENDING) {
            WaitForSingleObject(ol.hEvent, INFINITE);
            GetOverlappedResult(dev, &ol, &retSize, TRUE);
        }
        else
            ClearCommError(dev, &dwError, &cs);
    }

    if(ol.hEvent != INVALID_HANDLE_VALUE)
        CloseHandle(ol.hEvent);

    return retSize;
}
#else
int Serial::aRead(char *Data, const int Length)
{
    return ::read(dev, Data, Length);
}

int Serial::aWrite(const char *Data, const int Length)
{
    return ::write(dev, Data, Length);
}
#endif

void Serial::close()
{
#ifdef  _MSWINDOWS_
    CloseHandle(dev);
#else
    ::close(dev);
#endif

    dev = INVALID_HANDLE_VALUE;
}
























/*
const int iAsync::getTimeOuts(unsigned long & readTimeout, unsigned long & writeTimeout)
{
    return GetCommTimeouts(_TheHandle, &CommTimeOuts);
    }

const int iAsync::setTimeOuts(unsigned long readTimeout, unsigned long writeTimeout)
{
    COMMTIMEOUTS  CommTimeOuts ;

    getTimeOuts(CommTimeOuts);

    CommTimeOuts.ReadIntervalTimeout = readTimeout;
    CommTimeOuts.ReadTotalTimeoutMultiplier = 0 ;
    CommTimeOuts.ReadTotalTimeoutConstant = 0;
    CommTimeOuts.WriteTotalTimeoutMultiplier = 0 ;
    CommTimeOuts.WriteTotalTimeoutConstant = 1000;

    return GetCommTimeouts(_TheHandle, &CommTimeOuts);
    }
    return SetCommTimeouts(_TheHandle, &CommTimeOuts) ;
{
    DCB        dcb ;

    dcb.DCBlength = sizeof(DCB) ;

    GetCommState(_TheHandle, &dcb) ;

    // hardcode this stuff for now.....
    dcb.BaudRate = _TheBaudrate;
    dcb.ByteSize = 8;
    dcb.Parity   = NOPARITY;
    dcb.StopBits = ONESTOPBIT;
    dcb.fOutxDsrFlow = 0;
    dcb.fDtrControl = DTR_CONTROL_ENABLE ;
    dcb.fOutxCtsFlow = 1;
    dcb.fRtsControl = RTS_CONTROL_HANDSHAKE ;
    dcb.fInX = dcb.fOutX = 0;

    dcb.XonChar = ASCII_XON;
    dcb.XoffChar = ASCII_XOFF;
    dcb.XonLim = 100;
    dcb.XoffLim = 100;

    // other various settings

    dcb.fBinary = TRUE;
    dcb.fParity = TRUE;

    GetCommState(_TheHandle, &dcb) ;
    dcb.DCBlength = sizeof(DCB) ;
    dcb.BaudRate = _TheBaudrate;
    SetCommState(_TheHandle, &dcb) ;

    }
*/

Serial::Error Serial::setSpeed(unsigned long speed)
{
    unsigned long rate;

    switch(speed) {
#ifdef B115200
        case 115200:
                rate = B115200;
                break;
#endif
#ifdef B57600
        case 57600:
                rate = B57600;
                break;
#endif
#ifdef B38400
        case 38400:
                rate = B38400;
                break;
#endif
        case 19200:
                rate = B19200;
                break;
        case 9600:
                rate = B9600;
                break;
        case 4800:
                rate = B4800;
                break;
        case 2400:
                rate = B2400;
                break;
        case 1200:
                rate = B1200;
                break;
        case 600:
                rate = B600;
                break;
        case 300:
                rate = B300;
                break;
        case 110:
                rate = B110;
                break;
#ifdef  B0
        case 0:
                rate = B0;
                break;
#endif
    default:
        return error(errSpeedInvalid);
    }

#ifdef  _MSWINDOWS_

    DCB     * dcb = (DCB *)current;
    dcb->DCBlength = sizeof(DCB);
    GetCommState(dev, dcb);

    dcb->BaudRate = rate;
    SetCommState(dev, dcb) ;

#else
    struct termios *attr = (struct termios *)current;
    cfsetispeed(attr, rate);
    cfsetospeed(attr, rate);
    tcsetattr(dev, TCSANOW, attr);
#endif
    return errSuccess;
    }

Serial::Error Serial::setFlowControl(Flow flow)
{
#ifdef  _MSWINDOWS_

    DCB * attr = (DCB *)current;
    attr->XonChar = ASCII_XON;
    attr->XoffChar = ASCII_XOFF;
    attr->XonLim = 100;
    attr->XoffLim = 100;

    switch(flow) {
    case flowSoft:
        attr->fInX = attr->fOutX = 1;
        break;
    case flowBoth:
        attr->fInX = attr->fOutX = 1;
    case flowHard:
        attr->fOutxCtsFlow = 1;
        attr->fRtsControl = RTS_CONTROL_HANDSHAKE;
        break;
    case flowNone:
        break;
    default:
        return error(errFlowInvalid);
    }

    SetCommState(dev, attr);
#else

    struct termios *attr = (struct termios *)current;

    attr->c_cflag &= ~CRTSCTS;
    attr->c_iflag &= ~(IXON | IXANY | IXOFF);

    switch(flow) {
    case flowSoft:
        attr->c_iflag |= (IXON | IXANY | IXOFF);
        break;
    case flowBoth:
        attr->c_iflag |= (IXON | IXANY | IXOFF);
    case flowHard:
        attr->c_cflag |= CRTSCTS;
        break;
    case flowNone:
        break;
    default:
        return error(errFlowInvalid);
    }

    tcsetattr(dev, TCSANOW, attr);

#endif
    return errSuccess;
    }

Serial::Error Serial::setStopBits(int bits)
{
#ifdef  _MSWINDOWS_

    DCB * attr = (DCB *)current;
    switch(bits) {
    case 1:
        attr->StopBits = ONESTOPBIT;
        break;
    case 2:
        attr->StopBits = TWOSTOPBITS;
        break;
    default:
        return error(errStopbitsInvalid);
    }

    SetCommState(dev, attr);
#else
    struct termios *attr = (struct termios *)current;
    attr->c_cflag &= ~CSTOPB;

    switch(bits) {
    case 1:
        break;
    case 2:
        attr->c_cflag |= CSTOPB;
        break;
    default:
        return error(errStopbitsInvalid);
    }
    tcsetattr(dev, TCSANOW, attr);
#endif
    return errSuccess;
    }

Serial::Error Serial::setCharBits(int bits)
{
#ifdef  _MSWINDOWS_

    DCB * attr = (DCB *)current;
    switch(bits) {
    case 5:
    case 6:
    case 7:
    case 8:
        attr->ByteSize = bits;
        break;
    default:
        return error(errCharsizeInvalid);
    }
    SetCommState(dev, attr);
#else
    struct termios *attr = (struct termios *)current;
    attr->c_cflag &= ~CSIZE;

    switch(bits) {
    case 5:
        attr->c_cflag |= CS5;
        break;
    case 6:
        attr->c_cflag |= CS6;
        break;
    case 7:
        attr->c_cflag |= CS7;
        break;
    case 8:
        attr->c_cflag |= CS8;
        break;
    default:
        return error(errCharsizeInvalid);
    }
    tcsetattr(dev, TCSANOW, attr);
#endif
    return errSuccess;
    }

Serial::Error Serial::setParity(Parity parity)
{
#ifdef  _MSWINDOWS_

    DCB * attr = (DCB *)current;
    switch(parity) {
    case parityEven:
        attr->Parity = EVENPARITY;
        break;
    case parityOdd:
        attr->Parity = ODDPARITY;
        break;
    case parityNone:
        attr->Parity = NOPARITY;
        break;
    default:
        return error(errParityInvalid);
    }
    SetCommState(dev, attr);
#else
    struct termios *attr = (struct termios *)current;
    attr->c_cflag &= ~(PARENB | PARODD);

    switch(parity) {
    case parityEven:
        attr->c_cflag |= PARENB;
        break;
    case parityOdd:
        attr->c_cflag |= (PARENB | PARODD);
        break;
    case parityNone:
        break;
    default:
        return error(errParityInvalid);
    }
    tcsetattr(dev, TCSANOW, attr);
#endif
    return errSuccess;
    }

void Serial::sendBreak(void)
{
#ifdef  _MSWINDOWS_
    SetCommBreak(dev);
    Thread::sleep(100L);
    ClearCommBreak(dev);
#else
    tcsendbreak(dev, 0);
#endif
    }

void Serial::toggleDTR(timeout_t millisec)
{
#ifdef  _MSWINDOWS_
    EscapeCommFunction(dev, CLRDTR);
    if(millisec) {
        Thread::sleep(millisec);
        EscapeCommFunction(dev, SETDTR);
    }

#else
    struct termios tty, old;
    tcgetattr(dev, &tty);
    tcgetattr(dev, &old);
    cfsetospeed(&tty, B0);
    cfsetispeed(&tty, B0);
    tcsetattr(dev, TCSANOW, &tty);

    if(millisec) {
        Thread::sleep(millisec);
        tcsetattr(dev, TCSANOW, &old);
    }
#endif
    }

bool Serial::isPending(Pending pending, timeout_t timeout)
{
#ifdef  _MSWINDOWS_
    unsigned long   dwError;
    COMSTAT cs;

    ClearCommError(dev, &dwError, &cs);

    if(timeout == 0 || ((pending == pendingInput) && (0 != cs.cbInQue)) ||
       ((pending == pendingOutput) && (0 != cs.cbOutQue)) || (pending == pendingError))
    {
        switch(pending) {
        case pendingInput:
            return (0 != cs.cbInQue);
        case pendingOutput:
            return (0 != cs.cbOutQue);
        case pendingError:
            return false;
        }
        }
    else {
        OVERLAPPED ol;
        DWORD dwMask;
        DWORD dwEvents = 0;
        BOOL suc;

        memset(&ol, 0, sizeof(OVERLAPPED));
        ol.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

        if(pending == pendingInput)
            dwMask = EV_RXCHAR;
        else if(pending == pendingOutput)
            dwMask = EV_TXEMPTY;
        else   // on error
            dwMask = EV_ERR;

        SetCommMask(dev, dwMask);
        // let's wait for event or timeout
        if((suc = WaitCommEvent(dev, &dwEvents, &ol)) == FALSE) {
            if(GetLastError() == ERROR_IO_PENDING) {
                DWORD transferred;

                dwError = WaitForSingleObject(ol.hEvent, timeout);
                if (dwError != WAIT_OBJECT_0)
                    SetCommMask(dev, 0);

                suc = GetOverlappedResult(dev, &ol, &transferred, TRUE);
                if (suc)
                    suc = (dwEvents & dwMask) ? TRUE : FALSE;
                }
            else
                ClearCommError(dev, &dwError, &cs);
            }

        if(ol.hEvent != INVALID_HANDLE_VALUE)
            CloseHandle(ol.hEvent);

        if(suc == FALSE)
                return false;
        return true;
        }
#else


    int status = 0;
#ifdef HAVE_POLL
    struct pollfd pfd;

    pfd.fd = dev;
    pfd.revents = 0;
    switch(pending) {
    case pendingInput:
        pfd.events = POLLIN;
        break;
    case pendingOutput:
        pfd.events = POLLOUT;
        break;
    case pendingError:
        pfd.events = POLLERR | POLLHUP;
        break;
    }

    status = 0;
    while(status < 1) {
        if(timeout == TIMEOUT_INF)
            status = poll(&pfd, 1, -1);
        else
            status = poll(&pfd, 1, timeout);

        if(status < 1) {
            if(status == -1 && errno == EINTR)
                continue;
            return false;
        }
    }

    if(pfd.revents & pfd.events)
        return true;

#else
    struct timeval tv;
    fd_set grp;
    struct timeval *tvp = &tv;

    if(timeout == TIMEOUT_INF)
        tvp = NULL;
    else {
        tv.tv_usec = (timeout % 1000) * 1000;
        tv.tv_sec = timeout / 1000;
    }

    FD_ZERO(&grp);
    FD_SET(dev, &grp);
    switch(pending) {
    case pendingInput:
        status = select(dev + 1, &grp, NULL, NULL, tvp);
        break;
    case pendingOutput:
        status = select(dev + 1, NULL, &grp, NULL, tvp);
        break;
    case pendingError:
        status = select(dev + 1, NULL, NULL, &grp, tvp);
        break;
    }
    if(status < 1)
        return false;

    if(FD_ISSET(dev, &grp))
        return true;

#endif

#endif  //  WIN32

    return false;
    }





TTYStream::TTYStream(const char *filename, timeout_t to)
    :   streambuf(),
        Serial(filename),
#ifdef  HAVE_OLD_IOSTREAM
        iostream()
#else
        iostream((streambuf *)this)
#endif
{
#ifdef  HAVE_OLD_IOSTREAM
    init((streambuf *)this);
#endif
    gbuf = pbuf = NULL;
    timeout = to;

    if(INVALID_HANDLE_VALUE != dev)
        allocate();
        }

TTYStream::TTYStream()
    :   streambuf(),
        Serial(),
#ifdef  HAVE_OLD_IOSTREAM
        iostream()
#else
        iostream((streambuf *)this)
#endif
{
#ifdef  HAVE_OLD_IOSTREAM
    init((streambuf *)this);
#endif
    timeout = 0;
    gbuf = pbuf = NULL;
            }

TTYStream::~TTYStream()
{
    endStream();
    endSerial();
            }

void TTYStream::endStream(void)
{
    if(bufsize)
        sync();

    if(gbuf) {
        delete[] gbuf;
        gbuf = NULL;
    }
    if(pbuf) {
        delete[] pbuf;
        pbuf = NULL;
    }
    bufsize = 0;
    clear();
            }

void TTYStream::allocate(void)
{
    if(INVALID_HANDLE_VALUE == dev)
        return;

#ifdef _PC_MAX_INPUT
    bufsize = fpathconf(dev, _PC_MAX_INPUT);
#else
    bufsize = MAX_INPUT;
#endif

    gbuf = new char[bufsize];
    pbuf = new char[bufsize];

    if(!pbuf || !gbuf) {
        error(errResourceFailure);
        return;
    }

    clear();

#if !(defined(STLPORT) || defined(__KCC))
    setg(gbuf, gbuf + bufsize, 0);
#endif

    setg(gbuf, gbuf + bufsize, gbuf + bufsize);
    setp(pbuf, pbuf + bufsize);
            }

int TTYStream::doallocate()
{
    if(bufsize)
        return 0;

    allocate();
    return 1;
            }

void TTYStream::interactive(bool iflag)
{
#ifdef  _MSWINDOWS_
    if(dev == INVALID_HANDLE_VALUE)
#else
    if(dev < 0)
#endif
        return;

    if(bufsize >= 1)
        endStream();

    if(iflag) {
        // setting to unbuffered mode

        bufsize = 1;
        gbuf = new char[bufsize];

#if !(defined(STLPORT) || defined(__KCC))
#if defined(__GNUC__) && (__GNUC__ < 3)
        setb(0,0);
#endif
#endif
        setg(gbuf, gbuf+bufsize, gbuf+bufsize);
        setp(pbuf, pbuf);
        return;
    }

    if(bufsize < 2)
        allocate();
            }

int TTYStream::uflow(void)
{
    int rlen;
    unsigned char ch;

    if(bufsize < 2) {
        if(timeout) {
            if(Serial::isPending(pendingInput, timeout))
                rlen = aRead((char *)&ch, 1);
            else
                rlen = -1;
        }
        else
            rlen = aRead((char *)&ch, 1);
        if(rlen < 1) {
            if(rlen < 0)
                clear(ios::failbit | rdstate());
            return EOF;
        }
        return ch;
    }
    else {
        ch = underflow();
        gbump(1);
        return ch;
    }
            }

int TTYStream::underflow(void)
{
    ssize_t rlen = 1;

    if(!gptr())
        return EOF;

    if(gptr() < egptr())
        return (unsigned char)*gptr();

    rlen = (ssize_t)((gbuf + bufsize) - eback());
    if(timeout && !Serial::isPending(pendingInput, timeout))
        rlen = -1;
    else
        rlen = aRead((char *)eback(), rlen);

    if(rlen < 1) {
        if(rlen < 0) {
            clear(ios::failbit | rdstate());
            error(errInput);
        }
        return EOF;
    }

    setg(eback(), eback(), eback() + rlen);
    return (unsigned char) *gptr();
            }

int TTYStream::sync(void)
{
    if(bufsize > 1 && pbase() && ((pptr() - pbase()) > 0)) {
        overflow(0);
        waitOutput();
        setp(pbuf, pbuf + bufsize);
    }
    setg(gbuf, gbuf + bufsize, gbuf + bufsize);
    return 0;
            }

int TTYStream::overflow(int c)
{
    unsigned char ch;
    ssize_t rlen, req;

    if(bufsize < 2) {
        if(c == EOF)
            return 0;

        ch = (unsigned char)(c);
        rlen = aWrite((char *)&ch, 1);
        if(rlen < 1) {
            if(rlen < 0)
                clear(ios::failbit | rdstate());
            return EOF;
        }
        else
            return c;
    }

    if(!pbase())
        return EOF;

    req = (ssize_t)(pptr() - pbase());
    if(req) {
        rlen = aWrite((char *)pbase(), req);
        if(rlen < 1) {
            if(rlen < 0)
                clear(ios::failbit | rdstate());
            return EOF;
        }
        req -= rlen;
    }

    if(req)
//      memmove(pptr(), pptr() + rlen, req);
        memmove(pbuf, pbuf + rlen, req);
    setp(pbuf + req, pbuf + bufsize);

    if(c != EOF) {
        *pptr() = (unsigned char)c;
        pbump(1);
    }
    return c;
            }

bool TTYStream::isPending(Pending pending, timeout_t timer)
{
//  if(pending == pendingInput && in_avail())
//      return true;
//  else if(pending == pendingOutput)
//      flush();

    return Serial::isPending(pending, timer);
            }






ttystream::ttystream() :
TTYStream()
{
    setError(false);
            }

ttystream::ttystream(const char *name) :
TTYStream()
{
    setError(false);
    open(name);
            }

void ttystream::close(void)
{
#ifdef  _MSWINDOWS_
    if (INVALID_HANDLE_VALUE == dev)
#else
    if(dev < 0)
#endif
        return;

    endStream();
    restore();
    TTYStream::close();
            }

void ttystream::open(const char *name)
{
    const char *cpp;
    char *cp;
    char pathname[256];
    size_t namelen;
    long opt;

    if (INVALID_HANDLE_VALUE != dev) {
        restore();
        close();
    }

    cpp = strrchr(name, ':');
    if(cpp)
        namelen = cpp - name;
    else
        namelen = strlen(name);

    cp = pathname;

#ifndef _MSWINDOWS_
    if(*name != '/') {
        strcpy(pathname, "/dev/");
        cp += 5;
    }

    if((cp - pathname) + namelen > 255) {
        error(errResourceFailure);
        return;
    }
#endif
    setString(cp, pathname - cp + sizeof(pathname), name);
    cp += namelen;
#ifdef  _MSWINDOWS_
    *cp++ = ':';
#endif
    *cp = 0;

    Serial::open(pathname);

    if(INVALID_HANDLE_VALUE == dev) {
        error(errOpenFailed);
        return;
    }

    allocate();

    setString(pathname, sizeof(pathname), name + namelen);
    cp = pathname + 1;

    if(*pathname == ':')
        cp = strtok(cp, ",");
    else
        cp = NULL;

    while(cp) {
        switch(*cp) {
        case 'h':
        case 'H':
            setFlowControl(flowHard);
            break;
        case 's':
        case 'S':
            setFlowControl(flowSoft);
            break;
        case 'b':
        case 'B':
            setFlowControl(flowBoth);
            break;
        case 'n':
        case 'N':
            setParity(parityNone);
            break;
        case 'O':
        case 'o':
            setParity(parityOdd);
            break;
        case 'e':
        case 'E':
            setParity(parityEven);
            break;
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            opt = atol(cp);
            if(opt == 1 || opt == 2) {
                setStopBits((int)opt);
                break;
            }
            if(opt > 4 && opt < 9) {
                setCharBits((int)opt);
                break;
            }
            setSpeed(opt);
            break;
        default:
            error(errOptionInvalid);
        }
        cp = strtok(NULL, ",");
    }
            }

TTYSession::TTYSession(const char *filename, int pri, int stack) :
Thread(pri, stack), TTYStream(filename)
{
    setError(false);
            }


TTYSession::~TTYSession()
{
    terminate();
    endSerial();
            }

#ifndef _MSWINDOWS_
//  Not supporting this right now........
//

SerialPort::SerialPort(SerialService *svc, const char *name) :
Serial(name),
detect_pending(true),
detect_output(false),
detect_disconnect(true)
{
    next = prev = NULL;
    service = NULL;

#ifdef  _MSWINDOWS_
    if(INVALID_HANDLE_VALUE != dev)
#else
    if(dev > -1)
#endif
    {
        setError(false);
        service = svc;
        svc->attach(this);
    }
                }

SerialPort::~SerialPort()
{
    if(service)
        service->detach(this);

    endSerial();
                }

void SerialPort::expired(void)
{
                }

void SerialPort::pending(void)
{
                }

void SerialPort::disconnect(void)
{
                }

void SerialPort::output(void)
{
                }

void SerialPort::setTimer(timeout_t ptimer)
{
    TimerPort::setTimer(ptimer);
    service->update();
                }

void SerialPort::incTimer(timeout_t ptimer)
{
    TimerPort::incTimer(ptimer);
    service->update();
                }


void SerialPort::setDetectPending( bool val )
{
    if ( detect_pending != val ) {
        detect_pending = val;
#ifdef USE_POLL
        if ( ufd ) {
            if ( val ) {
                ufd->events |= POLLIN;
            } else {
                ufd->events &= ~POLLIN;
            }
        }
#endif
        service->update();
    }
                }

void SerialPort::setDetectOutput( bool val )
{
    if ( detect_output != val ) {
        detect_output = val;
#ifdef  USE_POLL
        if ( ufd ) {
            if ( val ) {
                ufd->events |= POLLOUT;
            } else {
                ufd->events &= ~POLLOUT;
            }
        }
#endif
        service->update();
    }
                }


SerialService::SerialService(int pri, size_t stack, const char *id) :
Thread(pri, stack), Mutex()
{
    long opt;

    first = last = NULL;
    count = 0;
    FD_ZERO(&connect);
    if(::pipe(iosync)) {
#ifdef  CCXX_EXCEPTIONS
                switch(Thread::getException()) {
                case throwObject:
                        throw(this);
                        return;
#ifdef  COMMON_STD_EXCEPTION
                case throwException:
                        throw(ThrException("no service pipe"));
                        return;
#endif
                default:
                        return;
                        }
#else
                return;
#endif
    }
    hiwater = iosync[0] + 1;
    FD_SET(iosync[0], &connect);

    opt = fcntl(iosync[0], F_GETFL);
    fcntl(iosync[0], F_SETFL, opt | O_NDELAY);
                }

SerialService::~SerialService()
{
    update(0);
    terminate();

    while(first)
        delete first;
                }

void SerialService::onUpdate(unsigned char flag)
{
                }

void SerialService::onEvent(void)
{
                }

void SerialService::onCallback(SerialPort *port)
{
                }

void SerialService::attach(SerialPort *port)
{
    enterMutex();
#ifdef  USE_POLL
    port->ufd = 0;
#endif
    if(last)
        last->next = port;

    port->prev = last;
    last = port;
    FD_SET(port->dev, &connect);
    if(port->dev >= hiwater)
        hiwater = port->dev + 1;

    if(!first) {
        first = port;
        leaveMutex();
        ++count;
        start();
    }
    else {
        leaveMutex();
        update();
        ++count;
    }
                }

void SerialService::detach(SerialPort *port)
{
    enterMutex();

#ifndef USE_POLL
    FD_CLR(port->dev, &connect);
#endif

    if(port->prev)
        port->prev->next = port->next;
    else
        first = port->next;

    if(port->next)
        port->next->prev = port->prev;
    else
        last = port->prev;

    --count;
    leaveMutex();
    update();
                }

void SerialService::update(unsigned char flag)
{
    if(::write(iosync[1], (char *)&flag, 1) < 1) {

#ifdef  CCXX_EXCEPTIONS
                switch(Thread::getException()) {
                case throwObject:
                        throw(this);
                        return;
#ifdef  COMMON_STD_EXCEPTION
                case throwException:
                        throw(ThrException("update failed"));
                        return;
#endif
                default:
                        return;
                        }
#else
                return;
#endif
    }
                }


void SerialService::run(void)
{
    timeout_t timer, expires;
    SerialPort *port;
    unsigned char buf;

#ifdef  USE_POLL

    Poller  mfd;
    pollfd *p_ufd;
    int lastcount = 0;

    // initialize ufd in all attached ports :
    // probably don't need this but it can't hurt.
    enterMutex();
    port = first;
    while(port) {
        port->ufd = 0;
        port = port->next;
    }
    leaveMutex();

#else
    struct timeval timeout, *tvp;
    fd_set inp, out, err;
    int dev;
    FD_ZERO(&inp);
    FD_ZERO(&out);
    FD_ZERO(&err);
#endif

    for(;;) {
        timer = TIMEOUT_INF;
        while(1 == ::read(iosync[0], (char *)&buf, 1)) {
            if(buf) {
                onUpdate(buf);
                continue;
            }

            Thread::exit();
        }

#ifdef  USE_POLL

        bool    reallocate = false;

        enterMutex();
        onEvent();
        port = first;
        while(port) {
            onCallback(port);
            if ( ( p_ufd = port->ufd ) ) {

                if ( ( POLLHUP | POLLNVAL ) & p_ufd->revents ) {
                    // Avoid infinite loop from disconnected sockets
                    port->detect_disconnect = false;
                    p_ufd->events &= ~POLLHUP;
                    port->disconnect();
                }

                if ( ( POLLIN | POLLPRI ) & p_ufd->revents )
                    port->pending();

                if ( POLLOUT & p_ufd->revents )
                    port->output();

            } else {
                reallocate = true;
            }

retry:
            expires = port->getTimer();
            if(expires > 0)
                if(expires < timer)
                    timer = expires;

            if(!expires) {
                port->endTimer();
                port->expired();
                goto retry;
            }

            port = port->next;
        }

        //
        // reallocate things if we saw a ServerPort without
        // ufd set !
        if ( reallocate || ( ( count + 1 ) != lastcount ) ) {
            lastcount = count + 1;
            p_ufd = mfd.getList( count + 1 );

            // Set up iosync polling
            p_ufd->fd = iosync[0];
            p_ufd->events = POLLIN | POLLHUP;
            p_ufd ++;

            port = first;
            while(port) {
                p_ufd->fd = port->dev;
                p_ufd->events =
                    ( port->detect_disconnect ? POLLHUP : 0 )
                    | ( port->detect_output ? POLLOUT : 0 )
                    | ( port->detect_pending ? POLLIN : 0 )
                ;
                port->ufd = p_ufd;
                p_ufd ++;
                port = port->next;
            }
        }
        leaveMutex();

        poll( mfd.getList(), count + 1, timer );

#else
        enterMutex();
        onEvent();
        port = first;

        while(port) {
            onCallback(port);
            dev = port->dev;
            if(FD_ISSET(dev, &err)) {
                port->detect_disconnect = false;
                port->disconnect();
            }

            if(FD_ISSET(dev, &inp))
                port->pending();

            if(FD_ISSET(dev, &out))
                port->output();

retry:
            expires = port->getTimer();
            if(expires > 0)
                if(expires < timer)
                    timer = expires;

            if(!expires) {
                port->endTimer();
                port->expired();
                goto retry;
            }

            port = port->next;
        }

        FD_ZERO(&inp);
        FD_ZERO(&out);
        FD_ZERO(&err);
        int so;
        port = first;
        while(port) {
            so = port->dev;

            if(port->detect_pending)
                FD_SET(so, &inp);

            if(port->detect_output)
                FD_SET(so, &out);

            if(port->detect_disconnect)
                FD_SET(so, &err);

            port = port->next;
        }

        leaveMutex();
        if(timer == TIMEOUT_INF)
            tvp = NULL;
        else {
            tvp = &timeout;
            timeout.tv_sec = timer / 1000;
            timeout.tv_usec = (timer % 1000) * 1000;
        }
        select(hiwater, &inp, &out, &err, tvp);
#endif
    }
                }

#endif

END_NAMESPACE

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 8
 * End:
 */
