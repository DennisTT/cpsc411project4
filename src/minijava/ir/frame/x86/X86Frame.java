package minijava.ir.frame.x86;

import junit.framework.Assert;
import minijava.codegen.assem.Instr;
import minijava.codegen.muncher.Muncher;
import minijava.codegen.x86.X86Muncher;
import minijava.ir.frame.Access;
import minijava.ir.frame.Frame;
import minijava.ir.interp.Interp;
import minijava.ir.interp.Word;
import minijava.ir.interp.X86SimFrame;
import minijava.ir.temp.Label;
import minijava.ir.temp.Temp;
import minijava.ir.tree.IR;
import minijava.ir.tree.IRExp;
import minijava.ir.tree.IRStm;
import minijava.util.IndentingWriter;
import minijava.util.List;

public class X86Frame extends Frame {
  public static final int FIRST_FORMAL_OFFSET = 8;
  public static final int FORMAL_INCREMENT = 4;
  
  public static X86Frame factory = new X86Frame(null, null);
  
  private static Temp fp = new Temp("ebp"),
                      rv = new Temp("eax");
  
  private int localCount = 0;
  
  private X86Frame(Label label, List<Access> formals)
  {
    super(label, formals);
  }
  
  public int numLocals()
  {
    return this.localCount;
  }
  
  @Override
  public void dump(IndentingWriter out)
  {
    out.println("X86Frame {");
    out.indent();
    
    out.print("label : ");
    out.println(this.getLabel());
    
    out.println("formals : ");
    out.indent();
    for(Access formal : this.getFormals())
    {
      out.println(formal);
    }
    out.outdent();
    
    out.outdent();
    out.print("}");
  }
  
  @Override
  public Frame newFrame(Label name, List<Boolean> formalsEscape)
  {
    int offset = FIRST_FORMAL_OFFSET;
    
    List<Access> newFormals = List.empty();
    for(Boolean b : formalsEscape)
    {
      if(b)
      {
        newFormals.add(new X86InFrame(offset));
        offset += this.wordSize();
      }
      else
      {
        newFormals.add(new X86InReg(new Temp()));
      }
    }
    
    return new X86Frame(name, newFormals);
  }
  
  @Override
  public Access allocLocal(boolean escapes)
  {
    return (escapes) ?  new X86InFrame(-(this.wordSize() * this.localCount++)) :
                        new X86InReg(new Temp());
  }
  
  @Override
  public IRExp FP()
  {
    return IR.TEMP(fp);
  }
  
  @Override
  public IRExp RV()
  {
    return IR.TEMP(rv);
  }
  
  @Override
  public int wordSize()
  {
    // x86 supports 32-bit addressing (4 bytes)
    return FORMAL_INCREMENT;
  }
  
  @Override
  public IRStm procEntryExit1(IRStm body)
  {
    return IR.SEQ(IR.LABEL(this.getLabel()), body);
  }
  
  @Override
  public X86SimFrame newSimFrame(Interp interp, List<Word> args)
  {
    return new X86SimFrame(interp, factory, args);
  }
  
  public Access alloc(int offset)
  {
    return new X86InFrame(offset);
  }

  @Override
  public Muncher newMuncher()
  {
    return new X86Muncher(this);
  }

  @Override
  public List<Instr> procEntryExit2(List<Instr> asmBody)
  {
    // TODO Fix with correct implementation of procEntryExit2
    Assert.assertNotNull(asmBody);
    return asmBody;
  }

  @Override
  public Access getOutArg(int i)
  {
    // TODO Auto-generated method stub
    Assert.fail("getOutArg not implemented");
    return null;
  }

  @Override
  public List<Temp> registers()
  {
    // TODO Auto-generated method stub
    Assert.fail("registers not implemented");
    return null;
  }

  @Override
  public void entrySequence(IndentingWriter out)
  {
    // TODO Auto-generated method stub
    out.println();
    
  }

  @Override
  public void exitSequence(IndentingWriter out)
  {
    // TODO Auto-generated method stub
    out.println();
  }
}
