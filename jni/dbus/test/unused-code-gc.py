#! /usr/bin/python

import os
import sys
import string
import re

## hash from symbol name to list of symbols with that name,
## where the list of symbols contains a list representing each symbol
symbols = {}
roots = {}

def createBacklinks(name, syms):
    for s in syms:
        refs = s[2]
        for r in refs:
            ## for each ref, add ourselves as a referencer
            if symbols.has_key(r):
                targets = symbols[r]
                for t in targets:
                    if name not in t[5]:
                        t[5].append(name)

def markSymbol(frm, name):
    if not symbols.has_key(name):
        print "%s referenced but was not in the objdump"
    syms = symbols[name]
    ## print ambiguous references unless they are internal noise like ".L129"
    if len(syms) > 1 and name[0] != '.':
        print "Reference to symbol '%s' from '%s' is ambiguous, marking all '%s'" % (name, frm, name)
        print syms
    for s in syms:
        if s[4]:
            pass ## already marked
        else:
            s[4] = 1
            refs = s[2]
            for r in refs:
                markSymbol(s[0], r)

def cmpFilename(a, b):
    v = cmp(a[1], b[1])
    if v == 0:
        v = cmp(a[0], b[0])
    return v

def sizeAsString(bytes):
    if bytes < 1024:
        return "%d bytes" % bytes
    elif bytes < 1024*1024:
        return "%.2gK" % (bytes / 1024.0)
    else:
        return "%.2gM" % (bytes / 1024.0 / 1024.0)

def printLost():
    list = []
    filename = None
    for (name, syms) in symbols.items():
        s = syms[0] ## we always mark all or none for now
        if not s[4] and name[0] != '.': ## skip .L129 type symbols
            filename = s[3]
            if not filename:
                filename = "unknown file"
            list.append ((name, filename, s[5], s[7]))

    file_summaries = []
    total_unused = 0
    total_this_file = 0
    filename = None
    list.sort(cmpFilename)
    for l in list:
        next_filename = l[1]
        if next_filename != filename:
            if total_this_file > 0:
                file_summaries.append ("  %s may be unused in %s" % (sizeAsString(total_this_file), filename))
            print "%s has these symbols not reachable from exported symbols:" % next_filename
            filename = next_filename
            total_this_file = 0
        print "    %s %s" % (l[0], sizeAsString(l[3]))
        total_unused = total_unused + l[3]
        total_this_file = total_this_file + l[3]
        for trace in l[2]:
            print "       referenced from %s" % trace

    for fs in file_summaries:
        print fs
    print "%s total may be unused" % sizeAsString(total_unused)

def main():

    ## 0001aa44 <_dbus_message_get_network_data>:
    sym_re = re.compile ('([0-9a-f]+) <([^>]+)>:')
    ## 1aa49:       e8 00 00 00 00          call   1aa4e <_dbus_message_get_network_data+0xa>
    ref_re = re.compile (' <([^>]+)> *$')
    ## /home/hp/dbus-cvs/dbus/dbus/dbus-message.c:139
    file_re = re.compile ('^(\/[^:].*):[0-9]+$')
    ## _dbus_message_get_network_data+0xa
    funcname_re = re.compile ('([^+]+)\+[0-9a-fx]+')
    ## 00005410 T dbus_address_entries_free
    dynsym_re = re.compile ('T ([^ \n]+)$')
    
    filename = sys.argv[1]

    command = """
    objdump -D --demangle -l %s
    """ % filename

    command = string.strip (command)

    print "Running: %s" % command
    
    f = os.popen(command)    

    ## first we find which functions reference which other functions
    current_sym = None
    lines = f.readlines()
    for l in lines:
        addr = None
        name = None
        target = None
        file = None
        
        match = sym_re.match(l)
        if match:
            addr = match.group(1)
            name = match.group(2)
        else:
            match = ref_re.search(l)
            if match:
                target = match.group(1)
            else:
                match = file_re.match(l)
                if match:
                    file = match.group(1)

        if name:
            ## 0 symname, 1 address, 2 references, 3 filename, 4 reached, 5 referenced-by 6 backlinked 7 approx size
            item = [name, addr, [], None, 0, [], 0, 0]
            if symbols.has_key(name):
                symbols[name].append(item)
            else:
                symbols[name] = [item]

            if current_sym:
                prev_addr = long(current_sym[1], 16)
                our_addr = long(item[1], 16)
                item[7] = our_addr - prev_addr
                if item[7] < 0:
                    print "Computed negative size %d for %s" % (item[7], item[0])
                    item[7] = 0
                                  
            current_sym = item
            
        elif target and current_sym:
            match = funcname_re.match(target)
            if match:
                ## dump the "+address"
                target = match.group(1)
            if target == current_sym[0]:
                pass ## skip self-references
            else:
                current_sym[2].append (target)

        elif file and current_sym:
            if file.startswith('/usr/include'):
                ## inlined libc thingy
                pass
            elif current_sym[0].startswith('.debug'):
                ## debug info
                pass
            elif current_sym[3] and current_sym[3] != file:
                raise Exception ("%s in both %s and %s" % (current_sym[0], current_sym[3], file))
            else:
                current_sym[3] = file

    ## now we need to find the roots (exported symbols)
    command = "nm -D %s" % filename
    print "Running: %s" % command
    f = os.popen(command)
    lines = f.readlines ()
    for l in lines:
        match = dynsym_re.search(l)
        if match:
            name = match.group(1)
            if roots.has_key(name):
                raise Exception("symbol %s exported twice?" % name)
            else:
                roots[name] = 1

    print "%d symbols exported from this object" % len(roots)

    ## these functions are used only indirectly, so we don't
    ## notice they are used. Manually add them as roots...
    vtable_roots = ['unix_finalize',
                    'unix_handle_watch',
                    'unix_disconnect',
                    'unix_connection_set',
                    'unix_do_iteration',
                    'unix_live_messages_changed',
                    'unix_get_unix_fd',
                    'handle_client_data_cookie_sha1_mech',
                    'handle_client_data_external_mech',
                    'handle_server_data_cookie_sha1_mech',
                    'handle_server_data_external_mech',
                    'handle_client_initial_response_cookie_sha1_mech',                  
                    'handle_client_initial_response_external_mech',
                    'handle_client_shutdown_cookie_sha1_mech',
                    'handle_client_shutdown_external_mech',
                    'handle_server_shutdown_cookie_sha1_mech',
                    'handle_server_shutdown_external_mech'
                    ]

    for vr in vtable_roots:
        if roots.has_key(vr):
            raise Exception("%s is already a root" % vr)
        roots[vr] = 1

    for k in roots.keys():
        markSymbol("root", k)

    for (k, v) in symbols.items():
        createBacklinks(k, v)

    print """

The symbols mentioned below don't appear to be reachable starting from
the dynamic exports of the library. However, this program is pretty
dumb; a limitation that creates false positives is that it can only
trace 'reachable' through hardcoded function calls, if a function is
called only through a vtable, it won't be marked reachable (and
neither will its children in the call graph).

Also, the sizes mentioned are more or less completely bogus.

"""
    
    print "The following are hardcoded in as vtable roots: %s" % vtable_roots
    
    printLost()
        
if __name__ == "__main__":
    main()
