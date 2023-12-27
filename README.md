## Project Summary:

This is a complete and functional compiler development project.

This compiler is built for a new language 'uC', which is a new language I developed. 'uC' is a simplified version of C language, which combines the grammars from several different programming languages, such as Java, Python, and C. It is an Object-oriented programming languages, and it supports many critical components of a programming language, such as control structures (if, while), functions, type checking, explicit/implicit type conversions, arrays, pointers. etc.

This compiler development project uses a parser generator: [ANTLR](https://www.antlr.org/), to generate a parser in Java that can build and walk parse trees from uC grammar. 



## Background of Compiler

The first step of a compiler is checking whether a program is valid: 1. Does it use the correct "vocabulary" (keywords, operators, variable names); 2. Does it use proper "grammar" (do for and while loops have the correct structure, etc.). A compiler uses two tools to accomplish the validation check:

1. A *scanner* that reads in a stream of characters and *tokenizes* them into the constituent words of the language -- the keywords, operators, variable names, etc.
2. A *parser* that consumes a stream of tokens (as identified by the scanner) into a *parse tree*, which is a representation of the structure of the program.



