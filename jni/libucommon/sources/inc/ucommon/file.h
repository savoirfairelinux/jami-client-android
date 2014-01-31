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
 * Adaption of C runtime FILE processing.
 * @file ucommon/file.h
 */

#ifndef _UCOMMON_FILE_H_
#define _UCOMMON_FILE_H_

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

#ifndef _UCOMMON_FSYS_H_
#include <ucommon/fsys.h>
#endif

#include <stdio.h>

NAMESPACE_UCOMMON

/**
 * Access standard files through character protocol.  This can also be
 * used as an alternative means to access files that manages file pointers.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT file : public CharacterProtocol
{
private:
    FILE *fp;
#ifdef _MSWINDOWS_
    HANDLE pid;
#else
    pid_t pid;
#endif
    char *tmp;

    int _putch(int code);

    int _getch(void);

public:
    typedef ::fpos_t bookmark_t;

    static file cin, cout, cerr;

    /**
     * Construct a file from an existing FILE pointer.
     * @param file to use.
     */
    file(FILE *file);

    /**
     * Construct an open file based on a path and mode.
     * @param path of file to open.
     * @param mode of file.
     * @param size of buffer, 0 = none, 1 = line mode, 2 = default
     */
    file(const char *path, const char *mode, size_t size = 2);

    /**
     * Construct an open file based on a pipe.
     * @param path of file to pipe.
     * @param argv of executable.
     * @param mode of file.
     * @param envp to give executable.
     */
    file(const char *path, char **argv, const char *mode, char **envp = NULL);

    /**
     * Construct an unopened file.
     */
    file();

    /**
     * Destroy object and close associated file.
     */
    ~file();

    /**
     * Test if file is opened.
     * @return true if opened.
     */
    inline operator bool()
        {return fp != NULL;}

    /**
     * Test if file is not opened.
     * @return true if not opened.
     */
    inline bool operator !()
        {return fp == NULL;}

    inline operator FILE *()
        {return fp;}

    /**
     * Open file path.  If a file is already opened, it is closed.
     * @param path of file to open.
     * @param mode of file to open.
     * @param size of buffering, 0 = none, 1 = line mode.
     */
    void open(const char *path, const char *mode, size_t size = 2);

    /**
     * Open an executable path.
     * @param path of executable.
     * @param argv to pass to executable.
     * @param mode of pipe (only "r" and "w" are valid).
     */
    void open(const char *path, char **argv, const char *mode, char **envp = NULL);

    /**
     * Close an open file.
     * @return process exit code if pipe.
     */
    int close(void);

    /**
     * Clear error state.
     */
    inline void clear(void)
        {if(fp) clearerr(fp);}

    /**
     * Check if file is good, no error or eof...
     * @return bool if file stream is good.
     */
    bool good(void);

    /**
     * Cancel pipe and close file.
     * @return process exit code if pipe.
     */
    int cancel(void);

    inline size_t put(const void *data, size_t size)
        { return fp == NULL ? 0 : fwrite(data, 1, size, fp);}

    inline size_t get(void *data, size_t size)
        { return fp == NULL ? 0 : fread(data, 1, size, fp);}

    inline int put(char value)
        { return fp == NULL ? EOF : fputc(value, fp);}

    inline int get(void)
        { return fp == NULL ? EOF : fgetc(fp);}

    inline int push(char value)
        { return fp == NULL ? EOF : ungetc(value, fp);}

    inline int puts(const char *data)
        { return fp == NULL ? 0 : fputs(data, fp);}

    inline char *gets(char *data, size_t size)
        { return fp == NULL ? NULL : fgets(data, size, fp);}

    template<typename T> inline size_t read(T* data, size_t count)
        { return fp == NULL ? 0 : fread(data, sizeof(T), count, fp);}

    template<typename T> inline size_t write(const T* data, size_t count)
        { return fp == NULL ? 0 : fwrite(data, sizeof(T), count, fp);}

    template<typename T> inline size_t read(T& data)
        { return fp == NULL ? 0 : fread(data, sizeof(T), 1, fp);}

    template<typename T> inline size_t write(const T& data)
        { return fp == NULL ? 0 : fwrite(data, sizeof(T), 1, fp);}

    inline void get(bookmark_t& pos)
        { if(fp) fsetpos(fp, &pos);}

    inline void set(bookmark_t& pos)
        { if(fp) fgetpos(fp, &pos);}

    int err(void) const;

    bool eof(void) const;

    template<typename T> inline void offset(long pos)
        {if(fp) fseek(fp, sizeof(const T) * pos, SEEK_CUR);}

    inline void seek(long offset)
        {if(fp) fseek(fp, offset, SEEK_SET);}

    inline void move(long offset)
        {if(fp) fseek(fp, offset, SEEK_CUR);}

    inline void append(void)
        {if (fp) fseek(fp, 0l, SEEK_END);}

    inline void rewind(void)
        {if(fp) ::rewind(fp);}

    inline void flush(void)
        {if(fp) ::fflush(fp);}

    size_t printf(const char *format, ...) __PRINTF(2, 3);

    size_t scanf(const char *format, ...) __SCANF(2, 3);

    bool is_tty(void) const;
};

/**
 * Convience type for file.
 */
typedef file file_t;

END_NAMESPACE

#endif

