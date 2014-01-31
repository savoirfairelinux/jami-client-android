// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This file is part of GNU uCommon C++.
//
// GNU uCommon C++ is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// GNU uCommon C++ is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with GNU uCommon C++.  If not, see <http://www.gnu.org/licenses/>.

/**
 * Thread-aware file system manipulation class.  This is used to provide
 * generic file operations that are OS independent and thread-safe in
 * behavior.  This is used in particular to wrap posix calls internally
 * to pth, and to create portable code between MSWINDOWS and Posix low-level
 * file I/O operations.
 * @file ucommon/fsys.h
 */

#ifndef _UCOMMON_FSYS_H_
#define _UCOMMON_FSYS_H_

#ifndef _UCOMMON_CONFIG_H_
#include <ucommon/platform.h>
#endif

#ifndef _UCOMMON_PROTOCOLS_H_
#include <ucommon/protocols.h>
#endif

#ifndef _UCOMMON_THREAD_H_
#include <ucommon/thread.h>
#endif

#ifndef _UCOMMON_STRING_H_
#include <ucommon/string.h>
#endif

#ifndef _UCOMMON_MEMORY_H_
#include <ucommon/memory.h>
#endif

#ifndef _MSWINDOWS_
#include <sys/stat.h>
#else
#include <io.h>
#ifndef R_OK
#define F_OK 0
#define X_OK 1
#define W_OK 2
#define R_OK 4
#endif
#endif

#include <errno.h>
#include <stdio.h>

#ifndef __S_ISTYPE
#define __S_ISTYPE(mode, mask)  (((mode) & S_IFMT) == (mask))
#endif

#if !defined(S_ISDIR) && defined(S_IFDIR)
#define S_ISDIR(mode)   __S_ISTYPE((mode), S_IFDIR)
#endif

#if !defined(S_ISCHR) && defined(S_IFCHR)
#define S_ISCHR(mode)   __S_ISTYPE((mode), S_IFCHR)
#elif !defined(S_ISCHR)
#define S_ISCHR(mode)   0
#endif

#if !defined(S_ISBLK) && defined(S_IFBLK)
#define S_ISBLK(mode)   __S_ISTYPE((mode), S_IFBLK)
#elif !defined(S_ISBLK)
#define S_ISBLK(mode)   0
#endif

#if !defined(S_ISREG) && defined(S_IFREG)
#define S_ISREG(mode)   __S_ISTYPE((mode), S_IFREG)
#elif !defined(S_ISREG)
#define S_ISREG(mode)   1
#endif

#if !defined(S_ISSOCK) && defined(S_IFSOCK)
#define S_ISSOCK(mode)  __S_ISTYPE((mode), S_IFSOCK)
#elif !defined(S_ISSOCK)
#define S_ISSOCK(mode)  (0)
#endif

#if !defined(S_ISFIFO) && defined(S_IFIFO)
#define S_ISFIFO(mode)  __S_ISTYPE((mode), S_IFIFO)
#elif !defined(S_ISFIFO)
#define S_ISFIFO(mode)  (0)
#endif

#if !defined(S_ISLNK) && defined(S_IFLNK)
#define S_ISLNK(mode)   __S_ISTYPE((mode), S_IFLNK)
#elif !defined(S_ISLNK)
#define S_ISLNK(mode)   (0)
#endif

NAMESPACE_UCOMMON

/**
 * Convenience type for loader operations.
 */
typedef void *mem_t;

/**
 * A container for generic and o/s portable threadsafe file system functions.
 * These are based roughly on their posix equivilents.  For libpth, the
 * system calls are wrapped.  The native file descriptor or handle may be
 * used, but it is best to use "class fsys" instead because it can capture
 * the errno of a file operation in a threadsafe and platform independent
 * manner, including for mswindows targets.
 */
class __EXPORT fsys
{
protected:
    fd_t    fd;
    int     error;

public:
    /**
     * Most of the common chmod values are predefined.
     */
    enum {
        OWNER_READONLY = 0400,
        GROUP_READONLY = 0440,
        PUBLIC_READONLY = 0444,
        OWNER_PRIVATE = 0600,
        OWNER_PUBLIC = 0644,
        GROUP_PRIVATE = 0660,
        GROUP_PUBLIC = 0664,
        EVERYONE = 0666,
        DIR_TEMPORARY = 01777
    };

    typedef struct stat fileinfo_t;

#ifdef  _MSWINDOWS_
    static int remapError(void);
#else
    inline static int remapError(void)
        {return errno;};
#endif

    /**
     * Enumerated file access modes.
     */
    typedef enum {
        RDONLY,
        WRONLY,
        REWRITE,
        RDWR = REWRITE,
        APPEND,
        SHARED,
        EXCLUSIVE,
        DEVICE,
        STREAM,
        RANDOM
    } access_t;

