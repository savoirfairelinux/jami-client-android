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

#include <cc++/config.h>
#include <cc++/export.h>
#include <cc++/thread.h>
#include <cc++/process.h>
#include <cc++/strchar.h>

#include <cstdlib>
#include <cstdio>
#include <cerrno>
#include <csignal>

#ifdef  MACOSX
#undef  _POSIX_PRIORITY_SCHEDULING
#endif

#ifndef WIN32
#ifdef  HAVE_SYS_TIME_H
#include <sys/time.h>
#endif

#ifdef  HAVE_SYS_WAIT_H
#include <sys/wait.h>
#endif

#include <pwd.h>
#include <grp.h>

#ifdef  SIGTSTP
#include <sys/file.h>
#include <sys/ioctl.h>
#endif

#ifndef _PATH_TTY
#define _PATH_TTY "/dev/tty"
#endif
#endif

#ifdef  WIN32
#include <process.h>
#include <stdio.h>
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

static char *_pUser = NULL;
static char *_pHome = NULL;

bool Process::rtflag = false;

#ifdef  WIN32

static SYSTEM_INFO sysinfo;
static LPSYSTEM_INFO lpSysInfo = NULL;

static void init_sysinfo(void)
{
    if(!lpSysInfo) {
        lpSysInfo = &sysinfo;
        memset(&sysinfo, 0, sizeof(sysinfo));
        GetSystemInfo(lpSysInfo);
    }
}

const char *Process::getUser(void)
{
    static char userid[65];
    DWORD length = sizeof(userid);

    if(GetUserName(userid, &length))
        return userid;

    return NULL;
}

size_t Process::getPageSize(void)
{
    init_sysinfo();
    return (size_t) lpSysInfo->dwPageSize;
}

int Process::spawn(const char *exename, const char **args, bool wait)
{
    int mode = P_NOWAIT;

    if(wait)
            mode = P_WAIT;

    return (int)::spawnvp(mode, (char *)exename, (char **)args);
}

int Process::join(int pid)
{
    int status, result;

    if(pid == -1)
        return pid;

    result = (int)cwait(&status, pid, WAIT_CHILD);
    if(status & 0x0)
        return -1;
    return result;
}

bool Process::cancel(int pid, int sig)
{
    HANDLE hPid = OpenProcess(PROCESS_TERMINATE, FALSE, pid);
    bool rtn = true;

    if(!hPid)
        return false;

    switch(sig) {
    case SIGABRT:
    case SIGTERM:
        if(!TerminateProcess(hPid, -1))
            rtn = false;
    case 0:
        break;
    default:
        rtn = false;
    }
    CloseHandle(hPid);
    return rtn;
}

void Process::setPriority(int pri)
{
    DWORD pc = NORMAL_PRIORITY_CLASS;
    DWORD pid = GetCurrentProcessId();
    HANDLE hProcess = OpenProcess(PROCESS_DUP_HANDLE, TRUE, pid);

#ifdef  BELOW_NORMAL_PRIORITY_CLASS
    if(pri == -1)
        pc = BELOW_NORMAL_PRIORITY_CLASS;
    else if(pri == 1)
        pc = ABOVE_NORMAL_PRIORITY_CLASS;
    else
#endif
    if(pri == 2)
        pc = HIGH_PRIORITY_CLASS;
    else if(pri > 2)
        pc = REALTIME_PRIORITY_CLASS;
    else if(pri < -1)
        pc = IDLE_PRIORITY_CLASS;

    SetPriorityClass(hProcess, pc);
    CloseHandle(hProcess);
}

void Process::setScheduler(const char *cp)
{
    if(!stricmp(cp, "fifo"))
        setPriority(3);
    else if(!stricmp(cp, "rr"))
        setPriority(2);
    else if(!stricmp(cp, "idle"))
        setPriority(-2);
    else
        setPriority(0);
}

void Process::setRealtime(int pri)
{
    setPriority(3);
}

bool Process::isScheduler(void)
{
    return false;
}

#else

#ifndef WEXITSTATUS
#define WEXITSTATUS(status) ((unsigned)(status) >> 8)
#endif

#ifndef WIFEXITED
#define WIFEXITED(status) (((status) & 255) == 0)
#endif

#ifndef WTERMSIG
#define WTERMSIG(status) (((unsigned)(status)) & 0x7F)
#endif

#ifndef WIFSIGNALLED
#define WIFSIGNALLED(status) (((status) & 255) != 0)
#endif

#ifndef WCOREDUMP
#define WCOREDUMP(status) (((status) & 0x80) != 0)
#endif

