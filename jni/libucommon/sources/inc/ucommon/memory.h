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
 * Private heaps, pools, and associations.
 * Private heaps often can reduce locking contention in threaded applications
 * since they do not require using the global "malloc" function.  Private
 * heaps also can be used as auto-release heaps, where all memory allocated
 * and handled out for small objects can be automatically released all at once.
 * Pager pools are used to optimize system allocation around page boundaries.
 * Associations allow private memory to be tagged and found by string
 * identifiers.
 * @file ucommon/memory.h
 */

#ifndef _UCOMMON_MEMORY_H_
#define _UCOMMON_MEMORY_H_

#ifndef _UCOMMON_CONFIG_H_
#include <ucommon/platform.h>
#endif

#ifndef _UCOMMON_PROTOCOLS_H_
#include <ucommon/protocols.h>
#endif

#ifndef  _UCOMMON_LINKED_H_
#include <ucommon/linked.h>
#endif

#ifndef _UCOMMON_STRING_H_
#include <ucommon/string.h>
#endif

NAMESPACE_UCOMMON

class PagerPool;

/**
 * A memory protocol pager for private heap manager.  This is used to allocate
 * in an optimized manner, as it assumes no mutex locks are held or used as
 * part of it's own internal processing.  It also is designed for optimized
 * performance.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT memalloc : public MemoryProtocol
{
private:
    friend class bufpager;

    size_t pagesize, align;
    unsigned count;

    typedef struct mempage {
        struct mempage *next;
        union {
            void *memalign;
            unsigned used;
        };
    }   page_t;

    page_t *page;

protected:
    unsigned limit;

    /**
     * Acquire a new page from the heap.  This is mostly used internally.
     * @return page structure of the newly acquired memory page.
     */
    page_t *pager(void);

    /**
     * Report runtime memory exhaustion.
     */
    virtual void fault(void) const;

public:
    /**
     * Construct a memory pager.
     * @param page size to use or 0 for OS allocation size.
     */
    memalloc(size_t page = 0);

    /**
     * Destroy a memory pager.  Release all pages back to the heap at once.
     */
    virtual ~memalloc();

    /**
     * Get the number of pages that have been allocated from the real heap.
     * @return pages allocated from heap.
     */
    inline unsigned pages(void) const
        {return count;}

    /**
     * Get the maximum number of pages that are permitted.  One can use a
     * derived class to set and enforce a maximum limit to the number of
     * pages that will be allocated from the real heap.  This is often used
     * to detect and bring down apps that are leaking.
     * @return page allocation limit.
     */
    inline unsigned max(void) const
        {return limit;}

    /**
     * Get the size of a memory page.
     * @return size of each pager heap allocation.
     */
    inline unsigned size(void) const
        {return pagesize;}

    /**
     * Determine fragmentation level of acquired heap pages.  This is
     * represented as an average % utilization (0-100) and represents the
     * used portion of each allocated heap page verse the page size.  Since
     * requests that cannot fit on an already allocated page are moved into
     * a new page, there is some unusable space left over at the end of the
     * page.  When utilization approaches 100, this is good.  A low utilization
     * may suggest a larger page size should be used.
     * @return pager utilization.
     */
    unsigned utilization(void) const;

    /**
     * Purge all allocated memory and heap pages immediately.
     */
    void purge(void);

    /**
     * Allocate memory from the pager heap.  The size of the request must be
     * less than the size of the memory page used.  This implements the
     * memory protocol allocation method.
     * @param size of memory request.
     * @return allocated memory or NULL if not possible.
     */
    virtual void *_alloc(size_t size);
};

