/*************************************************
*      Perl-Compatible Regular Expressions       *
*************************************************/

/* PCRE is a library of functions to support regular expressions whose syntax
and semantics are as close as possible to those of the Perl 5 language.

                  Main Library written by Philip Hazel
           Copyright (c) 1997-2011 University of Cambridge

  This JIT compiler regression test program was written by Zoltan Herczeg
                      Copyright (c) 2010-2011

-----------------------------------------------------------------------------
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.

    * Neither the name of the University of Cambridge nor the names of its
      contributors may be used to endorse or promote products derived from
      this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
-----------------------------------------------------------------------------
*/

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <stdio.h>
#include <string.h>
#include "pcre.h"

#define PCRE_BUG 0x80000000

/*
 Hungarian utf8 characters
 \xc3\xa9 = 0xe9 = 233 (e') \xc3\x89 = 0xc9 = 201 (E')
 \xc3\xa1 = 0xe1 = 225 (a') \xc3\x81 = 0xc1 = 193 (A')
 \xe6\x92\xad = 0x64ad = 25773 (a valid kanji)
 \xc2\x85 = 0x85 (NExt Line = NEL)
 \xc2\xa1 = 0xa1 (Inverted Exclamation Mark)
 \xe2\x80\xa8 = 0x2028 (Line Separator)
 \xc8\xba = 570 \xe2\xb1\xa5 = 11365 (lowercase length != uppercase length)
 \xcc\x8d = 781 (Something with Mark property)
*/

static void setstack(pcre_extra *extra);
static int regression_tests(void);

int main(void)
{
	int jit = 0;
	pcre_config(PCRE_CONFIG_JIT, &jit);
	if (!jit) {
		printf("JIT must be enabled to run pcre_jit_test\n");
		return 1;
	}
	return regression_tests();
}

static pcre_jit_stack* callback(void *arg)
{
	return (pcre_jit_stack *)arg;
}

static void setstack(pcre_extra *extra)
{
	static pcre_jit_stack *stack;
	if (stack) pcre_jit_stack_free(stack);
	stack = pcre_jit_stack_alloc(1, 1024 * 1024);
	pcre_assign_jit_stack(extra, callback, stack);
}

/* --------------------------------------------------------------------------------------- */

#define MUA     (PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_ANYCRLF)
#define MUAP    (PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_ANYCRLF | PCRE_UCP)
#define CMUA    (PCRE_CASELESS | PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_ANYCRLF)
#define CMUAP   (PCRE_CASELESS | PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_ANYCRLF | PCRE_UCP)
#define MA      (PCRE_MULTILINE | PCRE_NEWLINE_ANYCRLF)
#define MAP     (PCRE_MULTILINE | PCRE_NEWLINE_ANYCRLF | PCRE_UCP)
#define CMA     (PCRE_CASELESS | PCRE_MULTILINE | PCRE_NEWLINE_ANYCRLF)

struct regression_test_case {
	int flags;
	int start_offset;
	const char *pattern;
	const char *input;
};

static struct regression_test_case regression_test_cases[] = {
	/* Constant strings. */
	{ MUA, 0, "AbC", "AbAbC" },
	{ MUA, 0, "ACCEPT", "AACACCACCEACCEPACCEPTACCEPTT" },
	{ CMUA, 0, "aA#\xc3\xa9\xc3\x81", "aA#Aa#\xc3\x89\xc3\xa1" },
	{ MA, 0, "[^a]", "aAbB" },
	{ CMA, 0, "[^m]", "mMnN" },
	{ MA, 0, "a[^b][^#]", "abacd" },
	{ CMA, 0, "A[^B][^E]", "abacd" },
	{ CMUA, 0, "[^x][^#]", "XxBll" },
	{ MUA, 0, "[^a]", "aaa\xc3\xa1#Ab" },
	{ CMUA, 0, "[^A]", "aA\xe6\x92\xad" },
	{ MUA, 0, "\\W(\\W)?\\w", "\r\n+bc" },
	{ MUA, 0, "\\W(\\W)?\\w", "\n\r+bc" },
	{ MUA, 0, "\\W(\\W)?\\w", "\r\r+bc" },
	{ MUA, 0, "\\W(\\W)?\\w", "\n\n+bc" },
	{ MUA, 0, "[axd]", "sAXd" },
	{ CMUA, 0, "[axd]", "sAXd" },
	{ CMUA, 0, "[^axd]", "DxA" },
	{ MUA, 0, "[a-dA-C]", "\xe6\x92\xad\xc3\xa9.B" },
	{ MUA, 0, "[^a-dA-C]", "\xe6\x92\xad\xc3\xa9" },
	{ CMUA, 0, "[^\xc3\xa9]", "\xc3\xa9\xc3\x89." },
	{ MUA, 0, "[^\xc3\xa9]", "\xc3\xa9\xc3\x89." },
	{ MUA, 0, "[^a]", "\xc2\x80[]" },
	{ CMUA, 0, "\xf0\x90\x90\xa7", "\xf0\x90\x91\x8f" },
	{ CMA, 0, "1a2b3c4", "1a2B3c51A2B3C4" },
	{ PCRE_CASELESS, 0, "\xff#a", "\xff#\xff\xfe##\xff#A" },
	{ PCRE_CASELESS, 0, "\xfe", "\xff\xfc#\xfe\xfe" },
	{ PCRE_CASELESS, 0, "a1", "Aa1" },
	{ MA, 0, "\\Ca", "cda" },
	{ CMA, 0, "\\Ca", "CDA" },
	{ MA, 0, "\\Cx", "cda" },
	{ CMA, 0, "\\Cx", "CDA" },

