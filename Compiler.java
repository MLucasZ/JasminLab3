import CPP.Absyn.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class Compiler
{
    // The output of the compiler is a list of strings.
    private LinkedList<String> output;

    // Signature mapping function names to their JVM name and type
    private Map<String,Fun> sig;

    // Context mapping variable identifiers to their type.
    private List<Map<String,Type>> cxt;

    // Context mapping address identifiers to their type.
    private List<Map<String,Integer>> addrs;

    // Next free address for local variable;
    private int nextLocal = 0;

    // jasmin: Sets the number of local variables required by the method.
    private int limitLocals;

    // jasmin: Sets the maximum size of the operand stack required by the method.
    private int limitStack;

    // Current stack size
    private int currentStack;
    private BranchingUtils branchingUtils;
    private String className;

    private final String STM_EXP = "SExp";
    private final String STM_INIT = "SInit";
    private final String STM_BLOCK = "SBlock";
    private final String STM_WHILE = "SWhile";
    private final String STM_IFLESE = "SIfElse";
    private final String STM_RETURN = "SReturn";

    private final String PRINT_INT = "printInt";
    private final String READ_INT = "readInt";

    private final String NEW_LINE = "\n";




    public void compile(String name, CPP.Absyn.Program p) {
        branchingUtils = new BranchingUtils();
        // Initialize output
        output = new LinkedList();

        // boilerplate code before start execute everything
        className = name.substring(0, name.indexOf(".cc"));
        output.add(boilerPlateConst(className));

        // Create signature
        sig = new TreeMap();
        for (Def d: ((PDefs)p).listdef_) {
            DFun def = (DFun)d;
            sig.put(def.id_,
                    new Fun(def.id_, new FunType(def.type_, def.listarg_)));
        }

        // Run compiler
        p.accept(new ProgramVisitor(), null);
        try {
            PrintWriter out = new PrintWriter(className+".j");
            // Output result
            for (String s: output) {
                System.out.print(s);
                out.print(s);
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }





    public class ProgramVisitor implements Program.Visitor<Void,Void>
    {
        public Void visit(CPP.Absyn.PDefs p, Void arg)
        {
            for (Def def: p.listdef_)
            {
                def.accept(new DefVisitor(), null);
            }
            return null;
        }
    }





    public class DefVisitor implements Def.Visitor<Void,Void>
    {
        public Void visit(CPP.Absyn.DFun p, Void arg)
        {
            // reset state for new function
            cxt = new LinkedList();
            addrs = new LinkedList();
            cxt.add(new TreeMap());
            addrs.add(new TreeMap());
            nextLocal = 0;
            limitLocals = 0;
            limitStack  = 0;
            currentStack = 0;

            // save output so far and reset output;
            LinkedList<String> savedOutput = output;
            output = new LinkedList();

            // Compile function

            // Add function parameters to context
            for (Arg x: p.listarg_)
                x.accept (new ArgVisitor(), null);

            for (Stm s: p.liststm_) {
                s.accept(new StmVisitor(), null);
            }

            // we get new output by visiting the args and statements
            LinkedList<String> newOutput = output;
            // we reset the global output
            output = savedOutput;

            // then add the initial method code to the top of the output
            output.add("\n.method public static " + sig.get(p.id_).toJVM() + "\n");
            output.add("  .limit locals " + limitLocals + "\n");
            output.add("  .limit stack " + limitStack + "\n\n");

            // then we iterate the new output and append the items to the global output
            for (String s: newOutput) {
                output.add("  " + s);
            }

            // because not all test files have return 0!
            if(p.id_.equals("main")) {
                String ending = output.get(output.size()-1);
                if(!ending.contains("ireturn")) {
                    output.add("iconst_0\n");
                    output.add("ireturn\n");
                }
            } else {
                String ending = output.get(output.size()-1);
                if(!ending.contains("return")) {
                    output.add("return\n");
                }
            }

            // end the method
            output.add("\n.end method\n");
            return null;
        }
    }





    public class ArgVisitor implements Arg.Visitor<Void,Void>
    {
        public Void visit(CPP.Absyn.ADecl p, Void arg)
        {
            newVar (p.id_, p.type_);
            return null;
        }
    }





    public class StmVisitor implements Stm.Visitor<Void,String>
    {
        // e;
        public Void visit(CPP.Absyn.SExp p, String arg)
        {
            p.exp_.accept(new ExpVisitor(), STM_EXP);
            return null;
        }

        // int x,y,z;
        public Void visit(CPP.Absyn.SDecls p, String arg)
        {
            for (String x: p.listid_) {
                newVar(x, p.type_);
            }
            return null;
        }

        // int x = e;
        public Void visit(CPP.Absyn.SInit p, String arg)
        {
            newVar(p.id_, p.type_);
            Integer addr = lookupVar(p.id_);
            p.exp_.accept(new ExpVisitor(), STM_INIT);
            emit(new Store(p.type_, addr));
            return null;
        }

        public Void visit(CPP.Absyn.SBlock p, String arg)
        { /* Code For SBlock Goes Here */
            // every time block statement found
            // put its environment on top of the list
            addBlockLevel();

            for (Stm stm: p.liststm_) {
                stm.accept(new StmVisitor(), STM_BLOCK);
            }

            removeTopBlock();
            return null;
        }

        public Void visit(CPP.Absyn.SReturn p, String arg)
        { /* Code For SReturn Goes Here */
            // because int and boolean both use ireturn
            // while void doesn't have return statement
            Type t = new Type_int();
            if(branchingUtils.isConditionalExp(p.exp_)) {
                branchingUtils.genReturnBranch(p);
            } else {
                String var = p.exp_.accept(new ExpVisitor(), STM_RETURN);
                loadVariable(t, var);
                emit (new Return(t));
            }
            /*p.exp_.accept(new ExpVisitor(), STM_RETURN);
            emit (new Return(new Type_int()));*/
            return null;
        }

        /**loop adds label when the condition is true
         * and also label when the condition is false*/
        public Void visit(CPP.Absyn.SWhile p, String arg)
        { /* Code For SWhile Goes Here */
            branchingUtils.genWhileBranch(p);
            return null;
        }

        public Void visit(CPP.Absyn.SIfElse p, String arg)
        { /* Code For SIfElse Goes Here */
            branchingUtils.genIfElseBranch(p);
            return null;
        }
    }





    public class ExpVisitor implements Exp.Visitor<String,String>
    {
        public String visit(CPP.Absyn.ETrue p, String arg)
        { /* Code For ETrue Goes Here */
            emit(new IConst(1));
            return "true";
        }
        public String visit(CPP.Absyn.EFalse p, String arg)
        { /* Code For EFalse Goes Here */
            emit(new IConst(0));
            return "false";
        }
        public String visit(CPP.Absyn.EInt p, String arg)
        {
            emit(new IConst (p.integer_));
            return null;
        }

        // exclude double for now
        public String visit(CPP.Absyn.EDouble p, String arg)
        { /* Code For EDouble Goes Here */
            //p.double_;
            return null;
        }

        public String visit(CPP.Absyn.EId p, String arg)
        { /* Code For EId Goes Here */
            return p.id_;
        }
        public String visit(CPP.Absyn.EApp p, String arg)
        { /* Code For EApp Goes Here */
            boolean conditionalExp = false;
            for (Exp x: p.listexp_) {
                conditionalExp = branchingUtils.isConditionalExp(x);
                String id = x.accept(new ExpVisitor(), "EApp");
                Type t = new Type_int();
                loadVariable(t, id);
            }

            if(p.id_.equals(PRINT_INT)) {
                // name, return, args
                emit(new Call(PRINT_INT, FunType.ASM_VOID, FunType.ASM_INT));
            } else if(p.id_.equals(READ_INT)) {
                emit(new Call(READ_INT, FunType.ASM_INT, ""));
            } else {
                Fun fun = sig.get(p.id_);
                FunType funType = fun.funType;
                if(conditionalExp) {
                    branchingUtils.genMthBranch(fun);
                } else {
                    emit(new Call(className, fun));
                }
                if(funType.returnType instanceof Type_bool) {
                    branchingUtils.handleMthBoolReturn();
                }
            }
            return null;
        }
        public String visit(CPP.Absyn.EPostIncr p, String arg)
        { /* Code For EPostIncr Goes Here */
            String id = p.exp_.accept(new ExpVisitor(), arg);
            Type t = new Type_int();
            loadVariable(t, id);
            emit(new Inc(t, lookupVar(id), 1));

            return null;
        }
        public String visit(CPP.Absyn.EPostDecr p, String arg)
        { /* Code For EPostDecr Goes Here */
            String id = p.exp_.accept(new ExpVisitor(), arg);
            Type t = new Type_int();
            loadVariable(t, id);
            emit(new Inc(t, lookupVar(id), -1));

            return null;
        }
        public String visit(CPP.Absyn.EPreIncr p, String arg)
        { /* Code For EPreIncr Goes Here */
            String id = p.exp_.accept(new ExpVisitor(), arg);
            Type t = new Type_int();
            emit(new Inc(t, lookupVar(id), 1));
            loadVariable(t, id);
            return null;
        }
        public String visit(CPP.Absyn.EPreDecr p, String arg)
        { /* Code For EPreDecr Goes Here */
            String id = p.exp_.accept(new ExpVisitor(), arg);
            Type t = new Type_int();
            emit(new Inc(t, lookupVar(id), -1));
            loadVariable(t, id);
            return null;
        }
        public String visit(CPP.Absyn.ETimes p, String arg)
        { /* Code For ETimes Goes Here */
            Type t = new Type_int();

            String exp1 = p.exp_1.accept(new ExpVisitor(), arg);
            loadVariable(t, exp1);

            String exp2 = p.exp_2.accept(new ExpVisitor(), arg);
            loadVariable(t, exp2);

            emit(new Mul(t));
            return null;
        }
        public String visit(CPP.Absyn.EDiv p, String arg)
        { /* Code For EDiv Goes Here */
            Type t = new Type_int();

            String exp1 = p.exp_1.accept(new ExpVisitor(), arg);
            loadVariable(t, exp1);

            String exp2 = p.exp_2.accept(new ExpVisitor(), arg);
            loadVariable(t, exp2);

            emit(new Div(t));
            return null;
        }
        public String visit(CPP.Absyn.EPlus p, String arg)
        { /* Code For EPlus Goes Here */
            Type t = new Type_int();

            String exp1 = p.exp_1.accept(new ExpVisitor(), arg);
            loadVariable(t, exp1);

            String exp2 = p.exp_2.accept(new ExpVisitor(), arg);
            loadVariable(t, exp2);

            emit(new Add(t));
            return null;
        }
        public String visit(CPP.Absyn.EMinus p, String arg)
        { /* Code For EMinus Goes Here */
            Type t = new Type_int();

            String exp1 = p.exp_1.accept(new ExpVisitor(), arg);
            loadVariable(t, exp1);

            String exp2 = p.exp_2.accept(new ExpVisitor(), arg);
            loadVariable(t, exp2);

            emit(new Sub(t));
            return null;
        }
        public String visit(CPP.Absyn.ELt p, String arg)
        { /* Code For ELt Goes Here */
            Type t = new Type_int();

            String exp1 = p.exp_1.accept(new ExpVisitor(), arg);
            loadVariable(t, exp1);

            String exp2 = p.exp_2.accept(new ExpVisitor(), arg);
            loadVariable(t, exp2);

            branchingUtils.genBooleanBranch(p);
            return null;
        }
        public String visit(CPP.Absyn.EGt p, String arg)
        { /* Code For EGt Goes Here */
            Type t = new Type_int();

            String exp1 = p.exp_1.accept(new ExpVisitor(), arg);
            loadVariable(t, exp1);

            String exp2 = p.exp_2.accept(new ExpVisitor(), arg);
            loadVariable(t, exp2);

            branchingUtils.genBooleanBranch(p);
            return null;
        }
        public String visit(CPP.Absyn.ELtEq p, String arg)
        { /* Code For ELtEq Goes Here */
            Type t = new Type_int();

            String exp1 = p.exp_1.accept(new ExpVisitor(), arg);
            loadVariable(t, exp1);

            String exp2 = p.exp_2.accept(new ExpVisitor(), arg);
            loadVariable(t, exp2);

            branchingUtils.genBooleanBranch(p);
            return null;
        }
        public String visit(CPP.Absyn.EGtEq p, String arg)
        { /* Code For EGtEq Goes Here */
            Type t = new Type_int();

            String exp1 = p.exp_1.accept(new ExpVisitor(), arg);
            loadVariable(t, exp1);

            String exp2 = p.exp_2.accept(new ExpVisitor(), arg);
            loadVariable(t, exp2);

            branchingUtils.genBooleanBranch(p);
            return null;
        }
        public String visit(CPP.Absyn.EEq p, String arg)
        { /* Code For EEq Goes Here */
            Type t = new Type_int();

            String exp1 = p.exp_1.accept(new ExpVisitor(), arg);
            loadVariable(t, exp1);

            String exp2 = p.exp_2.accept(new ExpVisitor(), arg);
            loadVariable(t, exp2);

            branchingUtils.genBooleanBranch(p);
            return null;
        }
        public String visit(CPP.Absyn.ENEq p, String arg)
        { /* Code For ENEq Goes Here */
            Type t = new Type_int();

            String exp1 = p.exp_1.accept(new ExpVisitor(), arg);
            loadVariable(t, exp1);

            String exp2 = p.exp_2.accept(new ExpVisitor(), arg);
            loadVariable(t, exp2);

            branchingUtils.genBooleanBranch(p);
            return null;
        }
        public String visit(CPP.Absyn.EAnd p, String arg)
        { /* Code For EAnd Goes Here */
            // for && if exp1 is true then needs to evaluate exp2
            // but if exp1 is false, whatever value of exp2 will make entire statement false
            // thus, no need to evaluate exp2
            branchingUtils.lazyEvaluation(p, arg);
            return null;
        }
        public String visit(CPP.Absyn.EOr p, String arg)
        { /* Code For EOr Goes Here */
            // OR just need exp1 to be true and the rest won't be evaluated
            // since every value from exp2 will result the statement to be true
            branchingUtils.lazyEvaluation(p, arg);
            return null;
        }
        public String visit(CPP.Absyn.EAss p, String arg)
        { /* Code For EAss Goes Here */
            Type t = new Type_int();

            String exp2 = p.exp_2.accept(new ExpVisitor(), arg);
            loadVariable(t, exp2);

            String exp1 = p.exp_1.accept(new ExpVisitor(), arg);

            // sequence of istore, iload, pop
            emit(new Store(t, lookupVar(exp1)));
            emit(new Load(t, lookupVar(exp1)));

            // shouldn't pop when assign in conditional?
            if(!arg.equals(STM_WHILE))
                popStack();

            return null;
        }
    }





    class AdjustStack implements CodeVisitor<Void> {

        public Void visit (Store c) {
            decStack(c.type);
            return null;
        }

        public Void visit (Load c) {
            incStack(c.type);
            return null;
        }

        public Void visit (IConst c) {
            incStack(new Type_int());
            return null;
        }

        // exclude double for now
        public Void visit (DConst c) {
            // incStack(new Type_double());
            return null;
        }

        public Void visit (Dup c) {
            incStack(c.type);
            return null;
        }

        public Void visit (Pop c) {
            decStack(c.type);
            return null;
        }

        public Void visit (Return c) {
            return null;
        }

        public Void visit (Call c) {
            return null;
        }

        public Void visit (Label c) {
            return null;
        }

        public Void visit (Goto c) {
            return null;
        }

        public Void visit (IfZ c) {
            return null;
        }

        public Void visit (IfNZ c) {
            return null;
        }

        public Void visit (IfEq c) {
            return null;
        }

        public Void visit (IfNe c) {
            return null;
        }

        public Void visit (IfLt c) {
            return null;
        }

        public Void visit (IfGt c) {
            return null;
        }

        public Void visit (IfLe c) {
            return null;
        }

        public Void visit (IfGe c) {
            return null;
        }

        public Void visit (DGt c) {
            return null;
        }

        public Void visit (DLt c) {
            return null;
        }

        public Void visit (Inc c) {
            return null;
        }

        public Void visit (Add c) {
            decStack(c.type);
            return null;
        }

        public Void visit (Sub c) {
            decStack(c.type);
            return null;
        }

        public Void visit (Mul c) {
            decStack(c.type);
            return null;
        }

        public Void visit (Div c) {
            decStack(c.type);
            return null;
        }
    }





    /**FUNCTIONS AND TOOLS*/
    void emit (Code c) {
        output.add(c.accept(new CodeToJVM()));
        adjustStack(c);
    }

    void newVar(String x, Type t) {
        cxt.get(0).put(x,t);
        addrs.get(0).put(x,nextLocal);

        // increase the size of local var stack
        ++limitLocals;
        nextLocal = nextLocal + t.accept(new Size(), null);
    }

    private void popStack() {
        Type t = new Type_int();
        emit(new Pop(t));
    }

    // really robust implementation
    // assuming that the increment ops
    // are not immediately evaluated at the same line
    private void popUnusedLoad() {
        String load1 = output.get(output.size()-1);
        String load2 = output.get(output.size()-2);
        if(load1.contains("iload") && load2.contains("iinc")) {
            popStack();
        } else if(load2.contains("iload") && load1.contains("iinc")) {
            popStack();
        }
    }

    private boolean loadVariable(Type t, String id) {
        if(id != null) {
            int addr = lookupVar(id);
            if(addr != -1) {
                emit(new Load(t, addr));
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    Integer lookupVar (String id) {
        if(addrs.get(0).containsKey(id)) {
            return addrs.get(0).get(id);
        }
        return -1;
    }

    // update limitStack, currentStack according to instruction
    void adjustStack(Code c) {
        c.accept(new AdjustStack());
    }

    void incStack(Type t) {
        currentStack = currentStack + t.accept(new Size(), null);
        if (currentStack > limitStack) limitStack = currentStack;
    }

    void decStack(Type t) {
        currentStack = currentStack - t.accept(new Size(), null);
    }

    class Size implements Type.Visitor<Integer,Void> {
        // public Size() {}
        public Integer visit (Type_int t, Void arg) {
            return 1;
        }
        public Integer visit (Type_bool t, Void arg) {
            return 1;
        }
        public Integer visit (Type_void t, Void arg) {
            return 0;
        }

        // exclude double for now
        public Integer visit (Type_double t, Void arg) {
          return 2;
        }
    }

    private void addBlockLevel() {
        // copy the outer scope to the new scope
        Map<String,Type> vars = cxt.get(0);
        Map<String,Integer> addr = addrs.get(0);

        // add new index on top and set value
        cxt.add(0, new TreeMap<>(vars));
        addrs.add(0, new TreeMap<>(addr));
    }

    private void removeTopBlock() {
        Map<String,Type> vars = cxt.get(1);
        Map<String,Integer> addr = addrs.get(1);

        // just override the top block
        // with scope one level up of it
        cxt.set(0, new TreeMap<>(vars));
        // clean the next index
        cxt.remove(1);

        addrs.set(0, new TreeMap<>(addr));
        addrs.remove(1);
    }

    private String boilerPlateConst(String className) {
        String boilerPlate = ".class public " + className + NEW_LINE +
                ".super java/lang/Object" + NEW_LINE + NEW_LINE +
                ".method public <init>()V" + NEW_LINE +
                "aload_0" + NEW_LINE +
                "invokespecial java/lang/Object/<init>()V" + NEW_LINE +
                "return" + NEW_LINE +
                ".end method" + NEW_LINE + NEW_LINE +
                ".method public static main([Ljava/lang/String;)V" + NEW_LINE +
                ".limit locals 1" + NEW_LINE +
                "invokestatic "+ className +"/main()I" + NEW_LINE +
                "pop" + NEW_LINE +
                "return" + NEW_LINE +
                ".end method" + NEW_LINE + NEW_LINE;
        return boilerPlate;
    }




    /**Handles every operation that requires branching*/
    public class BranchingUtils {

        private int currentLabel = 0;

        public BranchingUtils() {}

        public void addLabelAddress() {
            currentLabel++;
        }

        public Label makeNewLabel() {
            return new Label(currentLabel);
        }

        private boolean evalBool(String arg) {
            if(arg != null) {
                if (loadVariable(new Type_int(), arg) || isBool(arg)) {
                    // emit boolean evaluation if
                    // it is a boolean variable or a boolean literal
                    Label lTrue = new Label(currentLabel);
                    Label lFalse = new Label(currentLabel + 1);
                    emit(new IfEq(new Type_bool(), lTrue));
                    emit(new Goto(lFalse));
                    return true;
                }
            }
            return false;
        }

        private boolean isBool(String s) {
            if(s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
                return true;
            } else { return false; }
        }

        public boolean isConditionalExp(Exp exp) {
            if(exp instanceof CPP.Absyn.ELt || exp instanceof CPP.Absyn.EGt ||
                    exp instanceof CPP.Absyn.ELtEq || exp instanceof CPP.Absyn.EGtEq ||
                    exp instanceof CPP.Absyn.EEq || exp instanceof CPP.Absyn.ENEq ||
                    exp instanceof CPP.Absyn.EAnd || exp instanceof CPP.Absyn.EOr) {
                return true;
            }
            return false;
        }

        public void handleMthBoolReturn() {
            Label lTrue = new Label(currentLabel);
            Label lFalse = new Label(currentLabel + 1);
            emit(new IfEq(new Type_bool(), lTrue));
            emit(new Goto(lFalse));
        }

        public void genWhileBranch(SWhile sWhile) {
            Exp condition = sWhile.exp_;
            Stm loopStm = sWhile.stm_;

            Label lCondition = makeNewLabel();
            addLabelAddress();

            emit(lCondition);
            String arg = condition.accept(new ExpVisitor(), STM_WHILE);
            evalBool(arg);

            Label lTrue = makeNewLabel();
            addLabelAddress();
            Label lFalse = makeNewLabel();
            addLabelAddress();

            emit(lTrue);
            loopStm.accept(new StmVisitor(), STM_WHILE);
            popUnusedLoad();
            emit(new Goto(lCondition));

            emit(lFalse);
        }

        public void genIfElseBranch(SIfElse sIfElse) {
            Exp condition = sIfElse.exp_;
            Stm stmTrue = sIfElse.stm_1;
            Stm stmFalse = sIfElse.stm_2;

            String arg = condition.accept(new ExpVisitor(), STM_IFLESE);
            evalBool(arg);

            Label lTrue = makeNewLabel();
            addLabelAddress();
            Label lFalse = makeNewLabel();
            addLabelAddress();
            Label lOut = makeNewLabel();
            addLabelAddress();

            emit(lTrue);
            stmTrue.accept(new StmVisitor(), STM_IFLESE);
            popUnusedLoad();
            emit(new Goto(lOut));

            emit(lFalse);
            stmFalse.accept(new StmVisitor(), STM_IFLESE);
            popUnusedLoad();
            emit(new Goto(lOut));

            emit(lOut);
        }

        public void genReturnBranch(SReturn sReturn) {
            Exp condition = sReturn.exp_;
            String arg = condition.accept(new ExpVisitor(), STM_RETURN);
            evalBool(arg);

            Label lTrue = makeNewLabel();
            addLabelAddress();
            Label lFalse = makeNewLabel();
            addLabelAddress();
            Label lOut = makeNewLabel();
            addLabelAddress();
            emit(lTrue);
            emit(new IConst(1));
            emit(new Return(new Type_bool()));
            emit(lFalse);
            emit(new IConst(0));
            emit(new Return(new Type_bool()));
            emit(lOut);
        }

        public void genMthBranch(Fun fun) {
            Label lTrue = makeNewLabel();
            addLabelAddress();
            Label lFalse = makeNewLabel();
            addLabelAddress();
            Label lOut = makeNewLabel();
            addLabelAddress();
            emit(lTrue);
            emit(new IConst(1));
            emit(new Call(className, fun));
            emit(new Goto(lOut));
            emit(lFalse);
            emit(new IConst(0));
            emit(new Call(className, fun));
            emit(new Goto(lOut));
            emit(lOut);
        }

        public void lazyEvaluation(Exp exp, String arg) {
            if(exp instanceof EAnd) {
                genEAndBranch(exp, arg);
            } else if(exp instanceof EOr) {
                genEOrBranch(exp, arg);
            }

            if(arg == null || arg.equals(STM_EXP)) {
                // the lazy evaluation doesn't belong
                // to any of the conditional statement,
                // so simply go to the next statement

                // add label to the next statement here
                Label lTrue = makeNewLabel();
                addLabelAddress();
                Label lFalse = makeNewLabel();
                addLabelAddress();
                // whether true or false just go to the next statement
                emit(lTrue);
                emit(new Goto(lFalse));
                emit(lFalse);
            } else {
                // no need to add next label. it is already handled by parent statement
            }
        }

        private void genEAndBranch(Exp exp, String arg) {
            EAnd eAnd = (EAnd) exp;

            String id1 = eAnd.exp_1.accept(new ExpVisitor(), arg);
            evalBool(id1);

            output.removeLast();
            LinkedList<String> save = output;
            output = new LinkedList<>();

            Label rhs = makeNewLabel();
            addLabelAddress();
            emit(rhs);
            String id2 = eAnd.exp_2.accept(new ExpVisitor(), arg);
            evalBool(id2);

            String last = output.getLast();
            save.add(last);
            save.addAll(output);
            output = save;
        }

        private void genEOrBranch(Exp exp, String arg) {
            EOr eOr = (EOr) exp;

            String id1 = eOr.exp_1.accept(new ExpVisitor(), arg);
            evalBool(id1);

            // we need to replace the label address in the future
            String ifne = output.get(output.size()-2);
            String orGoto = output.get(output.size()-1);
            String orGoto2 = orGoto.replace(orGoto.substring(orGoto.indexOf("L")-1),
                    " L"+currentLabel+NEW_LINE);
            output.set(output.size()-1, orGoto2);
            output.remove(output.size()-2);

            LinkedList<String> save = output;
            output = new LinkedList<>();

            Label rhs = makeNewLabel();
            addLabelAddress();
            emit(rhs);
            String id2 = eOr.exp_2.accept(new ExpVisitor(), arg);
            evalBool(id2);

            String last2 = output.get(output.size()-2);
            String label = last2.substring(last2.indexOf("L")-1);

            // replace the label address if exp1 true
            String replace = ifne.replace(ifne.substring(ifne.indexOf("L")-1), label);
            save.add(save.size()-1, replace);

            save.addAll(output);
            output = save;
        }

        private void genBooleanBranch(Exp exp) {
            Type t = new Type_int();
            Label lTrue = new Label(currentLabel);
            Label lFalse = new Label(currentLabel + 1);

            if(exp instanceof CPP.Absyn.ELt) {
                emit(new IfLt(t, lTrue));
            } else if(exp instanceof CPP.Absyn.EGt) {
                emit(new IfGt(t, lTrue));
            } else if(exp instanceof CPP.Absyn.ELtEq) {
                emit(new IfLe(t, lTrue));
            } else if(exp instanceof CPP.Absyn.EGtEq) {
                emit(new IfGe(t, lTrue));
            } else if(exp instanceof CPP.Absyn.EEq) {
                emit(new IfEq(t, lTrue));
            } else if(exp instanceof CPP.Absyn.ENEq) {
                emit(new IfNe(t, lTrue));
            }
            emit(new Goto(lFalse));
        }
    }
}