/**
 * A managed private heap for small allocations.  This is used to allocate
 * a large number of small objects from a paged heap as needed and to then
 * release them together all at once.  This pattern has significantly less
 * overhead than using malloc and offers less locking contention since the
 * memory pager can also have it's own mutex.  Pager pool allocated memory
 * is always aligned to the optimal data size for the cpu bus and pages are
 * themselves created from memory aligned allocations.  A page size for a
 * memory pager should be some multiple of the OS paging size.
 *
 * The mempager uses a strategy of allocating fixed size pages as needed
 * from the real heap and allocating objects from these pages as needed.
 * A new page is allocated from the real heap when there is insufficient
 * space in the existing page to complete a request.  The largest single
 * memory allocation one can make is restricted by the page size used, and
 * it is best to allocate objects a significant fraction smaller than the
 * page size, as fragmentation occurs at the end of pages when there is
 * insufficient space in the current page to complete a request.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT mempager : public memalloc, public LockingProtocol
{
private:
    pthread_mutex_t mutex;

protected:
    /**
     * Lock the memory pager mutex.  It will be more efficient to lock
     * the pager and then call the locked allocator than using alloc which
     * separately locks and unlocks for each request when a large number of
     * allocation requests are being batched together.
     */
    virtual void _lock(void);

    /**
     * Unlock the memory pager mutex.
     */
    virtual void _unlock(void);

public:
    /**
     * Construct a memory pager.
     * @param page size to use or 0 for OS allocation size.
     */
    mempager(size_t page = 0);

    /**
     * Destroy a memory pager.  Release all pages back to the heap at once.
     */
    virtual ~mempager();

    /**
     * Determine fragmentation level of acquired heap pages.  This is
     * represented as an average % utilization (0-100) and represents the
     * used portion of each allocated heap page verse the page size.  Since
     * requests that cannot fit on an already allocated page are moved into
     * a new page, there is some unusable space left over at the end of the
     * page.  When utilization approaches 100, this is good.  A low utilization
     * may suggest a larger page size should be used.
     * @return pager utilization.
     */
    unsigned utilization(void);

    /**
     * Purge all allocated memory and heap pages immediately.
     */
    void purge(void);

    /**
     * Return memory back to pager heap.  This actually does nothing, but
     * might be used in a derived class to create a memory heap that can
     * also receive (free) memory allocated from our heap and reuse it,
     * for example in a full private malloc implementation in a derived class.
     * @param memory to free back to private heap.
     */
    virtual void dealloc(void *memory);

    /**
     * Allocate memory from the pager heap.  The size of the request must be
     * less than the size of the memory page used.  This impliments the
     * memory protocol with mutex locking for thread safety.
     * is locked during this operation and then released.
     * @param size of memory request.
     * @return allocated memory or NULL if not possible.
     */
    virtual void *_alloc(size_t size);
};

class __EXPORT ObjectPager : protected memalloc
{
public:
    class __EXPORT member : public LinkedObject
    {
    private:
        void *mem;

    protected:
        friend class ObjectPager;

        inline void set(member *node)
            {Next = node;};

        inline void *get(void) const
            {return mem;};

        member(LinkedObject **root);
        member();

    public:
        inline void *operator*() const
            {return mem;};
    };

private:
    unsigned members;
    LinkedObject *root;
    size_t typesize;
    member *last;
    void **index;

protected:
    ObjectPager(size_t objsize, size_t pagesize = 256);

    /**
     * Get object from list.  This is useful when objectpager is
     * passed as a pointer and hence inconvenient for the [] operator.
     * @param item to access.
     * @return pointer to text for item, or NULL if out of range.
     */
    void *get(unsigned item) const;

    /**
     * Add object to list.
     * @param object to add.
     */
    void *add(void);

    void *push(void);

    /**
     * Remove element from front of list.  Does not release memory.
     * @return object removed.
     */
    void *pull(void);

    /**
     * Remove element from back of list.  Does not release memory.
     * @return object removed.
     */
    void *pop(void);

    /**
     * Invalid object...
     * @return typically NULL.
     */
    void *invalid(void) const;

public:
    /**
     * Purge all members and release pager member.  The list can then
     * be added to again.
     */
    void clear(void);

    /**
     * Get root of pager list.  This is useful for externally enumerating
     * the list of strings.
     * @return first member of list or NULL if empty.
     */
    inline ObjectPager::member *begin(void)
        {return static_cast<ObjectPager::member *>(root);};

    inline operator bool() const
        {return members > 0;}

    inline bool operator!() const
        {return !members;}

    /**
     * Get the number of items in the pager string list.
     * @return number of items stored.
     */
    inline unsigned count(void) const
        {return members;};

    /**
     * Convenience typedef for iterative pointer.
     */
    typedef linked_pointer<ObjectPager::member> iterator;

    inline size_t size(void)
        {return memalloc::size();}