	/* Assertions. */
	{ MUA, 0, "\\b[^A]", "A_B#" },
	{ MA, 0, "\\b\\W", "\n*" },
	{ MUA, 0, "\\B[^,]\\b[^s]\\b", "#X" },
	{ MAP, 0, "\\B", "_\xa1" },
	{ MAP, 0, "\\b_\\b[,A]\\B", "_," },
	{ MUAP, 0, "\\b", "\xe6\x92\xad!" },
	{ MUAP, 0, "\\B", "_\xc2\xa1\xc3\xa1\xc2\x85" },
	{ MUAP, 0, "\\b[^A]\\B[^c]\\b[^_]\\B", "_\xc3\xa1\xe2\x80\xa8" },
	{ MUAP, 0, "\\b\\w+\\B", "\xc3\x89\xc2\xa1\xe6\x92\xad\xc3\x81\xc3\xa1" },
	{ MUA, 0, "\\b.", "\xcd\xbe" },
	{ MA, 0, "\\R^", "\n" },
	{ MA, 1, "^", "\n" },
	{ 0, 0, "^ab", "ab" },
	{ 0, 0, "^ab", "aab" },
	{ PCRE_MULTILINE | PCRE_NEWLINE_CRLF, 0, "^a", "\r\raa\n\naa\r\naa" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_ANYCRLF, 0, "^-", "\xe2\x80\xa8--\xc2\x85-\r\n-" },
	{ PCRE_MULTILINE | PCRE_NEWLINE_ANY, 0, "^-", "a--b--\x85--" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_ANY, 0, "^-", "a--\xe2\x80\xa8--" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_ANY, 0, "^-", "a--\xc2\x85--" },
	{ 0, 0, "ab$", "ab" },
	{ 0, 0, "ab$", "ab\r\n" },
	{ PCRE_MULTILINE | PCRE_NEWLINE_CRLF, 0, "a$", "\r\raa\n\naa\r\naa" },
	{ PCRE_MULTILINE | PCRE_NEWLINE_ANY, 0, "a$", "aaa" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_ANYCRLF, 0, "#$", "#\xc2\x85###\r#" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_ANY, 0, "#$", "#\xe2\x80\xa9" },
	{ PCRE_NOTBOL | PCRE_NEWLINE_ANY, 0, "^a", "aa\naa" },
	{ PCRE_NOTBOL | PCRE_MULTILINE | PCRE_NEWLINE_ANY, 0, "^a", "aa\naa" },
	{ PCRE_NOTEOL | PCRE_NEWLINE_ANY, 0, "a$", "aa\naa" },
	{ PCRE_NOTEOL | PCRE_NEWLINE_ANY, 0, "a$", "aa\r\n" },
	{ PCRE_UTF8 | PCRE_DOLLAR_ENDONLY | PCRE_NEWLINE_ANY, 0, "\\p{Any}{2,}$", "aa\r\n" },
	{ PCRE_NOTEOL | PCRE_MULTILINE | PCRE_NEWLINE_ANY, 0, "a$", "aa\naa" },
	{ PCRE_NEWLINE_CR, 0, ".\\Z", "aaa" },
	{ PCRE_NEWLINE_CR | PCRE_UTF8, 0, "a\\Z", "aaa\r" },
	{ PCRE_NEWLINE_CR, 0, ".\\Z", "aaa\n" },
	{ PCRE_NEWLINE_CRLF, 0, ".\\Z", "aaa\r" },
	{ PCRE_NEWLINE_CRLF | PCRE_UTF8, 0, ".\\Z", "aaa\n" },
	{ PCRE_NEWLINE_CRLF, 0, ".\\Z", "aaa\r\n" },
	{ PCRE_NEWLINE_ANYCRLF | PCRE_UTF8, 0, ".\\Z", "aaa" },
	{ PCRE_NEWLINE_ANYCRLF | PCRE_UTF8, 0, ".\\Z", "aaa\r" },
	{ PCRE_NEWLINE_ANYCRLF | PCRE_UTF8, 0, ".\\Z", "aaa\n" },
	{ PCRE_NEWLINE_ANYCRLF | PCRE_UTF8, 0, ".\\Z", "aaa\r\n" },
	{ PCRE_NEWLINE_ANYCRLF | PCRE_UTF8, 0, ".\\Z", "aaa\xe2\x80\xa8" },
	{ PCRE_NEWLINE_ANYCRLF | PCRE_UTF8, 0, ".\\Z", "aaa" },
	{ PCRE_NEWLINE_ANYCRLF | PCRE_UTF8, 0, ".\\Z", "aaa\r" },
	{ PCRE_NEWLINE_ANYCRLF | PCRE_UTF8, 0, ".\\Z", "aaa\n" },
	{ PCRE_NEWLINE_ANYCRLF | PCRE_UTF8, 0, ".\\Z", "aaa\r\n" },
	{ PCRE_NEWLINE_ANY | PCRE_UTF8, 0, ".\\Z", "aaa\xc2\x85" },
	{ PCRE_NEWLINE_ANY | PCRE_UTF8, 0, ".\\Z", "aaa\xe2\x80\xa8" },
	{ MA, 0, "\\Aa", "aaa" },
	{ MA, 1, "\\Aa", "aaa" },
	{ MA, 1, "\\Ga", "aaa" },
	{ MA, 1, "\\Ga", "aba" },
	{ MA, 0, "a\\z", "aaa" },
	{ MA, 0, "a\\z", "aab" },

	/* Brackets. */
	{ MUA, 0, "(ab|bb|cd)", "bacde" },
	{ MUA, 0, "(?:ab|a)(bc|c)", "ababc" },
	{ MUA, 0, "((ab|(cc))|(bb)|(?:cd|efg))", "abac" },
	{ CMUA, 0, "((aB|(Cc))|(bB)|(?:cd|EFg))", "AcCe" },
	{ MUA, 0, "((ab|(cc))|(bb)|(?:cd|ebg))", "acebebg" },
	{ MUA, 0, "(?:(a)|(?:b))(cc|(?:d|e))(a|b)k", "accabdbbccbk" },

	/* Greedy and non-greedy ? operators. */
	{ MUA, 0, "(?:a)?a", "laab" },
	{ CMUA, 0, "(A)?A", "llaab" },
	{ MUA, 0, "(a)?\?a", "aab" }, /* ?? is the prefix of trygraphs in GCC. */
	{ MUA, 0, "(a)?a", "manm" },
	{ CMUA, 0, "(a|b)?\?d((?:e)?)", "ABABdx" },
	{ MUA, 0, "(a|b)?\?d((?:e)?)", "abcde" },
	{ MUA, 0, "((?:ab)?\?g|b(?:g(nn|d)?\?)?)?\?(?:n)?m", "abgnbgnnbgdnmm" },

	/* Greedy and non-greedy + operators */
	{ MUA, 0, "(aa)+aa", "aaaaaaa" },
	{ MUA, 0, "(aa)+?aa", "aaaaaaa" },
	{ MUA, 0, "(?:aba|ab|a)+l", "ababamababal" },
	{ MUA, 0, "(?:aba|ab|a)+?l", "ababamababal" },
	{ MUA, 0, "(a(?:bc|cb|b|c)+?|ss)+e", "accssabccbcacbccbbXaccssabccbcacbccbbe" },
	{ MUA, 0, "(a(?:bc|cb|b|c)+|ss)+?e", "accssabccbcacbccbbXaccssabccbcacbccbbe" },
	{ MUA, 0, "(?:(b(c)+?)+)?\?(?:(bc)+|(cb)+)+(?:m)+", "bccbcccbcbccbcbPbccbcccbcbccbcbmmn" },

	/* Greedy and non-greedy * operators */
	{ CMUA, 0, "(?:AA)*AB", "aaaaaaamaaaaaaab" },
	{ MUA, 0, "(?:aa)*?ab", "aaaaaaamaaaaaaab" },
	{ MUA, 0, "(aa|ab)*ab", "aaabaaab" },
	{ CMUA, 0, "(aa|Ab)*?aB", "aaabaaab" },
	{ MUA, 0, "(a|b)*(?:a)*(?:b)*m", "abbbaaababanabbbaaababamm" },
	{ MUA, 0, "(a|b)*?(?:a)*?(?:b)*?m", "abbbaaababanabbbaaababamm" },
	{ MA, 0, "a(a(\\1*)a|(b)b+){0}a", "aa" },
	{ MA, 0, "((?:a|)*){0}a", "a" },

	/* Combining ? + * operators */
	{ MUA, 0, "((bm)+)?\?(?:a)*(bm)+n|((am)+?)?(?:a)+(am)*n", "bmbmabmamaaamambmaman" },
	{ MUA, 0, "(((ab)?cd)*ef)+g", "abcdcdefcdefefmabcdcdefcdefefgg" },
	{ MUA, 0, "(((ab)?\?cd)*?ef)+?g", "abcdcdefcdefefmabcdcdefcdefefgg" },
	{ MUA, 0, "(?:(ab)?c|(?:ab)+?d)*g", "ababcdccababddg" },
	{ MUA, 0, "(?:(?:ab)?\?c|(ab)+d)*?g", "ababcdccababddg" },

