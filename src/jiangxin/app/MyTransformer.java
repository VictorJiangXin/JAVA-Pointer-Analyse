package jiangxin.app;

import java.util.List;
import java.util.Map;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AnyNewExpr;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ParameterRef;
import soot.jimple.ThisRef;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;


public class MyTransformer extends SceneTransformer {

	@Override
	protected void internalTransform(String arg0, Map<String, String> arg1) {
		// TODO Auto-generated method stub
		try {
			Analyzer analyzer = Analyzer.getAnalyzer();
			SootMethod mainMethod = Scene.v().getMainMethod();
			solveMethod(mainMethod, Contex.getInstance(analyzer));
			Printer.printResult("result.txt", analyzer.run());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	private void solveMethod(SootMethod method, Contex contex) {
		Body body = method.getActiveBody();
		UnitGraph graph = new BriefUnitGraph(body);
		Unit head = graph.getHeads().iterator().next();
		solveBlock(head, contex, graph);
	}
	
	private void solveBlock(Unit u, Contex contex, UnitGraph graph) {
		while (true) {
			solveUnit(u, contex);
			List<Unit> succs = graph.getSuccsOf(u);
			int succCount = succs.size();
			if (succCount == 1) {
				u = succs.iterator().next();
			} else if (succCount > 1) {
                for (Unit succ : succs) {
                    String branchSignature = succ.toString();
                    if (contex.isInBranchChain(branchSignature)) {
                        // 静态分析不处理循环
                        continue;
                    }
                    solveBlock(succ, contex.createBranchScope(branchSignature), graph);
                }
                return;
			} else {
				return;
			}
		}
	}
	
	private void solveUnit(Unit u, Contex contex) {
		try {
			Analyzer analyzer = contex.getAnalyzer();
			if (u instanceof IdentityStmt) {
				IdentityStmt is = (IdentityStmt)u;
				Value lop = is.getLeftOp();
				Value rop = is.getRightOp();
				if (rop instanceof ParameterRef) {
					ParameterRef pr = (ParameterRef)rop;
					contex.bindArg((Local)lop, pr.getIndex());
				} else if (rop instanceof ThisRef) {
                    contex.bindThis((Local) lop);
                } else {
					
				}
			} else if (u instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) u;
				Value lop = as.getLeftOp();
				Value rop = as.getRightOp();
				
				Variable rvar = null;
				if (rop instanceof AnyNewExpr) {
					rvar = contex.createVariable(rop);
					rvar.addAllocId(analyzer.getAllocId());
					analyzer.setAllocId(0);
				} else if (rop instanceof Local) {
					rvar = contex.getOrAdd((Local) rop);
				} else if (rop instanceof FieldRef) {
					FieldRef rfref = (FieldRef) rop;
					SootFieldRef rfield = rfref.getFieldRef();
					if (rop instanceof InstanceFieldRef) {
						InstanceFieldRef rifref = (InstanceFieldRef) rop;
						Local rbase = (Local) rifref.getBase();
						Variable rbaseVar = contex.getOrAdd(rbase);
						rvar = rbaseVar.getMember().getVariable(rfield);
					} else {
						
					}
				} else {
					return;
				}
				if (rvar != null) {
					if (lop instanceof Local) {
						Variable lvar = contex.getOrAdd((Local) lop);
						lvar.assign(rvar);
					} else if (lop instanceof FieldRef) {
						FieldRef lfref = (FieldRef) lop;
						SootFieldRef lfield = lfref.getFieldRef();
						if (lop instanceof InstanceFieldRef) {
							InstanceFieldRef lifref = (InstanceFieldRef) lop;
							Local lbase = (Local) lifref.getBase();
							Variable lbaseVar = contex.getOrAdd(lbase);
							lbaseVar.getMember().addField(lfield, rvar);
						} else {
							
						}
					} else {
						
					}
				}
			} else if (u instanceof InvokeStmt) {
				InvokeStmt is = (InvokeStmt) u;
				InvokeExpr ie = is.getInvokeExpr();
				if(ie != null) {
					SootMethod invokeMethod = ie.getMethod();
					String methodSignature = invokeMethod.getSignature();
					List<Value> invokeArgs = ie.getArgs();
					if (ie instanceof InstanceInvokeExpr) {
						switch (methodSignature) {
							case "<java.lang.Object: void <init>()>": return;
						}
						if (contex.isInRecursion(methodSignature)) return;
						InstanceInvokeExpr sie = (InstanceInvokeExpr) ie;
						Local base = (Local) sie.getBase();
						Variable baseVar = contex.getOrAdd(base);
						Contex invokeContex = contex.createInvokeContex(methodSignature, invokeArgs, baseVar);
						solveMethod(invokeMethod, invokeContex);
					} else {
						switch (methodSignature) {
                        	case "<benchmark.internal.Benchmark: void alloc(int)>": {
                        		int allocId = ((IntConstant) invokeArgs.get(0)).value;
                        		analyzer.setAllocId(allocId);
                        		break;
                        	}
                        	case "<benchmark.internal.Benchmark: void test(int,java.lang.Object)>": {
                        		int id = ((IntConstant) invokeArgs.get(0)).value;
                        		Local local = (Local) invokeArgs.get(1);
                        		analyzer.addQuery(id, local);
                        		break;
                        	}
						}
					}
				}
			} else {
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
