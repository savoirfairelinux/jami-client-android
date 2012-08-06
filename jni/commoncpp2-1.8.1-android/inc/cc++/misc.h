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
 * @file misc.h
 * @short Memory management, configuration keydata objects and string
 * tokenizer.
 **/

#ifndef CCXX_MISC_H_
#define CCXX_MISC_H_

#ifndef CCXX_MISSING_H_
#include <cc++/missing.h>
#endif

#ifndef CCXX_THREAD_H_
#include <cc++/thread.h>
#endif

#define KEYDATA_INDEX_SIZE  97
#define KEYDATA_PAGER_SIZE  512

#if defined(PATH_MAX)
#if PATH_MAX > 512
#define KEYDATA_PATH_SIZE   512
#else
#define KEYDATA_PATH_SIZE   PATH_MAX
#endif
#else
#define KEYDATA_PATH_SIZE   256
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

class __EXPORT Runlist;
class __EXPORT Runable;

/**
 * The memory pager is used to allocate cumulative memory pages for
 * storing object specific "persistant" data that is presumed to persist
 * during the life of a given derived object.  When the object is
 * destroyed, all accumulated data is automatically purged.
 *
 * There are a number of odd and specialized utility classes found in Common
 * C++.  The most common of these is the "MemPager" class.  This is basically
 * a class to enable page-grouped "cumulative" memory allocation; all
 * accumulated allocations are dropped during the destructor.  This class has
 * found it's way in a lot of other utility classes in Common C++.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Accumulative object memory allocator.
 */
class __EXPORT MemPager
{
private:
    friend class String;
    friend class MemPagerObject;

    size_t pagesize;
    unsigned int pages;

    struct _page {
        struct _page *next;
        size_t used;
    } *page;

protected:
    /**
     * Allocate first workspace from paged memory.  This method
     * scans all currently allocated blocks for available space
     * before adding new pages and hence is both slower and more
     * efficient.
     *
     * @param size size of memory to allocate.
     * @return pointer to allocated memory.
     */
    virtual void* first(size_t size);

    /**
     * Allocate memory from either the currently active page, or
     * allocate a new page for the object.
     *
     * @param size size of memory to allocate.
     * @return pointer to allocated memory.
     */
    virtual void* alloc(size_t size);

    /**
     * Allocate a string from the memory pager pool and copy the
     * string into it's new memory area.  This method allocates
     * memory by first searching for an available page, and then
     * allocating a new page if no space is found.
     *
     * @param str string to allocate and copy into paged memory pool.
     * @return copy of string from allocated memory.
     */
    char* first(char *str);

    /**
     * Allocate a string from the memory pager pool and copy the
     * string inti it's new memory area.  This checks only the
     * last active page for available space before allocating a
     * new page.
     *
     * @param str string to allocate and copy into paged memory pool.
     * @return copy of string from allocated memory.
     */
    char* alloc(const char *str);

    /**
     * Create a paged memory pool for cumulative storage.  This
     * pool allocates memory in fixed "pagesize" chunks.  Ideal
     * performance is achived when the pool size matches the
     * system page size.  This pool can only exist in derived
     * objects.
     *
     * @param pagesize page size to allocate chunks.
     */
    MemPager(size_t pagesize = 4096);

    /**
     * purge the current memory pool.
     */
    void purge(void);

    /**
     * Clean for memory cleanup before exiting.
     */
    void clean(void);

    /**
     * Delete the memory pool and all allocated memory.
     */
    virtual ~MemPager();

public:
    /**
     * Return the total number of pages that have been allocated
     * for this memory pool.
     *
     * @return number of pages allocated.
     */
    inline int getPages(void)
        {return pages;};
};

/**
 * The StackPager provides a repository to stash and retrieve working
 * data in last-in-first-out order.  The use of a mempager to support
 * it's operation allows storage of arbitrary sized objects with no
 * fixed limit.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short last in first out object pager.
 */
class __EXPORT StackPager : protected MemPager
{
private:
    typedef struct frame {
        struct frame *next;
        char data[1];
    }   frame_t;

    frame_t *stack;

public:
    /**
     * Create a lifo pager as a mempager.
     *
     * @param pagesize for memory allocation
     */
    StackPager(size_t pagesize);

    /**
     * Push an arbitrary object onto the stack.
     *
     * @return stack memory location.
     * @param object pointer to data
     * @param size of data.
     */
    void *push(const void *object, size_t size);

    /**
     * Push a string onto the stack.
     *
     * @return stack memory location.
     * @param string pointer.
     */
    void *push(const char *string);

    /**
     * Retrieve next object from stack.
     *
     * @return object.
     */
    void *pull(void);