	/* Single character iterators. */
	{ MUA, 0, "(a+aab)+aaaab", "aaaabcaaaabaabcaabcaaabaaaab" },
	{ MUA, 0, "(a*a*aab)+x", "aaaaabaabaaabmaabx" },
	{ MUA, 0, "(a*?(b|ab)a*?)+x", "aaaabcxbbaabaacbaaabaabax" },
	{ MUA, 0, "(a+(ab|ad)a+)+x", "aaabaaaadaabaaabaaaadaaax" },
	{ MUA, 0, "(a?(a)a?)+(aaa)", "abaaabaaaaaaaa" },
	{ MUA, 0, "(a?\?(a)a?\?)+(b)", "aaaacaaacaacacbaaab" },
	{ MUA, 0, "(a{0,4}(b))+d", "aaaaaabaabcaaaaabaaaaabd" },
	{ MUA, 0, "(a{0,4}?[^b])+d+(a{0,4}[^b])d+", "aaaaadaaaacaadddaaddd" },
	{ MUA, 0, "(ba{2})+c", "baabaaabacbaabaac" },
	{ MUA, 0, "(a*+bc++)+", "aaabbcaaabcccab" },
	{ MUA, 0, "(a?+[^b])+", "babaacacb" },
	{ MUA, 0, "(a{0,3}+b)(a{0,3}+b)(a{0,3}+)[^c]", "abaabaaacbaabaaaac" },
	{ CMUA, 0, "([a-c]+[d-f]+?)+?g", "aBdacdehAbDaFgA" },
	{ CMUA, 0, "[c-f]+k", "DemmFke" },
	{ MUA, 0, "([DGH]{0,4}M)+", "GGDGHDGMMHMDHHGHM" },
	{ MUA, 0, "([a-c]{4,}s)+", "abasabbasbbaabsbba" },
	{ CMUA, 0, "[ace]{3,7}", "AcbDAcEEcEd" },
	{ CMUA, 0, "[ace]{3,7}?", "AcbDAcEEcEd" },
	{ CMUA, 0, "[ace]{3,}", "AcbDAcEEcEd" },
	{ CMUA, 0, "[ace]{3,}?", "AcbDAcEEcEd" },
	{ MUA, 0, "[ckl]{2,}?g", "cdkkmlglglkcg" },
	{ CMUA, 0, "[ace]{5}?", "AcCebDAcEEcEd" },
	{ MUA, 0, "([AbC]{3,5}?d)+", "BACaAbbAEAACCbdCCbdCCAAbb" },
	{ MUA, 0, "([^ab]{0,}s){2}", "abaabcdsABamsDDs" },
	{ MUA, 0, "\\b\\w+\\B", "x,a_cd" },
	{ MUAP, 0, "\\b[^\xc2\xa1]+\\B", "\xc3\x89\xc2\xa1\xe6\x92\xad\xc3\x81\xc3\xa1" },
	{ CMUA, 0, "[^b]+(a*)([^c]?d{3})", "aaaaddd" },

	/* Basic character sets. */
	{ MUA, 0, "(?:\\s)+(?:\\S)+", "ab \t\xc3\xa9\xe6\x92\xad " },
	{ MUA, 0, "(\\w)*(k)(\\W)?\?", "abcdef abck11" },
	{ MUA, 0, "\\((\\d)+\\)\\D", "a() (83 (8)2 (9)ab" },
	{ MUA, 0, "\\w(\\s|(?:\\d)*,)+\\w\\wb", "a 5, 4,, bb 5, 4,, aab" },
	{ MUA, 0, "(\\v+)(\\V+)", "\x0e\xc2\x85\xe2\x80\xa8\x0b\x09\xe2\x80\xa9" },
	{ MUA, 0, "(\\h+)(\\H+)", "\xe2\x80\xa8\xe2\x80\x80\x20\xe2\x80\x8a\xe2\x81\x9f\xe3\x80\x80\x09\x20\xc2\xa0\x0a" },

	/* Unicode properties. */
	{ MUAP, 0, "[1-5\xc3\xa9\\w]", "\xc3\xa1_" },
	{ MUAP, 0, "[\xc3\x81\\p{Ll}]", "A_\xc3\x89\xc3\xa1" },
	{ MUAP, 0, "[\\Wd-h_x-z]+", "a\xc2\xa1#_yhzdxi" },
	{ MUAP, 0, "[\\P{Any}]", "abc" },
	{ MUAP, 0, "[^\\p{Any}]", "abc" },
	{ MUAP, 0, "[\\P{Any}\xc3\xa1-\xc3\xa8]", "abc" },
	{ MUAP, 0, "[^\\p{Any}\xc3\xa1-\xc3\xa8]", "abc" },
	{ MUAP, 0, "[\xc3\xa1-\xc3\xa8\\P{Any}]", "abc" },
	{ MUAP, 0, "[^\xc3\xa1-\xc3\xa8\\p{Any}]", "abc" },
	{ MUAP, 0, "[\xc3\xa1-\xc3\xa8\\p{Any}]", "abc" },
	{ MUAP, 0, "[^\xc3\xa1-\xc3\xa8\\P{Any}]", "abc" },
	{ MUAP, 0, "[b-\xc3\xa9\\s]", "a\xc\xe6\x92\xad" },
	{ CMUAP, 0, "[\xc2\x85-\xc2\x89\xc3\x89]", "\xc2\x84\xc3\xa9" },
	{ MUAP, 0, "[^b-d^&\\s]{3,}", "db^ !a\xe2\x80\xa8_ae" },
	{ MUAP, 0, "[^\\S\\P{Any}][\\sN]{1,3}[\\P{N}]{4}", "\xe2\x80\xaa\xa N\x9\xc3\xa9_0" },
	{ MUA, 0, "[^\\P{L}\x9!D-F\xa]{2,3}", "\x9,.DF\xa.CG\xc3\x81" },
	{ CMUAP, 0, "[\xc3\xa1-\xc3\xa9_\xe2\x80\xa0-\xe2\x80\xaf]{1,5}[^\xe2\x80\xa0-\xe2\x80\xaf]", "\xc2\xa1\xc3\x89\xc3\x89\xe2\x80\xaf_\xe2\x80\xa0" },
	{ MUAP, 0, "[\xc3\xa2-\xc3\xa6\xc3\x81-\xc3\x84\xe2\x80\xa8-\xe2\x80\xa9\xe6\x92\xad\\p{Zs}]{2,}", "\xe2\x80\xa7\xe2\x80\xa9\xe6\x92\xad \xe6\x92\xae" },
	{ MUAP, 0, "[\\P{L&}]{2}[^\xc2\x85-\xc2\x89\\p{Ll}\\p{Lu}]{2}", "\xc3\xa9\xe6\x92\xad.a\xe6\x92\xad|\xc2\x8a#" },
	{ PCRE_UCP, 0, "[a-b\\s]{2,5}[^a]", "AB  baaa" },

