//
// tcpservice.cpp
//
//  Copyright 2000 - Gianni Mariani <gianni@mariani.ws>
//
//  An example of a simple chatty server using CommonC++.
//
//  This simple application basically operates as a
//  very simple chat system. From a telnet session
//  on localhost:3999 , any messages typed from a telnet
//  client are written to all participating sessions.
//
//  This is free software licensed under the terms of the GNU
//  Public License
//
//  This example:
//
//  This demostrates a simple threaded server, actually,
//  the sessions are not all threaded though they could be
//  if that's what you wanted.	Basically it demonstrates the
//  use of SocketService, SocketPorts and Threads.
//
//  For those familiar with Unix network programming, SocketService
//  basically encapsulates all the work to communicate with
//  the select() or poll() system calls.  SocketPorts are
//  basically encapsulations of sessions or open file descriptors.
//
//  Anyhow, this example is a very simple echo server but
//  it echos to all connected clients.	So it's a poor man's
//  IRC !  You connect via telnet to localhost port 3999 and
//  it will echo to all other connected clients what you type in !
//

#include <cc++/socketport.h>

#include <iostream>

// For starters, we need a thread safe list, we'll make one
// out of the STL list<> template -
//  http://www.sgi.com/Technology/STL/index.html
//
// Thread safe list class
//
#include <list>

#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif

class ts_list_item;
typedef list<ts_list_item *> ts_list;

// a list head - containing a list and a Mutex.
// It would be really nice to teach stl to do this.

class ts_list_head {
public:

	// No point inheriting, I'd have to implement
	// alot of code. We'll hold off on that exercise.

	// Using the CommonC++ Mutex class.
	Mutex		    linkmutex;
	// And the STL template.
	ts_list		    list_o_items;

	// Not nessasary, but nice to be explicit.
	ts_list_head()
	: linkmutex(), list_o_items() {
	}

	// This thing knows how to remove and insert items.
	void RemoveListItem( ts_list_item * li );
	void InsertListItem( ts_list_item * li );

	// And it knows how to notify that it became empty
	// or an element was deleted and it was the last one.
	virtual void ListDepleted() {
	}

	virtual ~ts_list_head() {
	}
};


// This item knows how to remove itself from the
// list it belongs to.
class ts_list_item {
public:
	ts_list::iterator	   linkpoint;
	ts_list_head	  * listhead;

	virtual ~ts_list_item() {
	listhead->RemoveListItem( this );
	}

	ts_list_item( ts_list_head * head ) {
	listhead = head;
	head->InsertListItem( this );
	}
};

void ts_list_head::RemoveListItem( ts_list_item * li )
{
	bool    is_empty;
	linkmutex.enterMutex();
	list_o_items.erase( li->linkpoint );
	is_empty = list_o_items.empty();
	linkmutex.leaveMutex();

	// There is a slim possibility that at this time
	// we recieve a connection.
	if ( is_empty ) {
	ListDepleted();
	}
}

void ts_list_head::InsertListItem( ts_list_item * li )
{
	linkmutex.enterMutex();
	list_o_items.push_front( li );
	li->linkpoint = list_o_items.begin();
	linkmutex.leaveMutex();
}

// ChatterSession operates on the individual connections
// from clients as are managed by the SocketService
// contained in CCExec.  ChatterThread simply waits in
// a loop to create these, listening forever.
//
// Even though the SocketService contains a list of
// clients it serves, it may actually serve more than
// one type of session so we create our own list by
// inheriting the ts_list_item.
//

