/**
 *
 *  This class demonstrates use of the CommonC++ SocketPort class.
 *
 *  Copyright 2001 - Nick Liebmann <nick@ukmail.org>
 *
 *  The SampleSocketPort is an implementation of the CommonC++ SocketPort class
 *  that irons out some problems that I found with disconnection, and also demonstrates
 *  a way of using the SocketPort to reliably extract and send data from/to a TCP/IP socket.
 *
 *  In addition to this the SampleSocketPort includes some additional functionality
 *  to determine whether the data stream has become corrupted
 *  (missing terminator / incorrect formatting). For this feature a timer is used which if it is
 *  allowed to expire, indicates that a 'packet' took too long to arrive, and as such the data
 *  in the buffer is 'corrupt'.
 *
 *  The SampleSocketPort can be used as-is...Modify the contents of the pending()
 *  function if your data is formatted differently to the default (i.e. not terminated with \r\n)
 *
 *
 *  This sample code is distributed under the same terms and conditions of the CommonC++ library.
 *
 *  CHANGE HISTORY:
 *
 *
 *  07/01/02    NL      There have been slight changes to the way CommonC++ starts threads,
 *                      a possible bug in InetHostAddress constructor, and a bug fix for SocketService
 *                      #496276. The following changes address those issues
 *
 *                      New thread start semantics. SocketService Thread is now explicitly started
 *                      by the SampleSocketServiceServer.
 *
 *                      Added SampleSocketServiceServer::StartServer() to start the server and wait
 *                      for the thread to get up and running.
 *
 *                      Added SampleSocketServiceServer::StopServer() to cleanly stop the server, and
 *                      ensure that there are no partially constructed SocketPorts left lying around
 *
 *                      Removed setDetectOutput(true)...this does not seem to be required anymore
 *                      as the SocketService functions correctly now.
 *
 *                      InetHostAddress constructor does not treat INADDR_ANY as it used to.
 *
 *  07/01/02    NL      main() - now waits for a 'quit' command, and deletes the Server object.
 */


#include "SampleSocketPort.h"

SampleSocketPort::SampleSocketPort(SocketService *pService, TCPSocket & tcpSocket) :
SocketPort(pService, tcpSocket)
{
    tpport_t port;
    InetHostAddress ia = getPeer( & port );
    cerr << "connecting from " << ia.getHostname() << ":" << port << endl;

    // Set up non-blocking reads
    setCompletion( false );

    //1.9.3 THIS LINE DOES NOT SEEM TO BE REQUIRED ANYMORE!
    //This sorts out a bug which prevents connections after a disconnect
    //setDetectOutput(true);

    m_bOpen = true;
    m_bDoDisconnect = false;
    m_bTimedOut = false;
    m_bReceptionStarted = false;
    m_nLastBytesAvail = 0;
    m_pBuf = new char[MAX_RXBUF];
}


SampleSocketPort::~SampleSocketPort()
{
    endSocket();
    delete [] m_pBuf;
}

void SampleSocketPort::pending(void)
{
//cerr << "Pending called " << endl;
    if(!m_bOpen)
        return;

    // Read all available bytes into our buffer
    int nBytesAvail = peek(m_pBuf, MAX_RXBUF);
//cerr << "Pending .. " << nBytesAvail << endl;

    if(!m_bReceptionStarted)
    {   //Start the receive timer
        ResetReadTimeout(MAX_RXTIMEOUT);    //Got 'n' seconds to get all the data else we timeout
        m_bReceptionStarted = true;
    }
    else {
        if(m_bTimedOut) //The receive timer has expired...this is a timeout condition
        {
            ResetReadTimeout(MAX_RXTIMEOUT); //Clear the timeout flag
            m_nLastBytesAvail = 0;      //Reset the flags
            m_bReceptionStarted = false;
            OnRxTimeout();  //Do whatever 'we' do for a timeout (probably a flush or disconnect)...
            return;
        }
    }

    if(m_nLastBytesAvail == nBytesAvail)    //Check if any more data has been received since last time
    {                                       //No point in parsing unless this has changed!
        //Maybe yield in here!
        //Thread::yield();
        if(nBytesAvail == 0)        //If we have been called with 0 bytes available (twice now)
        {                           //a disconnection has occurred
            if(!m_bDoDisconnect) {
                CloseSocket();  //Force the close
            }
        }
        return;
    }

    //Depending on your application you may want to attempt to process the extra data
    //(or change your MAX_RXBUF).
    //
    //Here I just flush the whole lot, because I assume a 'legal' client wont send more than
    //we can receive....maybe someone is trying to flood / overrun us!
    if(nBytesAvail > MAX_RXBUF) {
        cerr << "TCP/IP overflow..." << endl;
        FlushRxData();
        m_nLastBytesAvail = 0;
        m_bReceptionStarted = false;
        return;
    }
    m_nLastBytesAvail = nBytesAvail;

    //In this loop you may parse the received data to determine whether a whole
    //'packet' has arrived. What you do in here depends on what data you are sending.
    //Here we will just look for a /r/n terminator sequence.
    for(int i=0; i < nBytesAvail; i++) {

/***************************SHOULD BE CUSTOMISED*******************/

        if(m_pBuf[i] == '\r') {
            if(i+1 < nBytesAvail) {
                if(m_pBuf[i+1] == '\n')
                {   //Terminator sequence found

                    /**************************************************************/
                    // COMPULSORY ... Clear the flag and count..
                    // do this when you have received a good packet
                    m_nLastBytesAvail = 0;
                    m_bReceptionStarted = false;
                    /**************************************************************/

                    // Now receive the data into a buffer and call our receive function
                    int nLen = i+2;
                    char *pszRxData = new char[nLen+1]; //Allow space for terminator
                    receive(pszRxData, nLen);       //Receive the data
                    pszRxData[nLen] = '\0';     //Terminate it
                    OnDataReceived(pszRxData, nLen);
                    delete [] pszRxData;
                    return;
                }
            }
        }
/***************************END CUSTOMISATION*******************/

    }
}

