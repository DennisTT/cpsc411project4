package minijava.codegen.x86;

import minijava.codegen.muncher.Muncher;
import minijava.codegen.muncher.MuncherRules;
import minijava.ir.frame.Frame;
import minijava.ir.temp.Temp;
import minijava.ir.tree.IRExp;
import minijava.ir.tree.IRStm;

public class X86Muncher extends Muncher
{
  protected X86Muncher( Frame frame,
                        MuncherRules<IRStm, Void> stmMunchers,
                        MuncherRules<IRExp, Temp> expMunchers)
  {
    super(frame, stmMunchers, expMunchers);
  }
}
