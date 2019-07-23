package jiangxin.app;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Local;
import soot.Value;

/**
 * 
 * @author xinjiang
 * 模拟一个函数上下文
 * 对于一个函数内部，有其相应的Local（变量），不同的函数内部，Local可以具有相同的名字
 * 对于该类需要解决：
 * 1. 在某个函数体A内，调用了函数B，如何将A中变量与B中变量相联系?
 * 	- 由于每个local都对应了一个Variable，那么我们可以看做添加对应的
 * 	- 赋值操作，A.r1 = B.p1，由于在指针分析中，赋值操作，是在r1的sourceId中
 * 	- 添加相应的p1的sourceId，因此，我们可以在r1对应的variables中，添加一个p1对应的variable
 * 	- ！！！！因此，我们需要保留调用该函数的所在的函数域“! ! ! !，便于将该函数的参数与调用域中的参数对应
 * 2. 为了便于调试，明朗化，需要知道当前调用的深度
 * 3. 返回值，怎么处理？（暂时未处理！）
 * 4. 如何判断是否进入了递归函数？是否要处理递归函数？如果按照我们的添加variable的想法，
 * 	处理递归函数会导致内存爆炸。因此，暂时不处理递归函数，如果要处理，需要修改一些机制。
 * 5. 还要处理分支函数，即If !!!!，第一次忽略了这个，一直跑不了
 */
public class Contex {
	private Analyzer analyzer;
	private int depth;
	private Contex invokeContex;	// 调用该函数域的上级函数
	private String methodSignature;	// 该函数的签名，用于判断是否是递归函数
	private List<Value> args;	// 函数参数
	private Contex preContex;	// 分支前一个
	private String branchSignature;
	private Variable thisVar;
	private Map<Local, Variable> localMap;
	
	private Contex(Analyzer analyzer,
			int depth,
			Contex invokeContex,
			String methodSignature,
			List<Value> args,
			Contex preContex,
			String branchSignature,
			Variable thisVar) {
		this.analyzer = analyzer;
		this.depth = depth;
		this.invokeContex = invokeContex;
		this.methodSignature = methodSignature;
		this.args = args;
		this.preContex = preContex;
		this.branchSignature = branchSignature;
		this.thisVar = thisVar;
		localMap = new HashMap<>();
	}
	
	public static Contex getInstance(Analyzer analyzer) {
		return new Contex(analyzer, 0, null, null, Collections.emptyList(), null, null, null);
	}
	
	/**
	 * 创建一个子函数域
	 */
	public Contex createInvokeContex(String methodSignature, List<Value> args, Variable thisVar) {
		return new Contex(analyzer, depth + 1, this, methodSignature, args, null, null, thisVar);
	}
	
	/**
	 * 创建一个分支域，注意分支域与之前一个域，参数相同，因此需要复制下Variable
	 * @param branchSignature
	 * @return
	 */
	public Contex createBranchScope(String branchSignature) {
		Contex contex = new Contex(analyzer, depth, invokeContex, methodSignature, args, this, branchSignature, thisVar);
		for (Map.Entry<Local, Variable> entry : this.localMap.entrySet()) {
			Variable copy = entry.getValue().copyDeep(true);
			contex.bindLocalAndVariable(copy.getLocal(), copy);
		}
		return contex;
	}
	
	public Analyzer getAnalyzer() {
		return analyzer;
	}
	
	private void bindLocalAndVariable(Local local, Variable var) {
		localMap.put(local, var);
		analyzer.addLocal(local, var);
	}
	
	public Variable getVariable(Local local) {
		return localMap.get(local);
	}
	
	public void bindArg(Local local, int paramIndex) {
		if (paramIndex >= 0 && paramIndex < args.size()) {
			Value argValue = args.get(paramIndex);
			if (argValue instanceof Local) {
				Variable argVar = invokeContex.getVariable((Local)argValue);
				Variable var;
				if (argVar != null) {
					var = argVar.copyDeep(false);
				} else {
					var = Variable.getInstance(argValue);
				}
				bindLocalAndVariable(local, var);
			}
		}
	}
	
	public Variable getOrAdd(Local local) {
		Variable var = getVariable(local);
		if (var == null) {
			var = Variable.getInstance(local);
			bindLocalAndVariable(local, var);
		}
		return var;
	}
	
	public void bindThis(Local local) {
		bindLocalAndVariable(local, thisVar.copyDeep(false));
	}
	
	public Variable createVariable(Value val) {
		return Variable.getInstance(val);
	}
	
	public boolean isInRecursion(String invokeSignature) {
		if (invokeSignature.equals(methodSignature)) {
			return true;
		}
		if (invokeContex != null) {
			return invokeContex.isInRecursion(invokeSignature);
		}
		return false;
	}
	
	public boolean isInBranchChain(String branchSignature) {
		if (branchSignature.equals(this.branchSignature)) {
			return true;
		}
		if (preContex != null) {
			return preContex.isInBranchChain(branchSignature);
		}
		return false;
	}
	
	public int getDepth() {
		return depth;
	}
	
	@Override
	public String toString() {
		return "Contex{ "
				+ this.localMap
				+ "}";
	}
}