void SampleSocketPort::disconnect(void)
{
    if(m_bOpen) {
        m_bDoDisconnect = true;
        CloseSocket();
    }
}

void SampleSocketPort::expired(void)
{
    if(m_bDoDisconnect && m_bOpen) {
        CloseSocket();
    }
    else if(m_bOpen && m_bReceptionStarted) {
        //Timer must have expired because the rx data has not all been received
        m_bTimedOut = true;
    }
}


bool SampleSocketPort::CloseSocket(void)
{
    if(m_bOpen && m_bDoDisconnect)
    {                                   //This is where the disconnection really occurs
        m_bOpen = false;                //If m_bDoDisconnect == true we know this has been called
        OnConnectionClosed();           //through the timer, so 'delete this' is safe!
        delete this;
    }
    else if(m_bOpen) {
        m_bDoDisconnect = true;         //Just set the timer and the flag so we can
        setTimer(DISCONNECT_MS);        //disconnect safely, in DISCONNECT_MS
    }
    return(true);
}

ssize_t SampleSocketPort::DoSend(void *buf, size_t len)
{
    //If we are disconnecting, just pretend all the bytes were sent
    if(m_bDoDisconnect)
        return((ssize_t)len);

    ssize_t nSent = send(buf, len);
    while(!isPending(Socket::pendingOutput, 0)) //Wait for output to complete
    {
        if(m_bDoDisconnect || !m_bOpen) {
            //If we are disconnecting, just pretend all the bytes were sent
            return((ssize_t)len);
        }
        //I like to yield whenever waiting for things...
        //this is optional and may not suit your implementation!
        Thread::yield();
    }
    return(nSent);
}

bool SampleSocketPort::WriteData(const char *szTxData, const size_t nByteCount)
{
    //First calculate how many bytes we are to send
    ssize_t nLen = nByteCount;

    if(nLen == -1)
        nLen = (ssize_t)strlen(szTxData);

    size_t nBytesToSend = nLen;

    while(m_bOpen && nLen) {
        nLen -= DoSend((void *)&(szTxData[nBytesToSend - nLen]), nLen);
    }

//  If we are sending a terminator.....uncomment the following lines
//  char chTerminator = '\n';
//  while(DoSend((void *)&chTerminator, 1) != 1);

    return(true);
}

#define WITH_EXAMPLE

#ifdef WITH_EXAMPLE


/************ THE FOLLOWING CODE DEMONSTRATES THE USE OF THE ABOVE CLASS ********************
 ****
 ****   To test it, compile with:
 ****
 ****   g++ SampleSocketPort.cpp -lccgnu -lpthread -ldl -oSampleSocketPort -ggdb -I/usr/local/include/cc++/
 ****   Run the program.
 ****
 ****   From another terminal telnet to port 3999 of the server
 ****
 ****       'telnet localhost 3999'
 ****
 ****   Anything you type should be sent back to you in reverse!
 ****
 ****   To test the corrupt data detection, send a control code (like ^D),
 ****   if the terminating charcters are not detected within the specified time
 ****   the receive timeout will occur.
 ****
 ****/


//define the following to include the example classes and functions

int g_nOpenPorts = 0;           //Dirty global to allow us to quit simply

class ReverserPort : public SampleSocketPort
{
public:
    ReverserPort(SocketService *pService, TCPSocket & tcpSocket) :
            SampleSocketPort(pService, tcpSocket) {
        g_nOpenPorts++;
    }
    virtual ~ReverserPort() {
        g_nOpenPorts--;
    }
    virtual void OnConnectionClosed(void)
    { cerr << "Connection Closed!" << endl; }

    /**
     *  Called when a 'packet' of data has been received.
     *  This implementation simply reverses all the data and sends it back
     */
    virtual void OnDataReceived(char *pszData, unsigned int nByteCount) {
        //Reverse the data and send it back

        size_t nLen = strlen(pszData);
        char *szToSend = new char[nLen+1];

        //No need to reverse the \r\n or \0
        size_t nIndex = nLen-3;

        size_t i;
        for(i=0; i < nLen - 2; i++) {
            szToSend[i] = pszData[nIndex - i];
        }
        szToSend[i++] = '\r';
        szToSend[i++] = '\n';
        szToSend[nLen] = '\0';

        WriteData(szToSend, nLen);
        delete [] szToSend;
    }

};

class ReverserServer : public SampleSocketServiceServer
{
public:
    ReverserServer(InetHostAddress & machine, int port) :
    TCPSocket(machine, port), Thread(), SampleSocketServiceServer(machine, port) {}

    virtual ~ReverserServer() {}

    virtual SocketPort *CreateSocketPort(SocketService *pService, TCPSocket & Socket) {
        return(new ReverserPort(pService, Socket));
    }
};


int main(void)
{
    InetHostAddress LocalHost;
    LocalHost = htonl(INADDR_ANY);
    ReverserServer *Server = NULL;
    try {
        Server = new ReverserServer(LocalHost, 3999);
        Server->StartServer();
    }
    catch(...) {
        cerr << "Failed to start server" << endl;
        return(false);
    }
    cerr << "Waiting for connections...type \"quit\" to exit." << endl;

    char cmd[255];

    cin.getline(cmd, 255);


    while(strcmp(cmd, "quit") != 0) {
        cin.getline(cmd, 255);
    }

    Server->StopServer();
    delete Server;
    return 0;
}

#endif  //WITH_EXAMPLE

