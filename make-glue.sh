SRCDIR=jni/sflphone/daemon/src


# Fix a problem with dbusxx generating *const introspect() instead of introspect()
echo "Generating callmanager glue..."
dbusxx-xml2cpp $SRCDIR/client/callmanager-introspec.xml --adaptor=$SRCDIR/client/android/callmanager-glue-tmp.h
sed -e 's/const introspect()/introspect()/' <$SRCDIR/client/android/callmanager-glue-tmp.h >$SRCDIR/client/android/callmanager-glue.h
rm $SRCDIR/client/android/callmanager-glue-tmp.h

echo "Generating configurationmanager glue..."
dbusxx-xml2cpp $SRCDIR/client/configurationmanager-introspec.xml --adaptor=$SRCDIR/client/android/configurationmanager-glue-tmp.h
sed -e 's/const introspect()/introspect()/' <$SRCDIR/client/android/configurationmanager-glue-tmp.h >$SRCDIR/client/android/configurationmanager-glue.h
rm $SRCDIR/client/android/configurationmanager-glue-tmp.h

#echo "Generating contactmanager glue..."
#dbusxx-xml2cpp $SRCDIR/dbus/contactmanager-introspec.xml --adaptor=$SRCDIR/dbus/contactmanager-glue-tmp.h
#sed -e 's/const introspect()/introspect()/' <$SRCDIR/dbus/contactmanager-glue-tmp.h >$SRCDIR/dbus/contactmanager-glue.h
#rm $SRCDIR/dbus/contactmanager-glue-tmp.h

echo "Generating instance glue..."
dbusxx-xml2cpp $SRCDIR/client/instance-introspec.xml --adaptor=$SRCDIR/client/android/instance-glue-tmp.h 
sed -e 's/const introspect()/introspect()/' <$SRCDIR/client/android/instance-glue-tmp.h >$SRCDIR/client/android/instance-glue.h
rm $SRCDIR/client/android/instance-glue-tmp.h