    /**
     * Purge the stack of all objects and memory allocations.
     */
    void purge(void);
};

/**
 * The shared mempager uses a mutex to protect key access methods.
 * This class is used when a mempager will be shared by multiple
 * threads.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short mutex protected memory pager.
 */
class __EXPORT SharedMemPager : public MemPager, public Mutex
{
protected:
    /**
     * Create a mempager mutex pool.
     *
     * @param pagesize page size for allocation.
     * @param name a name for the pool.
     */
    SharedMemPager(size_t pagesize = 4096, const char *name = NULL);

    /**
     * Purge the memory pool while locked.
     */
    void purge(void);

    /**
     * Get the first memory page after locking.
     *
     * @return allocated memory space.
     * @param size of request.
     */
    void* first(size_t size);

    /**
     * Get the last memory page after locking.
     *
     * @return allocated memory space.
     * @param size of request.
     */
    void* alloc(size_t size);
};

__EXPORT void endKeydata(void);

/**
 * Keydata objects are used to load and hold "configuration" data for
 * a given application.
 *
 * This class is used to load and then hold "<code>keyword = value</code>" pairs parsed from a text
 * based "config" file that has been divided into "<code>[sections]</code>". The syntax is:
 *
 * <code><pre>
 * [section_name]
 * key1=value1
 * key2=value2</pre></code>
 *
 * Essentially, the "path" is a "keypath" into a theoretical namespace of key
 * pairs, hence one does not use "real" filepaths that may be OS dependent. The "<code>/</code>" path refers
 * to "<code>/etc</code>" prefixed (on UNIX) directories and this is processed within the constructor. It
 * could refer to the <code>/config</code> prefix on QNX, or even, gasp, a "<code>C:\WINDOWS</code>". Hence, a keypath of
 * "<code>/bayonne.d/vmhost/smtp</code>" actually resolves to a "<code>/etc/bayonne.d/vmhost.conf</code>" and loads key
 * value pairs from the <code>[smtp]</code> section of that <code>.conf</code> file.
 *
 * Similarly, something like "<code>~bayonne/smtp</code>" path refers to a "<code>~/.bayonnerc</code>" and loads key pairs
 * from the <code>[smtp]</code> section. This coercion occurs before the name is passed to the open call.
 *
 * I actually use derived keydata based classes as global initialized objects, and they hence
 * automatically load and parse config file entries even before "main" has started.
 *
 * Keydata can hold multiple values for the same key pair.  This can
 * occur either from storing a "list" of data items in a config file,
 * or when overlaying multiple config sources (such as <code>/etc/....conf</code> and
 * <code>~/.confrc</code> segments) into a single object.  The keys are stored as
 * cumulative (read-only/replacable) config values under a hash index
 * system for quick retrieval.
 *
 * Keydata can
 * also load a table of "initialization" values for keyword pairs that were
 * not found in the external file.
 *
 * One typically derives an application specific keydata class to load a
 * specific portion of a known config file and initialize it's values.  One
 * can then declare a global instance of these objects and have
 * configuration data initialized automatically as the executable is loaded.
 *
 * Hence, if I have a "[paths]" section in a "<code>/etc/server.conf?</code>" file, I might
 * define something like:
 *
 * <code><pre>
 * class KeyPaths : public Keydata
 * {
 *   public:
 *     KeyPaths() : Keydata("/server/paths")
 *     {
 *       static Keydata::Define *defvalues = {
 *    {"datafiles", "/var/server"},
 *    {NULL, NULL}};
 *
 *       // override with [paths] from "~/.serverrc" if avail.
 *
 *       load("~server/paths");
 *       load(defvalues);
 *     }
 * };
 *
 * KeyPaths keypaths;
 * </pre></code>
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short load text configuration files into keyword pairs.
 */
class __EXPORT Keydata : protected MemPager
{
public:
#ifdef  CCXX_PACKED
#pragma pack(1)
#endif

    struct Keyval {
        Keyval *next;
        char val[1];
    };

    struct Keysym {
        Keysym *next;
        Keyval *data;
        const char **list;
        short count;
        char sym[1];
    };

    struct Define {
        const char *keyword;
        const char *value;
    };

#ifdef  CCXX_PACKED
#pragma pack()
#endif

private:
    static std::ifstream *cfgFile;
    static char lastpath[KEYDATA_PATH_SIZE + 1];
    static int count;
    static int sequence;

    int link;

    Keysym *keys[KEYDATA_INDEX_SIZE];

