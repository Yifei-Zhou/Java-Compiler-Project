## Background of Compiler

The first step of a compiler is checking whether a program is valid: 1. Does it use the correct "vocabulary" (keywords, operators, variable names); 2. Does it use proper "grammar" (do for and while loops have the correct structure, etc.). A compiler uses two tools to accomplish the validation check:

1. A *scanner* that reads in a stream of characters and *tokenizes* them into the constituent words of the language -- the keywords, operators, variable names, etc.
2. A *parser* that consumes a stream of tokens (as identified by the scanner) into a *parse tree* a representation of the structure of the program.

