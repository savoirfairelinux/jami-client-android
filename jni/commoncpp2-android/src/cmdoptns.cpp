// Copyright (C) 2001-2010 Gianni Mariani
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
// As a special exception, you may use this file as part of a free software
// library without restriction.  Specifically, if other files instantiate
// templates or use macros or inline functions from this file, or you compile
// this file and link it with other files to produce an executable, this
// file does not by itself cause the resulting executable to be covered by
// the GNU General Public License.  This exception does not however
// invalidate any other reasons why the executable file might be covered by
// the GNU General Public License.
//
// This exception applies only to the code released under the name GNU
// Common C++.  If you copy code from other releases into a copy of GNU
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for GNU Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.
//

//
// cmdoptns.cpp
//

#include <cc++/config.h>
#include <cc++/string.h>
#include <cc++/thread.h>
#include <cc++/exception.h>
#include <cc++/export.h>
#include <cc++/cmdoptns.h>

#ifndef HAVE_GETOPT_LONG
// fix problem with vc++ library
#undef __argc
#undef __argv
#include "getopt.h"
#else
#include <getopt.h>
#endif

#ifndef WIN32
#include <unistd.h>
#endif

#include <cstdlib>
#include <iostream>
#include <fstream>

using std::fstream;

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

//
// In most cases, users will use this default option list.
//
CommandOption * defaultCommandOptionList = 0;

CommandOption::CommandOption(
    const char      * inOptionName,
    const char      * inOptionLetter,
    const char      * inDescription,
    OptionType        inOptionType,
    bool              inRequired,
    CommandOption  ** ppNext
)
    :   optionName( inOptionName ),
        optionLetter( inOptionLetter ),
        description( inDescription ),
        optionType( inOptionType ),
        required( inRequired ),
        next( * ppNext ) {
    * ppNext = this;
}

CommandOptionWithArg::CommandOptionWithArg(
    const char      * inOptionName,
    const char      * inOptionLetter,
    const char      * inDescription,
    OptionType        inOptionType,
    bool              inRequired,
    CommandOption  ** ppNext
)
    : CommandOption(
        inOptionName,
        inOptionLetter,
        inDescription,
        inOptionType,
        inRequired,
        ppNext
    ), values( 0 ), numValue( 0 )
{
}


CommandOptionArg::CommandOptionArg(
    const char      * inOptionName,
    const char      * inOptionLetter,
    const char      * inDescription,
    bool              inRequired,
    CommandOption  ** ppNext
)
    : CommandOptionWithArg(
        inOptionName,
        inOptionLetter,
        inDescription,
        hasArg,
        inRequired,
        ppNext
    )
{
}

CommandOptionRest::CommandOptionRest(
    const char      * inOptionName,
    const char      * inOptionLetter,
    const char      * inDescription,
    bool              inRequired,
    CommandOption  ** ppNext
)
    : CommandOptionWithArg(
        inOptionName,
        inOptionLetter,
        inDescription,
        trailing,
        inRequired,
        ppNext
    )
{
}

CommandOptionCollect::CommandOptionCollect(
    const char      * inOptionName,
    const char      * inOptionLetter,
    const char      * inDescription,
    bool              inRequired,
    CommandOption  ** ppNext
)
    : CommandOptionWithArg(
        inOptionName,
        inOptionLetter,
        inDescription,
        collect,
        inRequired,
        ppNext
    )
{
}

CommandOptionNoArg::CommandOptionNoArg(
    const char      * inOptionName,
    const char      * inOptionLetter,
    const char      * inDescription,
    bool              inRequired,
    CommandOption  ** ppNext
)
    : CommandOption(
        inOptionName,
        inOptionLetter,
        inDescription,
        noArg,
        inRequired,
        ppNext
    ), numSet( 0 )
{
}

// ======== CommandOption =============================================
// PURPOSE:
//  CommandOption dummy methods ..
//

void CommandOption::parseDone( CommandOptionParse * cop )
{
}

void CommandOption::foundOption( CommandOptionParse * cop, const char * value)
{
}

void CommandOption::foundOption( CommandOptionParse * cop, const char ** value, int num )
{
}

void CommandOption::performTask( CommandOptionParse * cop )
{
}

bool CommandOption::hasValue()
{
    return true;
}

CommandOption::~CommandOption()
{
}

CommandOptionWithArg::~CommandOptionWithArg()
{
    if ( values ) {
        free( values );
        values = 0;
    }
}

CommandOptionParse::~CommandOptionParse(void)
{
}

// ======== CommandOptionArg ==========================================
// PURPOSE:
//  Methods for CommandOptionArg
//

bool CommandOptionWithArg::hasValue()
{
    return numValue > 0;
}

CommandOptionArg::~CommandOptionArg()
{
}

//
//
static void my_alloc( char *** vals, int num, int incr )
{
    int num_alloc = 0;
    if ( * vals ) {
        num_alloc = num | 3;
    }

    if ( ( incr + num ) > num_alloc ) {
        int newsiz = ( incr + num ) | 3;
        * vals = ( char ** ) realloc( * vals, sizeof( ** vals ) * newsiz );
    }
}

