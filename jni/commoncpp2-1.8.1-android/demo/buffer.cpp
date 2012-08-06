// Copyright (C) 2001-2005 Open Source Telecom Corporation.
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

#include <cc++/buffer.h>
#include <iostream>
#include <cstdlib>
#include <cstdio>


#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif


class MyThread : public Buffer, public Thread
{
private:
	short chars[8];
	unsigned head, tail;

	void run(void);
	size_t onWait(void *buf);
	size_t onPost(void *buf);
	size_t onPeek(void *buf);

public:
	MyThread();
	~MyThread();
};


MyThread::MyThread() :
Buffer(8), Thread()
{
	head = tail = 0;
}

MyThread::~MyThread()
{
	terminate();
}

size_t MyThread::onWait(void *buf)
{
	char *bp = (char *)buf;

	*bp = (char)chars[head++];
	if(head >= 8)
		head = 0;
	return 1;
}

size_t MyThread::onPost(void *buf)
{
	char *bp = (char *)buf;
	chars[tail++] = *bp;
	if(tail >= 8)
		tail = 0;
	return 1;
}

size_t MyThread::onPeek(void *buf)
{
	char *bp = (char *)buf;
	if(head == tail)
		return 0;

	*bp = (char)chars[head];
	return 1;
}

void MyThread::run(void)
{
	int rc;
	char cbuf;
	for(;;) {
		rc = wait(&cbuf, 5000);
		if(rc < 0)
			cout << "timeout" << endl;
		else {
			cout << "char " << cbuf << endl;
			Thread::sleep(50);
		}
	}
}

extern "C" int main(int argc, char **argv)
{
	char buffer[32];
	const char *cp;
	MyThread *thr = new MyThread();
	thr->start();

	for(;;) {
		cin.getline(buffer, sizeof(buffer));
		if(!strnicmp(buffer, "bye", 3))
			break;
		cp = buffer;
		while(*cp)
			thr->post((void *)(cp++), 0);
		cout << "post complete!" << endl;
	}
	delete thr;
	return 0;
}

