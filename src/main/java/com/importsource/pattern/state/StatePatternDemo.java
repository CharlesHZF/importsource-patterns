package com.importsource.pattern.state;

/**
 * <p>
 * StatePatternDemo, our demo class, will use Context and state objects to
 * demonstrate change in Context behavior based on type of state it is in.
 *<p>
 * In State pattern a class behavior changes based on its state. This type of
 * design pattern comes under behavior pattern.
 * <p>
 * In State pattern, we create objects which represent various states and a
 * context object whose behavior varies as its state object changes.
 * <p>
 * <p>
 * @author Hezf 
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