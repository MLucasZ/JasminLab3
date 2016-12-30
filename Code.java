// Code.java
// Created by github.com/andreasabel/java-adt

import CPP.Absyn.Type;
import CPP.Absyn.Type_bool;
import CPP.Absyn.Type_int;


abstract class Code {
    public abstract <R> R accept (CodeVisitor<R> v);
}

class Store extends Code {
    public Type type;
    public Integer addr;
    public Store (Type type, Integer addr) {
        this.type = type;
        this.addr = addr;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Load extends Code {
    public Type type;
    public Integer addr;
    public Load (Type type, Integer addr) {
        this.type = type;
        this.addr = addr;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IConst extends Code {
    public Integer immed;
    public IConst (Integer immed) {
        this.immed = immed;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class DConst extends Code {
    public Double immed;
    public DConst (Double immed) {
        this.immed = immed;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Dup extends Code {
    public Type type;
    public Dup (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Pop extends Code {
    public Type type;
    public Pop (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Return extends Code {
    public Type type;
    public Return (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Call extends Code {
    public Fun fun;
    public String id;
    public String typeReturn;
    public String typeArgs;
    public String className;
    public Call (String className, Fun fun) {
        this.fun = fun;
        this.className = className;
    }
    public Call (String id, String typeReturn, String typeArgs) {
        this.id = id;
        this.typeReturn = typeReturn;
        this.typeArgs = typeArgs;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Label extends Code {
    public Integer labelNumber;
    public Label (Integer label) {
        this.labelNumber = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Goto extends Code {
    public Label label;
    public Goto (Label label) {
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfZ extends Code {
    public Label label;
    public IfZ (Label label) {
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfNZ extends Code {
    public Label label;
    public IfNZ (Label label) {
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfEq extends Code {
    public Type type;
    public Label label;
    public IfEq(Type type, Label label) {
        this.type = type;
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfNe extends Code {
    public Type type;
    public Label label;
    public IfNe (Type type, Label label) {
        this.type = type;
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfLt extends Code {
    public Type type;
    public Label label;
    public IfLt (Type type, Label label) {
        this.type = type;
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfGt extends Code {
    public Type type;
    public Label label;
    public IfGt (Type type, Label label) {
        this.type = type;
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfLe extends Code {
    public Type type;
    public Label label;
    public IfLe (Type type, Label label) {
        this.type = type;
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class IfGe extends Code {
    public Type type;
    public Label label;
    public IfGe (Type type, Label label) {
        this.type = type;
        this.label = label;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class DGt extends Code {
    public DGt () {
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class DLt extends Code {
    public DLt () {
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Inc extends Code {
    public Type type;
    public Integer addr;
    public Integer delta;
    public Inc (Type type, Integer addr, Integer delta) {
        this.type = type;
        this.addr = addr;
        this.delta = delta;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Add extends Code {
    public Type type;
    public Add (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Sub extends Code {
    public Type type;
    public Sub (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Mul extends Code {
    public Type type;
    public Mul (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

class Div extends Code {
    public Type type;
    public Div (Type type) {
        this.type = type;
    }
    public <R> R accept (CodeVisitor<R> v) {
        return v.visit (this);
    }
}

interface CodeVisitor<R> {
    public R visit (Store c);
    public R visit (Load c);
    public R visit (IConst c);
    public R visit (DConst c);
    public R visit (Dup c);
    public R visit (Pop c);
    public R visit (Return c);
    public R visit (Call c);
    public R visit (Label c);
    public R visit (Goto c);
    public R visit (IfZ c);
    public R visit (IfNZ c);
    public R visit (IfEq c);
    public R visit (IfNe c);
    public R visit (IfLt c);
    public R visit (IfGt c);
    public R visit (IfLe c);
    public R visit (IfGe c);
    public R visit (DGt c);
    public R visit (DLt c);
    public R visit (Inc c);
    public R visit (Add c);
    public R visit (Sub c);
    public R visit (Mul c);
    public R visit (Div c);
}

class CodeToJVM implements CodeVisitor<String> {

    public String visit (Store c) {
        // TODO need to choose the right store command
        // with the corresponding type
        return "istore_" + c.addr + "\n";
    }

    public String visit (Load c) {
        if(c.type instanceof Type_int) {
            return "iload_" + c.addr + "\n";
        } else {
            return "";
        }
    }

    public String visit (IConst c) {
        int i = c.immed.intValue();
        if (i == -1) return "iconst_m1\n";
        if (i >= 0 && i <= 5) return "iconst_" + i + "\n";
        if (i >= -128 && i < 128) return "bipush " + i + "\n";
        return "ldc " + c.immed.toString() + "\n";
    }

    public String visit (DConst c) {
        return "";
    }

    public String visit (Dup c) {
        return "";
    }

    public String visit (Pop c) {
        // exclude double for now
        // if (c.type instanceof Type_double) return "pop2\n";
        return "pop\n";
    }

    public String visit (Return c) {
        Type type = c.type;
        // because java boolean uses jvm int
        if(type instanceof Type_int) {
            return "ireturn\n";
        } else if(type instanceof Type_bool) {
            return "ireturn\n";
        }
        return "return\n";
    }

    public String visit (Call c) {
        if(c.fun != null) {
            Fun fun = c.fun;
            FunType funType = fun.funType;

            // custom methods
            return "invokestatic " + c.className + "/" + fun.id + funType.toJVM() + "\n";
        } else {
            // built-in functions
            return "invokestatic " + mthInvokeSpec(c.id, c.typeArgs, c.typeReturn);
        }
    }

    private String mthInvokeSpec(String mthId, String asmParamType, String asmReturnType) {
        return "Runtime" + "/" + mthId + "(" + asmParamType + ")" + asmReturnType + "\n";
    }

    public String visit (Label c) {
        return "L" + c.labelNumber + ":\n";
    }

    public String visit (Goto c) {
        String jump = "L" + c.label.labelNumber;
        // goto <label_name>
        return "goto " + jump + "\n";
    }

    public String visit (IfZ c) {
        return "";
    }

    public String visit (IfNZ c) {
        return "";
    }

    public String visit (IfEq c) {
        String jump = "L" + c.label.labelNumber;
        // TODO because ifeq says branch only when 0
        if (c.type instanceof Type_bool) return "ifne " + jump + "\n";
        if (c.type instanceof Type_int) return "if_icmpeq " + jump + "\n";
        return "";
    }

    public String visit (IfNe c) {
        String jump = "L" + c.label.labelNumber;
        // TODO because ifne says branch only when not 0
        if (c.type instanceof Type_bool) return "ifeq " + jump + "\n";
        if (c.type instanceof Type_int) return "if_icmpne " + jump + "\n";
        return "";
    }

    public String visit (IfLt c) {
        String jump = "L" + c.label.labelNumber;
        // if_icmplt <label to jump>
        return "if_icmplt " + jump + "\n";
    }

    public String visit (IfGt c) {
        String jump = "L" + c.label.labelNumber;
        return "if_icmpgt " + jump + "\n";
    }

    public String visit (IfLe c) {
        String jump = "L" + c.label.labelNumber;
        return "if_icmple " + jump + "\n";
    }

    public String visit (IfGe c) {
        String jump = "L" + c.label.labelNumber;
        return "if_icmpge " + jump + "\n";
    }

    public String visit (DGt c) {
        return "";
    }

    public String visit (DLt c) {
        return "";
    }

    public String visit (Inc c) {
        return "iinc "+c.addr+" "+c.delta+"\n";
    }

    public String visit (Add c) {
        if(c.type instanceof Type_int) {
            return "iadd" + "\n";
        } else {
            return "";
        }
    }

    public String visit (Sub c) {
        if(c.type instanceof Type_int) {
            return "isub" + "\n";
        } else {
            return "";
        }
    }

    public String visit (Mul c) {
        if(c.type instanceof Type_int) {
            return "imul" + "\n";
        } else {
            return "";
        }
    }

    public String visit (Div c) {
        if(c.type instanceof Type_int) {
            return "idiv" + "\n";
        } else {
            return "";
        }
    }

}