    /**
     * Compute a hash key signature id for a symbol name.
     *
     * @return key signature index path.
     * @param sym symbol name.
     */
    unsigned getIndex(const char *sym);

protected:
    Keysym* getSymbol(const char *sym, bool create);

public:
    /**
     * Load additional key values into the currrent object from
     * the specfied config source (a config file/section pair).
     * These values will overlay the current keywords when matches
     * are found.  This can be used typically in a derived config
     * object class constructor to first load a <code>/etc</code> section, and
     * then load a matching user specific entry from <code>~/.</code> to override
     * default system values with user specific keyword values.
     *
     * @param keypath (filepath/section)
     */
    void load(const char *keypath);

    /**
     * Load additional key values into the currrent object from
     * the specfied config source (a config file/section pair).
     * These values will overlay the current keywords when matches
     * are found.  This can be used typically in a derived config
     * object class constructor to first load a <code>/etc</code> section, and
     * then load a matching user specific entry from <code>~/.</code> to override
     * default system values with user specific keyword values.
     * This varient puts a prefix in front of the key name.
     *
     * @param prefix
     * @param keypath (filepath/section)
     */
    void loadPrefix(const char *prefix, const char *keypath);

    /**
     * Load additional keys into the current object using a real
     * filename that is directly passed rather than a computed key
     * path.  This also uses a [keys] section as passed to the object.
     *
     * @param filepath to load from
     * @param keys section to parse from, or NULL to parse from head
     * @param pre optional key prefix
     */
    void loadFile(const char *filepath, const char *keys = NULL, const char *pre = NULL);

    /**
     * Load default keywords into the current object.  This only
     * loads keyword entries which have not already been defined
     * to reduce memory usage.  This form of Load is also commonly
     * used in the constructor of a derived Keydata class.
     *
     * @param pairs list of NULL terminated default keyword/value pairs.
     */
    void load(Define *pairs);

    /**
     * Create an empty key data object.
     */
    Keydata();

    /**
     * Create a new key data object and use "Load" method to load an
     * initial config file section into it.
     *
     * @param keypath (filepath/section)
     * specifies the home path.
     */
    Keydata(const char *keypath);

    /**
     * Alternate constructor can take a define list and an optional
     * pathfile to parse.
     *
     * @param pairs of keyword values from a define list
     * @param keypath of optional file and section to load from
     */
    Keydata(Define *pairs, const char *keypath = NULL);

    /**
     * Destroy the keydata object and all allocated memory.  This
     * may also clear the "cache" file stream if no other keydata
     * objects currently reference it.
     */
    virtual ~Keydata();

    /**
     * Unlink the keydata object from the cache file stream.  This
     * should be used if you plan to keepa Keydata object after it
     * is loaded once all keydata objects have been loaded, otherwise
     * the cfgFile stream will remain open.  You can also use
     * endKeydata().
     */
    void unlink(void);

    /**
     * Get a count of the number of data "values" that is associated
     * with a specific keyword.  Each value is from an accumulation of
     * "<code>load()</code>" requests.
     *
     * @param sym keyword symbol name.
     * @return count of values associated with keyword.
     */
    int getCount(const char *sym);

    /**
     * Get the first data value for a given keyword.  This will
     * typically be the <code>/etc</code> set global default.
     *
     * @param sym keyword symbol name.
     * @return first set value for this symbol.
     */
    const char* getFirst(const char *sym);

    /**
     * Get the last (most recently set) value for a given keyword.
     * This is typically the value actually used.
     *
     * @param sym keyword symbol name.
     * @return last set value for this symbol.
     */
    const char* getLast(const char *sym);

    /**
     * Find if a given key exists.
     *
     * @param sym keyword to find.
     * @return true if exists.
     */
    bool isKey(const char *sym);

    /**
     * Get a string value, with an optional default if missing.
     *
     * @param sym keyword name.
     * @param default if not present.
     * @return string value of key.
     */
    const char *getString(const char *sym, const char *def = NULL);

    /**
     * Get a long value, with an optional default if missing.
     *
     * @param sym keyword name.
     * @param default if not present.
     * @return long value of key.
     */
    long getLong(const char *sym, long def = 0);

    /**
     * Get a bool value.
     *
     * @param sym keyword name.
     * @return true or false.
     */
    bool getBool(const char *key);

    /**
     * Get a floating value.
     *
     * @param sym keyword name.
     * @param default if not set.
     * @return value of key.
     */
    double getDouble(const char *key, double def = 0.);

    /**
     * Get an index array of ALL keywords that are stored by the
     * current keydata object.
     *
     * @return number of keywords found.
     * @param data pointer of array to hold keyword strings.
     * @param max number of entries the array can hold.
     */
    unsigned getIndex(char **data, unsigned max);

    /**
     * Get the count of keyword indexes that are actually available
     * so one can allocate a table to receive getIndex.
     *
     * @return number of keywords found.
     */
    unsigned getCount(void);

