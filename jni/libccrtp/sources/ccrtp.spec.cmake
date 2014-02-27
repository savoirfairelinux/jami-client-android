%{!?release: %define release 0}
%{!?version: %define version @VERSION@}

%define _libname libccrtp2
%define _devname libccrtp-devel

Summary: A Common C++ Class Framework for RTP Packets
Name: libccrtp
Version: %{version}
Release: %{release}%{?dist}
License: LGPL v2 or later
Group: Development/Libraries/C and C++
URL: http://www.gnu.org/software/commoncpp/commoncpp.html
Source0: http://www.gnutelephony.org/dist/tarballs/ccrtp-%{version}.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root
BuildRequires: ucommon-devel >= 5.0.0
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

%package -n %{_devname}
Group: Development/Libraries/C and C++
Summary: Headers and static link library for ccrtp
Requires: %{name} = %{version}-%{release}
Requires: ucommon-devel >= 5.0.0
Requires: libgcrypt-devel

%description -n %{_devname}
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

%files
%defattr(-,root,root,0755)
%doc AUTHORS COPYING ChangeLog README COPYING.addendum
%{_libdir}/*.so.*

%files -n %{_devname}
%defattr(-,root,root,0755)
%{_libdir}/*.so
%{_libdir}/pkgconfig/*.pc
%dir %{_includedir}/ccrtp
%{_includedir}/ccrtp/*.h
%{_infodir}/ccrtp.info*

%post -p /sbin/ldconfig

%postun -p /sbin/ldconfig

%post devel
%install_info --info-dir=%{_infodir} %{_infodir}/ccrtp.info.gz

%postun devel
%install_info_delete --info-dir=%{_infodir} %{_infodir}/ccrtp.info.gz

%changelog
* Thu Jan 06 2011 - Werner Dittmann <werner.dittmann@t-online.de>
- Add Skein MAC authentication algorithm

