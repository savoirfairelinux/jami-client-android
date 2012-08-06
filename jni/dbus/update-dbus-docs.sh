#! /bin/bash

function die()
{
    echo $* 2>&1
    exit 1
}

if test -n "$FDUSER" ; then
    echo "Using freedesktop.org account $FDUSER"
    user="$FDUSER@"
else
    echo "Using freedesktop.org account from ~/.ssh/config, or local username"
fi

CHECKOUTDIR=`mktemp -d || echo /tmp/dbus-for-docs`
export CVSROOT=":ext:${user}cvs.freedesktop.org:/cvs/dbus"

cd $CHECKOUTDIR || die "could not changedir to $CHECKOUTDIR"

echo "Checking out to directory "`pwd`

/bin/rm -rf dbus/doc || true ## get rid of old doxygen, etc.
cvs co dbus || die "failed to cvs update"
cd dbus || die "could not cd to dbus"

echo "Configuring and building docs"

## the configure flags are explicit so if you lack xmlto, etc. 
## you won't fail to update those docs
./autogen.sh --enable-xml-docs=yes --enable-doxygen-docs=yes || die "could not autogen"
doxygen Doxyfile || die "could not run Doxygen"
make -C doc || die "could not build docs"

MANFILES=`find -name "dbus*.1"`
for M in $MANFILES ; do
    BASENAME=`basename $M`
    echo "Converting $M to $BASENAME.html"
    man2html $M > doc/$BASENAME.html
done

echo "Packing docs into tarball"
cp README HACKING AUTHORS NEWS COPYING ChangeLog doc/ || die "could not copy in assorted files"
tar cfz dbus-docs.tar.gz doc/*.dtd doc/*.xsl doc/*.xml doc/*.html doc/*.txt doc/api/html/*.html doc/api/html/*.css doc/api/html/*.png doc/api/html/*.gif doc/HACKING doc/AUTHORS doc/NEWS doc/ChangeLog doc/TODO doc/README doc/COPYING doc/*.png doc/*.svg || die "could not tar up docs"

tar tfz dbus-docs.tar.gz | sort > tarball.list || die "could not list tarball contents"
find doc -not -type d | grep -v CVS | grep -v -E '.~[0-9.]+~' | grep -v Makefile | grep -vE '.c$' | grep -v man3dbus | grep -v .cvsignore | sort > filesystem.list || die "could not list doc/* contents"

diff -u filesystem.list tarball.list || die "some files were not included"

echo "Uploading docs to server"
scp dbus-docs.tar.gz "${user}"dbus.freedesktop.org:
ssh "${user}"dbus.freedesktop.org '(cd /srv/dbus.freedesktop.org/www/ && /bin/cp -f ~/dbus-docs.tar.gz . && tar zxf dbus-docs.tar.gz && echo "Successfully unpacked tarball on server")'

echo "Uploading DTD to server"
scp -p doc/introspect.dtd "${user}"dbus.freedesktop.org:/srv/specifications.freedesktop.org/www/dbus/1.0/introspect.dtd
