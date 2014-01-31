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
 * @file file.h
 * @short Files and dynamic loader services.
 **/

#ifndef COMMONCPP_FILE_H_
#define COMMONCPP_FILE_H_

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

#ifndef COMMONCPP_THREAD_H_
#include <commoncpp/thread.h>
#endif

#ifndef COMMONCPP_EXCEPTION_H_
#include <commoncpp/exception.h>
#endif

#ifndef WIN32
# ifdef __BORLANDC__
#  include <stdio.h>
#  include <sys/types.h>
# else
#  include <fcntl.h>
#  include <cstdio>
# endif
# include <dirent.h>
# include <sys/stat.h>
# include <sys/mman.h>
#else
# if __BORLANDC__ >= 0x0560
#  include <dirent.h>
#  include <sys/stat.h>
# else
#  include <direct.h>
# endif
#endif

NAMESPACE_COMMONCPP

typedef unsigned long pos_t;
#ifndef _MSWINDOWS_
// use a define so that if the sys/types.h header already defines caddr_t
// as it may on BSD systems, we do not break it by redefining again.
#undef  caddr_t
#define caddr_t char *
typedef size_t ccxx_size_t;
#else
typedef DWORD ccxx_size_t;
#endif

#ifndef PATH_MAX
#define PATH_MAX    256
#endif

#ifndef NAME_MAX
#define NAME_MAX    64
#endif

class __EXPORT File
{
public:
    enum Error {
        errSuccess = 0,
        errNotOpened,
        errMapFailed,
        errInitFailed,
        errOpenDenied,
        errOpenFailed,
        errOpenInUse,
        errReadInterrupted,
        errReadIncomplete,
        errReadFailure,
        errWriteInterrupted,
        errWriteIncomplete,
        errWriteFailure,
        errLockFailure,
        errExtended
    };
    typedef enum Error Error;

    enum Access {
#ifndef _MSWINDOWS_
        accessReadOnly = O_RDONLY,
        accessWriteOnly= O_WRONLY,
        accessReadWrite = O_RDWR
#else
        accessReadOnly = GENERIC_READ,
        accessWriteOnly = GENERIC_WRITE,
        accessReadWrite = GENERIC_READ | GENERIC_WRITE
#endif
    };
    typedef enum Access Access;

protected:
    typedef struct _fcb {
        struct _fcb *next;
        caddr_t address;
        ccxx_size_t len;
        off_t pos;
        bool locked;
    } fcb_t;

public:
#ifdef  _MSWINDOWS_
    enum Open {
        openReadOnly, // = FILE_OPEN_READONLY,
        openWriteOnly, // = FILE_OPEN_WRITEONLY,
        openReadWrite, // = FILE_OPEN_READWRITE,
        openAppend, // = FILE_OPEN_APPEND,
        openTruncate // = FILE_OPEN_TRUNCATE
    };
#else
    enum Open {
        openReadOnly = O_RDONLY,
        openWriteOnly = O_WRONLY,
        openReadWrite = O_RDWR,
        openAppend = O_WRONLY | O_APPEND,
#ifdef  O_SYNC
        openSync = O_RDWR | O_SYNC,
#else
        openSync = O_RDWR,
#endif
        openTruncate = O_RDWR | O_TRUNC
    };
    typedef enum Open Open;

/* to be used in future */

#ifndef S_IRUSR
#define S_IRUSR 0400
#define S_IWUSR 0200
#define S_IRGRP 0040
#define S_IWGRP 0020
#define S_IROTH 0004
#define S_IWOTH 0002
#endif

#endif // !WIN32

#ifndef _MSWINDOWS_
    enum Attr {
        attrInvalid = 0,
        attrPrivate = S_IRUSR | S_IWUSR,
        attrGroup = attrPrivate | S_IRGRP | S_IWGRP,
        attrPublic = attrGroup | S_IROTH | S_IWOTH
    };
#else // defined WIN32
    enum Attr {
        attrInvalid=0,
        attrPrivate,
        attrGroup,
        attrPublic
    };
#endif // !WIN32
    typedef enum Attr Attr;

#ifdef  _MSWINDOWS_
    enum Complete {
        completionImmediate, // = FILE_COMPLETION_IMMEDIATE,
        completionDelayed, // = FILE_COMPLETION_DELAYED,
        completionDeferred // = FILE_COMPLETION_DEFERRED
    };

