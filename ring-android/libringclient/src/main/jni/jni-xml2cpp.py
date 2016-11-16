#!/usr/bin/python

#
# PACKAGE DEPENDENCIES: python python-lxml
#

#import easy to use xml parser called minidom:
from lxml import etree
#from copy import deepcopy
from lxml import objectify
import re, sys, getopt

def rreplace(s, old, new, occurrence):
    li = s.rsplit(old, occurrence)
    return new.join(li)

def usage():
    print "jni-xml2cpp.py --file <file> | -i <file>"

# main
inputfile = "./dbus/callmanager-introspec.xml"
outputfile = "./dbus/callmanager-jni.h"
try:
    opts, args = getopt.getopt(sys.argv[1:], "hi:o:", ["help", "input=", "output="])
except getopt.GetoptError, err:
    usage()
    print str(err)
    #print opts
    sys.exit(2)

for opt, arg in opts:
    if opt in ("-h", "--help"):
        usage()
        sys.exit(0)
    elif opt in ("-i", "--input"):
        inputfile = arg
    elif opt in ("-o", "--output"):
        outputfile = arg
    else:
        print "error: argument not recognized"
        sys.exit(3)

print "inputfile = %s" % (inputfile)
print "outputfile = %s" % (outputfile)
source = "".join(args)

# lxml.objectify
# FIXME relative path
cm_obj_tree = objectify.parse(inputfile)
cm_obj_root = cm_obj_tree.getroot()
# http://www.skymind.com/~ocrow/python_string/
# method 4: list of strings
prototype = []
# iteration on methods
for meth in cm_obj_root.interface.iter(tag="method"):
# iteration on arguments
    prototype.append(meth.get("name"))
    prototype.append("(")
    for argum in meth.iter(tag="arg"):
        name = argum.get("name")
        typ = argum.get("type")
# FIXME
        if typ == 's':
            prototype.append("string %s, " % (name))
        elif typ == 'i':
            prototype.append("int %s, " % (name))
        elif typ == 'd':
            prototype.append("unsigned int %s, " % (name))
        elif typ == 'as':
            prototype.append("std::vector< std::string > &%s, " % (name))
        else:
            prototype.append("void %s, " % (name))
    prototype.append(");\n")

# starting from the end of string,
# replace the first and 1-only comma by nothing
#rreplace(prototype[tostring(), ',', '', 1)

p = re.compile(", \);")
prototypes = p.sub(");", ''.join(prototype))

# FIXME relative path
outfile = open(outputfile, "w")
outfile.write(prototypes)
outfile.close()