void CommandOptionWithArg::foundOption( CommandOptionParse * cop, const char * value )
{
    if ( value ) {
        my_alloc( ( char *** ) & values, numValue ? numValue + 1 : 0, 1 );
        values[ numValue ++ ] = value;
        values[ numValue ] = 0;
    }
}

void CommandOptionWithArg::foundOption( CommandOptionParse * cop, const char ** value, int num )
{
    my_alloc( ( char *** ) & values, numValue ? numValue + 1 : 0, num + 1 );

    int j = 0;
    for ( int i = numValue; j < num; i ++, j ++ ) {
        values[ i ] = value[ j ];
    }
    numValue += num;
    values[ numValue ] = 0;
}


void CommandOptionNoArg::foundOption( CommandOptionParse * cop, const char * value)
{
    numSet ++;
}

// ======== CommandOptionParse ========================================
// PURPOSE:
//  methods for CommandOptionParse
//

class CommandOptionParse_impl : public CommandOptionParse {
public:

    const char  * comment;
    int               num_options;
    struct option   * long_options;
    CommandOption  ** opt_list;
    CommandOption  ** co_list;
    char            * optstring;
    int     argc;
    char        ** argv;
    bool        has_err;
    char        * fail_arg;
    bool        usage_string_set;
    bool        required_errors_set;
    String      error_msgs;
    CommandOption   * fail_option;
    CommandOption   * trailing;

    String            usage_string;

    virtual ~CommandOptionParse_impl() {
        delete[] opt_list;
        delete[] co_list;
        delete[] optstring;
        delete[] long_options;
    }

    CommandOptionParse_impl(
        int             in_argc,
        char         ** in_argv,
        const char        * in_comment,
        CommandOption * options
    ) :
        comment( in_comment ),
        argc( in_argc ),
        argv( in_argv ),
        has_err( false ),
        fail_arg( 0 ),
        usage_string_set( false ),
        required_errors_set( false ),
        error_msgs( "" ),
        fail_option( 0 ),
        trailing(0) {

        // First need to count all options.

        CommandOption       * to = options;
        int                   ocnt = 0;
        int                   ccnt = 0;
        int                   flag;

        while ( to ) {
            if ( to->optionName ) ocnt ++;
            ccnt ++;
            to = to->next;
        }

        num_options = ccnt;
#ifdef  __KCC
        co_list = new (CommandOption **)[ocnt];
        opt_list = new (CommandOption **)[ccnt];
#else
        // fix compiling bug in vc++
        typedef CommandOption* PCommandOption;
        co_list = new PCommandOption[ocnt];
        opt_list = new PCommandOption[ccnt];
#endif
        long_options = new option[ccnt+1];
        optstring = new char[ 2*ccnt+2 ];

        // initialize the last option count
        long_options[ ocnt ].name = 0;
        long_options[ ocnt ].has_arg = 0;
        long_options[ ocnt ].flag = 0;
        long_options[ ocnt ].val = 0;

        char    *tos = optstring;
        *(tos++) = '+';
        to = options;
        while ( to ) {

            if ( to->optionType == CommandOption::trailing ) {
                if ( ! trailing ) {
                    trailing = to;
                }
            } else if ( to->optionType == CommandOption::collect ) {
                trailing = to;
            }

            opt_list[ -- ccnt ] = to;

            if ( to->optionName ) {
                -- ocnt;
                co_list[ ocnt ] = to;
                long_options[ ocnt ].name = to->optionName;
                long_options[ ocnt ].has_arg = to->optionType == CommandOption::hasArg;
                long_options[ ocnt ].flag = & flag;
                long_options[ ocnt ].val = ocnt;
            }

            if (  to->optionLetter && to->optionLetter[ 0 ] ) {
                * tos ++ = to->optionLetter[ 0 ];
                if ( to->optionType == CommandOption::hasArg ) {
                    * tos ++ = ':';
                }
            }


            to = to->next;
        }
        * tos = 0;

        int c;
        int optionIndex;

        opterr = 0; // tell getopt_long not to print any errors
        flag = -1;
        while ( optind < argc ) {

            if (
                (
                    c = getopt_long(
                        argc, argv, optstring, long_options, &optionIndex
                    )
                ) == -1
            ) {
                if ( ! trailing ) {
                    break;
                } else if ( trailing->optionType == CommandOption::trailing ) {
                    break;
                } else {
                    optarg = argv[ optind ];
                    optind ++;
                    to = trailing;
                }

            } else if ( flag != -1 ) {
                to = co_list[ flag ];
                flag = -1;
            } else if ( c == '?' ) {

                if ( optind < 2 ) {
                    fail_arg = argv[ optind ];
                } else {
                    fail_arg = argv[ optind - 1 ];
                }

                has_err = true;

                return;

            } else {

                // need to search through the options.

                for ( int i = 0; i < num_options; i ++ ) {

                    to = opt_list[ i ];
                    if ( ! to->optionLetter ) continue;

                    if ( c == to->optionLetter[ 0 ] ) {

                        break;
                    }
                }
                // assert( to );
            }

            // do we terminate here ?
            if ( to->optionType == CommandOption::trailing ) {
                break;
            }

            if ( c != ':' ) {
                to->foundOption( this, optarg );
            } else {
                has_err = true;
                fail_option = to;
                break;
            }

        }

        if ( optind < argc ) {
            if ( trailing ) {
                trailing->foundOption(
                    this,
                    ( const char ** ) ( argv + optind ),
                    argc - optind
                );
            } else {
                has_err = true;
                fail_arg = argv[ optind ];
            }
        }

        // Now check to see that all required args made it !

        for ( int i = 0; i < num_options; i ++ ) {
            CommandOption   * toq = opt_list[ i ];

            // Tell this parameter that it's done now.
            toq->parseDone( this );

            if ( toq->required && ! toq->hasValue() ) {
                has_err = true;
                break;
            }
        }

    }