    inline unsigned pages(void)
        {return memalloc::pages();}

protected:
    /**
     * Gather index list.
     * @return index.
     */
    void **list(void);
};

/**
 * String pager for storing lists of NULL terminated strings.  This is
 * used for accumulating lists which can be destroyed all at once.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT StringPager : protected memalloc
{
private:
    unsigned members;
    LinkedObject *root;

protected:
    virtual const char *invalid(void) const;

public:
    /**
     * Filter text in a derived class.  The base class filter removes
     * newlines at end of text and filters out empty strings.
     * @param text to filter.
     * @param size of text buffer for transforms.
     * @return false if end of data.
     */
    virtual bool filter(char *text, size_t size);

    /**
     * Member of string list.  This is exposed so that the list of strings
     * can be externally enumerated with linked_pointer<StringPager::member>
     * if so desired, through the begin() method.
     * @author David Sugar <dyfet@gnutelephony.org>
     */
    class __EXPORT member : public LinkedObject
    {
    private:
        const char *text;

    protected:
        friend class StringPager;

        inline void set(member *node)
            {Next = node;};

        member(LinkedObject **root, const char *data);
        member(const char *data);

    public:
        inline const char *operator*() const
            {return text;};

        inline const char *get(void) const
            {return text;};
    };

    /**
     * Create a pager with a maximum page size.
     * @param size of pager allocation pages.
     */
    StringPager(size_t pagesize = 256);

    StringPager(char **list, size_t pagesize = 256);

    /**
     * Get the number of items in the pager string list.
     * @return number of items stored.
     */
    inline unsigned count(void) const
        {return members;};

    /**
     * Get string item from list.  This is useful when StringPager is
     * passed as a pointer and hence inconvenient for the [] operator.
     * @param item to access.
     * @return pointer to text for item, or NULL if out of range.
     */
    const char *get(unsigned item) const;

    /**
     * Replace string item in list.
     * @param item to replace.
     * @param string to replace with.
     */
    void set(unsigned item, const char *string);

    /**
     * Add text to list.
     * @param text to add.
     */
    void add(const char *text);

    /**
     * Add text to front of list.
     * @param text to add.
     */
    void push(const char *text);

    /**
     * Add text list to front of list.
     * @param text to add.
     */
    void push(char **text);

    /**
     * Remove element from front of list.  Does not release memory.
     * @return text removed.
     */
    const char *pull(void);

    /**
     * Remove element from back of list.  Does not release memory.
     * @return text removed.
     */
    const char *pop(void);

    /**
     * Add list to list.  This is a list of string pointers terminated with
     * NULL.
     * @param list of text to add.
     */
    void add(char **list);

    /**
     * Set list to list.  This is a list of string pointers terminated with
     * NULL.
     * @param list of text to set.
     */
    void set(char **list);

    /**
     * Purge all members and release pager member.  The list can then
     * be added to again.
     */
    void clear(void);

    /**
     * Return specified member from pager list.  This is a convenience
     * operator.
     * @param item to access.
     * @return text of item or NULL if invalid.
     */
    inline const char *operator[](unsigned item) const
        {return get(item);};

    inline const char *at(unsigned item) const
        {return get(item);};

    /**
     * Get root of pager list.  This is useful for externally enumerating
     * the list of strings.
     * @return first member of list or NULL if empty.
     */
    inline StringPager::member *begin(void) const
        {return static_cast<StringPager::member *>(root);};

    /**
     * Convenience operator to add to pager and auto-sort.
     * @param text to add to list.
     */
    inline void operator+=(const char *text)
        {add(text);};

    /**
     * Convenience operator to add to pager.
     * @param text to add to list.
     */
    inline StringPager& operator<<(const char *text)
        {add(text); return *this;}

    inline StringPager& operator>>(const char *text)
        {push(text); return *this;}

    /**
     * Sort members.
     */
    void sort(void);

    /**
     * Gather index list.
     * @return index.
     */
    char **list(void);

    /**
     * Tokenize a string and add each token to the StringPager.
     * @param text to tokenize.
     * @param list of characters to use as token separators.
     * @param quote pairs of characters for quoted text or NULL if not used.
     * @param end of line marker characters or NULL if not used.
     * @return number of tokens parsed.
     */
    unsigned token(const char *text, const char *list, const char *quote = NULL, const char *end = NULL);

    unsigned split(const char *text, const char *string, unsigned flags = 0);

    unsigned split(stringex_t& expr, const char *string, unsigned flags = 0);

    String join(const char *prefix = NULL, const char *middle = NULL, const char *suffix = NULL);

    inline operator bool()
        {return members > 0;}

    inline bool operator!()
        {return !members;}

    inline StringPager& operator=(char **list)
        {set(list); return *this;}

    inline const char *operator*()
        {return pull();}

    inline operator char **()
        {return list();};

    /**
     * Convenience typedef for iterative pointer.
     */
    typedef linked_pointer<StringPager::member> iterator;

    inline size_t size(void)
        {return memalloc::size();}

    inline unsigned pages(void)
        {return memalloc::pages();}

