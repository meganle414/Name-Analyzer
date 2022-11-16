import java.io.*;
import java.util.*;

// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a minim program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a list (for nodes that may have a variable number of 
// children) or as a fixed set of fields.
//
// The nodes for literals and ids contain line and character number
// information; for string literals and identifiers, they also contain a
// string; for integer literals, they also contain an integer value.
//
// Here are all the different kinds of AST nodes and what kinds of children
// they have.  All of these kinds of AST nodes are subclasses of "ASTnode".
// Indentation indicates further subclassing:
//
//     Subclass            Kids
//     --------            ----
//     ProgramNode         DeclListNode
//     DeclListNode        linked list of DeclNode
//     DeclNode:
//       VarDeclNode       TypeNode, IdNode, int
//       FnDeclNode        TypeNode, IdNode, FormalsListNode, FnBodyNode
//       FormalDeclNode    TypeNode, IdNode
//       StructDeclNode    IdNode, DeclListNode
//
//     FormalsListNode     linked list of FormalDeclNode
//     FnBodyNode          DeclListNode, StmtListNode
//     StmtListNode        linked list of StmtNode
//     ExpListNode         linked list of ExpNode
//
//     TypeNode:
//       IntNode           -- none --
//       BoolNode          -- none --
//       VoidNode          -- none --
//       StructNode        IdNode
//
//     StmtNode:
//       AssignStmtNode      AssignExpNode
//       PostIncStmtNode     ExpNode
//       PostDecStmtNode     ExpNode
//       ReadStmtNode        ExpNode
//       WriteStmtNode       ExpNode
//       IfStmtNode          ExpNode, DeclListNode, StmtListNode
//       IfElseStmtNode      ExpNode, DeclListNode, StmtListNode,
//                                    DeclListNode, StmtListNode
//       WhileStmtNode       ExpNode, DeclListNode, StmtListNode
//       CallStmtNode        CallExpNode
//       ReturnStmtNode      ExpNode
//
//     ExpNode:
//       IntLitNode          -- none --
//       StrLitNode          -- none --
//       TrueNode            -- none --
//       FalseNode           -- none --
//       IdNode              -- none --
//       DotAccessNode       ExpNode, IdNode
//       AssignExpNode       ExpNode, ExpNode
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode     
//         MinusNode
//         TimesNode
//         DivideNode
//         AndNode
//         OrNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         GreaterNode
//         LessEqNode
//         GreaterEqNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with linked lists of kids, or
// internal nodes with a fixed number of kids:
//
// (1) Leaf nodes:
//        IntNode,   BoolNode,  VoidNode,  IntLitNode,  StrLitNode,
//        TrueNode,  FalseNode, IdNode
//
// (2) Internal nodes with (possibly empty) linked lists of children:
//        DeclListNode, FormalsListNode, StmtListNode, ExpListNode
//
// (3) Internal nodes with fixed numbers of kids:
//        ProgramNode,     VarDeclNode,     FnDeclNode,     FormalDeclNode,
//        StructDeclNode,  FnBodyNode,      StructNode,     AssignStmtNode,
//        PostIncStmtNode, PostDecStmtNode, ReadStmtNode,   WriteStmtNode   
//        IfStmtNode,      IfElseStmtNode,  WhileStmtNode,  CallStmtNode
//        ReturnStmtNode,  DotAccessNode,   AssignExpNode,  CallExpNode,
//        UnaryExpNode,    BinaryExpNode,   UnaryMinusNode, NotNode,
//        PlusNode,        MinusNode,       TimesNode,      DivideNode,
//        AndNode,         OrNode,          EqualsNode,     NotEqualsNode,
//        LessNode,        GreaterNode,     LessEqNode,     GreaterEqNode
//
// **********************************************************************

// **********************************************************************
//   ASTnode class (base class for all other kinds of nodes)
// **********************************************************************

abstract class ASTnode { 
    // every subclass must provide an unparse operation
    abstract public void unparse(PrintWriter p, int indent);

    // this method can be used by the unparse methods to do indenting
    protected void doIndent(PrintWriter p, int indent) {
        for (int k = 0; k < indent; k++) p.print(" ");
    }
}

// **********************************************************************
//   ProgramNode,  DeclListNode, FormalsListNode, FnBodyNode,
//   StmtListNode, ExpListNode
// **********************************************************************