	/* Possible empty brackets. */
	{ MUA, 0, "(?:|ab||bc|a)+d", "abcxabcabd" },
	{ MUA, 0, "(|ab||bc|a)+d", "abcxabcabd" },
	{ MUA, 0, "(?:|ab||bc|a)*d", "abcxabcabd" },
	{ MUA, 0, "(|ab||bc|a)*d", "abcxabcabd" },
	{ MUA, 0, "(?:|ab||bc|a)+?d", "abcxabcabd" },
	{ MUA, 0, "(|ab||bc|a)+?d", "abcxabcabd" },
	{ MUA, 0, "(?:|ab||bc|a)*?d", "abcxabcabd" },
	{ MUA, 0, "(|ab||bc|a)*?d", "abcxabcabd" },
	{ MUA, 0, "(((a)*?|(?:ba)+)+?|(?:|c|ca)*)*m", "abaacaccabacabalabaacaccabacabamm" },
	{ MUA, 0, "(?:((?:a)*|(ba)+?)+|(|c|ca)*?)*?m", "abaacaccabacabalabaacaccabacabamm" },

	/* Start offset. */
	{ MUA, 3, "(\\d|(?:\\w)*\\w)+", "0ac01Hb" },
	{ MUA, 4, "(\\w\\W\\w)+", "ab#d" },
	{ MUA, 2, "(\\w\\W\\w)+", "ab#d" },
	{ MUA, 1, "(\\w\\W\\w)+", "ab#d" },

	/* Newline. */
	{ PCRE_MULTILINE | PCRE_NEWLINE_CRLF, 0, "\\W{0,2}[^#]{3}", "\r\n#....." },
	{ PCRE_MULTILINE | PCRE_NEWLINE_CR, 0, "\\W{0,2}[^#]{3}", "\r\n#....." },
	{ PCRE_MULTILINE | PCRE_NEWLINE_CRLF, 0, "\\W{1,3}[^#]", "\r\n##...." },

	/* Any character except newline or any newline. */
	{ PCRE_NEWLINE_CRLF, 0, ".", "\r" },
	{ PCRE_NEWLINE_CRLF | PCRE_UTF8, 0, ".(.).", "a\xc3\xa1\r\n\n\r\r" },
	{ PCRE_NEWLINE_ANYCRLF, 0, ".(.)", "a\rb\nc\r\n\xc2\x85\xe2\x80\xa8" },
	{ PCRE_NEWLINE_ANYCRLF | PCRE_UTF8, 0, ".(.)", "a\rb\nc\r\n\xc2\x85\xe2\x80\xa8" },
	{ PCRE_NEWLINE_ANY | PCRE_UTF8, 0, "(.).", "a\rb\nc\r\n\xc2\x85\xe2\x80\xa9$de" },
	{ PCRE_NEWLINE_ANYCRLF | PCRE_UTF8, 0, ".(.).", "\xe2\x80\xa8\nb\r" },
	{ PCRE_NEWLINE_ANY, 0, "(.)(.)", "#\x85#\r#\n#\r\n#\x84" },
	{ PCRE_NEWLINE_ANY | PCRE_UTF8, 0, "(.+)#", "#\rMn\xc2\x85#\n###" },
	{ PCRE_BSR_ANYCRLF, 0, "\\R", "\r" },
	{ PCRE_BSR_ANYCRLF, 0, "\\R", "\x85#\r\n#" },
	{ PCRE_BSR_UNICODE | PCRE_UTF8, 0, "\\R", "ab\xe2\x80\xa8#c" },
	{ PCRE_BSR_UNICODE | PCRE_UTF8, 0, "\\R", "ab\r\nc" },
	{ PCRE_NEWLINE_CRLF | PCRE_BSR_UNICODE | PCRE_UTF8, 0, "(\\R.)+", "\xc2\x85\r\n#\xe2\x80\xa8\n\r\n\r" },
	{ MUA, 0, "\\R+", "ab" },
	{ MUA, 0, "\\R+", "ab\r\n\r" },
	{ MUA, 0, "\\R*", "ab\r\n\r" },
	{ MUA, 0, "\\R*", "\r\n\r" },
	{ MUA, 0, "\\R{2,4}", "\r\nab\r\r" },
	{ MUA, 0, "\\R{2,4}", "\r\nab\n\n\n\r\r\r" },
	{ MUA, 0, "\\R{2,}", "\r\nab\n\n\n\r\r\r" },
	{ MUA, 0, "\\R{0,3}", "\r\n\r\n\r\n\r\n\r\n" },
	{ MUA, 0, "\\R+\\R\\R", "\r\n\r\n" },
	{ MUA, 0, "\\R+\\R\\R", "\r\r\r" },
	{ MUA, 0, "\\R*\\R\\R", "\n\r" },
	{ MUA, 0, "\\R{2,4}\\R\\R", "\r\r\r" },
	{ MUA, 0, "\\R{2,4}\\R\\R", "\r\r\r\r" },

	/* Atomic groups (no fallback from "next" direction). */
	{ MUA, 0, "(?>ab)ab", "bab" },
	{ MUA, 0, "(?>(ab))ab", "bab" },
	{ MUA, 0, "(?>ab)+abc(?>de)*def(?>gh)?ghe(?>ij)+?k(?>lm)*?n(?>op)?\?op",
			"bababcdedefgheijijklmlmnop" },
	{ MUA, 0, "(?>a(b)+a|(ab)?\?(b))an", "abban" },
	{ MUA, 0, "(?>ab+a|(?:ab)?\?b)an", "abban" },
	{ MUA, 0, "((?>ab|ad|)*?)(?>|c)*abad", "abababcababad" },
	{ MUA, 0, "(?>(aa|b|)*+(?>(##)|###)*d|(aa)(?>(baa)?)m)", "aabaa#####da" },
	{ MUA, 0, "((?>a|)+?)b", "aaacaaab" },
	{ MUA, 0, "(?>x|)*$", "aaa" },
	{ MUA, 0, "(?>(x)|)*$", "aaa" },
	{ MUA, 0, "(?>x|())*$", "aaa" },
	{ MUA, 0, "((?>[cxy]a|[a-d])*?)b", "aaa+ aaab" },
	{ MUA, 0, "((?>[cxy](a)|[a-d])*?)b", "aaa+ aaab" },
	{ MUA, 0, "(?>((?>(a+))))bab|(?>((?>(a+))))bb", "aaaabaaabaabab" },
	{ MUA, 0, "(?>(?>a+))bab|(?>(?>a+))bb", "aaaabaaabaabab" },
	{ MUA, 0, "(?>(a)c|(?>(c)|(a))a)b*?bab", "aaaabaaabaabab" },
	{ MUA, 0, "(?>ac|(?>c|a)a)b*?bab", "aaaabaaabaabab" },
	{ MUA, 0, "(?>(b)b|(a))*b(?>(c)|d)?x", "ababcaaabdbx" },
	{ MUA, 0, "(?>bb|a)*b(?>c|d)?x", "ababcaaabdbx" },
	{ MUA, 0, "(?>(bb)|a)*b(?>c|(d))?x", "ababcaaabdbx" },
	{ MUA, 0, "(?>(a))*?(?>(a))+?(?>(a))??x", "aaaaaacccaaaaabax" },
	{ MUA, 0, "(?>a)*?(?>a)+?(?>a)??x", "aaaaaacccaaaaabax" },
	{ MUA, 0, "(?>(a)|)*?(?>(a)|)+?(?>(a)|)??x", "aaaaaacccaaaaabax" },
	{ MUA, 0, "(?>a|)*?(?>a|)+?(?>a|)??x", "aaaaaacccaaaaabax" },
	{ MUA, 0, "(?>a(?>(a{0,2}))*?b|aac)+b", "aaaaaaacaaaabaaaaacaaaabaacaaabb" },
	{ CMA, 0, "(?>((?>a{32}|b+|(a*))?(?>c+|d*)?\?)+e)+?f", "aaccebbdde bbdaaaccebbdee bbdaaaccebbdeef" },
	{ MUA, 0, "(?>(?:(?>aa|a||x)+?b|(?>aa|a||(x))+?c)?(?>[ad]{0,2})*?d)+d", "aaacdbaabdcabdbaaacd aacaabdbdcdcaaaadaabcbaadd" },
	{ MUA, 0, "(?>(?:(?>aa|a||(x))+?b|(?>aa|a||x)+?c)?(?>[ad]{0,2})*?d)+d", "aaacdbaabdcabdbaaacd aacaabdbdcdcaaaadaabcbaadd" },
	{ MUA, 0, "\\X", "\xcc\x8d\xcc\x8d" },
	{ MUA, 0, "\\X", "\xcc\x8d\xcc\x8d#\xcc\x8d\xcc\x8d" },
	{ MUA, 0, "\\X+..", "\xcc\x8d#\xcc\x8d#\xcc\x8d\xcc\x8d" },
	{ MUA, 0, "\\X{2,4}", "abcdef" },
	{ MUA, 0, "\\X{2,4}?", "abcdef" },
	{ MUA, 0, "\\X{2,4}..", "#\xcc\x8d##" },
	{ MUA, 0, "\\X{2,4}..", "#\xcc\x8d#\xcc\x8d##" },
	{ MUA, 0, "(c(ab)?+ab)+", "cabcababcab" },
	{ MUA, 0, "(?>(a+)b)+aabab", "aaaabaaabaabab" },