private:
    member *last;
    char **index;
};

/**
 * Directory pager is a paged string list for directory file names.
 * This protocol is used to convert a directory into a list of filenames.
 * As a protocol it offers a filtering method to select which files to
 * include in the list.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT DirPager : protected StringPager
{
protected:
    const char *dir;

    /**
     * Filter filenames in a derived class.  The default filter
     * drops "." special files.
     * @param filename to filter.
     * @param size of filename buffer.
     * @return true if include in final list.
     */
    virtual bool filter(char *filename, size_t size);

    /**
     * Load a directory path.
     * @param path to load.
     * @return true if valid.
     */
    bool load(const char *path);

public:
    DirPager();

    DirPager(const char *path);

    void operator=(const char *path);

    inline const char *operator*() const
        {return dir;};

    inline operator bool() const
        {return dir != NULL;};

    inline bool operator!() const
        {return dir == NULL;};

    inline unsigned count(void) const
        {return StringPager::count();};

    /**
     * Return specified filename from directory list.  This is a convenience
     * operator.
     * @param item to access.
     * @return text of item or NULL if invalid.
     */
    inline const char *operator[](unsigned item) const
        {return StringPager::get(item);};

    inline const char *get(unsigned item) const
        {return StringPager::get(item);};

    inline const char *at(unsigned item) const
        {return StringPager::get(item);};

    inline size_t size(void)
        {return memalloc::size();}

    inline unsigned pages(void)
        {return memalloc::pages();}
};

/**
 * Buffered pager for storing paged strings for character protocol.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT bufpager : public memalloc, public CharacterProtocol
{
private:
    typedef struct cpage {
        struct cpage *next;
        char *text;
        unsigned size, used;
    } cpage_t;

    cpage_t *first, *last, *current, *freelist;
    unsigned cpos;
    unsigned long ccount;
    bool eom;       /* null written or out of memory */

    virtual int _getch(void);
    virtual int _putch(int code);

protected:
    virtual void *_alloc(size_t size);

public:
    /**
     * Reset pager text buffer protocol.
     */
    void reset(void);

    /**
     * Rewind to start of text buffer protocol.
     */
    void rewind(void);

    /**
     * Create an output string from buffer.
     * @return output string allocated.
     */
    char *dup(void);

    /**
     * Set text of string buffer.
     * @param text to set.
     */
    void set(const char *text);

    /**
     * Add text to string buffer.
     * @param text to add.
     */
    void add(const char *text);

    /**
     * Get string from buffer.
     * @param text to save into.
     * @param size of buffer.
     * @return count of characters copied.
     */
    size_t get(char *text, size_t size);

    /**
     * Put memory string into buffer including NULL byte.
     * @param text to add.
     * @param size of text to add.
     */
    void put(const char *text, size_t size);

    /**
     * Get total size.
     * @return number of characters in buffer.
     */
    inline unsigned long used(void) const
        {return ccount;}

    /**
     * Convenience operator to get text.
     * @return text string of buffer.
     */
    inline char *operator *()
        {return dup();}

    /**
     * Convenience operator to add to pager.
     * @param text to add to list.
     */
    inline bufpager& operator<<(const char *text)
        {add(text); return *this;};

    bufpager(size_t page = 0);

    /**
     * Request character buffer to write into directly.
     * @param iosize made available.
     * @return pointer to buffer or NULL if out of memory.
     */
    char *request(size_t *iosize);

    /**
     * Get pointer to copy character data.  The memory pointer is
     * positioned at the next chunk automatically.
     * @param iosize of data you can copy.
     * @return to data from buffer or NULL if past end.
     */
    char *copy(size_t *iosize);

    /**
     * Used to complete a request method.
     * @param size of data actually written.
     */
    void update(size_t size);

    /**
     * Check if can still save into buffer.
     * @return true if buffer is full.
     */
    inline bool operator!()
        {return eom;}

    /**
     * Check if can still save into buffer.  Used for is() function.
     * @return true if pager can still store more.
     */
    inline operator bool()
        {return !eom;}
};

