# Name-Analyzer

The name analyzer will perform the following tasks:

Build symbol tables. You will use the "list of hashtables" approach (using the SymTable class from program 1).

Find multiply declared names, uses of undeclared names, bad struct accesses, and bad declarations. Like C, the minim language allows the same name to be declared in non-overlapping or nested scopes. The formal parameters of a function are considered to be in the same scope as the function body. All names must be declared before they are used. A bad struct access happens when either the left-hand side of the dot-access is not a name already declared to be of a struct type or the right-hand side of the dot-access is not the name of a field for the appropriate type of struct. A bad declaration is a declaration of anything other than a function to be of type void as well as the declaration of a variable to be of a bad struct type (the name of the struct type doesn't exist or is not a struct type).

Add IdNode links: For each IdNode in the abstract-syntax tree that represents a use of a name (not a declaration) add a "link" to the corresponding symbol-table entry. (As stated above, you will need to modify the IdNode class in ast.java to have a new field of type Sym. That is the field that your name analyzer will fill in with a link to the Sym returned by the symbol table's globalLookup method.)

You must implement your name analyzer by writing appropriate methods for the different subclasses of ASTnode. Exactly what methods you write is up to you (as long as they do name analysis as specified).

It may help to start by writing the name analysis method for ProgramNode, then work "top down", adding a method for DeclListNode (the child of a ProgramNode), then for each kind of DeclNode (except StructDeclNode), and so on (and then handle StructDeclNode and perhaps other struct related nodes at the end). Be sure to think about which nodes' methods need to add a new hashtable to the symbol table (i.e., when is a new scope being entered) and which methods need to remove a hashtable from the symbol table (i.e., when is a scope being exited).

Some of the methods will process the declarations in the program (checking for bad declarations and checking whether the names are multiply declared, and if not, adding appropriate symbol-table entries) and some will process the statements in the program (checking that every name used in a statement has been declared and adding links). Note that you should not add a link for an IdNode that represents a use of an undeclared name.
