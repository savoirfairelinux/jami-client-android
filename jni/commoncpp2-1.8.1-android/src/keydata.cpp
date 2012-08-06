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
#include <cc++/file.h>
#include <cc++/misc.h>
#include <sys/stat.h>
#include "private.h"
#include <cstdlib>
#include <cstdio>

#ifndef CAPE_REGISTRY_APPLICATIONS
#define CAPE_REGISTRY_APPLICATIONS  "SOFTWARE\\CAPE Applications"
#define CAPE_REGISTRY_USERSETTINGS  "Software\\CAPE Applications"
#endif

#ifdef  WIN32
class Registry
{
public:
    const char *configdir;
    Registry();
};

Registry::Registry()
{
    configdir = getenv("SystemRoot");
    if(!configdir || !*configdir)
        configdir = getenv("windir");
    if(!configdir || !*configdir)
        configdir = "C:\\WINDOWS";
}

static Registry registry;
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
using namespace std;
#endif

int Keydata::count = 0;
int Keydata::sequence = 1;
ifstream *Keydata::cfgFile = NULL;
char Keydata::lastpath[KEYDATA_PATH_SIZE + 1];

void endKeydata();

static unsigned bitsize(void)
{
    if(sizeof(void *) > 4)
        return 2;
    return 1;
}

Keydata::Keydata() :
MemPager(KEYDATA_PAGER_SIZE * bitsize())
{
    link = 0;
    memset(&keys, 0, sizeof(keys));
}

Keydata::Keydata(const char *path) :
MemPager(KEYDATA_PAGER_SIZE * bitsize())
{
    link = 0;
    memset(&keys, 0, sizeof(keys));
    load(path);
}

Keydata::Keydata(Define *pairs, const char *path) :
MemPager(KEYDATA_PAGER_SIZE * bitsize())
{
    link = 0;
    memset(&keys, 0, sizeof(keys));
    load(pairs);
    if(path)
        load(path);
}

Keydata::~Keydata()
{
    clean();
    unlink();
    if(count < 1)
        endKeydata();
}

Keydata::Keysym *Keydata::getSymbol(const char *sym, bool create)
{
    unsigned path = getIndex(sym);
    size_t len = strlen(sym) + 1;
    Keysym *key = keys[path];

    while(key) {
        if(!stricmp(sym, key->sym))
            return key;
        key = key->next;
    }
    if(!create)
        return NULL;

    // note: keysym has 1 byte for null character already
    key = (Keysym *)alloc(sizeof(Keysym) + len - 1);
    setString(key->sym, len, sym);

    key->count = 0;
    key->next = keys[path];
    key->data = NULL;
    key->list = NULL;
    keys[path] = key;
    return key;
}

unsigned Keydata::getIndex(const char *str)
{
    unsigned key = 0;

    while(*str)
        key = (key << 1) ^ (*(str++) & 0x1f);

    return key % KEYDATA_INDEX_SIZE;
}

int Keydata::getCount(const char *sym)
{
    Keysym *key = getSymbol(sym, false);
    if(!key)
        return 0;

    return key->count;
}

const char *Keydata::getFirst(const char *sym)
{
    Keysym *key = getSymbol(sym, false);
    Keyval *val;

    if(!key)
        return NULL;

    val = key->data;
    if(!val)
        return NULL;

    while(val->next)
        val = val->next;

    return val->val;
}

const char *Keydata::getString(const char *sym, const char *def)
{
    const char *cp = getLast(sym);

    if(!cp)
        return def;

    return cp;
}

long Keydata::getLong(const char *sym, long def)
{
    const char *cp = getLast(sym);

    if(!cp)
        return def;

    return atol(cp);
}

double Keydata::getDouble(const char *sym, double def)
{
    const char *cp = getLast(sym);

    if(!cp)
        return def;

    return atof(cp);
}

bool Keydata::getBool(const char *sym)
{
    const char *cp = getLast(sym);

    if(!cp)
        return false;

    switch(*cp) {
    case 'y':
    case 'Y':
    case 't':
    case 'T':
        return true;
    default:
        return false;
    }
}

bool Keydata::isKey(const char *sym)
{
    if(getLast(sym))
        return true;

    return false;
}

const char *Keydata::getLast(const char *sym)
{
    Keysym *key = getSymbol(sym, false);
    if(!key)
        return NULL;

    if(!key->data)
        return NULL;

    return key->data->val;
}

unsigned Keydata::getCount(void)
{
    unsigned icount = 0;
    Keysym *key;
    int idx;

    for(idx = 0; idx < KEYDATA_INDEX_SIZE; ++idx) {
        key = keys[idx];
        while(key) {
            ++icount;
            key = key->next;
        }
    }
    return icount;
}

unsigned Keydata::getIndex(char **data, unsigned max)
{
    int idx;
    Keysym *key;
    unsigned icount = 0;

    for(idx = 0; idx < KEYDATA_INDEX_SIZE; ++idx) {
        if(icount >= max)
            break;
        key = keys[idx];
        while(key && icount < max) {
            *(data++) = key->sym;
            ++icount;
            key = key->next;
        }
    }
    *data = NULL;
    return icount;
}

