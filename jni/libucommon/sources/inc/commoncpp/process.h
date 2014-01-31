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
 * @file commoncpp/process.h
 * @short Process services.
 **/

#ifndef COMMONCPP_PROCESS_H_
#define COMMONCPP_PROCESS_H_

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

#ifndef COMMONCPP_THREAD_H_
#include <commoncpp/thread.h>
#endif

NAMESPACE_COMMONCPP

/**
 * A class for containing portable process related functions
 * that help create portable code.  These are typically
 * referenced thru Process::xxx static member functions.
 * Many of these members are used both for win32 and posix
 * systems although some may be platform specific.
 *
 * @short Peocess wrapper class.
 * @author David Sugar <dyfet@ostel.com>
 */
class __EXPORT Process
{
private:
    static bool rtflag;

public:
#ifndef _MSWINDOWS_
    typedef void (*Trap)(int);

    /**
     * Detach current process into a daemon, posix
     * only.  Perhaps a similar method can be used
     * for creating win32 "services"?
     */
    static void detach(void);

    /**
     * Attach the current process to another device
     * or i/o session.  It is deamonified and dissasociated
     * with the prior parent process and controlling terminal.
     *
     * @param devname path to attach to.
     */
    static void attach(const char *devname);

    /**
     * Set a posix compliant signal handler.
     *
     * @return previous handler.
     * @param signo signal no.
     * @param handler trap handler.
     */
    static Trap setPosixSignal(int signo, Trap handler);

    /**
     * Set system call interuptable signal handler.
     *
     * #return previous handler.
     * @param signo   signal no.
     * @param handler trap handler.
     */
    static Trap setInterruptSignal(int signo, Trap handler);
#endif
    /**
     * Lock a process in memory.  Ideally you should be deep enough
     * where additional memallocs for functions will not kill you,
     * or use false for future.
     *
     * @return true if successful.
     * @param future pages as well...
     */
    bool lock(bool future = true);

    /**
     * Unlock process pages.
     */
    void unlock(void);

    /**
     * Spawn a process and wait for it's exit code.  In win32
     * this is done with the spawn system call.  In posix,
     * this is done with a fork, an execvp, and a waitpid.
     *
     * @warning The implementation differences between posix and
     * win32 systems may cause side effects. For instance, if you
     * use atexit() and this spawn method, on posix systems the
     * function set up with atexit() will be called when the
     * parent process of the fork exits, which will not happen on
     * Win32 systems.
     *
     * @return error code from process.
     * @param exec name of executable.
     * @param argv list of command arguments.
     * @param wait for process to exit before return.
     */
    static int spawn(const char *exec, const char **argv, bool wait = true);

    /**
     * Get the exit status of another process, waiting for it
     * to exit.
     *
     * @return exit code from process.
     * @param pid process id.
     */
    static int join(int pid);

    /**
     * Cancel a running child process.
     *
     * @return 0 on success.
     * @param pid process id.
     * @param sig cancel signal to apply.
     */
    static bool cancel(int pid, int sig = 0);

    /**
     * Get system environment.
     *
     * @return system environ symbol.
     * @param name of symbol.
     */
    static const char *getEnv(const char *name);

    /**
     * Set system environment in a standard manner.
     *
     * @param name of environment symbol to set.
     * @param value of environment symbol.
     * @param overwrite true if replace existing symbol.
     */
    static void setEnv(const char *name, const char *value, bool overwrite);

    /**
     * Get etc prefix path.
     *
     * @return etc prefix.
     */
    static const char *getConfigDir(void);

    /**
     * Get home directory.
     *
     * @return user home directory.
     */
    static const char *getHomeDir(void);

    /**
     * Get user name.
     *
     * @return user login id.
     */
    static const char *getUser(void);

    /**
     * Set user id by name.
     *
     * @return true if successful.
     */
    static bool setUser(const char *id, bool grp = true);

    /**
     * Set the effective group id by name.
     *
     * @return true if successful.
     */
    static bool setGroup(const char *id);

    /**
     * Return the effective operating system page size.
     *
     * @return system page size.
     */
    static size_t getPageSize(void);

    /**
     * Used to set process priority and optionally enable realtime.
     */
    static void setPriority(int pri);

    /**
     * Used to set process scheduling policy.
     */
    static void setScheduler(const char *policy);

    /**
     * Portable shortcut for setting realtime...
     */
    static void setRealtime(int pri = 0);

    /**
     * Return true if scheduler settable.
     */
    static bool isScheduler(void);

    /**
     * Return true if realtime scheduling.
     */
    static inline bool isRealtime(void)
        {return rtflag;};
};

/**
 * This class is used to create a "named" lock entity that can be used
 * to control access to a resource between multiple processes.  The
 * posix implimentation uses a pidfile and the win32 version uses a
 * globally visible mutex.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short System-wide named lock
 */
class __EXPORT Lockfile
{
private:
#ifdef  _MSWINDOWS_
    HANDLE  _mutex;
    bool    _flagged;
#else
    char *_path;
#endif

public:
    /**
     * Create a lock under a known name.
     *
     * @param name of system-wide lock to create.
     */
    Lockfile(const char *name);

    /**
     * Create a new lock object that can be used to make locks.
     */
    Lockfile();

    /**
     * Destroy the current lock and release it.
     */
    ~Lockfile()
        {unlock();};

    /**
     * Lock a system-wide name for this process.  If the lock
     * is successful, return true.  If an existing lock was
     * already acquired, release it first.
     *
     * @return true if lock successful.
     * @param name system-wide lock to use.
     */
    bool lock(const char *name);

    /**
     * Release an acquired lock.
     */
    void unlock(void);

    /**
     * Flag if the current process has aqcuired a lock.
     *
     * @return true if we have the lock.
     */
    bool isLocked(void);
};

END_NAMESPACE

#endif