/**
 * Create a linked list of auto-releasable objects.  LinkedObject derived
 * objects can be created that are assigned to an autorelease object list.
 * When the autorelease object falls out of scope, all the objects listed'
 * with it are automatically deleted.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT autorelease
{
private:
    LinkedObject *pool;

public:
    /**
     * Create an initially empty autorelease pool.
     */
    autorelease();

    /**
     * Destroy an autorelease pool and delete member objects.
     */
    ~autorelease();

    /**
     * Destroy an autorelease pool and delete member objects.  This may be
     * used to release an existing pool programmatically when desired rather
     * than requiring the object to fall out of scope.
     */
    void release(void);

    /**
     * Add a linked object to the autorelease pool.
     * @param object to add to pool.
     */
    void operator+=(LinkedObject *object);
};

/**
 * This is a base class for objects that may be created in pager pools.
 * This is also used to create objects which can be maintained as managed
 * memory and returned to a pool.  The linked list is used when freeing
 * and re-allocating the object.  These objects are reference counted
 * so that they are returned to the pool they come from automatically
 * when falling out of scope.  This can be used to create automatic
 * garbage collection pools.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT PagerObject : public LinkedObject, public CountedObject
{
protected:
    friend class PagerPool;

    PagerPool *pager;

    /**
     * Create a pager object.  This is object is constructed by a PagerPool.
     */
    PagerObject();

    /**
     * Reset state of object.
     */
    void reset(void);

    /**
     * Release a pager object reference.
     */
    void release(void);

    /**
     * Return the pager object back to it's originating pool.
     */
    void dealloc(void);
};

/**
 * Pager pool base class for managed memory pools.  This is a helper base
 * class for the pager template and generally is not used by itself.  If
 * different type pools are intended to use a common memory pager then
 * you will need to mixin a memory protocol object that performs
 * redirection such as the MemoryRedirect class.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT PagerPool : public MemoryProtocol
{
private:
    LinkedObject *freelist;
    pthread_mutex_t mutex;

protected:
    PagerPool();
    virtual ~PagerPool();

    PagerObject *get(size_t size);

public:
    /**
     * Return a pager object back to our free list.
     * @param object to return to pool.
     */
    void put(PagerObject *object);
};

class __EXPORT charmem : public CharacterProtocol
{
protected:
    char *buffer;
    size_t inp, out, size;
    bool dynamic;

    int _getch(void);
    int _putch(int code);

public:
    charmem(char *mem, size_t size);
    charmem(size_t size);
    charmem();
    virtual ~charmem();

    void release(void);

    void set(char *mem, size_t size);

    void set(size_t size);

    inline void reset(void)
        {inp = out = 0;}

    inline void rewind(void)
        {inp = 0;}
};

class __EXPORT chartext : public CharacterProtocol
{
private:
    char *pos;
    size_t max;

    int _putch(int code);
    int _getch(void);

public:
    chartext();
    chartext(char *buf);
    chartext(char *buf, size_t size);
    virtual ~chartext();
};

/**
 * A class to hold memory pointers referenced by string names.  This is
 * used to form a typeless data pointer that can be associated and
 * referenced by string/logical name.  The memory used for forming
 * the string names can itself be managed in reusable memory pools and
 * the entire structure uses it's own private pager heap.  This allows
 * new string named pointers to be added and deleted at runtime in a thread-
 * safe manner.  This might typically be used as a session id manager or for
 * symbol tables.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT keyassoc : protected mempager
{
private:
    /**
     * Internal paged memory residing data class for name associated pointers.
     */
    class __LOCAL keydata : public NamedObject
    {
    public:
        void *data;
        char text[8];

        keydata(keyassoc *assoc, const char *id, unsigned max, unsigned bufsize);
    };

    friend class keydata;

    unsigned keycount;
    unsigned paths;
    size_t keysize;
    NamedObject **root;
    LinkedObject **list;