void Keydata::setValue(const char *sym, const char *data)
{
    size_t len = strlen(data) + 1;
    Keysym *key = getSymbol(sym, true);
    Keyval *val;

    if(!data)
        data = "";

    // note keyval has 1 byte for null already
    val = (Keyval *)alloc(sizeof(Keyval) + len - 1);
    ++key->count;
    key->list = NULL;
    val->next = key->data;
    key->data = val;
    setString(val->val, len, data);
}

const char *const *Keydata::getList(const char *sym)
{
    int icount;
    Keysym *key = getSymbol(sym, false);
    Keyval *data;

    if(!key)
        return NULL;

    icount = key->count;
    if(!icount)
        return NULL;

    ++icount;
    if(!key->list) {
        key->list =(const char **)first(sizeof(const char**) * icount);
        key->list[--icount] = NULL;
        data = key->data;
        while(icount && data) {
            key->list[--icount] = data->val;
            data = data->next;
        }
        while(icount)
            key->list[--icount] = "";
    }
    return key->list;
}

void Keydata::clrValue(const char *sym)
{
    Keysym *key = getSymbol(sym, false);
    if(!key)
        return;

    key->count = 0;
    key->list = NULL;
    key->data = NULL;
}

void Keydata::load(Define *defs)
{
    Keysym *key;

    while(defs->keyword) {
        key = getSymbol(defs->keyword, true);
        if(!key->data)
            setValue(defs->keyword, defs->value);
        ++defs;
    }
}

void Keydata::load(const char *keypath)
{
    loadPrefix(NULL, keypath);
}

void Keydata::loadPrefix(const char *pre, const char *keypath)
{
    // FIXME: use string of dinamic allocation
    char path[KEYDATA_PATH_SIZE];
    char seek[33];
    const char *prefix = NULL;
    const char *ext;
#ifdef  WIN32
    const char *ccp;
#endif
    char *cp;
    bool etcpath = false, etctest = false;
#ifndef WIN32
    struct stat ino;
#endif

    path[0] = 0;

#ifdef  WIN32
    HKEY key;
    LONG value;
    DWORD keynamelen, keytextlen;
    TCHAR keyname[256];
    TCHAR keytext[256];
    DWORD keyindex = 0;
    char *regprefix = getenv("CONFIG_REGISTRY");

    if(!regprefix)
        regprefix="";

    ccp = keypath;
    if(*ccp == '~') {
        ++ccp;
        if(*ccp == '/')
            ++ccp;
        snprintf(path, sizeof(path), CAPE_REGISTRY_USERSETTINGS "/%s%s/", regprefix, ccp);
    }
    else {
        if(*ccp == '/')
            ++ccp;
        snprintf(path, sizeof(path), CAPE_REGISTRY_APPLICATIONS "/%s%s/", regprefix, ccp);
    }

    cp = path;
    while(NULL != (cp = strchr(cp, '/')))
        *cp = '\\';

    if(*keypath == '~')
        value = RegOpenKey(HKEY_CURRENT_USER, path, &key);
    else
        value = RegOpenKey(HKEY_LOCAL_MACHINE, path, &key);

    // if registry key path is found, then we use registry values
    // and ignore .ini files...

    if(value == ERROR_SUCCESS) {
        for(;;) {
            keynamelen = 256;
            value = RegEnumKeyEx(key, keyindex, keyname, &keynamelen, NULL, NULL, NULL, NULL);
            if(value != ERROR_SUCCESS)
                break;

            keytextlen = 256;
            keytext[0] = '\0';
            value = RegEnumValue(key, keyindex++, keytext, &keytextlen, NULL, NULL, NULL, NULL);
            if(value != ERROR_SUCCESS)
                continue;

            if(pre) {
                snprintf(path, sizeof(path), "%s.%s", pre, keyname);
                setValue(path, keytext);
            }
            else
                setValue(keyname, keytext);
        }
        RegCloseKey(key);
        return;
    }

    // windows will not support subdir .ini tree; now if not in
    // registry, then assume unavailable

    ccp = strchr(keypath + 3, '/');
    if(ccp)
        ccp = strchr(ccp, '/');

    if(ccp)
        return;

    if(*keypath == '~') {
        prefix = getenv("USERPROFILE");
        if(!prefix)
            return;

        setString(path, sizeof(path) - 8, prefix);
        addString(path, sizeof(path), "\\");
        ++keypath;
        cp = path;
        while(NULL != (cp = strchr(cp, '\\')))
            *cp = '/';
    }

#else

    if(*keypath == '~') {
        prefix = getenv("HOME");
        if(!prefix)
            return;

        setString(path, sizeof(path) - 8, prefix);
        addString(path, sizeof(path), "/.");
        ++keypath;
    }

#endif

    if(!prefix) {
#ifdef  WIN32
        if(!prefix || !*prefix)
            prefix = registry.configdir;
#else

retry:
#ifdef  ETC_CONFDIR
        if(!prefix || !*prefix) {
            if(etcpath)
                prefix = ETC_PREFIX;
            else
                prefix = ETC_CONFDIR;
            etctest = true;
            if(!stricmp(ETC_PREFIX, ETC_CONFDIR))
                etcpath = true;
        }
#else

        if(!prefix || !*prefix) {
            etctest = true;
            prefix = ETC_PREFIX;
        }
#endif

#endif

        setString(path, sizeof(path) - 8, prefix);

#ifdef  WIN32
        cp = path;
        while(NULL != (cp = strchr(cp, '\\')))
            *cp = '/';

        cp = path + strlen(path) - 1;
        if(*cp != '/') {
            *(++cp) = '/';
            *(++cp) = 0;
        }
#endif
        prefix = NULL;
    }

    if(*keypath == '/' || *keypath == '\\')
        ++keypath;

    addString(path, sizeof(path), keypath);
    cp = strrchr(path, '/');
    setString(seek, sizeof(seek), cp + 1);
    *cp = 0;

    ext = strrchr(path, '/');
    if(ext)
        ext = strrchr(ext + 2, '.');
    else
        ext = strrchr(path + 1, '.');

#ifdef  WIN32
    if(!ext)
        addString(path, sizeof(path), ".ini");
#else
    if(!prefix && !ext)
        addString(path, sizeof(path), ".conf");
    else if(prefix && !ext)
        addString(path, sizeof(path), "rc");

    ino.st_uid = (unsigned)-1;

    if(stat(path, &ino) < 0 && etctest && !etcpath) {
        etcpath = true;
        goto retry;
    }

    // if root, make sure root owned config...

    if(!geteuid() && ino.st_uid)
        return;

    // if root, make sure from a etc path only...

    if(!geteuid() && !etctest)
        return;

#endif
    loadFile(path, seek, pre);
}

