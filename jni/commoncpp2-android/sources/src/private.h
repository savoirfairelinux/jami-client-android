#ifndef	CCXX_PRIVATE_H_
#define	CCXX_PRIVATE_H_

#ifdef CCXX_NAMESPACES
namespace ost {
#endif

class ThreadImpl
{
friend class Thread;
friend class DummyThread;
friend class PosixThread;
friend class Slog;

	ThreadImpl(int type):
		_msgpos(0),
		_throw(Thread::throwObject),
		_tid(0),
		_suspendEnable(true),
		_type(type),
#ifndef WIN32
		_jtid(0)
#else
		_detached(false),
		_active(false),
		_hThread(NULL),
		_cancellation(NULL)
#endif
	{ ; };

	// derived class copy constructor creates new instance, so base
	// copy constructor of ThreadImpl should do nothing...

	ThreadImpl(const ThreadImpl& copy)
		{;};

	ThreadImpl &operator=(const ThreadImpl& copy)
		{return *this;};

#ifdef	_THR_MACH
	mach_port_t	_mach;
#endif

#ifndef WIN32
	pthread_attr_t _attr;
	AtomicCounter _suspendcount;
	static ThreadKey _self;
#else
	size_t _stack;
	int _priority;
	HANDLE _cancellation;
#endif
	// log information
	size_t _msgpos;
	char _msgbuf[128];
	Thread::Throw _throw;
	cctid_t _tid;

#ifndef WIN32
	friend Thread *getThread(void);
	volatile bool _suspendEnable:1;
	unsigned int _type:3;
	cctid_t _jtid;
#else
	bool _detached:1;
	bool _active:1;
	bool _suspendEnable:1;
	unsigned int _type:3;
	static unsigned __stdcall Execute(Thread *th);
	HANDLE	_hThread;
#endif

public:
	// C binding functions
	static inline void ThreadExecHandler(Thread* th);
#ifndef WIN32
	static inline RETSIGTYPE ThreadSigSuspend(int);
	static inline void ThreadCleanup(Thread* arg);
	static inline void ThreadDestructor(Thread* arg);
	static inline void PosixThreadSigHandler(int signo);
#endif
};

#ifdef CCXX_NAMESPACES
}
#endif

#endif
