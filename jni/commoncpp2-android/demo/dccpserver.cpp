// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2009 Leandro Melo de Sales <leandroal@gmail.com>
//
// This example demonstrates the operation of a server DCCP,
// where awaiting connections locally, when a client connects,
// the server creates a client to make operations for reading and writing data
// in socket.
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
// of Common C++.
//
// The exception is that, if you link the Common C++ library with other
// files to produce an executable, this does not by itself cause the
// resulting executable to be covered by the GNU General Public License.
// Your use of that executable is in no way restricted on account of
// linking the Common C++ library code into it.
//
// This exception does not however invalidate any other reasons why
// the executable file might be covered by the GNU General Public License.
//
// This exception applies only to the code released under the
// name Common C++.  If you copy code from other releases into a copy of
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.

#include <cc++/socket.h>
#include <iostream>
#include <cstdlib>

#ifdef CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif

class MyDCCPSocket : public DCCPSocket
{

public:
    MyDCCPSocket(IPV4Host &ia, tpport_t port);
    ssize_t myReadLine(char* buf);
    ssize_t myWriteData(const char *buf);
    void myBufferSize(unsigned size);
};

void MyDCCPSocket::myBufferSize(unsigned size){
	this->bufferSize(size);
}

MyDCCPSocket::MyDCCPSocket(IPV4Host &ia, tpport_t port) : DCCPSocket(ia, port) {}

//This method read data on socket
ssize_t MyDCCPSocket::myReadLine(char* buf){
    return this->readLine(buf, 200);
}

//This method writes data on socket
ssize_t MyDCCPSocket::myWriteData(const char *buf){
	return this-> writeData(buf,strlen(buf));
}

int main(int argc, char *argv[])
{
    //address of local interface
    tpport_t port = 7000;
    IPV4Host addr = "127.0.0.1";

    //Startup of server DCCP
    MyDCCPSocket server(addr,port);

    //message to be written to the socket
    char mss[] = "Hello Client\n";

    char buffer[200];
    cout << "Server wait connections in " << addr << ":" << port << endl;

    //loop of waiting for connections
    while(server.isPendingConnection(30000)) {
        //accept the connection to the client
        if (server.onAccept(addr,port)) {
            //Is created to a client operation of reading and writing data in socket
            MyDCCPSocket client(server);

            //reading data from socket
            client.myReadLine(buffer);

            client.myBufferSize(15);
            //printing the data read
            cout << buffer << endl;

            ///call the method of writing data
            client.myWriteData(mss);

        }
    	else cout << "Unable to accept the connection to the remote client" << endl;
    }
    cout << "timeout after 30 seconds inactivity, exiting" << endl;
    return 0;
}