protected:
    /**
     * Allocate object stored in pager also.
     * @param name of object entry.
     * @param size of object to allocate.
     * @return pointer to allocated object or NULL on failure.
     */
    void *allocate(const char *name, size_t size);

public:
    /**
     * Create a key associated memory pointer table.
     * @param indexing size for hash map.
     * @param max size of a string name if names are in reusable managed memory.
     * @param page size of memory pager.
     */
    keyassoc(unsigned indexing = 177, size_t max = 0, size_t page = 0);

    /**
     * Destroy association object.  Release all pages back to the heap.
     */
    ~keyassoc();

    /**
     * Get the number of associations we have in our object.
     * @return number of associations stored.
     */
    inline unsigned count(void) const
        {return keycount;};

    /**
     * Lookup the data pointer of a string by direct operation.
     * @param name to lookup.
     * @return pointer to data or NULL if not found.
     */
    inline void *operator()(const char *name)
        {return locate(name);};

    /**
     * Purge all associations and return allocated pages to heap.
     */
    void purge(void);

    /**
     * Lookup the data pointer by the string name given.
     * @param name to lookup.
     * @return pointer to data or NULL if not found.
     */
    void *locate(const char *name);

    /**
     * Assign a name to a data pointer.  If the name exists, it is re-assigned
     * with the new pointer value, otherwise it is created.
     * @param name to assign.
     * @param pointer value to assign with name.
     * @return false if failed because name is too long for managed table.
     */
    bool assign(const char *name, void *pointer);

    /**
     * Create a new name in the association table and assign it's value.
     * @param name to create.
     * @param pointer value to assign with name.
     * @return false if already exists or name is too long for managed table.
     */
    bool create(const char *name, void *pointer);

    /**
     * Remove a name and pointer association.  If managed key names are used
     * then the memory allocated for the name will be re-used.
     * @param name to remove.
     * @return pointer value of the name or NULL if not found.
     */
    void *remove(const char *name);
};

template <class T, size_t P = 0>
class listof : private ObjectPager
{
public:
    inline listof() : ObjectPager(sizeof(T), P) {};

    inline T& operator[](unsigned item) const
        {return (T&)ObjectPager::get(item);}

    inline T* operator()(unsigned item) const
        {return (T*)ObjectPager::get(item);}

    inline const T& at(unsigned item) const
        {return (const T&)ObjectPager::get(item);}

    inline T* pull(void)
        {return (T*)ObjectPager::pull();}

    inline T* pop(void)
        {return (T*)ObjectPager::pop();}

    inline operator T**()
        {return (T**)ObjectPager::list();}

    inline T** list(void)
        {return (T**)ObjectPager::list();}

    inline T* operator++(void)
        {T* tmp = ObjectPager::add(); if(tmp) new((caddr_t)tmp) T; return tmp;}

    inline T* add(const T& object)
        {T* tmp = ObjectPager::add(); if(tmp) new((caddr_t)tmp) T(object); return tmp;}

    inline T* push(const T& object)
        {T* tmp = ObjectPager::push(); if(tmp) new((caddr_t)tmp) T(object); return tmp;}

    inline listof& operator<<(const T& object)
        {T* tmp = ObjectPager::add(); if(tmp) new((caddr_t)tmp) T(object); return *this;}

    inline listof& operator>>(const T& object)
        {T* tmp = ObjectPager::push(); if(tmp) new((caddr_t)tmp) T(object); return *this;}

};

template <class T, unsigned I = 177, size_t M = 0, size_t P = 0>
class mapof : private keyassoc
{
public:
    /**
     * Construct an associated pointer hash map based on the class template.
     */
    inline mapof() : keyassoc(I, M, P) {};

    /**
     * Get the count of typed objects stored in our hash map.
     * @return typed objects in map.
     */
    inline unsigned count(void) const
        {return keyassoc::count();};

