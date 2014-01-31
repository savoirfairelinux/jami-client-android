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

#include <ucommon-config.h>
#include <ucommon/export.h>
#include <ucommon/linked.h>
#include <ucommon/memory.h>
#include <ucommon/keydata.h>
#include <ucommon/string.h>
#include <ctype.h>

using namespace UCOMMON_NAMESPACE;

keydata::keyvalue::keyvalue(keyfile *allocator, keydata *section, const char *kv, const char *dv) :
OrderedObject(&section->index)
{
    assert(allocator != NULL);
    assert(section != NULL);
    assert(kv != NULL);

    id = allocator->dup(kv);

    if(dv)
        value = allocator->dup(dv);
    else
        value = "";
}

keydata::keydata(keyfile *file, const char *id) :
OrderedObject(&file->index), index()
{
    assert(file != NULL);
    assert(id != NULL);

    name = file->dup(id);
    root = file;
}

keydata::keydata(keyfile *file) :
OrderedObject(), index()
{
    root = file;
    name = "-";
}

const char *keydata::get(const char *key) const
{
    assert(key != NULL);

    iterator keys = begin();

    while(is(keys)) {
        if(eq_case(key, keys->id))
            return keys->value;
        keys.next();
    }
    return NULL;
}

void keydata::clear(const char *key)
{
    assert(key != NULL);

    iterator keys = begin();

    while(is(keys)) {
        if(eq_case(key, keys->id)) {
            keys->delist(&index);
            return;
        }
        keys.next();
    }
}

void keydata::set(const char *key, const char *value)
{
    assert(key != NULL);

    caddr_t mem = (caddr_t)root->alloc(sizeof(keydata::keyvalue));
    keydata::iterator keys = begin();

    while(is(keys)) {
        if(eq_case(key, keys->id)) {
            keys->delist(&index);
            break;
        }
        keys.next();
    }
    new(mem) keydata::keyvalue(root, this, key, value);
}


keyfile::keyfile(size_t pagesize) :
memalloc(pagesize), index()
{
    errcode = 0;
    defaults = NULL;
}

keyfile::keyfile(const char *path, size_t pagesize) :
memalloc(pagesize), index()
{
    errcode = 0;
    defaults = NULL;
    load(path);
}

keyfile::keyfile(const keyfile& copy, size_t pagesize) :
memalloc(pagesize), index()
{
    errcode = 0;
    defaults = NULL;
    load(&copy);
}

void keyfile::release(void)
{
    defaults = NULL;
    index.reset();
    memalloc::purge();
}

keydata *keyfile::get(const char *key) const
{
    assert(key != NULL);

    iterator keys = begin();

    while(is(keys)) {
        if(eq_case(key, keys->name))
            return *keys;
        keys.next();
    }
    return NULL;
}

keydata *keyfile::create(const char *id)
{
    assert(id != NULL);

    caddr_t mem = (caddr_t)alloc(sizeof(keydata));
    keydata *old = get(id);

    if(old)
        old->delist(&index);

    return new(mem) keydata(this, id);
}

#ifdef _MSWINDOWS_

bool keyfile::save(HKEY keys, keydata *section, const char *path)
{
    HKEY subkey;

    if(path) {
        if(RegCreateKeyEx(keys, path, 0L, NULL, REG_OPTION_NON_VOLATILE, KEY_ALL_ACCESS, NULL, &subkey, NULL) == ERROR_SUCCESS) {
            save(subkey, section);
            RegCloseKey(subkey);
        }
        else
            errcode = EBADF;
        return false;
    }

    if(!section) {
        if(defaults)
            save(keys, defaults);
        linked_pointer<keydata> kp = begin();
        while(is(kp)) {
            if(RegCreateKeyEx(keys, kp->get(), 0L, NULL, REG_OPTION_NON_VOLATILE, KEY_ALL_ACCESS, NULL, &subkey, NULL) == ERROR_SUCCESS) {
                save(subkey, *kp);
                RegCloseKey(subkey);
            }
            kp.next();
        }
    } else {
        linked_pointer<keydata::keyvalue> kv = section->begin();
        while(is(kv)) {
            const char *value = kv->value;
            RegSetValueEx(keys, kv->id, 0L, REG_SZ, (const BYTE *)value, strlen(value) + 1);
        }
    }
    return true;
}

void keyfile::load(HKEY keys, keydata *section, const char *path)
{
    DWORD index = 0;
    TCHAR keyvalue[256];
    TCHAR keyname[4096];
    DWORD ksize = sizeof(keyname);
    DWORD vsize, vtype;
    FILETIME fTime;
    HKEY subkey;

    if(path) {
        if(RegOpenKeyEx(keys, path, 0, KEY_READ, &subkey) == ERROR_SUCCESS) {
            load(subkey, section);
            RegCloseKey(subkey);
        }
        else
            errcode = EBADF;
        return;
    }

    while(!section && RegEnumKeyEx(keys, index++, keyname, &ksize, NULL, NULL, NULL, &fTime) == ERROR_SUCCESS) {
        if(RegOpenKeyEx(keys, keyname, 0, KEY_READ, &subkey) == ERROR_SUCCESS) {
            section = create(keyname);
            load(subkey, section);
            RegCloseKey(subkey);
        }
        ksize = sizeof(keyname);
    }
    index = 0;
    vsize = sizeof(keyvalue);
    if(vsize > size() - 64)
        vsize = size() - 64;
    while((RegEnumValue(keys, index++, keyname, &ksize, NULL, &vtype, (BYTE *)keyvalue, &vsize) == ERROR_SUCCESS) && (vtype == REG_SZ) && (keyname[0] != 0)) {
        if(section)
            section->set(keyname, keyvalue);
        else
            defaults->set(keyname, keyvalue);
        ksize = sizeof(keyname);
        vsize = sizeof(keyvalue);
        if(vsize > size() - 64)
            vsize = size() - 64;
    }
}

