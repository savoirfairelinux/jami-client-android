BUILT_SOURCES = TestAppIntroProviderPrivate.h TestAppIntroPrivate.h

# We don't want to install this header
noinst_HEADERS = $(BUILT_SOURCES)

# Correctly clean the generated headers, but keep the xml description
CLEANFILES = $(BUILT_SOURCES)

EXTRA_DIST = TestAppIntro.xml

noinst_PROGRAMS = \
	TestApp

## Rule to generate the binding headers

TestAppIntroProviderPrivate.h:  TestAppIntro.xml
	$(top_builddir)/tools/dbusxx-xml2cpp $< --adaptor=$@

TestAppIntroPrivate.h:  TestAppIntro.xml
	$(top_builddir)/tools/dbusxx-xml2cpp $< --proxy=$@

TestApp_SOURCES = \
	TestAppMain.cpp \
	TestApp.cpp  \
	TestApp.h  \
	TestAppIntroProviderPrivate.h \
	TestAppIntroPrivate.h \
	TestAppIntro.h \
	TestAppIntroProvider.h

TestApp_LDADD = \
	$(top_builddir)/src/libdbus-c++-1.la \
	$(PTHREAD_LIBS) \
	$(RT_LIBS)

TestApp_CXXFLAGS = \
	-I$(top_srcdir)/include

AM_CPPFLAGS = 

## File created by the gnome-build tools