static void lookup(void)
{
    struct passwd *pw = NULL;
#ifdef  HAVE_GETPWUID_R
    struct passwd pwd;
    char buffer[1024];

    ::getpwuid_r(geteuid(), &pwd, buffer, 1024, &pw);
#else
    pw = ::getpwuid(geteuid());
#endif

    if(_pHome)
        delString(_pHome);
    if(_pUser)
        delString(_pUser);

    _pUser = _pHome = NULL;

    if(pw != NULL && pw->pw_dir != NULL)
        _pHome = newString(pw->pw_dir);

    if(pw != NULL && pw->pw_name != NULL)
        _pUser = newString(pw->pw_name);

    endpwent();
}

const char *Process::getUser(void)
{
    if(!_pUser)
        lookup();

    return (const char *)_pUser;
}

const char *Process::getConfigDir(void)
{
#ifdef  ETC_CONFDIR
    return ETC_CONFDIR;
#else
    return ETC_PREFIX;
#endif
}

const char *Process::getHomeDir(void)
{
    if(!_pHome)
        lookup();

    return (const char *)_pHome;
}

#ifdef  HAVE_GETPAGESIZE
size_t Process::getPageSize(void)
{
    return (size_t)getpagesize();
}

#else

size_t Process::getPageSize(void)
{
    return 1024;
}

#endif

bool Process::setUser(const char *id, bool grp)
{
    struct passwd *pw = NULL;
#ifdef  HAVE_GETPWNAM_R
    struct passwd pwd;
    char buffer[1024];

    ::getpwnam_r(id, &pwd, buffer, 1024, &pw);
#else
    pw = ::getpwnam(id);
#endif
    if(!pw)
        return false;

    if(grp)
        if(setgid(pw->pw_gid))
            return false;

    if(setuid(pw->pw_uid))
        return false;

    lookup();
    return true;
}

bool Process::setGroup(const char *id)
{
    struct group *group = NULL;
#ifdef  HAVE_GETGRNAM_R
    struct group grp;
    char buffer[2048];

    ::getgrnam_r(id, &grp, buffer, 1024, &group);
#else
    group = ::getgrnam(id);
#endif
    if(!group) {
        /* endgrent(); */
        return false;
    }

#ifdef  HAVE_SETEGID
    setegid(group->gr_gid);
#endif
    if(setgid(group->gr_gid)) {
        /* endgrent(); */
        return false;
    }

    /* endgrent(); */
    return true;
}

bool Process::cancel(int pid, int sig)
{
    if(!sig)
        sig = SIGTERM;

    if(pid < 1)
        return false;

    if(::kill(pid, sig))
        return false;

    return true;
}

int Process::join(int pid)
{
    int status;

    if(pid < 1)
        return -1;

#ifdef  HAVE_WAITPID
    waitpid(pid, &status, 0);
#else
#ifdef  HAVE_WAIT4
    wait4(pid, &status, 0, NULL);
#else
    int result;
    while((result = wait(&status)) != pid && result != -1)
        ;
#endif
#endif

    if(WIFEXITED(status))
        return WEXITSTATUS(status);
    else if(WIFSIGNALLED(status))
        return -WTERMSIG(status);
    else
        return -1;
}

int Process::spawn(const char *exename, const char **args, bool wait)
{
    int pid;

    pid = vfork();
    if(pid == -1)
        return -1;

    if(!pid) {
        execvp((char *)exename, (char **)args);
        _exit(-1);
    }

    if(!wait)
        return pid;

    return join(pid);
}

Process::Trap Process::setInterruptSignal(int signo, Trap func)
{
    struct  sigaction   sig_act, old_act;

    memset(&sig_act, 0, sizeof(sig_act));
    sig_act.sa_handler = func;
    sigemptyset(&sig_act.sa_mask);
    if(signo != SIGALRM)
        sigaddset(&sig_act.sa_mask, SIGALRM);

    sig_act.sa_flags = 0;
#ifdef  SA_INTERRUPT
    sig_act.sa_flags |= SA_INTERRUPT;
#endif
    if(sigaction(signo, &sig_act, &old_act) < 0)
        return SIG_ERR;

    return old_act.sa_handler;
}

Process::Trap Process::setPosixSignal(int signo, Trap func)
{
    struct  sigaction   sig_act, old_act;

    memset(&sig_act, 0, sizeof(sig_act));
    sig_act.sa_handler = func;
    sigemptyset(&sig_act.sa_mask);
    sig_act.sa_flags = 0;
    if(signo == SIGALRM) {
#ifdef  SA_INTERRUPT
        sig_act.sa_flags |= SA_INTERRUPT;
#endif
    }
    else {
        sigaddset(&sig_act.sa_mask, SIGALRM);
#ifdef  SA_RESTART
        sig_act.sa_flags |= SA_RESTART;
#endif
    }
    if(sigaction(signo, &sig_act, &old_act) < 0)
        return SIG_ERR;
    return old_act.sa_handler;
}

void    Process::detach(void)
{
    attach("/dev/null");
}

