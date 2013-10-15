// Copyright (C) 2001 Gianni Mariani
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
//
// As a special exception to the GNU General Public License, permission is
// granted for additional uses of the text contained in its release
// of Common C++.
//
// The exception is that, if you link the Common C++ library with other
// files to produce an executable, this does not by itself cause the
// resulting executable to be covered by the GNU General Public License.
// Your use of that executable is in no way restricted on account of
// linking the Common C++ library code into it.
//
// This exception does not however invalidate any other reasons why
// the executable file might be covered by the GNU General Public License.
//
// This exception applies only to the code released under the
// name Common C++.  If you copy code from other releases into a copy of
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.
//


//
// Example for Common C++ the command line parser interface.
//
//
// This exmaple code shows how to use the command line parser provided by
// CommonC++.  The command line parser provides an interface which is
// "object oriented" such that command line parameters are true "objects".
//
// Each command line option needs to be created.  By defining "CommandOption"s
// statically, the C++ constructor is called when the objects are loaded and
// before the "main" function is called.  The constructor links itself to
// a list of other CommandOptionXXX in the list provided.  If no
// list is specified in the constructor, a default one is used. Because of
// the undefined nature as to the order in which constructors are called,
// no assumption as to the order in which the CommandOptionXXX constructors
// are called should be made.
//
// CommandOptionXXX classes can be used to derive specialized parameter
// classes that are specific to applications.  The second example shows
// just how this can be done.
//

//
// Include the CommandOption definitions
//
#include <cc++/common.h>

#include <iostream>
#ifndef	WIN32
#include <cstdlib>
#endif

#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif

//
// The following definition of options all use the list header
// defaultCommandOptionList (which is specified as the value of the
// default parameter in the constructor.  This convention would
// allow other object files to link into the same list and add parameters
// to the command line of this executable.

CommandOptionArg	test_option1(
	"test_option1", "p", "This option takes an argument", true
);

CommandOptionNoArg	test_noarg(
	"test_noarg", "b", "This option does not take an argument"
);

CommandOptionNoArg	helparg(
	"help", "?", "Print help usage"
);

CommandOptionCollect	restoargs(
	0, 0, "Collect all the parameters", true
);


//
// Normally this would me the regular main().  In this example
// this processes the first command option list.
//
int Example_main( int argc, char ** argv )
{

	// Create a CommandOptionParse object.  This takes the
	// defaultCommandOptionList and parses the command line arguments.
	//
	CommandOptionParse * args = makeCommandOptionParse(
		argc, argv,
		"CommonC++ command like option interface.  This is example\n"
		"	code only."
	);

	// If the user requested help then suppress all the usage error
	// messages.
	if ( helparg.numSet ) {
		cerr << args->printUsage();
		::exit(0);
	}

	// Print usage your way.
	if ( args->argsHaveError() ) {
		cerr << args->printErrors();
		cerr << args->printUsage();
		::exit(1);
	}

	// Go off and run any option specific task
	args->performTask();

	// print all the -p options
	for ( int i = 0; i < test_option1.numValue; i ++ ) {
		cerr << "test_option1 = " << test_option1.values[ i ] << endl;
	}

	// print all the other options.
	for ( int i = 0; i < restoargs.numValue; i ++ ) {
		cerr << "restoargs " << i << " : " << restoargs.values[ i ] << endl;
	}

	delete args;

	return 0;
}


//
// This shows how to build a second option list.  The example is similar to
// the first as well as it shows how to derive a new command object.
//

CommandOption * TestList = 0;

extern CommandOptionRest	test_restoargs;

#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <sys/wait.h>
#include <strstream>

//
// This is a parameter class derived from CommandOptionArg that takes
// a file name parameter and detects wether the file is accessible
// flagging an error if the file is inaccessible to read.
//
class file_option : public CommandOptionArg {
public:

	// the constructor calls the regular CommandOptionArg constructor
	// and all should be well.
	file_option(
		const char      * in_option_name,
		const char      * in_option_letter,
		const char      * in_description,
		bool              in_required = false,
		CommandOption  ** pp_next = & defaultCommandOptionList
	)
		: CommandOptionArg(
			in_option_name,
			in_option_letter,
			in_description,
			in_required,
			pp_next
		)
	{
	}