    enum Mapping {
        mappedRead,
        mappedWrite,
        mappedReadWrite
    };
#else
    enum Mapping {
        mappedRead = accessReadOnly,
        mappedWrite = accessWriteOnly,
        mappedReadWrite = accessReadWrite
    };
    enum Complete {
        completionImmediate,
        completionDelayed,
        completionDeferred
    };
#endif
    typedef enum Complete Complete;
    typedef enum Mapping Mapping;

public:
    static const char *getExtension(const char *path);
    static const char *getFilename(const char *path);
    static char *getFilename(const char *path, char *buffer, size_t size = NAME_MAX);
    static char *getDirname(const char *path, char *buffer, size_t size = PATH_MAX);
    static char *getRealpath(const char *path, char *buffer, size_t size = PATH_MAX);
};

/**
 * A low level portable directory class.  Used to support ccstd Directory
 * container.  This provides a basic mechanism for allocating and
 * accessing file entries.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short low level directory access class.
 */
class __EXPORT Dir : public File
{
private:
#ifndef _MSWINDOWS_
    DIR *dir;
    struct dirent *save;
    char save_space[sizeof(struct dirent) + PATH_MAX + 1];
    struct dirent *entry;
#else
    HANDLE hDir;
    WIN32_FIND_DATA data, fdata;
    char *name;
#endif

public:
    Dir(const char *name = NULL);

    static bool create(const char *path, Attr attr = attrGroup);
    static bool remove(const char *path);
    static bool setPrefix(const char *path);
    static bool getPrefix(char *path, size_t size = PATH_MAX);

    void open(const char *name);
    void close(void);

    virtual ~Dir();

    const char *getName(void);

    const char *operator++()
        {return getName();};

    const char *operator++(int)
        {return getName();};

    const char *operator*();

    bool rewind(void);

    bool operator!()
#ifndef _MSWINDOWS_
        {return !dir;};
#else
        {return hDir != INVALID_HANDLE_VALUE;};
#endif

    bool isValid(void);
};

/**
 * A generic class to walk a hierarchical directory structure.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Directory tree walking.
 */
class __EXPORT  DirTree
{
private:
    char path[PATH_MAX + 1];
    Dir *dir;
    unsigned max, current, prefixpos;

protected:
    /**
     * Virtual method to filter results.  Virtual override methods
     * should call baseclass method to assure . and .. names are
     * stripped out.
     *
     * @return true if current filename is accepted.
     * @param file path to examine
     * @param ino info of type, date, etc.
     */
    virtual bool filter(const char *file, struct stat *ino);

public:
    /**
     * Construct a directory tree walk starting at the specified
     * prefix.  A maximum subdirectory depth is also specified.
     *
     * @param prefix to start walk.
     * @param maxdepth subdirectory depth to examine.
     */
    DirTree(const char *prefix, unsigned maxdepth);

    /**
     * Construct an un-opened directory tree of a known maximum depth
     *
     * @param maxdepth subdirectory subdirectory depth.
     */
    DirTree(unsigned maxdepth);

    virtual ~DirTree();

    /**
     * Open a directory tree path.
     *
     * @param prefix directory path to open.
     */
    void open(const char *prefix);

    /**
     * Close the directory path.
     */
    void close(void);

    /**
     * Extract the next full pathname from the directory walk.
     * When returning directories, a '/' is appended.  The
     * returned string is a buffer of MAX_PATH size.
     *
     * @return path of next subdirectory entry or NULL.
     */
    char *getPath(void);

    /**
     * This is used to step through the filter virtual for an
     * entire subtree, and is used for cases where a derived
     * DirTree class performs it's primary operations through
     * filter rather than externally by calling getPath().
     *
     * @return number of files and directories examined.
     * @param prefix directory path to examine.
     */
    unsigned perform(const char *prefix);
};

