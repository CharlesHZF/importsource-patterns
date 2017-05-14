package com.importsource.pattern.filter;
import java.util.ArrayList;
import java.util.List;

public class MaleCriteria implements Criteria {

   public List<Person> meetCriteria(List<Person> persons) {
      List<Person> malePersons = new ArrayList<Person>(); 
      
      for (Person person : persons) {
         if(person.getGender().equalsIgnoreCase("MALE")){
            malePersons.add(person);
         }
      }
      return malePersons;
   }
}