import CPP.Absyn.*;

import java.util.LinkedList;

public class FunType {

    public LinkedList<Arg> args;
    public Type returnType;

    public static final String ASM_INT = "I";
    public static final String ASM_BOOL = "Z";
    public static final String ASM_VOID = "V";

    public FunType(Type type, ListArg listArg) {
        this.returnType = type;
        this.args = listArg;
    }

    public String toJVM () {
        String argTypes = "";
        for (Arg a: args) {
            ADecl decl = (ADecl)a;
            argTypes = argTypes + decl.type_.accept (new TypeVisitor (), null);
        }
        return "(" + argTypes + ")" + returnType.accept (new TypeVisitor(), null);
    }

    public boolean hasArgs() {
        for (Arg a: args) {
            ADecl decl = (ADecl)a;
            decl.type_.accept (new TypeVisitor (), null);

            return true;
        }

        return false;
    }
}



class TypeVisitor implements Type.Visitor<String,Void> {

    public String visit(CPP.Absyn.Type_bool p, Void arg)
    {
        return FunType.ASM_BOOL;
    }
    public String visit(CPP.Absyn.Type_int p, Void arg)
    {
        return FunType.ASM_INT;
    }
    public String visit(CPP.Absyn.Type_void p, Void arg)
    {
        return FunType.ASM_VOID;
    }

    // exclude double for now
    public String visit(CPP.Absyn.Type_double p, Void arg)
    {
        return "D";
    }
}