class ProgramNode extends ASTnode {
    // 1 kid
    private DeclListNode myDeclList;

    public ProgramNode(DeclListNode L) {
        myDeclList = L;
    }

    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        myDeclList.addAndAnalyzeDecl(symTable);
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    }
}

class DeclListNode extends ASTnode {
    // list of kids (DeclNodes)
    private List<DeclNode> myDecls;

    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    }

    public List<Sym> addAndAnalyzeDecl(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        List<Sym> result = new ArrayList<>();
        for (DeclNode myDecl : myDecls) {
            result.add(myDecl.addAndAnalyzeDecl(symTable));
        }
        return result;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<DeclNode> it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode) it.next()).unparse(p, indent);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }
}

class FormalsListNode extends ASTnode {
    // list of kids (FormalDeclNodes)
    private List<FormalDeclNode> myFormals;

    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    }

    public List<Sym> addAndAnalyzeDecl(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        List<Sym> result = new ArrayList<>();
        for (FormalDeclNode fdNode : myFormals) {
            result.add(fdNode.addAndAnalyzeDecl(symTable));
        }
        return result;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) { // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        }
    }
}

class FnBodyNode extends ASTnode {
    // 2 kids
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;

    public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        myDeclList.addAndAnalyzeDecl(symTable);
        myStmtList.analyze(symTable);
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    }
}

class StmtListNode extends ASTnode {
    // list of kids (StmtNodes)
    private List<StmtNode> myStmts;

    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        for (StmtNode e : myStmts) {
            e.analyze(symTable);
        }
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        }
    }
}

class ExpListNode extends ASTnode {
    // list of kids (ExpNodes)
    private List<ExpNode> myExps;

    public ExpListNode(List<ExpNode> S) {
        myExps = S;
    }

    public void checkDefined(SymTable symTable) throws EmptySymTableException {
        for (ExpNode e : myExps) {
            e.checkDefinedAndGetSym(symTable);
        }
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) { // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        }
    }
}

// **********************************************************************
// DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
    abstract public Sym addAndAnalyzeDecl(SymTable symTable) throws EmptySymTableException, DuplicateSymException;
}

class VarDeclNode extends DeclNode {
    public static int NOT_STRUCT = -1;
    // 3 kids
    private TypeNode myType;
    private IdNode myId;
    private int mySize; // use value NOT_STRUCT if this is not a struct type
    private Sym sym;

    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    }

    public Sym addAndAnalyzeDecl(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        sym = symTable.lookupLocal(myId.toString());

        if (sym != null && sym.getCategory().equals(Category.FORMAL))
            return sym;


        if (mySize == NOT_STRUCT)
            myId.addAndAnalyzeDecl(symTable, myType.toString(), Category.NORMAL);
        else
            myId.addAndAnalyzeDecl(symTable, myType.toString(), Category.STRUCT_VAR);

        return sym;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.println(";");
    }
}

class FnDeclNode extends DeclNode {
    // 4 kids
    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FnBodyNode myBody;

    public FnDeclNode(TypeNode type, IdNode id, FormalsListNode formalList, FnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    }

    public Sym addAndAnalyzeDecl(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        Sym sym = myId.addAndAnalyzeDecl(symTable, myType.toString(), Category.FUNCTION);

        if (sym.getCategory().equals(Category.UNDEFINED)) {
        	return sym;
        }

        FunctionSym myIdSym = (FunctionSym) sym;

        symTable.addScope();
        myIdSym.setFormalList(myFormalsList.addAndAnalyzeDecl(symTable));
        myBody.analyze(symTable);
        symTable.removeScope();


        return myIdSym;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.print("(");
        myFormalsList.unparse(p, 0);
        p.println(") {");
        myBody.unparse(p, indent + 4);
        p.println("}\n");
    }
}

class FormalDeclNode extends DeclNode {
    // 2 kids
    private TypeNode myType;
    private IdNode myId;

    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    }

    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
    }


    public Sym addAndAnalyzeDecl(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        return myId.addAndAnalyzeDecl(symTable, myType.toString(), Category.FORMAL);
    }
}

class StructDeclNode extends DeclNode {

    private IdNode myId;
    private DeclListNode myDeclList;