    /**
     * Set (replace) the value of a given keyword.  This new value
     * will become the value returned from getLast(), while the
     * prior value will still be stored and found from <code>getList()</code>.
     *
     * @param sym keyword name to set.
     * @param data string to store for the keyword.
     */
    void setValue(const char *sym, const char *data);

    /**
     * Return a list of all values set for the given keyword
     * returned in order.
     *
     * @return list pointer of array holding all keyword values.
     * @param sym keyword name to fetch.
     */
    const char * const* getList(const char *sym);

    /**
     * Clear all values associated with a given keyword.  This does
     * not de-allocate the keyword from memory, however.
     *
     * @return keyword name to clear.
     */
    void clrValue(const char *sym);

    /**
     * A convient notation for accessing the keydata as an associative
     * array of keyword/value pairs through the [] operator.
     */
    inline const char *operator[](const char *keyword)
        {return getLast(keyword);};

    /**
     * static member to end keydata i/o allocations.
     */
    static void end(void);

    /**
     * Shutdown the file stream cache.  This should be used before
     * detaching a deamon, <code>exec()</code>, <code>fork()</code>, etc.
     */
    friend inline void endKeydata(void)
        {Keydata::end();};
};

/**
 * This class is used to create derived classes which are constructed
 * within a memory pager pool.
 *
 * @short create objects in a memory pager.
 * @author David Sugar <dyfet@ostel.com>
 */
class __EXPORT MemPagerObject
{
public:
    /**
     * Allocate memory from a memory pager.
     *
     * @param size of new passed from operator.
     * @param pager to allocate from.
     */
    inline void *operator new(size_t size, MemPager &pager)
        {return pager.alloc(size);};

    /**
     * Allocate array from a memory pager.
     *
     * @param size of new passed from operator.
     * @param pager to allocate from.
     */
    inline void *operator new[](size_t size, MemPager &pager)
        {return pager.alloc(size);};

    /**
     * Mempager delete does nothing; the pool purges.
     */
    inline void operator delete(void *) {};

    /**
     * Array mempager delete does nothing; the pool purges.
     */
    inline void operator delete[](void *) {};
};

/**
 * This class is used to associate (object) pointers with named strings.
 * A virtual is used to allocate memory which can be overriden in the
 * derived class.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short associate names with pointers.
 */
class __EXPORT Assoc
{
private:
    struct entry {
        const char *id;
        entry *next;
        void *data;
    };

    entry *entries[KEYDATA_INDEX_SIZE];

protected:
    Assoc();
    virtual ~Assoc();

    void clear(void);

    virtual void *getMemory(size_t size) = 0;

public:
    void *getPointer(const char *id) const;
    void setPointer(const char *id, void *data);
};

/**
 * A runlist is used to restrict concurrent exection to a limited set
 * of concurrent sessions, much like a semaphore.  However, the runlist
 * differs in that it notifies objects when they become ready to run,
 * rather than requiring them to wait and "block" for the semaphore
 * count to become low enough to continue.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short list of runable objects.
 */
class __EXPORT Runlist : public Mutex
{
private:
    Runable *first, *last;

protected:
    unsigned limit, used;
    void check(void);

public:
    /**
     * Create a new runlist with a specified limit.
     *
     * @param count limit before wait queuing.
     */
    Runlist(unsigned count = 1);

    /**
     * Add a runable object to this runlist.  If the number of
     * entries running is below the limit, then add returns true
     * otherwise the entry is added to the list.
     *
     * @return true if immediately ready to run
     * @param run pointer to runable object.
     */
    bool add(Runable *run);

    /**
     * Remove a runable object from the wait list or notify when
     * it is done running so that the used count can be decremented.
     *
     * @param run pointer to runable object.
     */
    void del(Runable *run);

    /**
     * Set the limit.
     *
     * @param limit to use.
     */
    void set(unsigned limit);
};

/**
 * A container for objects that can be queued against a runlist.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short runable object with notify when ready.
 */
class __EXPORT Runable
{
private:
    friend class Runlist;
    Runlist *list;
    Runable *next, *prev;

protected:
    Runable();
    virtual ~Runable();

    /**
     * Method handler that is invoked when a wait-listed object
     * becomes ready to run.
     */
    virtual void ready(void) = 0;

public:
    /**
     * Start the object against a run list.
     *
     * @return true if immediately available to run.
     * @param list to start under.
     */
    bool starting(Runlist *list);

    /**
     * Stop the object, called when stopping or ready completes.
     * May also be used for a task that has not yet started to
     * remove it from the wait list.
     */
    void stoping(void);
};

#ifdef  CCXX_NAMESPACES
}
#endif

#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
