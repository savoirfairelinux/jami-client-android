include $(top_srcdir)/globals.mak

noinst_LTLIBRARIES = libdbus.la

BUILT_SOURCES=                      \
    callmanager-glue.h              \
    configurationmanager-glue.h     \
    instance-glue.h

if SFL_VIDEO
BUILT_SOURCES+=video_controls-glue.h
video_controls-glue.h: video_controls-introspec.xml Makefile.am
	dbusxx-xml2cpp $< --adaptor=$@
endif

# Rule to generate the binding headers
%-glue.h: %-introspec.xml Makefile.am
	dbusxx-xml2cpp $< --adaptor=$@

libdbus_la_SOURCES = \
    callmanager.cpp \
    configurationmanager.cpp  \
    instance.cpp  \
    dbusmanager.cpp

if SFL_VIDEO
libdbus_la_SOURCES+=video_controls.cpp
endif

if USE_NETWORKMANAGER
libdbus_la_SOURCES += networkmanager.cpp
NETWORKMANAGER=-DUSE_NETWORKMANAGER
else
NETWORKMANAGER=
endif

libdbus_la_CXXFLAGS = \
            -DPREFIX=\"$(prefix)\" \
            -DPROGSHAREDIR=\"${datadir}/sflphone\" \
            $(NETWORKMANAGER)


noinst_HEADERS =            \
    callmanager.h           \
    configurationmanager.h  \
    instance.h              \
    dbusmanager.h           \
    networkmanager_proxy.h  \
    networkmanager.h        \
    dbus_cpp.h        		\
    $(BUILT_SOURCES)

if SFL_VIDEO
noinst_HEADERS+=video_controls.h
endif

# Dbus service file
servicedir = $(datadir)/dbus-1/services
service_in_files = org.sflphone.SFLphone.service.in
service_DATA = $(service_in_files:.service.in=.service)

# Rule to make the service file with bindir expanded
$(service_DATA): $(service_in_files) Makefile
	sed -e "s|libexec|$(libdir)/sflphone|" $<> $@

EXTRA_DIST = README $(service_in_files) \
			 callmanager-introspec.xml \
			 configurationmanager-introspec.xml \
			 instance-introspec.xml \
			 org.freedesktop.NetworkManager.xml

if SFL_VIDEO
EXTRA_DIST += video_controls-introspec.xml
endif

CLEANFILES= \
    $(BUILT_SOURCES) \
	org.sflphone.SFLphone.service