void Keydata::loadFile(const char *path, const char *keys, const char *pre)
{
    char seek[33];
    char find[33];
    char line[256];
    char buffer[256];
    char *cp, *ep;
    int fpos;

    if(keys)
        setString(seek, sizeof(seek), keys);
    else
        seek[0] = 0;

    if(strcmp(path, lastpath)) {
        endKeydata();
        if(canAccess(path))
            cfgFile->open(path, ios::in);
        else
            return;
        if(!cfgFile->is_open())
            return;
        setString(lastpath, sizeof(lastpath), path);
    }

    if(link != sequence) {
        link = sequence;
        ++count;
    }

    find[0] = 0;
    cfgFile->seekg(0);
    while(keys && stricmp(seek, find)) {
        cfgFile->getline(line, sizeof(line) - 1);
        if(cfgFile->eof()) {
            lastpath[0] = 0;
            cfgFile->close();
            cfgFile->clear();
            return;
        }

        cp = line;
        while(*cp == ' ' || *cp == '\n' || *cp == '\t')
            ++cp;

        if(*cp != '[')
            continue;

        ep = strchr(cp, ']');
        if(ep)
            *ep = 0;
        else
            continue;

        setString(find, 32, ++cp);
    }

    for(;;) {
        if(cfgFile->eof()) {
            lastpath[0] = 0;
            cfgFile->close();
            cfgFile->clear();
            return;
        }

        cfgFile->getline(line, sizeof(line) - 1);

        cp = line;
        while(*cp == ' ' || *cp == '\t' || *cp == '\n')
            ++cp;

        if(!*cp || *cp == '#' || *cp == ';' || *cp == '!')
            continue;

        if(*cp == '[')
            return;

        fpos = 0;
        while(*cp && *cp != '=') {
            if(*cp == ' ' || *cp == '\t') {
                ++cp;
                continue;
            }
            find[fpos] = *(cp++);
            if(fpos < 32)
                ++fpos;
        }
        find[fpos] = 0;
        if(*cp != '=')
            continue;

        ++cp;
        while(*cp == ' ' || *cp == '\t' || *cp == '\n')
            ++cp;

        ep = cp + strlen(cp);
        while((--ep) > cp) {
            if(*ep == ' ' || *ep == '\t' || *ep == '\n')
                *ep = 0;
            else
                break;
        }

        if(*cp == *ep && (*cp == '\'' || *cp == '\"')) {
            ++cp;
            *ep = 0;
        }

        if(pre) {
#ifdef HAVE_SNPRINTF
            snprintf(buffer, 256, "%s.%s", pre, find);
#else
            setString(buffer, 256, pre);
            addString(buffer, 256, ".");
            addString(buffer, 256, find);
#endif
            setValue(buffer, cp);
        }
        else
            setValue(find, cp);
    }
}

void Keydata::unlink(void)
{
    if(link != sequence) {
        link = 0;
        return;
    }

    link = 0;
    --count;
}

void Keydata::end(void)
{
    Keydata::count = 0;
    ++Keydata::sequence;
    if(!Keydata::sequence)
        ++Keydata::sequence;

    Keydata::lastpath[0] = 0;
    if(!Keydata::cfgFile)
        Keydata::cfgFile = new std::ifstream();
    else if(Keydata::cfgFile->is_open()) {
        Keydata::cfgFile->close();
        Keydata::cfgFile->clear();
    }
}

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