    public StructDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
        myDeclList = declList;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("struct ");
        myId.unparse(p, 0);
        p.println("{");
        myDeclList.unparse(p, indent + 4);
        doIndent(p, indent);
        p.println("};\n");
    }

    public Sym addAndAnalyzeDecl(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        Sym sym = myId.addAndAnalyzeDecl(symTable, "struct", Category.STRUCT_DECL);

        if (sym.getCategory().equals(Category.UNDEFINED)) {
        	return sym;
        }

        SymTable structSymTable = new SymTable();
        myDeclList.addAndAnalyzeDecl(structSymTable);
        StructDeclSym structVarSym = (StructDeclSym) sym;
        structVarSym.setSymTable(structSymTable);
        return sym;
    }
}

// **********************************************************************
// TypeNode and its Subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {

}

class IntNode extends TypeNode {
    public IntNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("int");
    }

    @Override
    public String toString() {
        return "int";
    }
}

class BoolNode extends TypeNode {
    public BoolNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("bool");
    }

    @Override
    public String toString() {
        return "bool";
    }
}

class VoidNode extends TypeNode {
    public VoidNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }

    @Override
    public String toString() {
        return "void";
    }
}

class StructNode extends TypeNode {
    // 1 kid
    private IdNode myId;

    public StructNode(IdNode id) {
        myId = id;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("struct ");
        myId.unparse(p, 0);
    }

    @Override
    public String toString() {
        return myId.toString();
    }
}

// **********************************************************************
// StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
    abstract public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException;
}

class AssignStmtNode extends StmtNode {
    // 1 kid
    private AssignExpNode myAssign;

    public AssignStmtNode(AssignExpNode assign) {
        myAssign = assign;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myAssign.unparse(p, -1); // no parentheses
        p.println(";");
    }

    @Override
    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        myAssign.checkDefinedAndGetSym(symTable);
    }
}

class PostIncStmtNode extends StmtNode {
    // 1 kid
    private ExpNode myExp;

    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("++;");
    }

    @Override
    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        myExp.checkDefinedAndGetSym(symTable);
    }
}

class PostDecStmtNode extends StmtNode {
    // 1 kid
    private ExpNode myExp;

    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("--;");
    }

    @Override
    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        myExp.checkDefinedAndGetSym(symTable);
    }
}

class ReadStmtNode extends StmtNode {
    // 1 kid (actually can only be an IdNode or an ArrayExpNode)
    private ExpNode myExp;

    public ReadStmtNode(ExpNode e) {
        myExp = e;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("input >> ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    @Override
    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        myExp.checkDefinedAndGetSym(symTable);
    }
}

class WriteStmtNode extends StmtNode {
    // 1 kid
    private ExpNode myExp;

    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("disp << ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    @Override
    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        myExp.checkDefinedAndGetSym(symTable);
    }
}

class IfStmtNode extends StmtNode {
    // e kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;

    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent + 4);
        myStmtList.unparse(p, indent + 4);
        doIndent(p, indent);
        p.println("}");
    }

    @Override
    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        myExp.checkDefinedAndGetSym(symTable);
        symTable.addScope();
        myDeclList.addAndAnalyzeDecl(symTable);
        myStmtList.analyze(symTable);
        symTable.removeScope();
    }
}

class IfElseStmtNode extends StmtNode {
    // 5 kids
    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;

    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1, StmtListNode slist1, DeclListNode dlist2,
                          StmtListNode slist2) {
        myExp = exp;

        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myThenDeclList.unparse(p, indent + 4);
        myThenStmtList.unparse(p, indent + 4);
        doIndent(p, indent);
        p.println("}");
        doIndent(p, indent);
        p.println("else {");
        myElseDeclList.unparse(p, indent + 4);
        myElseStmtList.unparse(p, indent + 4);
        doIndent(p, indent);
        p.println("}");
    }

    @Override
    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        myExp.checkDefinedAndGetSym(symTable);
        symTable.addScope();
        myThenDeclList.addAndAnalyzeDecl(symTable);
        myThenStmtList.analyze(symTable);
        symTable.removeScope();
        symTable.addScope();
        myElseDeclList.addAndAnalyzeDecl(symTable);
        myElseStmtList.analyze(symTable);
        symTable.removeScope();
    }
}

class WhileStmtNode extends StmtNode {
    // 3 kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;

    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("while (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent + 4);
        myStmtList.unparse(p, indent + 4);
        doIndent(p, indent);
        p.println("}");
    }