    /**
     * File offset type.
     */
    typedef long offset_t;

    /**
     * Used to mark "append" in set position operations.
     */
    static const offset_t end;

    /**
     * Construct an unattached fsys descriptor.
     */
    fsys();

    /**
     * Contruct fsys from raw file handle.
     */
    fsys(fd_t handle);

    /**
     * Copy (dup) an existing fsys descriptor.
     * @param descriptor to copy from.
     */
    fsys(const fsys& descriptor);

    /**
     * Create a fsys descriptor by opening an existing file or directory.
     * @param path of file to open for created descriptor.
     * @param access mode of file.
     */
    fsys(const char *path, access_t access);

    /**
     * Create a fsys descriptor by creating a file.
     * @param path of file to create for descriptor.
     * @param access mode of file access.
     * @param permission mode of file.
     */
    fsys(const char *path, unsigned permission, access_t access);

    /**
     * Close and release a file descriptor.
     */
    ~fsys();

    /**
     * Get the descriptor from the object by pointer reference.
     * @return low level file handle.
     */
    inline fd_t operator*() const
        {return fd;};

    /**
     * Get the descriptor from the object by casting reference.
     * @return low level file handle.
     */
    inline operator fd_t() const
        {return fd;}

    /**
     * Reset error flag.
     */
    inline void reset(void)
        {error = 0;}

    /**
     * Test if file descriptor is open.
     * @return true if open.
     */
    inline operator bool() const
        {return fd != INVALID_HANDLE_VALUE;}

    /**
     * Test if file descriptor is closed.
     * @return true if closed.
     */
    inline bool operator!() const
        {return fd == INVALID_HANDLE_VALUE;}

    /**
     * Assign file descriptor by duplicating another descriptor.
     * @param descriptor to dup from.
     */
    void operator=(const fsys& descriptor);

    /**
     * Replace current file descriptor with an external descriptor.  This
     * does not create a duplicate.  The external descriptor object is
     * marked as invalid.
     */
    void operator*=(fd_t& descriptor);

    /**
     * Assing file descriptor from system descriptor.
     * @param descriptor to dup from.
     */
    void operator=(fd_t descriptor);

    /**
     * Get the native system descriptor handle of the file descriptor.
     * @return native os descriptor.
     */
    inline fd_t handle(void) const
        {return fd;};

    /**
     * Set with external descriptor.  Closes existing file if open.
     * @param descriptor of open file.
     */
    void set(fd_t descriptor);

    /**
     * Release descriptor, do not close.
     * @return handle being released.
     */
    fd_t release(void);

    /**
     * Set the position of a file descriptor.
     * @param offset from start of file or "end" to append.
     * @return error number or 0 on success.
     */
    int seek(offset_t offset);

    /**
     * Drop cached data from start of file.
     * @param size of region to drop or until end of file.
     * @return error number or 0 on success.
     */
    int drop(offset_t size = 0);

    /**
     * See if current file stream is a tty device.
     * @return true if device.
     */
    bool is_tty(void);

    /**
     * See if the file handle is a tty device.
     * @return true if device.
     */
    static bool is_tty(fd_t fd);

    /**
     * Read data from descriptor or scan directory.
     * @param buffer to read into.
     * @param count of bytes to read.
     * @return bytes transferred, -1 if error.
     */
    ssize_t read(void *buffer, size_t count);

    /**
     * Write data to descriptor.
     * @param buffer to write from.
     * @param count of bytes to write.
     * @return bytes transferred, -1 if error.
     */
    ssize_t write(const void *buffer, size_t count);

    /**
     * Get status of open descriptor.
     * @param buffer to save status info in.
     * @return error number or 0 on success.
     */
    int info(fileinfo_t *buffer);

    /**
     * Truncate file to specified length.  The file pointer is positioned
     * to the new end of file.
     * @param offset to truncate to.
     * @return true if truncate successful.
     */
    int trunc(offset_t offset);

    /**
     * Commit changes to the filesystem.
     * @return error number or 0 on success.
     */
    int sync(void);

    /**
     * Set directory prefix (chdir).
     * @param path to change to.
     * @return error number or 0 on success.
     */
    static int prefix(const char *path);

    /**
     * Get current directory prefix (pwd).
     * @param path to save directory into.
     * @param size of path we can save.
     * @return error number or 0 on success.
     */
    static int prefix(char *path, size_t size);

    static string_t prefix(void);

    /**
     * Stat a file.
     * @param path of file to stat.
     * @param buffer to save stat info.
     * @return error number or 0 on success.
     */
    static int info(const char *path, fileinfo_t *buffer);

    /**
     * Erase (remove) a file only.
     * @param path of file.
     * @return error number or 0 on success.
     */
    static int erase(const char *path);