	/* Possessive quantifiers. */
	{ MUA, 0, "(?:a|b)++m", "mababbaaxababbaam" },
	{ MUA, 0, "(?:a|b)*+m", "mababbaaxababbaam" },
	{ MUA, 0, "(?:a|b)*+m", "ababbaaxababbaam" },
	{ MUA, 0, "(a|b)++m", "mababbaaxababbaam" },
	{ MUA, 0, "(a|b)*+m", "mababbaaxababbaam" },
	{ MUA, 0, "(a|b)*+m", "ababbaaxababbaam" },
	{ MUA, 0, "(a|b(*ACCEPT))++m", "maaxab" },
	{ MUA, 0, "(?:b*)++m", "bxbbxbbbxm" },
	{ MUA, 0, "(?:b*)++m", "bxbbxbbbxbbm" },
	{ MUA, 0, "(?:b*)*+m", "bxbbxbbbxm" },
	{ MUA, 0, "(?:b*)*+m", "bxbbxbbbxbbm" },
	{ MUA, 0, "(b*)++m", "bxbbxbbbxm" },
	{ MUA, 0, "(b*)++m", "bxbbxbbbxbbm" },
	{ MUA, 0, "(b*)*+m", "bxbbxbbbxm" },
	{ MUA, 0, "(b*)*+m", "bxbbxbbbxbbm" },
	{ MUA, 0, "(?:a|(b))++m", "mababbaaxababbaam" },
	{ MUA, 0, "(?:(a)|b)*+m", "mababbaaxababbaam" },
	{ MUA, 0, "(?:(a)|(b))*+m", "ababbaaxababbaam" },
	{ MUA, 0, "(a|(b))++m", "mababbaaxababbaam" },
	{ MUA, 0, "((a)|b)*+m", "mababbaaxababbaam" },
	{ MUA, 0, "((a)|(b))*+m", "ababbaaxababbaam" },
	{ MUA, 0, "(a|(b)(*ACCEPT))++m", "maaxab" },
	{ MUA, 0, "(?:(b*))++m", "bxbbxbbbxm" },
	{ MUA, 0, "(?:(b*))++m", "bxbbxbbbxbbm" },
	{ MUA, 0, "(?:(b*))*+m", "bxbbxbbbxm" },
	{ MUA, 0, "(?:(b*))*+m", "bxbbxbbbxbbm" },
	{ MUA, 0, "((b*))++m", "bxbbxbbbxm" },
	{ MUA, 0, "((b*))++m", "bxbbxbbbxbbm" },
	{ MUA, 0, "((b*))*+m", "bxbbxbbbxm" },
	{ MUA, 0, "((b*))*+m", "bxbbxbbbxbbm" },
	{ MUA, 0, "(?>(b{2,4}))(?:(?:(aa|c))++m|(?:(aa|c))+n)", "bbaacaaccaaaacxbbbmbn" },
	{ MUA, 0, "((?:b)++a)+(cd)*+m", "bbababbacdcdnbbababbacdcdm" },
	{ MUA, 0, "((?:(b))++a)+((c)d)*+m", "bbababbacdcdnbbababbacdcdm" },
	{ MUA, 0, "(?:(?:(?:ab)*+k)++(?:n(?:cd)++)*+)*+m", "ababkkXababkkabkncXababkkabkncdcdncdXababkkabkncdcdncdkkabkncdXababkkabkncdcdncdkkabkncdm" },
	{ MUA, 0, "(?:((ab)*+(k))++(n(?:c(d))++)*+)*+m", "ababkkXababkkabkncXababkkabkncdcdncdXababkkabkncdcdncdkkabkncdXababkkabkncdcdncdkkabkncdm" },

