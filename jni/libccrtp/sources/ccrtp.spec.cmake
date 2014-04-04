# Copyright (c) 2014 David Sugar, Tycho Softworks.
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY, to the extent permitted by law; without
# even the implied warranty of MERCHANTABILITY or FITNESS FOR A
# PARTICULAR PURPOSE.

%{!?release: %define release 0}
%{!?version: %define version @VERSION@}

%define libname libccrtp2

Summary: A Common C++ Class Framework for RTP Packets
Name: libccrtp
Version: @VERSION@
Release: 0
License: LGPL v2 or later
Group: Development/Libraries/C and C++
URL: http://www.gnu.org/software/commoncpp/commoncpp.html
Source0: http://www.gnutelephony.org/dist/tarballs/ccrtp-%{version}.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root
BuildRequires: ucommon-devel >= 6.0.0
BuildRequires: pkgconfig libstdc++-devel libgcrypt-devel gcc-c++ cmake

%define srcdirname %{name}-%{version}

%description
ccRTP is a generic, extensible and efficient C++ framework for
developing applications based on the Real-Time Transport Protocol
(RTP) from the IETF. It is based on Common C++ and provides a full
RTP/RTCP stack for sending and receiving of realtime data by the use
of send and receive packet queues. ccRTP supports unicast,
multi-unicast and multicast, manages multiple sources, handles RTCP
automatically, supports different threading models and is generic as
for underlying network and transport protocols.

%package devel
Group: Development/Libraries/C and C++
Summary: Headers and static link library for ccrtp
Requires: %{libname} = %{version}-%{release}
Requires: ucommon-devel >= 6.0.0
Requires: libgcrypt-devel

%description devel
This package provides the header files, link libraries, and
documentation for building applications that use GNU ccrtp

%prep
%setup -q -n ccrtp-%version

%build
%{__mkdir} build
cd build
cmake -DCMAKE_INSTALL_PREFIX=%{_prefix} \
      -DSYSCONFDIR=%{_sysconfdir} \
      -DMANDIR=%{_mandir} \
      -DCMAKE_VERBOSE_MAKEFILE=TRUE \
      -DCMAKE_C_FLAGS_RELEASE:STRING="$RPM_OPT_FLAGS" \
      -DCMAKE_CXX_FLAGS_RELEASE:STRING="$RPM_OPT_FLAGS" \
      ..
%{__make} %{?_smp_mflags}

%install
cd build
%{__rm} -rf %{buildroot}
make install DESTDIR=%{buildroot}
%{__rm} -rf %{buildroot}%{_libdir}/*.la

%clean
%{__rm} -rf %{buildroot}

%files -n %libname
%defattr(-,root,root,-)
%doc AUTHORS COPYING ChangeLog README COPYING.addendum
%{_libdir}/*.so.*

%files devel
%defattr(-,root,root,-)
%{_libdir}/*.so
%{_libdir}/pkgconfig/*.pc
%dir %{_includedir}/ccrtp
%{_includedir}/ccrtp/*.h
%{_infodir}/ccrtp.info*

%post -n %libname -p /sbin/ldconfig

%postun -n %libname -p /sbin/ldconfig

%post devel
%install_info --info-dir=%{_infodir} %{_infodir}/ccrtp.info.gz

%postun devel
%install_info_delete --info-dir=%{_infodir} %{_infodir}/ccrtp.info.gz

%changelog
* Thu Jan 06 2011 - Werner Dittmann <werner.dittmann@t-online.de>
- Add Skein MAC authentication algorithm

