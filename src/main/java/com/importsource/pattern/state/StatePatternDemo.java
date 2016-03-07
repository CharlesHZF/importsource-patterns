package com.importsource.pattern.state;
/**
 * 
 * @author Hezf
 *http://www.tutorialspoint.com/design_pattern/state_pattern.htm
 */
public class StatePatternDemo {
   public static void main(String[] args) {
	   
      Context context = new Context();

      StartState startState = new StartState();
      startState.doAction(context);

      System.out.println(context.getState().toString());

      StopState stopState = new StopState();
      stopState.doAction(context);

      System.out.println(context.getState().toString());
   }
}