    @Override
    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        myExp.checkDefinedAndGetSym(symTable);
        symTable.addScope();
        myDeclList.addAndAnalyzeDecl(symTable);
        myStmtList.analyze(symTable);
        symTable.removeScope();
    }
}



class CallStmtNode extends StmtNode {
    // 1 kid
    private CallExpNode myCall;

    public CallStmtNode(CallExpNode call) {
        myCall = call;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myCall.unparse(p, indent);
        p.println(";");
    }

    @Override
    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        myCall.checkDefinedAndGetSym(symTable);
    }
}

class ReturnStmtNode extends StmtNode {
    // 1 kid
    private ExpNode myExp; // possibly null

    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, 0);
        }
        p.println(";");
    }

    @Override
    public void analyze(SymTable symTable) throws EmptySymTableException, DuplicateSymException {
        if (myExp != null)
            myExp.checkDefinedAndGetSym(symTable);
    }
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {
    protected int myLineNum;
    protected int myCharNum;

    abstract public Sym checkDefinedAndGetSym(SymTable symTable) throws EmptySymTableException;
}

class IntLitNode extends ExpNode {
    private int myIntVal;

    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
    }

    @Override
    public Sym checkDefinedAndGetSym(SymTable symTable) throws EmptySymTableException {
        return new UndefinedSym();
    }
}

class StringLitNode extends ExpNode {
    private String myStrVal;

    public StringLitNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
    }

    @Override
    public Sym checkDefinedAndGetSym(SymTable symTable) throws EmptySymTableException {
        return new UndefinedSym();
    }
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("true");
    }

    @Override
    public Sym checkDefinedAndGetSym(SymTable symTable) throws EmptySymTableException {
        return new UndefinedSym();
    }
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("false");
    }

    @Override
    public Sym checkDefinedAndGetSym(SymTable symTable) throws EmptySymTableException {
        return new UndefinedSym();
    }
}

class IdNode extends ExpNode {
    private String myStrVal;
    private Sym sym;
    private boolean decl = false;

    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public Sym addAndAnalyzeDecl(SymTable symTable, String type, Category category)
            throws EmptySymTableException, DuplicateSymException {
        decl = true;

        boolean success = true;

        if (!category.equals(Category.FUNCTION) && type.equals("void")) {
        	ErrMsg.fatal(myLineNum, myCharNum, "Non-function declared void");
            success = false;
        }

        if (category.equals(Category.STRUCT_VAR) && symTable.lookupGlobal(type) == null) {
        	ErrMsg.fatal(myLineNum, myCharNum, "Name of struct type invalid");
            success = false;
        }

        switch (category) {
            case STRUCT_VAR:
                sym = new StructVarSym(type);
                break;
            case STRUCT_DECL:
                sym = new StructDeclSym(type);
                break;
            case NORMAL:
                sym = new Sym(type);
                break;
            case FUNCTION:
                sym = new FunctionSym(type);
                break;
            case FORMAL:
                sym = new FormalSym(type);
                break;
            default:
                sym = new UndefinedSym();
                break;
        }


        try {
            symTable.addDecl(myStrVal, sym);
        } catch (DuplicateSymException e) {
        	ErrMsg.fatal(myLineNum, myCharNum, "Identifier multiply-declared");
            success = false;
        }

        return success ? sym : new UndefinedSym();
    }

    @Override
    public Sym checkDefinedAndGetSym(SymTable symTable) {
        try {
        	decl = false;
        	sym = symTable.lookupGlobal(myStrVal);
        	if (sym == null) {
        		ErrMsg.fatal(myLineNum, myCharNum, "Identifier undeclared");
        		sym = new UndefinedSym();
        	}
        } catch (EmptySymTableException e) {
        	
        }
        return sym;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
        if (decl || sym == null) return;
        p.print("(" + sym + ")");
    }

    @Override
    public String toString() {
        return myStrVal;
    }
}

class DotAccessExpNode extends ExpNode {
    // 2 kids
    private ExpNode myLoc;
    private IdNode myId;
    private Sym myLocSym;
    private Sym myIdSym;

    public DotAccessExpNode(ExpNode loc, IdNode id) {
        myLoc = loc;
        myId = id;
        myCharNum = loc.myCharNum;
        myLineNum = loc.myLineNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myLoc.unparse(p, 0);
        p.print(").");
        myId.unparse(p, 0);
        p.print("(" + myIdSym + ")");
    }