class ChatterSession :
	public virtual SocketPort,
	public virtual ts_list_item {
public:

	enum { size_o_buf = 2048 };

	// Nothing special to do here, it's all handled
	// by SocketPort and ts_list_item

	virtual ~ChatterSession() {
	cerr << "ChatterSession deleted !\n";
	}

	// When you create a ChatterSession it waits to accept a
	// connection.  This is done by it's own
	ChatterSession(
	TCPSocket      & server,
	SocketService	* svc,
	ts_list_head	* head
	) :
	SocketPort( NULL, server ),
	ts_list_item( head ) {
	cerr << "ChatterSession Created\n";

	tpport_t port;
	InetHostAddress ia = getPeer( & port );

	cerr << "connecting from " << ia.getHostname() <<
	":" << port << endl;

	// Set up non-blocking reads
	setCompletion( false );

	// Set yerself to time out in 10 seconds
	setTimer( 100000 );
	attach(svc);
	}

	//
	// This is called by the SocketService thread when it the
	// object has expired.
	//

	virtual void expired() {
	// Get outa here - this guy is a LOOSER - type or terminate
	cerr << "ChatterSession Expired\n";
	delete this;
	}

	//
	// This is called by the SocketService thread when it detects
	// that there is somthing to read on this connection.
	//

	virtual void pending() {
	// Implement the echo

	cerr << "Pending called\n";

	// reset the timer
	setTimer( 100000 );
	try {
	    int    len;
	    unsigned int total = 0;
	    char    buf[ size_o_buf ];

	    while ( (len = receive(buf, sizeof(buf) )) > 0 ) {
		total += len;
		cerr << "Read '";
		cerr.write( buf, len );
		cerr << "'\n";

		// Send it to all the sessions.
		// We probably don't really want to lock the
		// entire list for the entire time.
		// The best way to do this would be to place the
		// message somewhere and use the service function.
		// But what are examples for ?

		bool sent = false;
		listhead->linkmutex.enterMutex();
		for (
		   ts_list::iterator iter = listhead->list_o_items.begin();
		   iter != listhead->list_o_items.end();
		   iter ++
		) {
		   ChatterSession * sess =
		    dynamic_cast< ChatterSession * >( * iter );
		   if ( sess != this ) {
		    sess->send( buf, len );
		    sent = true;
		   }
		}
		listhead->linkmutex.leaveMutex();

		if ( ! sent ) {
		   send(
		    ( void * ) "No one else listening\n",
		    sizeof( "No one else listening\n" ) - 1
		   );

		   send( buf, len );
		}
	    }
	    if (total == 0) {
		cerr << "Broken connection!\n" << endl;
		delete this;
	    }
	}
	catch ( ... ) {
	    // somthing wrong happened here !
	    cerr << "Socket port write sent an exception !\n";
	}

	}

	virtual void disconnect() {
	// Called by the SocketService thread when the client
	// hangs up.
	cerr << "ChatterSession disconnected!\n";

	delete this;
	}

};

class ChatterThread;

//
// This is the main application object containing all the
// state for the application.  It uses a SocketService object
// (and thread) to do all the work, however, that object could
// theoretically be use by more than one main application.
//
// It creates a ChatterThread to sit and wait for connections
// from clients.

class CCExec : public virtual ts_list_head {
public:

	SocketService	    * service;
	ChatterThread	       * my_Chatter;
	Semaphore		      mainsem[1];

	CCExec():my_Chatter(NULL) {
	service = new SocketService( 0 );
	}

	virtual void ListDepleted();

	// These methods defined later.
	virtual ~CCExec();
	int RunApp( char * hn = (char *)"localhost" );

};

//
// ChatterThread simply creates ChatterSession all the time until
// it has an error.  I suspect you could create as many of these
// as the OS could take.
//

class ChatterThread : public virtual TCPSocket, public virtual Thread {
public:

	CCExec		* exec;

	void run () {
	while ( 1 ) {
	    try {
		// new does all the work to accept a new connection
		// attach itself to the SocketService AND include
		// itself in the CCExec list of sessions.
		new ChatterSession(
		   * ( TCPSocket * ) this,
		   exec->service,
		   exec
		);
	    }
	    catch ( ... ) {
		// Bummer - there was an error.
		cerr << "ChatterSession create failed\n";
		exit();
	    }
	}
	}

	ChatterThread(
	InetHostAddress & machine,
	int	      port,
	CCExec	       * inexec

	) : TCPSocket( machine, port ),
	Thread(),
	exec( inexec ) {
	    start();
	}


};

//
// Bug here, this should go ahead and shut down all sessions
// for application.  An exercise left to the reader.

CCExec::~CCExec()
{
	// MUST delete my_Chatter first or it may end up using
	// a deleted service.
	if ( my_Chatter ) delete my_Chatter;

	// Delete whatever is left.
	delete service;
}

//
// Run App would normally read some config file or take some
// parameters about which port to connect to and then
// do that !
int CCExec::RunApp( char * hn )
{
	// which port ?

	InetHostAddress machine( hn );

	if ( machine.isInetAddress() == false ) {
	cerr << "machine is not address" << endl;
	}

	cerr << "machine is " << machine.getHostname() << endl;

	// Start accepting connections - this will bind to the
	// port as well.
	try {
	my_Chatter = new ChatterThread(
	    machine,
	    3999,
	    this
	);
	}
	catch ( ... ) {
	cerr << "Failed to bind\n";
	return false;
	}

	return true;
}

// When there is no one else connected - terminate !
void CCExec::ListDepleted()
{
	mainsem->post();
}


int main( int argc, char ** argv )
{
	CCExec	* server;

	server = new CCExec();

	// take the first command line option as a hostname
	// to listen to.
	if ( argc > 1 ) {
		server->RunApp( argv[ 1 ] );
	} else {
		server->RunApp();
	}

	server->mainsem->wait();

	delete server;

	return 0;
}
