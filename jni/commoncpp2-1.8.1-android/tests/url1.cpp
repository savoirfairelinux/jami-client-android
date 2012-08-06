#include <cc++/common.h>

// This was a test base64 stuff

#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif

#define BUFLEN 512
char buf1[BUFLEN];
char buf2[BUFLEN];

bool errorOccurred = false;
char status[256] = "";

void printBug(const char*msg)
{
	errorOccurred = true;
	printf("status = %s\n%s!\n",status,msg);
}

const char fillChar='&';

void initBuf(char* buf)
{
	memset(buf,fillChar,BUFLEN);
}

void checkBuf(char* buf,int prev,int size)
{
	int i;
	for(i=0;i<prev;++i)
		if (buf[i] != fillChar) {
			printBug("buffer overflow founded");
			return;
		}
	for(i=prev+size;i<BUFLEN;++i)
		if (buf[i] != fillChar) {
			printBug("buffer overflow founded");
			return;
		}
}

// check with binary functions
void check1(unsigned char* s,size_t len,size_t buflen1,size_t buflen2,bool checkEqual=false)
{
	initBuf(buf1);
	b64Encode(s,len,buf1+16,buflen1);
	checkBuf(buf1,16,buflen1);
	initBuf(buf2);
	b64Decode(buf1+16,(unsigned char*)buf2+16,buflen2);
	checkBuf(buf2,16,buflen2);
	if (checkEqual && memcmp(s,buf2+16,len) != 0)
		printBug ("buffer different");
}

// check with old string
void check2(const char* s,size_t buflen,bool checkEqual=false)
{
	if (!buflen) return;
	initBuf(buf1);
	b64Encode(s,buf1+16,buflen);
	checkBuf(buf1,16,buflen);
	initBuf(buf2);
	size_t buflen2 = strlen(buf1+16)+1;
	b64Decode(buf1+16,buf2+16);
	checkBuf(buf2,16,buflen2);
	if (checkEqual && strcmp(s,buf2+16) != 0) {
		printBug ("buffer different");
		printf("'%s' != '%s'\n'%s'\n",s,buf2+16,buf1+16);
	}
}

// check buffer overflow on string
void checkStringOverflow(char* s,unsigned int len)
{
	bool execCheck2 = (strlen(s) == len);
	for(unsigned int l1=0;l1<32;++l1) {
		sprintf(status,"%s %d",s,l1);
		if (execCheck2)
			check2(s,l1,l1 >= (len+2)/3*4+1);
		for(unsigned int l2=0;l2<32;++l2)
			check1((unsigned char*)s,len,l1,l2,
					(l1 >= (len+2)/3*4+1)
					&& (l2 >= len) );
	}
}

int main()
{
	checkStringOverflow((char *)"",0);
	checkStringOverflow((char *)"aaa",3);
	if (!errorOccurred)
		printf("All seem ok\n");
	return 0;
}