    @Override
    public Sym checkDefinedAndGetSym(SymTable symTable) throws EmptySymTableException {
        myLocSym = myLoc.checkDefinedAndGetSym(symTable);
        if (!myLocSym.getCategory().equals(Category.STRUCT_VAR)) {
        	ErrMsg.fatal(myLineNum, myCharNum, "Dot-access of non-struct type");
            return new UndefinedSym();
        }

        StructDeclSym structDeclSym = (StructDeclSym) symTable.lookupGlobal(myLocSym.getType());

        myIdSym = structDeclSym.getSymTable().lookupGlobal(myId.toString());
        if (myIdSym == null) {
        	ErrMsg.fatal(myLineNum, myCharNum, "Struct field name invalid");
            myIdSym = new UndefinedSym();
        }
        return myIdSym;
    }
}

class AssignExpNode extends ExpNode {
    // 2 kids
    private ExpNode myLhs;
    private ExpNode myExp;

    public AssignExpNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
        myCharNum = lhs.myCharNum;
        myLineNum = lhs.myLineNum;
    }

    public void unparse(PrintWriter p, int indent) {
        if (indent != -1)
            p.print("(");
        myLhs.unparse(p, 0);
        p.print(" = ");
        myExp.unparse(p, 0);
        if (indent != -1)
            p.print(")");
    }

    @Override
    public Sym checkDefinedAndGetSym(SymTable symTable) throws EmptySymTableException {
        myExp.checkDefinedAndGetSym(symTable);
        return myLhs.checkDefinedAndGetSym(symTable);
    }
}

class CallExpNode extends ExpNode {
    // 2 kids
    private IdNode myId;
    private ExpListNode myExpList; // possibly null

    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
        myCharNum = name.myCharNum;
        myLineNum = name.myLineNum;
    }

    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    }

    // ** unparse **
    public void unparse(PrintWriter p, int indent) {
        myId.unparse(p, 0);
        p.print("(");
        if (myExpList != null) {
            myExpList.unparse(p, 0);
        }
        p.print(")");
    }

    @Override
    public Sym checkDefinedAndGetSym(SymTable symTable) throws EmptySymTableException {
        myExpList.checkDefined(symTable);
        return myId.checkDefinedAndGetSym(symTable);
    }
}

abstract class UnaryExpNode extends ExpNode {
    // one child
    protected ExpNode myExp;

    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
        myCharNum = exp.myCharNum;
        myLineNum = exp.myLineNum;
    }

    @Override
    public Sym checkDefinedAndGetSym(SymTable symTable) throws EmptySymTableException {
        return myExp.checkDefinedAndGetSym(symTable);
    }
}

abstract class BinaryExpNode extends ExpNode {
    // two kids
    protected ExpNode myExp1;
    protected ExpNode myExp2;

    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
        myCharNum = exp1.myCharNum;
        myLineNum = exp1.myLineNum;
    }

    @Override
    public Sym checkDefinedAndGetSym(SymTable symTable) throws EmptySymTableException {
        myExp2.checkDefinedAndGetSym(symTable);
        return myExp1.checkDefinedAndGetSym(symTable);
    }
}

// **********************************************************************
// Subclasses of UnaryExpNode
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode {
    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(-");
        myExp.unparse(p, 0);
        p.print(")");
    }
}

class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(!");
        myExp.unparse(p, 0);
        p.print(")");
    }
}

// **********************************************************************
// Subclasses of BinaryExpNode
// **********************************************************************

class PlusNode extends BinaryExpNode {
    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" + ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class MinusNode extends BinaryExpNode {
    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" - ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class TimesNode extends BinaryExpNode {
    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" * ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class DivideNode extends BinaryExpNode {
    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" / ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class AndNode extends BinaryExpNode {
    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" && ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class OrNode extends BinaryExpNode {
    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" || ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class EqualsNode extends BinaryExpNode {
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" == ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class NotEqualsNode extends BinaryExpNode {
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" != ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class LessNode extends BinaryExpNode {
    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" < ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class GreaterNode extends BinaryExpNode {
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" > ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class LessEqNode extends BinaryExpNode {
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" <= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class GreaterEqNode extends BinaryExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" >= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}