    /**
     * Purge the hash map of typed objects.
     */
    inline void purge(void)
        {keyassoc::purge();};

    /**
     * Lookup a typed object by name.
     * @param name of typed object to locate.
     * @return typed object pointer or NULL if not found.
     */
    inline T *locate(const char *name)
        {return static_cast<T*>(keyassoc::locate(name));}

    inline T *operator[](const char *name)
        {return static_cast<T*>(keyassoc::locate(name));}

    /**
     * Reference a typed object directly by name.
     * @param name of typed object to locate.
     * @return typed object pointer or NULL if not found.
     */
    inline T *operator()(const char *name)
        {return locate(name);};

    /**
     * Create mapped entry from scratch.
     * @param name to assign.
     */
    inline T *map(const char *name)
        {T *tmp = keyassoc::allocate(name, sizeof(T)); if(tmp) new((caddr_t)tmp) T;}

    /**
     * Remove a name and typed pointer association.  If managed key names are
     * used then the memory allocated for the name will be re-used.
     * @param name to remove.
     */
    inline void unmap(const char *name)
        {keyassoc::remove(name);};

    /**
     * Access to pager utilization stats.  This is needed because we
     * inherit keyassoc privately.
     * @return pager utilization, 0-100.
     */
    inline unsigned utilization(void)
        {return mempager::utilization();};

    /**
     * Access to number of pages allocated from heap for our associated
     * index pointer.  This is needed because we inherit keyassoc
     * privately.
     * @return count of heap pages used.
     */
    inline unsigned pages(void) const
        {return mempager::pages();};
};

/**
 * A typed template for using a key association with typed objects.
 * This essentially forms a form of "smart pointer" that is a reference
 * to specific typed objects by symbolic name.  This is commonly used as
 * for associated indexing of typed objects.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <class T, unsigned I = 177, size_t M = 0, size_t P = 0>
class assoc_pointer : private keyassoc
{
public:
    /**
     * Construct an associated pointer hash map based on the class template.
     */
    inline assoc_pointer() : keyassoc(I, M, P) {};

    /**
     * Get the count of typed objects stored in our hash map.
     * @return typed objects in map.
     */
    inline unsigned count(void) const
        {return keyassoc::count();}

    /**
     * Purge the hash map of typed objects.
     */
    inline void purge(void)
        {keyassoc::purge();}

    /**
     * Lookup a typed object by name.
     * @param name of typed object to locate.
     * @return typed object pointer or NULL if not found.
     */
    inline T *locate(const char *name)
        {return static_cast<T*>(keyassoc::locate(name));}

    inline T *operator[](const char *name)
        {return static_cast<T*>(keyassoc::locate(name));}


    /**
     * Reference a typed object directly by name.
     * @param name of typed object to locate.
     * @return typed object pointer or NULL if not found.
     */
    inline T *operator()(const char *name)
        {return locate(name);}

    /**
     * Assign a name for a pointer to a typed object.  If the name exists,
     * it is re-assigned with the new pointer value, otherwise it is created.
     * @param name to assign.
     * @param pointer of typed object to assign with name.
     * @return false if failed because name is too long for managed table.
     */
    inline bool assign(char *name, T *pointer)
        {return keyassoc::assign(name, pointer);}

    /**
     * Create a new name in the association table and assign typed object.
     * @param name to create.
     * @param pointer of typed object to assign with name.
     * @return false if already exists or name is too long for managed table.
     */
    inline bool create(char *name, T *pointer)
        {return keyassoc::create(name, pointer);}

    /**
     * Remove a name and typed pointer association.  If managed key names are
     * used then the memory allocated for the name will be re-used.
     * @param name to remove.
     */
    inline void remove(char *name)
        {keyassoc::remove(name);}

    /**
     * Access to pager utilization stats.  This is needed because we
     * inherit keyassoc privately.
     * @return pager utilization, 0-100.
     */
    inline unsigned utilization(void) const
        {return mempager::utilization();}

    /**
     * Access to number of pages allocated from heap for our associated
     * index pointer.  This is needed because we inherit keyassoc
     * privately.
     * @return count of heap pages used.
     */
    inline unsigned pages(void) const
        {return mempager::pages();}
};

