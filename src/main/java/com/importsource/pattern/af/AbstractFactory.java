package com.importsource.pattern.af;

/**
 * Abstract Factory patterns work around a super-factory which creates other
 * factories. This factory is also called as factory of factories. This type of
 * design pattern comes under creational pattern as this pattern provides one of
 * the best ways to create an object.
 * <p>
 * In Abstract Factory pattern an interface is responsible for creating a
 * factory of related objects without explicitly specifying their classes. Each
 * generated factory can give the objects as per the Factory pattern.
 * 
 * @author Hezf
 *
 */
public abstract class AbstractFactory {
	abstract Color getColor(String color);

	abstract Shape getShape(String shape);
}