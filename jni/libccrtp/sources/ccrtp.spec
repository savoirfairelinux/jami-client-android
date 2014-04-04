# Copyright (c) 2014 David Sugar, Tycho Softworks.
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY, to the extent permitted by law; without
# even the implied warranty of MERCHANTABILITY or FITNESS FOR A
# PARTICULAR PURPOSE.

Summary: A Common C++ Class Framework for RTP Packets
Name: ccrtp
Version: 2.0.8
Release: 0%{?dist}
License: LGPL v2 or later
Group: System/Library
URL: http://www.gnu.org/software/commoncpp/commoncpp.html
Source0: ccrtp-%{version}.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root
BuildRequires: ucommon-devel >= 6.0.0
BuildRequires: pkgconfig
BuildRequires: libgcrypt-devel
BuildRequires: gcc-c++

%description
GNU ccRTP is a generic, extensible and efficient C++ framework for
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
Requires: %{name} = %{version}
Requires: ucommon-devel >= 6.0.0
Requires: libgcrypt-devel

%description devel
This package provides the header files, link libraries, and
documentation for building applications that use GNU ccrtp.

%prep
%setup -q

%build
%configure \
    --with-pkg-config \
    --disable-static \

%{__make} %{?_smp_mflags}

%install
%{__rm} -rf %{buildroot}
%{__make} DESTDIR=%{buildroot} INSTALL="install -p" install
%{__rm} %{buildroot}%{_libdir}/*.la
%{__rm} -rf %{buildroot}/%{_infodir}

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%doc AUTHORS COPYING ChangeLog README COPYING.addendum
%{_libdir}/*.so.*

%files devel
%defattr(-,root,root,-)
%{_libdir}/*.a
%{_libdir}/*.so
%{_libdir}/*.la
%{_libdir}/pkgconfig/*.pc
%dir %{_includedir}/ccrtp
%{_includedir}/ccrtp/*.h

%post -p /sbin/ldconfig

%postun -p /sbin/ldconfig

%changelog
* Thu Jan 06 2011 - Werner Dittmann <werner.dittmann@t-online.de>
- Add Skein MAC authentication algorithm
