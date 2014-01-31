// Copyright (C) 2010 David Sugar, Tycho Softworks.
//
// This file is part of GNU uCommon C++.
//
// GNU uCommon C++ is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// GNU uCommon C++ is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with GNU uCommon C++.  If not, see <http://www.gnu.org/licenses/>.

#include <ucommon-config.h>
#include <ucommon/ucommon.h>
#include <ucommon/export.h>
#include <ucommon/secure.h>
#include <errno.h>
#include "md5.h"
#include "sha1.h"

#ifdef  _MSWINDOWS_
#include <wincrypt.h>
#endif

#define NAMESPACE_LOCAL namespace __secure__ {
#define LOCAL_NAMESPACE __secure__

NAMESPACE_LOCAL
using namespace UCOMMON_NAMESPACE;

#ifdef  _MSWINDOWS_
extern HCRYPTPROV _handle;
#endif

END_NAMESPACE

using namespace UCOMMON_NAMESPACE;
using namespace LOCAL_NAMESPACE;

