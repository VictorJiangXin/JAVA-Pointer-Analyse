package jiangxin.app;

import java.io.File;

import soot.PackManager;
import soot.Transform;

/**
 * 程序入口
 * @author xinjiang
 *
 */
public class Main {
	
	// args[0] = "/you/code/path"
	// args[1] = "<the main class you analyse>"
	public static void main(String[] args) {
		String classpath = args[0] 
				+ File.pathSeparator + args[0] + File.separator + "rt.jar"
				+ File.pathSeparator + args[0] + File.separator + "jce.jar";
		System.out.println(classpath);
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.mypta", new MyTransformer()));
		soot.Main.main(new String[] {
			"-w",
			"-p", "cg.spark", "enabled:true",
			"-p", "wjtp.mypta", "enabled:true",
			"-soot-class-path", classpath,
			args[1]		
		});
	}
}
