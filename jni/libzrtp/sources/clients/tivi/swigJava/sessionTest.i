/*
 * Copyright (c) 2013 Slient Circle LLC.  All rights reserved.
 *
 *
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 */

/*
 * Call swig:         "swig -java -c++ -package wd.tivi -outdir wd/tivi session.i"
 * Compile wrapper:   "g++ -c -I$JAVA_HOME/include -I.. session_wrap.cxx"
 * Compile Java code: "javac -d class wd/tivi/*.java"
 */


%module (directors="1") tiviSession
%{
#include "CtZrtpCallback.h"
#include "CtZrtpSession.h"
%}
%include "typemaps.i"
%include "std_string.i"

/*
 * Actually there is no difference in handling of unsigned int and unsigned long
 * with repect to C++/Java mapping. SWIG maps both to Java Long and retains the
 * "size_t" in the JNI C code. Refer to SWIG's java/typemap.i file (usually located
 * in /usr/share/swig/... )
 *
 * However, I leave this in to point out that there may be a different handling
 * required for other architectures.
 */
#ifdef ARCH_32
typedef unsigned int size_t;
%apply unsigned int *INOUT { size_t *newLength };
%apply unsigned int *INOUT { size_t *maxLen };
%apply unsigned int *INOUT { size_t *sendLength };

#else

typedef unsigned long int size_t;
%apply unsigned long *INOUT { size_t *newLength };
%apply unsigned long *INOUT { size_t *maxLen };
%apply unsigned long *INOUT { size_t *sendLength };
#endif

typedef int int32_t;

%include "various.i"


%typemap(in)     (uint8_t *BYTE, size_t LENGTH) {
    $1 = (uint8_t *) JCALL2(GetByteArrayElements, jenv, $input, 0);
    $2 = (size_t)    JCALL1(GetArrayLength,       jenv, $input);
}
%typemap(jni)    (uint8_t *BYTE, size_t LENGTH) "jbyteArray"
%typemap(jtype)  (uint8_t *BYTE, size_t LENGTH) "byte[]"
%typemap(jstype) (uint8_t *BYTE, size_t LENGTH) "byte[]"
%typemap(javain) (uint8_t *BYTE, size_t LENGTH) "$javainput"


/*
 * Define a INOUT typemap for uint8_t, largely copied from SWIG's java/typemap.i
 */
%typemap(in)     (uint8_t *BYTE) {
  if (!$input) {
    SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "array null");
    return $null;
  }
  if (JCALL1(GetArrayLength, jenv, $input) == 0) {
    SWIG_JavaThrowException(jenv, SWIG_JavaIndexOutOfBoundsException, "Array must contain at least 1 element");
    return $null;
  }
  $1 = ($1_ltype) JCALL2(GetByteArrayElements, jenv, $input, 0);

}

%typemap(argout) (uint8_t* BYTE)
{ JCALL3(ReleaseByteArrayElements, jenv, $input, (jbyte *)$1, 0); }

%typemap(jni)    (uint8_t *BYTE) "jbyteArray"
%typemap(jtype)  (uint8_t *BYTE) "byte[]"
%typemap(jstype) (uint8_t *BYTE) "byte[]"
%typemap(javain) (uint8_t *BYTE) "$javainput"

/*
 * Define when to apply the new typemap: if SWIG sees a method signature (also
 * partial signature) it applies the patterns. "BYTE" is the placeholder for
 * the real parameter name.
 */
%apply (uint8_t *BYTE)   { (uint8_t *buffer) };

/*
 * Converts char* to Java byte[] array and not to Java String as usual.
 * A Java byte[] is more versatile because we can modify data inside the array
 * and reuse it as input to some other method.
 * 
 * The typemap 'char* BYTE' is availabe in SWIG's java/various.i library file thus
 * we can apply it immediately.
 */
%apply char *BYTE { char *helloHash };
%apply char *BYTE { char *cryptoString };
%apply char *BYTE { char *sendCryptoStr };
%apply char *BYTE { char *recvCryptoStr };

/*
 * Typemaps for the two callback classes. SWIG implements Callback to Java using
 * its director mechanism. This mechanism allows that you can extend a C++ class
 * with a Java class and call Java methods that overwrite C++ methods either from
 * Java code or from C++ code.
 */

/*
 * First define the typemap pattern and the lines to insert if this pattern triggers
 *
 * The first typemap defines the actions that move the data from the C++ buffer to
 * a Java byte[]. This is called "input to director java proxy". SWIG inserts
 * these lines before it calls the Java proxy.
 *
 * These typemaps also use the maps defined above for uint8_t* / size_t combination.
 * The standard SWIG library defines only char* / int combination.
 */
%typemap(directorin, descriptor="[B") (uint8_t *BYTE, size_t LENGTH) {
   jbyteArray jb = (jenv)->NewByteArray($2);
   (jenv)->SetByteArrayRegion(jb, 0, $2, (jbyte *)$1);
   $input = jb;
}

/*
 * Perform these actions if the Java method returns. In this case we copy the Java
 * array back to the C++ buffer.
 */
%typemap(directorargout) (uint8_t *BYTE, size_t LENGTH)
%{(jenv)->GetByteArrayRegion($input, 0, $2, (jbyte *)$1); %}

%typemap(javadirectorin) (uint8_t *BYTE, size_t LENGTH) "$jniinput"
/* %typemap(javadirectorout) (uint8_t *BYTE, size_t LENGTH) "$javacall" */

%apply (uint8_t *BYTE, size_t LENGTH)   { (uint8_t* packet, size_t length) };

/*
 * Use the director feature for the callback classes only.
 * CAVEAT: these a pure virtual C++ classes. The Java implementation MUST overwrite
 * all methods, otherwise you get a nice error message and process termination.
 */
%feature("director") CtZrtpCb;
%feature("director") CtZrtpSendCb;

/* Don't confuse SWIG with the __EXPORT macro */
#define __EXPORT
%include ../CtZrtpSession.h
%include ../CtZrtpCallback.h
