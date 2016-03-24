package com.importsource.pattern.servicelocator;
public class Service2 implements Service {
   public void execute(){
      System.out.println("Executing Service2");
   }

   public String getName() {
      return "Service2";
   }
}