	//
	// When parsing is done check if the file is accessible and register
	// an error with the CommandOptionParse object to let it know so.
	virtual void parseDone( CommandOptionParse * cop ) {
		if ( numValue ) {
			if ( ::access( values[ numValue - 1 ], R_OK ) ) {
				int	errno_s = errno;
				strstream msg;
				msg << "Error: " << optionName << " '" << values[ numValue - 1 ];
				msg << "' : " << ::strerror( errno_s );

				cop->registerError( msg.str() );
			}
		}
	}

	//
	// Open said file.  Do some operations on things - like open the file.
	int OpenFile() {
		// Should put in way more error handling here ...
		return ::open( values[ numValue - 1 ], O_RDONLY );
	}

	//
	// The most elaborate way to spit the contents of a file
	// to standard output.
	pid_t   pid;
	virtual void performTask( CommandOptionParse * cop ) {
		pid = ::fork();

		if ( pid ) {
			return;
		}

		int fd = OpenFile();
		if ( fd < 0 ) {
			int errno_s = errno;
			cerr
				<< "Error:  '"
				<< values[ numValue - 1 ]
				<< "' : "
				<< ::strerror( errno_s )
			;

			::exit( 1 );
		}
		dup2(fd, 0);
		::execvp( test_restoargs.values[0], (char**) test_restoargs.values );
		::exit(1);
	}

	~file_option() {
		if ( pid <= 0 ) return;
		int status;
		::wait(&status);
	}
};


//
// This is the linked list head for the options in the second example.
// Note that the first example used the default value defined in the
// method.  Here it is explicitly specified as TestList in all the following
// CommandOption constructors.

file_option	test_file(
	"test_file", "f", "Filename to read from", true, &TestList
);

CommandOptionNoArg	test_xnoarg(
	"test_xnoarg", "b", "This option does not take an argument", false, &TestList
);

CommandOptionNoArg	test_helparg(
	"help", "?", "Print help usage", false, &TestList
);

CommandOptionRest	test_restoargs(
	0, 0, "Command to be executed", true, &TestList
);

//
// in most apps this would be the regular "main" function.
int Test_main( int argc, char ** argv )
{
	CommandOptionParse * args = makeCommandOptionParse(
		argc, argv,
		"Command line parser X test.\n"
		"	This example is executed when the command ends in 'x'\n"
		"	It shows how the -f parameter can be specialized.\n",
		TestList
	);

	// If the user requested help then suppress all the usage error
	// messages.
	if ( test_helparg.numSet ) {
		cerr << args->printUsage();
		::exit(0);
	}

	// Print usage your way.
	if ( args->argsHaveError() ) {
		cerr << args->printErrors();
		cerr << "Get help by --help\n";
		::exit(1);
	}

	// Go off and run any option specific task
	args->performTask();

	for ( int i = 0; i < test_file.numValue; i ++ ) {
		cerr << "test_file = " << test_file.values[ i ] << endl;
	}

	for ( int i = 0; i < test_restoargs.numValue; i ++ ) {
		cerr << "test_restoargs " << i << " : " << test_restoargs.values[ i ] << endl;
	}

	delete args;

	return 0;
}


//
// This switches behaviour of this executable depending of wether it is
// invoked with a command ending in "x".  This is mimicking for example
// the behaviour of bunzip2 and bzip2.  These executables are THE SAME
// file i.e.
//   0 lrwxrwxrwx    1 root     root    5 Oct 11 14:04 /usr/bin/bunzip2 -> bzip2*
// and the behaviour is determined by the executable name.
//
// This example is way more complex than the way most people will end up
// using feature.

int main( int argc, char ** argv )
{

	int i = ::strlen( argv[ 0 ] );

	// determine which real "main" function do I call
	if ( argv[ 0 ][ i - 1 ] == 'x' ) {
		return Test_main( argc, argv );
	} else {
		return Example_main( argc, argv );
	}

}