#endif

void keyfile::load(const keydata *copy)
{
    keydata *section = get(copy->get());
    if(!section)
        section = create(copy->get());

    linked_pointer<keydata::keyvalue> vp = copy->begin();
    while(is(vp)) {
        section->set(vp->id, vp->value);
        vp.next();
    }
}

void keyfile::load(const keyfile *copy)
{
    linked_pointer<keydata::keyvalue> vp = (keydata::keyvalue*)NULL;

    if(copy->defaults)
        vp = copy->defaults->begin();

    if(copy->defaults && !defaults) {
        caddr_t mem = (caddr_t)alloc(sizeof(keydata));
        defaults = new(mem) keydata(this);
    }

    while(is(vp)) {
        defaults->set(vp->id, vp->value);
        vp.next();
    }

    keydata *section;
    linked_pointer<keydata> kp = copy->begin();
    while(is(kp)) {
        vp = kp->begin();
        section = get(kp->get());
        if(!section)
            section = create(kp->get());
        while(section && is(vp)) {
            section->set(vp->id, vp->value);
            vp.next();
        }
        kp.next();
    }
}

bool keyfile::save(const char *path)
{
    assert(path != NULL);

    if(!path[0])
        return false;

#ifdef  _MSWINDOWS_
    if(eq(path, "~\\", 2))
        return save(HKEY_CURRENT_USER, NULL, path);
    else if(eq(path, "-\\", 2))
        return save(HKEY_LOCAL_MACHINE, NULL, path);
#endif

    FILE *fp = fopen(path, "w");
    if(!fp) {
        errcode = EBADF;
        return false;
    }

    linked_pointer<keydata::keyvalue> vp = (keydata::keyvalue*)NULL;

    if(defaults)
        vp = defaults->begin();

    while(is(vp)) {
        if(strchr(vp->value, '\"'))
            fprintf(fp, "%s=%s\n", vp->id, vp->value);
        else
            fprintf(fp, "%s=\"%s\"\n", vp->id, vp->value);
        vp.next();
    }
    fprintf(fp, "\n");

    linked_pointer<keydata> kp = begin();
    while(is(kp)) {
        fprintf(fp, "[%s]\n", kp->get());
        vp = kp->begin();
        while(is(vp)) {
            if(strchr(vp->value, '\"'))
                fprintf(fp, "%s=%s\n", vp->id, vp->value);
            else
                fprintf(fp, "%s=\"%s\"\n", vp->id, vp->value);
            vp.next();
        }
        fprintf(fp, "\n");
        kp.next();
    }

    fclose(fp);
    return true;
}

void keyfile::load(const char *path)
{
    assert(path != NULL);

    if(!path[0])
        return;

#ifdef  _MSWINDOWS_
    if(eq(path, "~\\", 2)) {
        load(HKEY_CURRENT_USER, NULL, path);
        return;
    }
    else if(eq(path, "-\\", 2)) {
        load(HKEY_LOCAL_MACHINE, NULL, path);
        return;
    }
#endif

    char linebuf[1024];
    char *lp = linebuf;
    char *ep;
    unsigned size = sizeof(linebuf);
    FILE *fp = fopen(path, "r");
    keydata *section = NULL;
    const char *key;
    char *value;

    errcode = 0;

    if(!fp) {
        errcode = EBADF;
        return;
    }

    if(!defaults) {
        caddr_t mem = (caddr_t)alloc(sizeof(keydata));
        defaults = new(mem) keydata(this);
    }

    for(;;) {
        *lp = 0;
        if(NULL == fgets(lp, size, fp)) {
            errcode = ferror(fp);
            lp[0] = 0;
        }
        else
            String::chop(lp, "\r\n\t ");
        ep = lp + strlen(lp);
        if(ep != lp) {
            --ep;
            if(*ep == '\\') {
                lp = ep;
                size = (linebuf + sizeof(linebuf) - ep);
                continue;
            }
        }
        if(!linebuf[0] && feof(fp))
            break;

        lp = linebuf;
        while(isspace(*lp))
            ++lp;

        if(!*lp)
            goto next;

        if(*lp == '[') {
            ep = strchr(lp, ']');
            if(!ep)
                goto next;
            *ep = 0;
            section = create(String::strip(++lp, " \t"));
            goto next;
        }
        else if(!isalnum(*lp) || !strchr(lp, '='))
            goto next;

        ep = strchr(lp, '=');
        *ep = 0;
        key = String::strip(lp, " \t");
        value = String::strip(++ep, " \t\r\n");
        value = String::unquote(value, "\"\"\'\'{}()");
        if(section)
            section->set(key, value);
        else
            defaults->set(key, value);
next:
        lp = linebuf;
        size = sizeof(linebuf);
    }
    fclose(fp);
}

