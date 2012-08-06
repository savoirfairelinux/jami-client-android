if DBUS_DOXYGEN_DOCS_ENABLED

.PHONY: doc

PACKAGE_DOCNAME = $(PACKAGE_TARNAME)-$(PACKAGE_VERSION)-doc

EXTRA_DIST = Doxyfile.in $(wildcard img/*.*) head.html foot.html dbus-c++.css mainpage

doc-clean:
	rm -rf html/ $(top_builddir)/$(PACKAGE_DOCNAME).tar*

all: doc-clean
	doxygen
	cp $(srcdir)/img/* html/
	rm -rf $(PACKAGE_DOCNAME).tar*
	mkdir -p $(PACKAGE_DOCNAME)/doc
	cp -R html/ $(PACKAGE_DOCNAME)/doc
	tar cf $(PACKAGE_DOCNAME).tar $(PACKAGE_DOCNAME)/
	bzip2 -9 $(PACKAGE_DOCNAME).tar
	rm -rf $(PACKAGE_DOCNAME)/
	mv $(PACKAGE_DOCNAME).tar.bz2 $(top_builddir)

clean-local: doc-clean

endif

MAINTAINERCLEANFILES = \
	Makefile.in