/**
 * The purpose of this class is to define a base class for low level
 * random file access that is portable between Win32 and Posix systems.
 * This class is a foundation both for optimized thread shared and
 * traditional locked file access that is commonly used to build
 * database services, rather than the standard C++ streaming file classes.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Portable random disk file access.
 */
class __EXPORT RandomFile : protected Mutex, public File
{
private:
    Error errid;
    char *errstr;

protected:
#ifndef _MSWINDOWS_
    int fd;
    // FIXME: WIN32 as no access member
    Access access;
#else
    HANDLE fd;
#endif
    char *pathname;

    struct {
        unsigned count : 16;
        bool thrown : 1;
        bool initial : 1;
#ifndef _MSWINDOWS_
        bool immediate : 1;
#endif
        bool temp : 1;
    } flags;

    /**
     * Create an unopened random access file.
     */
    RandomFile(const char *name = NULL);

    /**
     * Default copy constructor.
     */
    RandomFile(const RandomFile &rf);

    /**
     * Post an error event.
     *
     * @return error code.
     * @param errid error code.
     * @param errstr error message string.
     */
    Error error(Error errid, char *errstr = NULL);

    /**
     * Post an extended string error message.
     *
     * @return errExtended.
     * @param err error string.
     */
    inline Error error(char *err)
        {return error(errExtended, err);};

    /**
     * Used to enable or disable throwing of exceptions on
     * errors.
     *
     * @param enable true if errors will be thrown.
     */
    inline void setError(bool enable)
        {flags.thrown = !enable;};

#ifndef _MSWINDOWS_
    /**
     * Used to set file completion modes.
     *
     * @return errSuccess if okay.
     * @param mode completion mode.
     * @todo implement in win32
     */
    Error setCompletion(Complete mode);
#endif

    /**
     * Used to set the temporary attribute for the file.  Temporary
     * files are automatically deleted when closed.
     *
     * @param enable true for marking as temporary.
     */
    inline void setTemporary(bool enable)
        {flags.temp = enable;};

    /**
     * This method is used to initialize a newly created file as
     * indicated by the "initial" flag.  This method also returns
     * the file access permissions that should be associated with
     * the file.  This method should never be called directly, but
     * is instead used to impliment the "Initial" method.  Typically
     * one would use this to build an empty database shell when a
     * previously empty database file is created.
     *
     * @return access, or attrInvalid if should be removed.
     */
    virtual Attr initialize(void);

    /**
     * Close the file.
     */
    void final(void);

public:
    /**
     * Destroy a random access file or it's derived class.
     */
    virtual ~RandomFile();

    /**
     * This method should be called right after a RandomFile derived
     * object has been created.  This method will invoke initialize
     * if the object is newly created, and set file access permissions
     * appropriately.
     *
     * @return true if file had to be initialized.
     */
    bool initial(void);

    /**
     * Get current file capacity.
     *
     * @return total file size.
     */
    off_t getCapacity(void);

    /**
     * This method is commonly used to close and re-open an existing
     * database.  This may be used when the database has been unlinked
     * and an external process provides a new one to use.
     */
    virtual Error restart(void);

    /**
     * Return current error id.
     *
     * @return last error identifier set.
     */
    inline Error getErrorNumber(void)
        {return errid;};

    /**
     * Return current error string.
     *
     * @return last error string set.
     */
    inline char *getErrorString(void)
        {return errstr;};

    bool operator!(void);
};

/**
 * This class defines a database I/O file service that can be shared
 * by multiple processes.  Each thread should access a dup of the database
 * object, and mutex locks can be used to preserve transaction
 * integrety if multiple threads are used.
 *
 * SharedFile is used when a database may be shared between multiple
 * processes.  SharedFile automatically applies low level byte-range "file
 * locks", and provides an interface to fetch and release byte-range locked
 * portions of a file.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short This class defines a database I/O file service that can be shared by multiple processes.
 */
class __EXPORT SharedFile : public RandomFile
{
private:
    fcb_t fcb;
    Error open(const char *path);

public:
    /**
     * Open or create a new database file.  You should also use
     * Initial.
     *
     * @param path pathname of database to open.
     */
    SharedFile(const char *path);