	/* Back references. */
	{ MUA, 0, "(aa|bb)(\\1*)(ll|)(\\3*)bbbbbbc", "aaaaaabbbbbbbbc" },
	{ CMUA, 0, "(aa|bb)(\\1+)(ll|)(\\3+)bbbbbbc", "bBbbBbCbBbbbBbbcbbBbbbBBbbC" },
	{ CMA, 0, "(a{2,4})\\1", "AaAaaAaA" },
	{ MUA, 0, "(aa|bb)(\\1?)aa(\\1?)(ll|)(\\4+)bbc", "aaaaaaaabbaabbbbaabbbbc" },
	{ MUA, 0, "(aa|bb)(\\1{0,5})(ll|)(\\3{0,5})cc", "bbxxbbbbxxaaaaaaaaaaaaaaaacc" },
	{ MUA, 0, "(aa|bb)(\\1{3,5})(ll|)(\\3{3,5})cc", "bbbbbbbbbbbbaaaaaaccbbbbbbbbbbbbbbcc" },
	{ MUA, 0, "(aa|bb)(\\1{3,})(ll|)(\\3{3,})cc", "bbbbbbbbbbbbaaaaaaccbbbbbbbbbbbbbbcc" },
	{ MUA, 0, "(\\w+)b(\\1+)c", "GabGaGaDbGaDGaDc" },
	{ MUA, 0, "(?:(aa)|b)\\1?b", "bb" },
	{ CMUA, 0, "(aa|bb)(\\1*?)aa(\\1+?)", "bBBbaaAAaaAAaa" },
	{ MUA, 0, "(aa|bb)(\\1*?)(dd|)cc(\\3+?)", "aaaaaccdd" },
	{ CMUA, 0, "(?:(aa|bb)(\\1?\?)cc){2}(\\1?\?)", "aAaABBbbAAaAcCaAcCaA" },
	{ MUA, 0, "(?:(aa|bb)(\\1{3,5}?)){2}(dd|)(\\3{3,5}?)", "aaaaaabbbbbbbbbbaaaaaaaaaaaaaa" },
	{ CMA, 0, "(?:(aa|bb)(\\1{3,}?)){2}(dd|)(\\3{3,}?)", "aaaaaabbbbbbbbbbaaaaaaaaaaaaaa" },
	{ MUA, 0, "(?:(aa|bb)(\\1{0,3}?)){2}(dd|)(\\3{0,3}?)b(\\1{0,3}?)(\\1{0,3})", "aaaaaaaaaaaaaaabaaaaa" },
	{ MUA, 0, "(a(?:\\1|)a){3}b", "aaaaaaaaaaab" },
	{ MA, 0, "(a?)b(\\1\\1*\\1+\\1?\\1*?\\1+?\\1??\\1*+\\1++\\1?+\\1{4}\\1{3,5}\\1{4,}\\1{0,5}\\1{3,5}?\\1{4,}?\\1{0,5}?\\1{3,5}+\\1{4,}+\\1{0,5}+#){2}d", "bb#b##d" },
	{ MUAP, 0, "(\\P{N})\\1{2,}", ".www." },
	{ MUAP, 0, "(\\P{N})\\1{0,2}", "wwwww." },
	{ MUAP, 0, "(\\P{N})\\1{1,2}ww", "wwww" },
	{ MUAP, 0, "(\\P{N})\\1{1,2}ww", "wwwww" },
	{ PCRE_UCP, 0, "(\\P{N})\\1{2,}", ".www." },

	/* Assertions. */
	{ MUA, 0, "(?=xx|yy|zz)\\w{4}", "abczzdefg" },
	{ MUA, 0, "(?=((\\w+)b){3}|ab)", "dbbbb ab" },
	{ MUA, 0, "(?!ab|bc|cd)[a-z]{2}", "Xabcdef" },
	{ MUA, 0, "(?<=aaa|aa|a)a", "aaa" },
	{ MUA, 2, "(?<=aaa|aa|a)a", "aaa" },
	{ MA, 0, "(?<=aaa|aa|a)a", "aaa" },
	{ MA, 2, "(?<=aaa|aa|a)a", "aaa" },
	{ MUA, 0, "(\\d{2})(?!\\w+c|(((\\w?)m){2}n)+|\\1)", "x5656" },
	{ MUA, 0, "((?=((\\d{2,6}\\w){2,}))\\w{5,20}K){2,}", "567v09708K12l00M00 567v09708K12l00M00K45K" },
	{ MUA, 0, "(?=(?:(?=\\S+a)\\w*(b)){3})\\w+\\d", "bba bbab nbbkba nbbkba0kl" },
	{ MUA, 0, "(?>a(?>(b+))a(?=(..)))*?k", "acabbcabbaabacabaabbakk" },
	{ MUA, 0, "((?(?=(a))a)+k)", "bbak" },
	{ MUA, 0, "((?(?=a)a)+k)", "bbak" },
	{ MUA, 0, "(?=(?>(a))m)amk", "a k" },
	{ MUA, 0, "(?!(?>(a))m)amk", "a k" },
	{ MUA, 0, "(?>(?=(a))am)amk", "a k" },
	{ MUA, 0, "(?=(?>a|(?=(?>(b+))a|c)[a-c]+)*?m)[a-cm]+k", "aaam bbam baaambaam abbabba baaambaamk" },
	{ MUA, 0, "(?> ?\?\\b(?(?=\\w{1,4}(a))m)\\w{0,8}bc){2,}?", "bca ssbc mabd ssbc mabc" },
	{ MUA, 0, "(?:(?=ab)?[^n][^n])+m", "ababcdabcdcdabnababcdabcdcdabm" },
	{ MUA, 0, "(?:(?=a(b))?[^n][^n])+m", "ababcdabcdcdabnababcdabcdcdabm" },
	{ MUA, 0, "(?:(?=.(.))??\\1.)+m", "aabbbcbacccanaabbbcbacccam" },
	{ MUA, 0, "(?:(?=.)??[a-c])+m", "abacdcbacacdcaccam" },
	{ MUA, 0, "((?!a)?(?!([^a]))?)+$", "acbab" },
	{ MUA, 0, "((?!a)?\?(?!([^a]))?\?)+$", "acbab" },

	/* Not empty, ACCEPT, FAIL */
	{ MUA | PCRE_NOTEMPTY, 0, "a*", "bcx" },
	{ MUA | PCRE_NOTEMPTY, 0, "a*", "bcaad" },
	{ MUA | PCRE_NOTEMPTY, 0, "a*?", "bcaad" },
	{ MUA | PCRE_NOTEMPTY_ATSTART, 0, "a*", "bcaad" },
	{ MUA, 0, "a(*ACCEPT)b", "ab" },
	{ MUA | PCRE_NOTEMPTY, 0, "a*(*ACCEPT)b", "bcx" },
	{ MUA | PCRE_NOTEMPTY, 0, "a*(*ACCEPT)b", "bcaad" },
	{ MUA | PCRE_NOTEMPTY, 0, "a*?(*ACCEPT)b", "bcaad" },
	{ MUA | PCRE_NOTEMPTY, 0, "(?:z|a*(*ACCEPT)b)", "bcx" },
	{ MUA | PCRE_NOTEMPTY, 0, "(?:z|a*(*ACCEPT)b)", "bcaad" },
	{ MUA | PCRE_NOTEMPTY, 0, "(?:z|a*?(*ACCEPT)b)", "bcaad" },
	{ MUA | PCRE_NOTEMPTY_ATSTART, 0, "a*(*ACCEPT)b", "bcx" },
	{ MUA | PCRE_NOTEMPTY_ATSTART, 0, "a*(*ACCEPT)b", "" },
	{ MUA, 0, "((a(*ACCEPT)b))", "ab" },
	{ MUA, 0, "(a(*FAIL)a|a)", "aaa" },
	{ MUA, 0, "(?=ab(*ACCEPT)b)a", "ab" },
	{ MUA, 0, "(?=(?:x|ab(*ACCEPT)b))", "ab" },
	{ MUA, 0, "(?=(a(b(*ACCEPT)b)))a", "ab" },
	{ MUA | PCRE_NOTEMPTY, 0, "(?=a*(*ACCEPT))c", "c" },

