package com.importsource.pattern.filter;
import java.util.ArrayList;
import java.util.List;

public class FemaleCriteria implements Criteria {

   public List<Person> meetCriteria(List<Person> persons) {
      List<Person> femalePersons = new ArrayList<Person>(); 
      
      for (Person person : persons) {
         if(person.getGender().equalsIgnoreCase("FEMALE")){
            femalePersons.add(person);
         }
      }
      return femalePersons;
   }
}