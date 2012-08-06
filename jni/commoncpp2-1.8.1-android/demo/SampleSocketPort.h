/**
 *
 *	This class demonstrates use of the CommonC++ SocketPort class.
 *
 *	Copyright 2001 - Nick Liebmann <nick@ukmail.org>
 *
 *	This sample code is distributed under the same terms and conditions of the CommonC++ library.
 */

#include <cc++/socketport.h>

#ifdef	CCXX_NAMESPACES
using namespace ost;
using namespace std;
#endif

#define MAX_RXBUF		32768		/**< Specifies the maximum number of bytes in a 'packet' */
#define MAX_RXTIMEOUT	10000		/**< Specifies how long we will wait for a complete packet */
#define	DISCONNECT_MS	500			/**< Specifies the timeout for the diconnect timer */

class SampleSocketPort : public SocketPort
{
public:
	SampleSocketPort(SocketService *pService, TCPSocket & tcpSocket);
	virtual ~SampleSocketPort();

	/**
	 *	Overridden from class SocketPort.
	 *	Called when data is available in the receive buffer.
	 */
	virtual void pending();

	/**
	 *	Overridden from class SocketPort.
	 *	Called when the socket has been disconnected from the client-side.
	 *	Under some conditions this function is NOT called, which is why we have
	 *	some additional disconnection functionality within pending().
	 */
	virtual void disconnect(void);

	/**
	 *	Overridden from class SocketPort.
	 *	This function is called by the system when our timer expires.
	 *	We use the timer for 2 things:
	 *	1) To determine whether reception has timed out. (Timer started in pending())
	 *	2) To call CloseInterface to safely destroy the port.
	 */
	virtual void expired(void);


	/**
	 *	This function will send the specified number of bytes, or the whole string
	 *	(without the terminating '\0')
	 */
	bool WriteData(const char *szTxData, const size_t nByteCount = -1);

	/**
	 *	Our function to provide uniform closure of the Socket.
	 *	Can be called from the outside!
	 */
	bool CloseSocket(void);

	/**
	 *	This function should be called from pending() when the first bytes of our
	 *	data has been received. If the complete data has not been received by the time
	 *	this expires we consider this an error.
	 *
	 */
	void ResetReadTimeout(timeout_t timeout) {
		m_bTimedOut = false;
		setTimer(timeout);
	}

	/**
	 *	This function should be use in the event of a reception error, to flush out
	 *	the receive buffer.
	 */
	void FlushRxData(void) {
		while(receive(m_pBuf, MAX_RXBUF) > 0);
		cerr << "FLUSHED" << endl;
	}


	/*
	 *	Some virtual function placeholders.....
	 */

	/**
	 *	This function is called just before the port is closed.
	 *	Do not send any data from this function!
	 */
	virtual void OnConnectionClosed(void) {
		cerr << "Connection Closed!" << endl;
	}
	/**
	 *	Called when the receive timeout occurs
	 */
	virtual void OnRxTimeout(void) {
		cerr << "Receive timeout occurred" << endl;
		FlushRxData();
	}
	/**
	 *	Called when a 'packet' of data has been received.
	 */
	virtual void OnDataReceived(char *pszData, unsigned int nByteCount) {
	}
protected:
	bool m_bOpen;				/**< Flag set to true while Socket is open */
	bool m_bDoDisconnect;		/**< Flag set to true when disconnection event has occurred */
	bool m_bTimedOut;			/**< Flag set to true when reception has timed out */
	bool m_bReceptionStarted;	/**< Flag set to true when the first bytes of a transmission have arrived */
	int m_nLastBytesAvail;		/**< Count of last number of bytes received in pending() */
	char *m_pBuf;				/**< Buffer used to store received data for parsing */

	/**
	 *	Little utility function for sending data to the client.
	 *	@return Number of bytes sent to client
	 */
	ssize_t DoSend(void *buf, size_t len);
};


/*
 *	This class implements a Thread that manages a SocketService. Simply
 *	create an instance of this class with the specified address and port, and
 *	signal the semaphore when you want it to start.
 *
 *	A new SampleSocketPort object will be created for every connection that arrives.
 *
 */

class SampleSocketServiceServer :  public virtual TCPSocket, public virtual Thread
{
public:
	SampleSocketServiceServer(InetHostAddress & machine, int port) :
					TCPSocket(machine, port), Thread(), m_bQuitServer(true) {
		m_pSocketService = new SocketService(0);

		//IMPORTANT SOCKET SERVICE MUST NOW BE EXPLICITLY STARTED
		m_pSocketService->start();
	}

	virtual ~SampleSocketServiceServer()
	{ terminate(); delete m_pSocketService; }

	virtual void run(void) {
   		waitMutex.enterMutex();
   		m_bQuitServer = false;
		while(!m_bQuitServer) {
			try {
				// new does all the work to accept a new connection
				// and attach itself to the SocketService.
				CreateSocketPort(m_pSocketService, *((TCPSocket *)this));
			}
			catch ( ... ) {
				// Bummer - there was an error.
				cerr << "SampleSocketPort create failed\n";
				exit();
			}
		}
		waitMutex.leaveMutex();
	}
	/**
	 *	This abstract function is used to create a SocketPort of the desired type.
	 */
	virtual SocketPort *CreateSocketPort(SocketService *pService, TCPSocket & Socket) = 0;
	virtual void StartServer() {
		m_bQuitServer = true;
		start();
		while(m_bQuitServer) {
			Thread::yield();
		}
	}

	/**
	 *	If the server is not stopped like this then the SocketPort created in CreateSocketPort
	 *	is leaked. This allows it to complete construction, and be deleted cleanly.
	 */
	virtual void StopServer() {
		m_bQuitServer = true;

		InetHostAddress host;
		tpport_t port;
		host = getLocal(&port);

		//This is required so that CreateSocketPort can return.
		TCPStream strm(host, port);

		waitMutex.enterMutex();
		waitMutex.leaveMutex();
	}
protected:
	SocketService *m_pSocketService;
	bool m_bQuitServer;
	Mutex waitMutex;
private:
};