/**
 * Mempager managed type factory for pager pool objects.  This is used to
 * construct a type factory that creates and manages typed objects derived
 * from PagerObject which can be managed through a private heap.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <typename T>
class pager : private MemoryRedirect, private PagerPool
{
public:
    /**
     * Construct a pager and optionally assign a private pager heap.
     * @param heap pager to use.  If NULL, uses global heap.
     */
    inline pager(mempager *heap = NULL) : MemoryRedirect(heap), PagerPool() {};

    /**
     * Create a managed object by casting reference.
     * @return pointer to typed managed pager pool object.
     */
    inline T *operator()(void)
        {return new(get(sizeof(T))) T;};

    /**
     * Create a managed object by pointer reference.
     * @return pointer to typed managed pager pool object.
     */
    inline T *operator*()
        {return new(get(sizeof(T))) T;};
};

/**
 * A template class for a hash pager.  This creates objects from a pager
 * pool when they do not already exist in the hash map.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <class T, unsigned M = 177>
class keypager : public mempager
{
private:
    NamedObject *idx[M];

public:
    /**
     * Create the object cache.
     * @param size of allocation units.
     */
    inline keypager(size_t size) : mempager(size) {};

    /**
     * Destroy the hash pager by purging the index chains and memory pools.
     */
    inline ~keypager()
        {NamedObject::purge(idx, M); mempager::purge();};

    /**
     * Find a typed object derived from NamedObject in the hash map by name.
     * If the object is not found, it is created from the memory pool.
     * @param name to search for.
     * @return typed object if found through map or NULL.
     */
    inline T *get(const char *name) const {
        T *node = (static_cast<T*>(NamedObject::map(idx, name, M)));
        if(!node) {
            node = init<T>(static_cast<T*>(mempager::_alloc(sizeof(T))));
            node->NamedObject::add(idx, name, M);
        }
        return node;
    }

    /**
     * Test if a name exists in the pool.
     * @param name to test.
     * @return true if found.
     */
    bool test(const char *name) const
        {return NamedObject::map(idx, name, M) != NULL;};

    /**
     * Find a typed object derived from NamedObject in the hash map by name.
     * If the object is not found, it is created from the pager pool.
     * @param name to search for.
     * @return typed object if found through map or NULL.
     */
    inline T *operator[](const char *name) const
        {return get(name);};

    /**
     * Find first typed object in hash map to iterate.
     * @return first typed object or NULL if nothing in list.
     */
    inline T *begin(void) const
        {return static_cast<T*>(NamedObject::skip(idx, NULL, M));};

    /**
     * Find next typed object in hash map for iteration.
     * @param current typed object we are referencing.
     * @return next iterative object or NULL if past end of map.
     */
    inline T *next(T *current) const
        {return static_cast<T*>(NamedObject::skip(idx, current, M));};

    /**
     * Count the number of typed objects in our hash map.
     * @return count of typed objects.
     */
    inline unsigned count(void) const
        {return NamedObject::count(idx, M);};

    /**
     * Convert our hash map into a linear object pointer array.  The
     * object pointer array is created from the heap and must be deleted
     * when no longer used.
     * @return array of typed named object pointers.
     */
    inline T **index(void) const
        {return NamedObject::index(idx, M);};

    /**
     * Convert our hash map into an alphabetically sorted linear object
     * pointer array.  The object pointer array is created from the heap
     * and must be deleted when no longer used.
     * @return sorted array of typed named object pointers.
     */
    inline T **sort(void) const
        {return NamedObject::sort(NamedObject::index(idx, M));};

    /**
     * Convenience typedef for iterative pointer.
     */
    typedef linked_pointer<T> iterator;
};

/**
 * A convenience type for paged string lists.
 */
typedef StringPager stringlist_t;

/**
 * A convenience type for paged string list items.
 */
typedef StringPager::member stringlistitem_t;

/**
 * A convenience type for using DirPager directly.
 */
typedef DirPager dirlist_t;

inline const char *shift(stringlist_t& list)
    {return list.pull();}

inline void unshift(stringlist_t& list, const char *text)
    {list.push(text);}


inline String str(StringPager& list, const char *prefix = NULL, const char *middle = NULL, const char *suffix = NULL)
    {return list.join(prefix, middle, suffix);}

END_NAMESPACE

#endif