    /**
     * Copy a file.
     * @param source file.
     * @param target file.
     * @param size of buffer.
     * @return error number or 0 on success.
     */
    static int copy(const char *source, const char *target, size_t size = 1024);

    /**
     * Rename a file.
     * @param oldpath to rename from.
     * @param newpath to rename to.
     * @return error number or 0 on success.
     */
    static int rename(const char *oldpath, const char *newpath);

    /**
     * Change file access mode.
     * @param path to change.
     * @param value of mode to assign.
     * @return error number or 0 on success.
     */
    static int mode(const char *path, unsigned value);

    /**
     * Test if path exists.
     * @param path to test.
     * @return if true.
     */
    static bool is_exists(const char *path);

    /**
     * Test if path readable.
     * @param path to test.
     * @return if true.
     */
    static bool is_readable(const char *path);

    /**
     * Test if path writable.
     * @param path to test.
     * @return if true.
     */
    static bool is_writable(const char *path);

    /**
     * Test if path is executable.
     * @param path to test.
     * @return if true.
     */
    static bool is_executable(const char *path);

    /**
     * Test if path is a file.
     * @param path to test.
     * @return true if exists and is file.
     */
    static bool is_file(const char *path);

    /**
     * Test if path is a directory.
     * @param path to test.
     * @return true if exists and is directory.
     */
    static bool is_dir(const char *path);

    /**
     * Test if path is a symlink.
     * @param path to test.
     * @return true if exists and is symlink.
     */
    static bool is_link(const char *path);

    /**
     * Test if path is a device path.
     * @param path to test.
     * @return true of is a device path.
     */
    static bool is_device(const char *path);

    /**
     * Test if path is a hidden file.
     * @param path to test.
     * @return true if exists and is hidden.
     */
    static bool is_hidden(const char *path);

    /**
     * Open a file or directory.
     * @param path of file to open.
     * @param access mode of descriptor.
     */
    void open(const char *path, access_t access);

    /**
     * Assign descriptor directly.
     * @param descriptor to assign.
     */
    inline void assign(fd_t descriptor)
        {close(); fd = descriptor;};

    /**
     * Assign a descriptor directly.
     * @param object to assign descriptor to.
     * @param descriptor to assign.
     */
    inline static void assign(fsys& object, fd_t descriptor)
        {object.close(); object.fd = descriptor;};

    /**
     * Open a file descriptor directly.
     * @param path of file to create.
     * @param access mode of descriptor.
     * @param mode of file if created.
     */
    void open(const char *path, unsigned mode, access_t access);

    /**
     * Remove a symbolic link explicitly.  Other kinds of files are also
     * deleted.  This should be used when uncertain about symlinks requiring
     * special support.
     * @param path to remove.
     * @return error number or 0 on success.
     */
    static int unlink(const char *path);

    /**
     * Create a symbolic link.
     * @param path to create.
     * @param target of link.
     * @return error number or 0 on success.
     */
    static int link(const char *path, const char *target);

    /**
     * Create a hard link.
     * @param path to create link to.
     * @param target of link.
     * @return error number or 0 on success.
     */
    static int hardlink(const char *path, const char *target);

    /**
     * Read a symbolic link to get it's target.
     * @param path of link.
     * @param buffer to save target into.
     * @param size of buffer.
     */
    static int linkinfo(const char *path, char *buffer, size_t size);

    /**
     * Close a fsys resource.
     * @return error code as needed.
     */
    int close(void);

    /**
     * Get last error.
     * @return error number.
     */
    inline int err(void) const
        {return error;}

    /**
     * Direct means to open a read-only file path and return a descriptor.
     * @param path to open.
     * @return descriptor on success, invalid handle on failure.
     */
    static fd_t input(const char *path);

    /**
     * Direct means to create or access a writable path and return descriptor.
     * @param path to create.
     * @return descriptor on success, invalid handle on failure.
     */
    static fd_t output(const char *path);

    /**
     * Direct means to create or append a writable path and return descriptor.
     * @param path to create.
     * @return descriptor on success, invalid handle on failure.
     */
    static fd_t append(const char *path);

    /**
     * Release a file descriptor.
     * @param descriptor to release.
     */
    static void release(fd_t descriptor);

    /**
     * Create pipe.  These are created inheritable by default.
     * @param input descriptor.
     * @param output descriptor.
     * @param size of buffer if supported.
     * @return 0 or error code.
     */
    static int pipe(fd_t& input, fd_t& output, size_t size = 0);

    /**
     * Changle inheritable handle.  On windows this is done by creating a
     * duplicate handle and then closing the original.  Elsewhere this
     * is done simply by setting flags.
     * @param descriptor to modify.
     * @param enable child process inheritence.
     * @return 0 on success, error on failure.
     */
    static int inherit(fd_t& descriptor, bool enable);

    /**
     * Create inheritable /dev/null handle.
     * @return null device handle.
     */
    static fd_t null(void);

