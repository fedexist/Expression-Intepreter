## Expression-Intepreter
Java Expression Intepreter made as an assignment.

This application is used to calculate arhitmetical expressions based on the following context-free grammar:

program → statement list

statement list → statement statement list | e

statement → variable def | expression def

variable def → ( SET variable id expression )

expression def → ( GET expression )

expression → 	( ADD expression expression )

		| ( SUB expression expression )

		| ( MUL expression expression )

		| ( DIV expression expression )

		| number

		| variable id

variable id → alpha list

alpha list → alpha alpha list | alpha

alpha → a | b | c | . . . | z | A | B | C | . . . | Z

number → 0 | sigdigit rest

sigdigit → 1 | . . . | 9

rest → digit rest | e

digit → 0 | sigdigit

###How to write an expression

These are some examples on how to use the grammar:

(GET (ADD (MUL 14 10) 25)) 

(SET var (MUL 10 5))

(GET (DIV 1000 var))

(SET var (MUL 10 20))

(GET (DIV 1000 var))

###TODO: Translate comments from Italian to English

See Main.java on how to use it
