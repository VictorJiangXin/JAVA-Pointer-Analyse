package test;

import benchmark.internal.Benchmark;
import benchmark.objects.A;
import benchmark.objects.B;

/*
 * @testcase FieldSensitivity2
 * 
 * @version 1.0
 * 
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 * 
 * @description Field Sensitivity without static method
 */
public class FieldSensitivity {

  public FieldSensitivity() {}

  private void assign(A x, A y) {
    y.f = x.f;
  }

  private void test() {	  
    Benchmark.alloc(1);
    B b = new B();
    Benchmark.alloc(2);
    A a = new A(b);
    Benchmark.alloc(3);
    A c = new A();
    Benchmark.alloc(4);
    B e = new B();
    assign(a, c);
    B d = c.f;

    Benchmark.test(1, d); // expected: 1
  }

  public static void main(String[] args) {

    FieldSensitivity fs2 = new FieldSensitivity();
    fs2.test();
  }

}
