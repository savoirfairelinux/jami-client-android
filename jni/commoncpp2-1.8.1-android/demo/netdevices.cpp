// Copyright (C) 2002 Christian Prochnow.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

#include <cc++/network.h>
#include <iostream>

int main(int argc, char* argv[])
{
  std::vector<ost::NetworkDeviceInfo> devs;

  if(!enumNetworkDevices(devs)) {
	  std::cerr << "Could not get list of network devices!" << std::endl;
	  return -1;
	}

  std::cout << "Available network devices:" << std::endl;
  std::vector<ost::NetworkDeviceInfo>::const_iterator i = devs.begin();
  while(i != devs.end()) {
	  std::cout << (*i).name() << " address: " << (*i).address()
		<< ", brdcast: " << (*i).broadcast()
		<< ", netmask: " << (*i).netmask()
		<< ", mtu: " << (*i).mtu() << std::endl;
	  ++i;
	}
}
