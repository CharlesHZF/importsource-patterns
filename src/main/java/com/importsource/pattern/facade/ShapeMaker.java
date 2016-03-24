package com.importsource.pattern.facade;

/**
 * This class is the facade class.
 * <p>
 * We can use this class to call the circle
 * implementing method.
 * 
 * @author Hezf
 *
 */
public class ShapeMaker {
	//circle field
	private Shape circle;
	//rectangle field
	private Shape rectangle;
	//square field
	private Shape square;
    
	/**
	 * contrcutor of the facade
	 */
	public ShapeMaker() {
		circle = new Circle();
		rectangle = new Rectangle();
		square = new Square();
	}

	
	//below is the facade method
	public void drawCircle() {
		circle.draw();
	}
    
	public void drawRectangle() {
		rectangle.draw();
	}

	public void drawSquare() {
		square.draw();
	}
}