    /**
     * Load a library into memory.
     * @param path to plugin.
     * @return 0 on success, else error.
     */
    static int load(const char *path);

    /**
     * Execute a process and get exit code.
     * @param path to execute.
     * @param argv list.
     * @param optional env.
     * @return exit code.
     */
    static int exec(const char *path, char **argv, char **envp = NULL);

    static inline bool is_file(struct stat *inode)
        {return S_ISREG(inode->st_mode);}

    static inline bool is_dir(struct stat *inode)
        {return S_ISDIR(inode->st_mode);}

    static inline bool is_link(struct stat *inode)
        {return S_ISLNK(inode->st_mode);}

    static inline bool is_dev(struct stat *inode)
        {return S_ISBLK(inode->st_mode) || S_ISCHR(inode->st_mode);}

    static inline bool is_char(struct stat *inode)
        {return S_ISCHR(inode->st_mode);}

    static inline bool is_disk(struct stat *inode)
        {return S_ISBLK(inode->st_mode);}

    static inline bool is_sys(struct stat *inode)
        {return S_ISSOCK(inode->st_mode) || S_ISFIFO(inode->st_mode);}
};

/**
 * Convenience class for library plugins.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT dso
{
private:
    friend class fsys;

#ifdef  _MSWINDOWS_
    HINSTANCE   ptr;
#else
    void    *ptr;
#endif
    int     error;

public:
#ifdef  _MSWINDOWS_
    typedef int (FAR WINAPI *addr_t)();
#else
    typedef void *addr_t;
#endif

    /**
     * Create dso object for use by load functions.
     */
    dso();

    /**
     * Create and map a dso object.
     * @param path of library to map.
     */
    dso(const char *path);

    /**
     * Destroy dso and release library.
     */
    ~dso();

    /**
     * Map library object with library.
     * @param name of library to load.
     */
    void map(const char *path);

    /**
     * Release loaded library.
     */
    void release(void);

    /**
     * Find symbol in loaded module.
     * @param module to search.
     * @param symbol to search for.
     * @return address of symbol or NULL if not found.
     */
    addr_t find(const char *symbol) const;

    inline int err(void) const
        {return error;}

    inline addr_t operator[](const char *symbol) const
        {return find(symbol);}

    inline addr_t operator()(const char *symbol) const
        {return find(symbol);}

    inline operator bool()
        {return ptr != NULL;}

    inline bool operator!()
        {return ptr == NULL;}
};

/**
 * Convenience class for directories.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT dir : private fsys
{
private:
#ifdef  _MSWINDOWS_
    WIN32_FIND_DATA *ptr;
    HINSTANCE   mem;
#else
    void    *ptr;
#endif

public:
    /**
     * Construct and open a directory path.
     * @param path of directory.
     */
    dir(const char *path);

    /**
     * Construct an unopened directory.
     */
    dir();

    /**
     * Close and release directory.
     */
    ~dir();

    /**
     * Simple direct method to create a directory.
     * @param path of directory to create.
     * @param mode of directory.
     * @return error number or 0 on success.
     */
    static int create(const char *path, unsigned mode);

    /**
     * Remove an empty directory.
     * @param path of directory.
     * @return error number or 0 on success.
     */
    static int remove(const char *path);

    /**
     * Open a directory path for reading.
     * @param path to open.
     */
    void open(const char *path);

    /**
     * Read data from directory.
     * @param buffer to read into.
     * @param count of bytes to read.
     * @return bytes transferred, -1 if error.
     */
    ssize_t read(char *buffer, size_t count);

    /**
     * Close and release directory object.
     */
    void close(void);

    inline int err(void) const
        {return fsys::err();}

    inline void reset(void)
        {fsys::reset();}

    /**
     * Test if file descriptor is open.
     * @return true if open.
     */
    inline operator bool() const
        {return ptr != NULL;};

    /**
     * Test if file descriptor is closed.
     * @return true if closed.
     */
    inline bool operator!() const
        {return ptr == NULL;};
};

/**
 * Convience type for fsys.
 */
typedef fsys fsys_t;

typedef dir dir_t;

typedef dso dso_t;

inline bool is_exists(const char *path)
    {return fsys::is_exists(path);}

inline bool is_readable(const char *path)
    {return fsys::is_readable(path);}

inline bool is_writable(const char *path)
    {return fsys::is_writable(path);}

inline bool is_executable(const char *path)
    {return fsys::is_executable(path);}

inline bool is_file(const char *path)
    {return fsys::is_file(path);}

inline bool is_dir(const char *path)
    {return fsys::is_dir(path);}

inline bool is_link(const char *path)
    {return fsys::is_link(path);}

inline bool is_device(const char *path)
    {return fsys::is_device(path);}

END_NAMESPACE

#endif