	/* Conditional blocks. */
	{ MUA, 0, "(?(?=(a))a|b)+k", "ababbalbbadabak" },
	{ MUA, 0, "(?(?!(b))a|b)+k", "ababbalbbadabak" },
	{ MUA, 0, "(?(?=a)a|b)+k", "ababbalbbadabak" },
	{ MUA, 0, "(?(?!b)a|b)+k", "ababbalbbadabak" },
	{ MUA, 0, "(?(?=(a))a*|b*)+k", "ababbalbbadabak" },
	{ MUA, 0, "(?(?!(b))a*|b*)+k", "ababbalbbadabak" },
	{ MUA, 0, "(?(?!(b))(?:aaaaaa|a)|(?:bbbbbb|b))+aaaak", "aaaaaaaaaaaaaa bbbbbbbbbbbbbbb aaaaaaak" },
	{ MUA, 0, "(?(?!b)(?:aaaaaa|a)|(?:bbbbbb|b))+aaaak", "aaaaaaaaaaaaaa bbbbbbbbbbbbbbb aaaaaaak" },
	{ MUA | PCRE_BUG, 0, "(?(?!(b))(?:aaaaaa|a)|(?:bbbbbb|b))+bbbbk", "aaaaaaaaaaaaaa bbbbbbbbbbbbbbb bbbbbbbk" },
	{ MUA, 0, "(?(?!b)(?:aaaaaa|a)|(?:bbbbbb|b))+bbbbk", "aaaaaaaaaaaaaa bbbbbbbbbbbbbbb bbbbbbbk" },
	{ MUA, 0, "(?(?=a)a*|b*)+k", "ababbalbbadabak" },
	{ MUA, 0, "(?(?!b)a*|b*)+k", "ababbalbbadabak" },
	{ MUA, 0, "(?(?=a)ab)", "a" },
	{ MUA, 0, "(?(?<!b)c)", "b" },
	{ MUA, 0, "(?(DEFINE)a(b))", "a" },
	{ MUA, 0, "a(?(DEFINE)(?:b|(?:c?)+)*)", "a" },
	{ MUA, 0, "(?(?=.[a-c])[k-l]|[A-D])", "kdB" },
	{ MUA, 0, "(?(?!.{0,4}[cd])(aa|bb)|(cc|dd))+", "aabbccddaa" },
	{ MUA, 0, "(?(?=[^#@]*@)(aaab|aa|aba)|(aba|aab)){3,}", "aaabaaaba#aaabaaaba#aaabaaaba@" },
	{ MUA, 0, "((?=\\w{5})\\w(?(?=\\w*k)\\d|[a-f_])*\\w\\s)+", "mol m10kk m088k _f_a_ mbkkl" },
	{ MUA, 0, "(c)?\?(?(1)a|b)", "cdcaa" },
	{ MUA, 0, "(c)?\?(?(1)a|b)", "cbb" },
	{ MUA | PCRE_BUG, 0, "(?(?=(a))(aaaa|a?))+aak", "aaaaab aaaaak" },
	{ MUA, 0, "(?(?=a)(aaaa|a?))+aak", "aaaaab aaaaak" },
	{ MUA, 0, "(?(?!(b))(aaaa|a?))+aak", "aaaaab aaaaak" },
	{ MUA, 0, "(?(?!b)(aaaa|a?))+aak", "aaaaab aaaaak" },
	{ MUA | PCRE_BUG, 0, "(?(?=(a))a*)+aak", "aaaaab aaaaak" },
	{ MUA, 0, "(?(?=a)a*)+aak", "aaaaab aaaaak" },
	{ MUA, 0, "(?(?!(b))a*)+aak", "aaaaab aaaaak" },
	{ MUA, 0, "(?(?!b)a*)+aak", "aaaaab aaaaak" },
	{ MUA, 0, "(?(?=(?=(?!(x))a)aa)aaa|(?(?=(?!y)bb)bbb))*k", "abaabbaaabbbaaabbb abaabbaaabbbaaabbbk" },
	{ MUA, 0, "(?P<Name>a)?(?P<Name2>b)?(?(Name)c|d)*l", "bc ddd abccabccl" },
	{ MUA, 0, "(?P<Name>a)?(?P<Name2>b)?(?(Name)c|d)+?dd", "bcabcacdb bdddd" },
	{ MUA, 0, "(?P<Name>a)?(?P<Name2>b)?(?(Name)c|d)+l", "ababccddabdbccd abcccl" },

	/* Set start of match. */
	{ MUA, 0, "(?:\\Ka)*aaaab", "aaaaaaaa aaaaaaabb" },
	{ MUA, 0, "(?>\\Ka\\Ka)*aaaab", "aaaaaaaa aaaaaaaaaabb" },
	{ MUA, 0, "a+\\K(?<=\\Gaa)a", "aaaaaa" },
	{ MUA | PCRE_NOTEMPTY, 0, "a\\K(*ACCEPT)b", "aa" },
	{ MUA | PCRE_NOTEMPTY_ATSTART, 0, "a\\K(*ACCEPT)b", "aa" },

	/* First line. */
	{ MUA | PCRE_FIRSTLINE, 0, "\\p{Any}a", "bb\naaa" },
	{ MUA | PCRE_FIRSTLINE, 0, "\\p{Any}a", "bb\r\naaa" },
	{ MUA | PCRE_FIRSTLINE, 0, "(?<=a)", "a" },
	{ MUA | PCRE_FIRSTLINE, 0, "[^a][^b]", "ab" },
	{ MUA | PCRE_FIRSTLINE, 0, "a", "\na" },
	{ MUA | PCRE_FIRSTLINE, 0, "[abc]", "\na" },
	{ MUA | PCRE_FIRSTLINE, 0, "^a", "\na" },
	{ MUA | PCRE_FIRSTLINE, 0, "^(?<=\n)", "\na" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_ANY | PCRE_FIRSTLINE, 0, "#", "\xc2\x85#" },
	{ PCRE_MULTILINE | PCRE_NEWLINE_ANY | PCRE_FIRSTLINE, 0, "#", "\x85#" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_ANY | PCRE_FIRSTLINE, 0, "^#", "\xe2\x80\xa8#" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_CRLF | PCRE_FIRSTLINE, 0, "\\p{Any}", "\r\na" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_CRLF | PCRE_FIRSTLINE, 0, ".", "\r" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_CRLF | PCRE_FIRSTLINE, 0, "a", "\ra" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_CRLF | PCRE_FIRSTLINE, 0, "ba", "bbb\r\nba" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_CRLF | PCRE_FIRSTLINE, 0, "\\p{Any}{4}|a", "\r\na" },
	{ PCRE_MULTILINE | PCRE_UTF8 | PCRE_NEWLINE_CRLF | PCRE_FIRSTLINE, 1, ".", "\r\n" },

