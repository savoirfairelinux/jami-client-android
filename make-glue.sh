SRCDIR=jni/sflphone/daemon/src


# Fix a problem with dbusxx generating *const introspect() instead of introspect()
echo "Generating callmanager glue..."
dbusxx-xml2cpp $SRCDIR/dbus/callmanager-introspec.xml --adaptor=$SRCDIR/dbus/callmanager-glue-tmp.h
sed -e 's/const introspect()/introspect()/' <$SRCDIR/dbus/callmanager-glue-tmp.h >$SRCDIR/dbus/callmanager-glue.h
rm $SRCDIR/dbus/callmanager-glue-tmp.h

echo "Generating configurationmanager glue..."
dbusxx-xml2cpp $SRCDIR/dbus/configurationmanager-introspec.xml --adaptor=$SRCDIR/dbus/configurationmanager-glue-tmp.h
sed -e 's/const introspect()/introspect()/' <$SRCDIR/dbus/configurationmanager-glue-tmp.h >$SRCDIR/dbus/configurationmanager-glue.h
rm $SRCDIR/dbus/configurationmanager-glue-tmp.h

#echo "Generating contactmanager glue..."
#dbusxx-xml2cpp $SRCDIR/dbus/contactmanager-introspec.xml --adaptor=$SRCDIR/dbus/contactmanager-glue-tmp.h
#sed -e 's/const introspect()/introspect()/' <$SRCDIR/dbus/contactmanager-glue-tmp.h >$SRCDIR/dbus/contactmanager-glue.h
#rm $SRCDIR/dbus/contactmanager-glue-tmp.h

echo "Generating instance glue..."
dbusxx-xml2cpp $SRCDIR/dbus/instance-introspec.xml --adaptor=$SRCDIR/dbus/instance-glue-tmp.h 
sed -e 's/const introspect()/introspect()/' <$SRCDIR/dbus/instance-glue-tmp.h >$SRCDIR/dbus/instance-glue.h
rm $SRCDIR/dbus/instance-glue-tmp.h