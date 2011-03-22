package minijava.codegen.x86;

import static minijava.codegen.patterns.IRPat.*;
import static minijava.util.List.list;
import minijava.codegen.assem.A_LABEL;
import minijava.codegen.assem.A_MOVE;
import minijava.codegen.assem.A_OPER;
import minijava.codegen.assem.Instr;
import minijava.codegen.muncher.MunchRule;
import minijava.codegen.muncher.Muncher;
import minijava.codegen.muncher.MuncherRules;
import minijava.codegen.patterns.Matched;
import minijava.codegen.patterns.Pat;
import minijava.ir.frame.Frame;
import minijava.ir.temp.Label;
import minijava.ir.temp.Temp;
import minijava.ir.tree.IRExp;
import minijava.ir.tree.IRStm;
import minijava.util.List;

public class X86Muncher extends Muncher
{
  /**
   * Empty list of Temps, use this  constant if you experience problems with just
   * using List.empty() (the Java type ckecker doesn't seem to like it
   * because it sometimes can't infer the type from its usage context.)
   */
  private static final List<Temp> noTemps = List.empty();
  
  private static MuncherRules<IRStm, Void> sm = new MuncherRules<IRStm, Void>();
  private static MuncherRules<IRExp, Temp> em = new MuncherRules<IRExp, Temp>();
  
  public X86Muncher(Frame frame) {
    super(frame, sm, em);
  }
  
  //////////// The munching rules ///////////////////////////////
  
  static { //Done only once, at class loading time.
    
    // Pattern "variables" (used by the rules below)
    
    final Pat<IRExp>         _e_ = Pat.any();
    final Pat<IRExp>         _f_ = Pat.any();
    final Pat<Temp>          _t_ = Pat.any();
    final Pat<Integer>       _i_ = Pat.any();
    
    final Pat<Label>        _lab_ = Pat.any();
    final Pat<List<IRExp>>  _es_  = Pat.any();
    
    // An example of a Stm muncher rule:
    sm.add(new MunchRule<IRStm, Void>( MOVE(TEMP(_t_), _e_) ) {
      @Override
      protected Void trigger(Muncher m, Matched c) {
        m.emit(A_MOV( c.get(_t_),
                  m.munch(c.get(_e_)) ));
        return null;
      }
    });
    
    // An example of an Exp muncher rule
    em.add(new MunchRule<IRExp, Temp>(PLUS(_e_, CONST(_i_))) {
      @Override
      protected Temp trigger(Muncher m, Matched c) {
        Temp sum = new Temp();
        m.emit( A_MOV(sum, m.munch(c.get(_e_))) );
        m.emit( A_ADD(sum, c.get(_i_)) );
        return sum;
      }
    });
    
    // LABEL
    sm.add(new MunchRule<IRStm, Void>(LABEL(_lab_))
    {
      @Override
      protected Void trigger(Muncher m, Matched c)
      {
        m.emit(A_LABEL(c.get(_lab_)));
        return null;
      }
    });
    
    // CALL
    em.add(new MunchRule<IRExp, Temp>(CALL(NAME(_lab_), _es_))
    {
      @Override
      protected Temp trigger(Muncher m, Matched c)
      {
        Label name = c.get(_lab_);
        List<IRExp> args = c.get(_es_);
        for(int i = args.size() - 1; i >= 0; --i)
        {
          // Munch argument and move to appropriate location
          // TODO: Use frame.getOutArg(i) instead of new Temp()
          m.emit(A_MOV(new Temp(), m.munch(args.get(i))));
        }
        
        m.emit(A_CALL(name));
        
        // TODO: See if RV can be decoupled from X86Muncher using X86Frame
        return new Temp("eax");
      }
    });
    
    // CONST
    em.add(new MunchRule<IRExp, Temp>(CONST(_i_))
    {
      @Override
      protected Temp trigger(Muncher m, Matched c)
      {
        Temp num = new Temp();
        m.emit(A_CONST(num, c.get(_i_)));
        return num;
      }
    });
    
    // PLUS
    em.add(new MunchRule<IRExp, Temp>(PLUS(_e_, _f_))
    {
      @Override
      protected Temp trigger(Muncher m, Matched c)
      {
        Temp d = m.munch(c.get(_e_));
        m.emit(A_ADD_REG_REG(d, m.munch(c.get(_f_))));
        return d;
      }
    });
    
    // MUL
    em.add(new MunchRule<IRExp, Temp>(MUL(_e_, _f_))
    {
      @Override
      protected Temp trigger(Muncher m, Matched c)
      {
        Temp d = m.munch(c.get(_e_));
        m.emit(A_MUL_REG_REG(d, m.munch(c.get(_f_))));
        return d;
      }
    });
    
    // MEM
    em.add(new MunchRule<IRExp, Temp>(MEM(_e_))
    {
      @Override
      protected Temp trigger(Muncher m, Matched c)
      {
        Temp res = new Temp();
        m.emit(A_MEM(res, m.munch(c.get(_e_))));
        return res;
      }
    });
    
    // MEM MOVE
    sm.add(new MunchRule<IRStm, Void>(MOVE(MEM(_f_), _e_))
    {
      @Override
      protected Void trigger(Muncher m, Matched c)
      {
        m.emit(A_MOV_MEM(m.munch(c.get(_f_)), m.munch(c.get(_e_))));
        return null;
      }
    });
    
    // TEMP
    em.add(new MunchRule<IRExp, Temp>(TEMP(_t_))
    {
      @Override
      protected Temp trigger(Muncher m, Matched c)
      {
        return c.get(_t_);
      }
    });
    
    // EXP
    sm.add(new MunchRule<IRStm, Void>(EXP(_e_))
    {
      @Override
      protected Void trigger(Muncher m, Matched c)
      {
        m.munch(c.get(_e_));
        return null;
      }
    });
  }
  
  ///////// Helper methods to generate X86 assembly instructions //////////////////////////////////////
  
  private static Instr A_ADD(Temp reg, int i) {
    return new A_OPER("addl    $"+i+", `d0", 
        list(reg),
        list(reg));
  }
  
  private static Instr A_ADD_REG_REG(Temp d, Temp s) {
    return new A_OPER("addl    `s0, `d0", 
        list(d),
        list(s));
  }
  
  private static Instr A_MUL_REG_REG(Temp d, Temp s) {
    return new A_OPER("imul    `s0, `d0", 
        list(d),
        list(s));
  }
  
  private static Instr A_MOV(Temp d, Temp s) {
    return new A_MOVE("movl    `s0, `d0", d, s);
  }
  
  private static Instr A_LABEL(Label l)
  {
    return new A_LABEL(l.toString() + ":\n", l);
  }
  
  private static Instr A_CALL(Label l)
  {
    return new A_OPER("call    " + l.toString() + "\n", noTemps, noTemps);
  }
  
  private static Instr A_CONST(Temp reg, int i)
  {
    return new A_OPER("movl    $" + i + ", `d0", list(reg), list(reg));
  }
  
  private static Instr A_MOV_MEM(Temp d, Temp s)
  {
    return new A_MOVE("movl    `s0, (`d0)", d, s);
  }
  
  private static Instr A_MEM(Temp d, Temp s)
  {
    return new A_MOVE("movl    (`s0), `d0", d, s);
  }
  
  /**
   * For debugging. This shows you a representation of the actual rules in your
   * Muncher, as well as some usage statistics (how many times each rule got triggered).
   */
  public static void dumpRules() {
    System.out.println("StmMunchers: "+sm);
    System.out.println("ExpMunchers: "+em);
  }
}
