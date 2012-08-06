// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2008 David Sugar, Tycho Softworks.
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
// As a special exception to the GNU General Public License, permission is
// granted for additional uses of the text contained in its release
// of APE.
//
// The exception is that, if you link the APE library with other files
// to produce an executable, this does not by itself cause the
// resulting executable to be covered by the GNU General Public License.
// Your use of that executable is in no way restricted on account of
// linking the APE library code into it.
//
// This exception does not however invalidate any other reasons why
// the executable file might be covered by the GNU General Public License.
//
// This exception applies only to the code released under the
// name APE.  If you copy code from other releases into a copy of
// APE, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for APE, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.

#include <cc++/socket.h>
#include <cstdlib>

#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif

class myTCPSocket : public TCPSocket
{
protected:
	bool onAccept(const InetHostAddress &ia, tpport_t port);

public:
	myTCPSocket(InetAddress &ia);
};

class myTCPSession : public TCPSession
{
private:
	static Mutex mutex;
	void run(void);
	void final(void);

public:
	myTCPSession(TCPSocket &server);
	static volatile int count;
};

myTCPSocket::myTCPSocket(InetAddress &ia) : TCPSocket(ia, 4096) {}

bool myTCPSocket::onAccept(const InetHostAddress &ia, tpport_t port)
{
	cout << "accepting from: " << ia.getHostname() << ":" << port << endl;;
	return true;
}

volatile int myTCPSession::count = 0;

Mutex myTCPSession::mutex;

myTCPSession::myTCPSession(TCPSocket &server) :
TCPSession(server)
{
	cout << "creating session client object" << endl;
	mutex.enterMutex();
	++count;
	mutex.leaveMutex();
	// unsetf(ios::binary);
}

void myTCPSession::run(void)
{
	tpport_t port;
	IPV4Address addr = getLocal(&port);
	*tcp() << "welcome to " << addr.getHostname() << " from socket " << (int)so << endl;
	mutex.enterMutex();
	*tcp() << "called from thread " << count << endl;
	mutex.leaveMutex();
	sleep(5000);
	*tcp() << "ending session" << endl;
}

void myTCPSession::final(void)
{
}

int main()
{
	myTCPSession *tcp;
	BroadcastAddress addr;
	addr = "255.255.255.255";
	cout << "testing addr: " << addr.getHostname() << ":" << 4096 << endl;
	addr = "127.0.0.1";
	cout << "binding for: " << addr.getHostname() << ":" << 4096 << endl;

	try {
		myTCPSocket server(addr);

		while(server.isPendingConnection(30000)) {
			cout << "before create" << endl;
			tcp = new myTCPSession(server);
			cout << "after create" << endl;
			tcp->detach();
		}
	}
	catch(Socket *socket) {
		tpport_t port;
		int err = socket->getErrorNumber();
		InetAddress saddr = (InetAddress)socket->getPeer(&port);
		cerr << "socket error " << saddr.getHostname() << ":" << port << " = " << err << endl;
		if(err == Socket::errBindingFailed) {
			cerr << "bind failed; port busy" << endl;
			::exit(-1);
		}
		else
			cerr << "client socket failed" << endl;
	}
	cout << "timeout after 30 seconds inactivity, exiting" << endl;
	return 0;
}