    /**
     * Create a shared file as a duplicate of an existing shared
     * file.
     *
     * @param file original file.
     */
    SharedFile(const SharedFile &file);

    /**
     * Close and finish a database file.
     */
    virtual ~SharedFile();

    /**
     * Restart an existing database; close and re-open.
     *
     * @return errSuccess if successful.
     */
    Error restart(void)
        {return open(pathname);};

    /**
     * Lock and Fetch a portion of the file into physical memory.
     * This can use state information to fetch the current record
     * multiple times.
     *
     * @return errSuccess on success.
     * @param address  address to use, or NULL if same as last I/O.
     * @param length   length to use, or 0 if same as last I/O.
     * @param position file position to use -1 if same as last I/O.
     */
    Error fetch(caddr_t address = NULL, ccxx_size_t length = 0, off_t position = -1);

    /**
     * Update a portion of a file from physical memory.  This can use
     * state information to commit the last read record.  The current
     * lock is also cleared.
     *
     * @return errSuccess on success.
     * @param address  address to use, or NULL if same as last I/O.
     * @param length   length to use, or 0 if same as last I/O.
     * @param position file position to use or -1 if same as last I/O.
     */
    Error update(caddr_t address = NULL, ccxx_size_t length = 0, off_t position = -1);

    /**
     * Clear a lock held from a previous fetch operation without
     * updating.
     *
     * @return errSuccess on success.
     * @param length length to use, or 0 if same as last I/O.
     * @param pos    file position to use or -1 if same as last I/O.
     */
    Error clear(ccxx_size_t length = 0, off_t pos = -1);

    /**
     * Add new data to the end of the file.  Locks file during append.
     *
     * @param address address to use, or NULL if same as last I/O.
     * @param length  length to use, or 0 if same as last I/O.
     */
    Error append(caddr_t address = NULL, ccxx_size_t length = 0);

    /**
     * Fetch the current file position marker for this thread.
     *
     * @return file position offset.
     */
    off_t getPosition(void);

    bool operator++(void);
    bool operator--(void);
};

/**
 * Create and map a disk file into memory.  This portable class works
 * under both Posix via mmap and under the win32 API. A mapped file
 * can be referenced directly by it's memory segment. One can map
 * and unmap portions of a file on demand, and update
 * changed memory pages mapped from files immediately through sync().
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Map a named disk file into memory.
 */
class __EXPORT MappedFile : public RandomFile
{
private:
    fcb_t fcb;
    int prot;
#ifdef  _MSWINDOWS_
    HANDLE map;
    char mapname[64];
#endif

public:
    /**
     * Open a file for mapping.  More than one segment of a file
     * may be mapped into seperate regions of memory.
     *
     * @param fname file name to access for mapping.
     * @param mode  access mode to map file.
     */
    MappedFile(const char *fname, Access mode);

    /**
     * Create if not exists, and map a file of specified size
     * into memory.
     *
     * @param fname file name to access for mapping.
     * @param mode access mode to map file.
     * @param size of file to map.
     */
    MappedFile(const char *fname, Access mode, size_t size);

    /**
     * Map a portion or all of a specified file in the specified
     * shared memory access mode.  Valid mapping modes include
     * mappedRead, mappedWrite, and mappedReadWrite.
     *
     * @param fname pathname of file to map into memory.
     * @param offset from start of file to begin mapping in bytes.
     * @param size of mapped area in bytes.
     * @param mode to map file.
     */
    MappedFile(const char *fname, pos_t offset, size_t size, Access mode);

    /**
     * Release a mapped section of memory associated with a file.  The
     * mapped area is updated back to disk.
     */
    virtual ~MappedFile();

    // FIXME: not use library function in header ??
    /**
     * Synchronize the contents of the mapped portion of memory with
     * the disk file and wait for completion.  This assures the memory
     * mapped from the file is written back.
     */
    void sync(void);

    /**
     * Synchronize a segment of memory mapped from a segment fetch.
     *
     * @param address memory address to update.
     * @param len size of segment.
     */
    void sync(caddr_t address, size_t len);