void    Process::attach(const char *dev)
{
    int pid;
    int fd;

    if(getppid() == 1)
        return;

    ::close(0);
    ::close(1);
    ::close(2);

#ifdef  SIGTTOU
    setPosixSignal(SIGTTOU, SIG_IGN);
#endif

#ifdef  SIGTTIN
    setPosixSignal(SIGTTIN, SIG_IGN);
#endif

#ifdef  SIGTSTP
    setPosixSignal(SIGTSTP, SIG_IGN);
#endif

    if((pid = fork()) < 0)
        THROW(pid);
    else if(pid > 0)
        exit(0);


#if defined(SIGTSTP) && defined(TIOCNOTTY)
    if(setpgid(0, getpid()) == -1)
        THROW(-1);

    if((fd = open(_PATH_TTY, O_RDWR)) >= 0) {
        ioctl(fd, TIOCNOTTY, NULL);
        close(fd);
    }
#else

#ifdef  HAVE_SETPGRP
    if(setpgrp() == -1)
        THROW(-1);
#else
    if(setpgid(0, getpid()) == -1)
        THROW(-1);
#endif

    setPosixSignal(SIGHUP, SIG_IGN);

    if((pid = fork()) < 0)
        THROW(-1);
    else if(pid > 0)
        exit(0);
#endif

    if(dev && *dev) {
        ::open(dev, O_RDWR);
        ::open(dev, O_RDWR);
        ::open(dev, O_RDWR);
    }
}

void Process::setScheduler(const char *pol)
{
#ifdef  _POSIX_PRIORITY_SCHEDULING
    struct sched_param p;
    int policy;

    sched_getparam(0, &p);

    if(pol) {
#if defined(SCHED_TS)
        policy = SCHED_TS;
#elif defined(SCHED_OTHER)
        policy = SCHED_OTHER;
#else
        policy = 0;
#endif

#ifdef  SCHED_RR
        if(!stricmp(pol, "rr"))
            policy = SCHED_RR;
#endif
#if !defined(SCHED_RR) && defined(SCHED_FIFO)
        if(!stricmp(pol, "rr"))
            policy = SCHED_FIFO;
#endif
#ifdef  SCHED_FIFO
        if(!stricmp(pol, "fifo")) {
            rtflag = true;
            policy = SCHED_FIFO;
        }
#endif
#ifdef  SCHED_TS
        if(!stricmp(pol, "ts"))
            policy = SCHED_TS;
#endif
#ifdef  SCHED_OTHER
        if(!stricmp(pol, "other"))
            policy = SCHED_OTHER;
#endif
    }
    else
        policy = sched_getscheduler(0);

    int min = sched_get_priority_min(policy);
    int max = sched_get_priority_max(policy);

    if(p.sched_priority < min)
        p.sched_priority = min;
    else if(p.sched_priority > max)
        p.sched_priority = max;

    sched_setscheduler(0, policy, &p);
#endif
}


void Process::setPriority(int pri)
{
#ifdef  _POSIX_PRIORITY_SCHEDULING
    struct sched_param p;
    int policy = sched_getscheduler(0);
    int min = sched_get_priority_min(policy);
    int max = sched_get_priority_max(policy);

    sched_getparam(0, &p);

    if(pri < min)
        pri = min;
    if(pri > max)
        pri = max;
    p.sched_priority = pri;
    sched_setparam(0, &p);
#else
    if(pri < -20)
        pri = -20;
    if(pri > 20)
        pri = 20;
    nice(-pri);
#endif
}

bool Process::isScheduler(void)
{
#ifdef  _POSIX_PRIORITY_SCHEDULING
    return true;
#else
    return false;
#endif
}

void Process::setRealtime(int pri)
{
    if(pri < 1)
        pri = 1;

    setScheduler("rr");
    setPriority(pri);
}

#endif  // not win32

#ifdef  _OSF_SOURCE
#undef  HAVE_SETENV
#endif

void Process::setEnv(const char *name, const char *value, bool overwrite)
{
#ifdef  HAVE_SETENV
    ::setenv(name, value, (int)overwrite);
#else
    char strbuf[256];

    snprintf(strbuf, sizeof(strbuf), "%s=%s", name, value);
    if(!overwrite)
        if(getenv(strbuf))
            return;

    ::putenv(strdup(strbuf));
#endif
}

const char *Process::getEnv(const char *name)
{
    return ::getenv(name);
}

#if defined(HAVE_MLOCKALL) && defined(MCL_FUTURE)

#include <sys/mman.h>

bool Process::lock(bool future)
{
    int rc;

    if(future)
        rc = mlockall(MCL_CURRENT | MCL_FUTURE);
    else
        rc = mlockall(MCL_CURRENT);
    if(rc)
        return false;

    return true;
}

void Process::unlock(void)
{
    munlockall();
}
#else

bool Process::lock(bool future)
{
    return false;
}

void Process::unlock(void)
{
}

#endif

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