	/* Recurse. */
	{ MUA, 0, "(a)(?1)", "aa" },
	{ MUA, 0, "((a))(?1)", "aa" },
	{ MUA, 0, "(b|a)(?1)", "aa" },
	{ MUA, 0, "(b|(a))(?1)", "aa" },
	{ MUA, 0, "((a)(b)(?:a*))(?1)", "aba" },
	{ MUA, 0, "((a)(b)(?:a*))(?1)", "abab" },
	{ MUA, 0, "((a+)c(?2))b(?1)", "aacaabaca" },
	{ MUA, 0, "((?2)b|(a)){2}(?1)", "aabab" },
	{ MUA, 0, "(?1)(a)*+(?2)(b(?1))", "aababa" },
	{ MUA, 0, "(?1)(((a(*ACCEPT)))b)", "axaa" },
	{ MUA, 0, "(?1)(?(DEFINE) (((ac(*ACCEPT)))b) )", "akaac" },
	{ MUA, 0, "(a+)b(?1)b\\1", "abaaabaaaaa" },
	{ MUA, 0, "(?(DEFINE)(aa|a))(?1)ab", "aab" },
	{ MUA, 0, "(?(DEFINE)(a\\Kb))(?1)+ababc", "abababxabababc" },
	{ MUA, 0, "(a\\Kb)(?1)+ababc", "abababxababababc" },
	{ MUA, 0, "(a\\Kb)(?1)+ababc", "abababxababababxc" },
	{ MUA, 0, "b|<(?R)*>", "<<b>" },
	{ MUA, 0, "(a\\K){0}(?:(?1)b|ac)", "ac" },
	{ MUA, 0, "(?(DEFINE)(a(?2)|b)(b(?1)|(a)))(?:(?1)|(?2))m", "ababababnababababaam" },
	{ MUA, 0, "(a)((?(R)a|b))(?2)", "aabbabaa" },
	{ MUA, 0, "(a)((?(R2)a|b))(?2)", "aabbabaa" },
	{ MUA, 0, "(a)((?(R1)a|b))(?2)", "ababba" },
	{ MUA, 0, "(?(R0)aa|bb(?R))", "abba aabb bbaa" },
	{ MUA, 0, "((?(R)(?:aaaa|a)|(?:(aaaa)|(a)))+)(?1)$", "aaaaaaaaaa aaaa" },
	{ MUA, 0, "(?P<Name>a(?(R&Name)a|b))(?1)", "aab abb abaa" },

	/* Deep recursion. */
	{ MUA, 0, "((((?:(?:(?:\\w)+)?)*|(?>\\w)+?)+|(?>\\w)?\?)*)?\\s", "aaaaa+ " },
	{ MUA, 0, "(?:((?:(?:(?:\\w*?)+)??|(?>\\w)?|\\w*+)*)+)+?\\s", "aa+ " },
	{ MUA, 0, "((a?)+)+b", "aaaaaaaaaaaaa b" },

	/* Deep recursion: Stack limit reached. */
	{ MA, 0, "a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?a?aaaaaaaaaaaaaaaaaaaaaaa", "aaaaaaaaaaaaaaaaaaaaaaa" },
	{ MA, 0, "(?:a+)+b", "aaaaaaaaaaaaaaaaaaaaaaaa b" },
	{ MA, 0, "(?:a+?)+?b", "aaaaaaaaaaaaaaaaaaaaaaaa b" },
	{ MA, 0, "(?:a*)*b", "aaaaaaaaaaaaaaaaaaaaaaaa b" },
	{ MA, 0, "(?:a*?)*?b", "aaaaaaaaaaaaaaaaaaaaaaaa b" },

	{ 0, 0, NULL, NULL }
};

static int regression_tests(void)
{
	pcre *re;
	struct regression_test_case *current = regression_test_cases;
	const char *error;
	pcre_extra *extra;
	int utf8 = 0, ucp = 0;
	int ovector1[32];
	int ovector2[32];
	int return_value1, return_value2;
	int i, err_offs;
	int total = 0, succesful = 0;
	int counter = 0;
	int disabled_flags = PCRE_BUG;

	/* This test compares the behaviour of interpreter and JIT. Although disabling
	utf8 or ucp may make tests fail, if the pcre_exec result is the SAME, it is
	still considered successful from pcre_jit_test point of view. */

	pcre_config(PCRE_CONFIG_UTF8, &utf8);
	pcre_config(PCRE_CONFIG_UNICODE_PROPERTIES, &ucp);
	if (!utf8)
		disabled_flags |= PCRE_UTF8;
	if (!ucp)
		disabled_flags |= PCRE_UCP;

	printf("Running JIT regression tests with utf8 %s and ucp %s:\n", utf8 ? "enabled" : "disabled", ucp ? "enabled" : "disabled");
	while (current->pattern) {
		/* printf("\nPattern: %s :\n", current->pattern); */
		total++;

		error = NULL;
		re = pcre_compile(current->pattern, current->flags & ~(PCRE_NOTBOL | PCRE_NOTEOL | PCRE_NOTEMPTY | PCRE_NOTEMPTY_ATSTART | disabled_flags), &error, &err_offs, NULL);

		if (!re) {
			if (utf8 && ucp)
				printf("\nCannot compile pattern: %s\n", current->pattern);
			else {
				/* Some patterns cannot be compiled when either of utf8
				or ucp is disabled. We just skip them. */
				printf(".");
				succesful++;
			}
			current++;
			continue;
		}

		error = NULL;
		extra = pcre_study(re, PCRE_STUDY_JIT_COMPILE, &error);
		if (!extra) {
			printf("\nCannot study pattern: %s\n", current->pattern);
			current++;
			continue;
		}

		if (!(extra->flags & PCRE_EXTRA_EXECUTABLE_JIT)) {
			printf("\nJIT compiler does not support: %s\n", current->pattern);
			current++;
			continue;
		}

		counter++;
		if ((counter & 0x3) != 0)
			setstack(extra);

		for (i = 0; i < 32; ++i)
			ovector1[i] = -2;
		return_value1 = pcre_exec(re, extra, current->input, strlen(current->input), current->start_offset, current->flags & (PCRE_NOTBOL | PCRE_NOTEOL | PCRE_NOTEMPTY | PCRE_NOTEMPTY_ATSTART), ovector1, 32);

		for (i = 0; i < 32; ++i)
			ovector2[i] = -2;
		return_value2 = pcre_exec(re, NULL, current->input, strlen(current->input), current->start_offset, current->flags & (PCRE_NOTBOL | PCRE_NOTEOL | PCRE_NOTEMPTY | PCRE_NOTEMPTY_ATSTART), ovector2, 32);

		/* If PCRE_BUG is set, just run the test, but do not compare the results.
		Segfaults can still be captured. */
		if (!(current->flags & PCRE_BUG)) {
			if (return_value1 != return_value2) {
				printf("\nReturn value differs(%d:%d): '%s' @ '%s'\n", return_value1, return_value2, current->pattern, current->input);
				current++;
				continue;
			}

			if (return_value1 >= 0) {
				return_value1 *= 2;
				err_offs = 0;
				for (i = 0; i < return_value1; ++i)
					if (ovector1[i] != ovector2[i]) {
						printf("\nOvector[%d] value differs(%d:%d): '%s' @ '%s' \n", i, ovector1[i], ovector2[i], current->pattern, current->input);
						err_offs = 1;
					}
				if (err_offs) {
					current++;
					continue;
				}
			}
		}

		pcre_free_study(extra);
		pcre_free(re);

		/* printf("[%d-%d]%s", ovector1[0], ovector1[1], (current->flags & PCRE_CASELESS) ? "C" : ""); */
		printf(".");
		fflush(stdout);
		current++;
		succesful++;
	}

	if (total == succesful) {
		printf("\nAll JIT regression tests are successfully passed.\n");
		return 0;
	} else {
		printf("\nSuccessful test ratio: %d%%\n", succesful * 100 / total);
		return 1;
	}
}

/* End of pcre_jit_test.c */