    /**
     * Map a portion of the memory mapped from the file back to the
     * file and do not wait for completion.  This is useful when mapping
     * a database file and updating a single record.
     *
     * @param offset offset into the mapped region of memory.
     * @param len length of partial region (example, record length).
     */
    void update(size_t offset = 0, size_t len = 0);

    /**
     * Update a mapped region back to disk as specified by address
     * and length.
     *
     * @param address address of segment.
     * @param len length of segment.
     */
    void update(caddr_t address, size_t len);

    /**
     * Release (unmap) a memory segment.
     *
     * @param address address of memory segment to release.
     * @param len length of memory segment to release.
     */
    void release(caddr_t address, size_t len);

    /**
     * Fetch a pointer to an offset within the memory mapped portion
     * of the disk file.  This really is used for convience of matching
     * operations between Update and Fetch, as one could simply have
     * accessed the base pointer where the file was mapped directly.
     *
     * @param offset from start of mapped memory.
     */
    inline caddr_t fetch(size_t offset = 0)
        {return ((char *)(fcb.address)) + offset;};

    /**
     * Fetch and map a portion of a disk file to a logical memory
     * block.
     *
     * @return pointer to memory segment.
     * @param pos offset of file segment to map.
     * @param len size of memory segment to map.
     */
    caddr_t fetch(off_t pos, size_t len);

    /**
     * Lock the currently mapped portion of a file.
     *
     * @return true if pages are locked.
     */
    bool lock(void);

    /**
     * Unlock a locked mapped portion of a file.
     */
    void unlock(void);

    /**
     * Compute map size to aligned page boundry.
     *
     * @param size request.
     * @return page aligned size.
     */
    size_t pageAligned(size_t size);
};


/**
 * The DSO dynamic loader class is used to load object files.  On
 * elf based systems this is typically done with dlopen.  A dummy
 * stub class is generated for non-dl capable systems.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Dynamic class file loader.
 */
class __EXPORT DSO
{
private:
    const char *err;
    static Mutex mutex;
    static DSO *first;
    static DSO *last;
    DSO *next, *prev;
    const char *id;
    void *image;

    typedef ucommon::dso::addr_t addr_t;

protected:
    void loader(const char *filename, bool resolve);

public:
    /**
     * Construct and load a DSO object file.
     *
     * @param filename pathname of object file to load.
     */
    DSO(const char *filename)
        {loader(filename, true);};

    DSO(const char *filename, bool resolve)
        {loader(filename, resolve);};

    /**
     * Retrieve error indicator associated with DSO failure.  This
     * is often used in catch handlers.
     */
    inline const char *getError(void)
        {return err;};

    /**
     * Detach a DSO object from running memory.
     */
    virtual ~DSO();

    /**
     * Lookup a symbol in the loaded file.
     */
    addr_t operator[](const char *sym);

    static void dynunload(void);

    /**
     * Find a specific DSO object by filename.
     *
     * @param name of DSO object file (partial).
     */
    static DSO *getObject(const char *name);

    /**
     * See if DSO object is valid.
     *
     * @return true if valid.
     */
    bool isValid(void);

    /**
     * Install debug handler...
     */
    static void setDebug(void);
};

/** @relates RandomFile */
bool __EXPORT isDir(const char *path);
/** @relates RandomFile */
bool __EXPORT isFile(const char *path);
#ifndef WIN32
/** @relates RandomFile */
bool __EXPORT isDevice(const char *path);
#else
/** @relates RandomFile */
inline bool isDevice(const char *path)
{ return false; }
#endif
/** @relates RandomFile */
bool __EXPORT canAccess(const char *path);
/** @relates RandomFile */
bool __EXPORT canModify(const char *path);
/** @relates RandomFile */
time_t __EXPORT lastModified(const char *path);
/** @relates RandomFile */
time_t __EXPORT lastAccessed(const char *path);

#ifdef  COMMON_STD_EXCEPTION

class DirException : public IOException
{
public:
    DirException(const String &str) : IOException(str) {};
};

class __EXPORT DSOException : public IOException
{
public:
    DSOException(const String &str) : IOException(str) {};
};

class __EXPORT FileException : public IOException
{
public:
    FileException(const String &str) : IOException(str) {};
};

#endif

END_NAMESPACE

#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
