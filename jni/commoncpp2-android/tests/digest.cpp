#include <cc++/misc.h>
#include <cc++/digest.h>
#include <iostream>
#include <cstdio>

#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif

int main()
{
	ost::MD5Digest digest;

	digest << "pippo";
	std::cout << digest << std::endl;;
	return 0;
}


