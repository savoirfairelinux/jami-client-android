%{!?release: %define release 0}
%{!?version: %define version 1.8.0}

%define _libname libccrtp1
%define _devname libccrtp-devel

Summary: A Common C++ Class Framework for RTP Packets
Name: libccrtp
Version: %{version}
Release: %{release}%{?dist}
License: LGPL v2 or later
Group: Development/Libraries/C and C++
URL: http://www.gnu.org/software/commoncpp/commoncpp.html
Source0: ccrtp-%{version}.tar.bz2
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root 
BuildRequires: commoncpp2-devel >= 1.4.0
BuildRequires: pkgconfig
BuildRequires: libstdc++-devel
BuildRequires: libgcrypt-devel
BuildRequires: gcc-c++

%description
ccRTP is a generic, extensible and efficient C++ framework for
developing applications based on the Real-Time Transport Protocol
(RTP) from the IETF. It is based on Common C++ and provides a full
RTP/RTCP stack for sending and receiving of realtime data by the use
of send and receive packet queues. ccRTP supports unicast,
multi-unicast and multicast, manages multiple sources, handles RTCP
automatically, supports different threading models and is generic as
for underlying network and transport protocols.

%package -n %{_libname}
Group: Development/Libraries/C and C++
Summary: Runtime library for GNU RTP Stack
Provides: %{name} = %{version}-%{release}

%package -n %{_devname}
Group: Development/Libraries/C and C++
Summary: Headers and static link library for ccrtp
Requires: %{_libname} = %{version} 
Requires: commoncpp2-devel >= 1.4.0
Requires: libgcrypt-devel
Provides: %{name}-devel = %{version}-%{release}

%description -n %{_libname}
This package contains the runtime library needed by applications that use 
the GNU RTP stack.

%description -n %{_devname}
This package provides the header files, link libraries, and 
documentation for building applications that use GNU ccrtp. 

%prep
%setup
%build
%configure

make %{?_smp_mflags} LDFLAGS="-s" CXXFLAGS="$RPM_OPT_FLAGS"

%install

%makeinstall
rm -rf %{buildroot}/%{_infodir} 

%clean
rm -rf %{buildroot}

%files -n %{_libname}
%defattr(-,root,root,0755)
%doc AUTHORS COPYING ChangeLog README COPYING.addendum
%{_libdir}/*.so.*

%files -n %{_devname}
%defattr(-,root,root,0755)
%{_libdir}/*.a
%{_libdir}/*.so
%{_libdir}/*.la   
%{_libdir}/pkgconfig/*.pc
%dir %{_includedir}/ccrtp
%{_includedir}/ccrtp/*.h

%post -n %{_libname} -p /sbin/ldconfig

%postun -n %{_libname} -p /sbin/ldconfig  

%changelog
* Tue Jan 06 2011 - Werner Dittmann <werner.dittmann@t-online.de>
- Add Skein MAC authentication algorithm