    bool    argsHaveError();

    virtual const char * printUsage();
    virtual const char * printErrors();

    void makePrintErrors() {
        if ( required_errors_set ) return;
        required_errors_set = true;

        if ( fail_arg ) {
            error_msgs = error_msgs + "Unknown/malformed option '" + fail_arg + "' \n";
        } else if ( fail_option ) {
            String name;
            bool name_msg;
            if ( fail_option->optionName ) {
                name_msg = true;
                name = fail_option->optionName;
            } else if ( fail_option->optionLetter ) {
                name_msg = true;
                name = fail_option->optionLetter;
            } else if ( fail_option == trailing ) {
                name_msg = false;
            } else {
                name = "--option with no name--";
                name_msg = true;
            }
            if ( name_msg ) {
                error_msgs = error_msgs + "Option '" + name + "' requires value\n";
            }
        } else if ( has_err ) {

            // loop thru all required args

            for ( int i = 0; i < num_options; i ++ ) {
                CommandOption   * to = opt_list[ i ];

                if ( to->required && ! to->hasValue() ) {
                    error_msgs = error_msgs + "Value required for option '";

                    if ( to->optionName ) {
                        error_msgs = error_msgs + "--" + to->optionName;
                    } else if ( to->optionLetter && to->optionLetter[ 0 ] ) {
                        error_msgs = error_msgs + '-' + to->optionLetter[ 0 ];
                    } else {
                        error_msgs = error_msgs + to->description;
                    }

                    error_msgs = error_msgs + "' is missing\n";
                }
            }

        }

    }


    void makePrintUsage() {
        if ( usage_string_set ) return;

        String  str( "" );

        String  str_argv0 = argv[ 0 ];

        str = str + (char *)"Usage : ";

        String::size_type slashpos = str_argv0.rfind('/');
        if ( slashpos > str_argv0.length() ) {
            slashpos = 0;
        } else {
            slashpos ++;
        }

        str.append( str_argv0, slashpos, str_argv0.length() - slashpos );

        str = str + ' ' + comment + '\n';

        for ( int i = 0; i < num_options; i ++ ) {

            CommandOption   * to = opt_list[ i ];
            char            * begin = (char *)"\t";
            char            * obegin = (char *)"\t";

            to = opt_list[ i ];

            if ( to->optionLetter && to->optionLetter[ 0 ] ) {
                str = str + begin + '-' + to->optionLetter[ 0 ];
                begin = (char *)", ";
                obegin = (char *)" - ";
            }

            if ( to->optionName ) {
                str = str + begin + "--" + to->optionName;
                begin = (char *)", ";
                obegin = (char *)" - ";
            }

            if ( to->optionType == CommandOption::hasArg ) {
                str = str + begin + " <value>";
            } else if ( to->optionType == CommandOption::trailing ) {
                str = str + begin + " <rest of command...>";
            } else if ( to->optionType == CommandOption::collect ) {
                str = str + begin + " <...>";
            }

            str = str + obegin + to->description + "\n";
        }

        usage_string = str;
    }

    virtual void registerError( const char * errMsg ) {
        error_msgs = error_msgs + errMsg + '\n';
        has_err = true;
    }

    virtual void performTask() {
        for ( int i = 0; i < num_options; i ++ ) {
            CommandOption   * to = opt_list[ i ];

            // Each parameter has this invoked
            to->performTask( this );

        }
    }

};

CommandOptionParse * makeCommandOptionParse(
    int                argc,
    char            ** argv,
    const char       * comment,
    CommandOption    * options
) {
    return new CommandOptionParse_impl( argc, argv, comment, options );
}

bool    CommandOptionParse_impl::argsHaveError()
{
    return has_err;
}

const char * CommandOptionParse_impl::printUsage()
{
    makePrintUsage();
    return usage_string.c_str();
}

const char * CommandOptionParse_impl::printErrors()
{
    makePrintErrors();
    return error_msgs.c_str();
}

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */

