#
# spec file for package libccrtp (Version 1.6.2)
#
# Copyright (c) 2009 SUSE LINUX Products GmbH, Nuernberg, Germany.
#
# All modifications and additions to the file contributed by third parties
# remain the property of their copyright owners, unless otherwise agreed
# upon. The license for this file, and modifications and additions to the
# file, is the same license as for the pristine package itself (unless the
# license for the pristine package is not an Open Source License, in which
# case the license is the MIT License). An "Open Source License" is a
# license that conforms to the Open Source Definition (Version 1.9)
# published by the Open Source Initiative.

# Please submit bugfixes or comments via http://bugs.opensuse.org/
#

# norootforbuild


Name:           libccrtp
BuildRequires:  commoncpp2-devel gcc-c++ openssl-devel pkgconfig
Url:            http://www.gnu.org/software/ccrtp/
License:        GPL v3 or later
Group:          System/Libraries
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
AutoReqProv:    on
Summary:        A Common C++ Class Framework for RTP Packets
Version:        1.8.0
Release:        2
Source0:        ccrtp-%{version}.tar.bz2
Source1:        rpmlintrc
# Patch1:         ccrtp-gcc43.diff

%description
The ccrtp package offers a generic framework for sending and receiving
real-time streaming data over UDP packets using sending and receiving
packet queues.



Authors:
--------
    David Sugar <dyfet@ostel.com>
    Frederico Montesino Pouzols <p5087@quintero.fie.us.es>

%package -n libccrtp1
License:        GPL v3 or later
Group:          System/Libraries
Summary:        A Common C++ Class Framework for RTP Packets
Provides:       ccrtp = %{version}
Provides:       %{name} = %{version}
Obsoletes:      ccrtp <= %{version}
Obsoletes:      %{name} <= %{version}

%description -n libccrtp1
The ccrtp package offers a generic framework for sending and receiving
real-time streaming data over UDP packets using sending and receiving
packet queues.



Authors:
--------
    David Sugar <dyfet@ostel.com>
    Frederico Montesino Pouzols <p5087@quintero.fie.us.es>

%package devel
License:        GPL v3 or later
Summary:        Include-files and documentation for ccrtp
Group:          Development/Libraries/Other
Requires:       %{name} = %{version} commoncpp2-devel
PreReq:         %install_info_prereq

%description devel
This package contains files needed when developing applications using
ccrtp



Authors:
--------
    David Sugar <dyfet@ostel.com>
    Frederico Montesino Pouzols <p5087@quintero.fie.us.es>

%prep
%setup -q -n ccrtp-%version
# %patch1

%build
%configure --with-pic --disable-static --with-gnu-ld
make

%install
make DESTDIR=$RPM_BUILD_ROOT install
(cd doc; make DESTDIR=$RPM_BUILD_ROOT install)
rm -f %{buildroot}%{_libdir}/libccrtp1.la

%clean
rm -rf $RPM_BUILD_ROOT;

%post -n libccrtp1 -p /sbin/ldconfig

%postun -n libccrtp1 -p /sbin/ldconfig

%files -n libccrtp1
%defattr(-,root,root,0755)
%_libdir/libccrtp*.so.*

%files devel
%defattr(-,root,root,0755)
%doc AUTHORS COPYING NEWS README TODO ChangeLog
%_libdir/libccrtp*.so
%_libdir/pkgconfig/libccrtp1.pc
%dir %{_includedir}/ccrtp
%{_includedir}/ccrtp/*.h
%{_infodir}/ccrtp.info*

%post devel
%install_info --info-dir=%{_infodir} %{_infodir}/ccrtp.info.gz

%postun devel
%install_info_delete --info-dir=%{_infodir} %{_infodir}/ccrtp.info.gz

%changelog
