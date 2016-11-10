package com.appdynamics.extensions.kafka.filters;


import java.util.List;
import java.util.Set;

public class ExcludeFilter {

    private List dictionary;

    public ExcludeFilter(List excludeDictionary) {
        this.dictionary = excludeDictionary;
    }

    public void apply(Set<String> filteredSet, List<String> allMetrics){
        if(allMetrics == null || dictionary == null){
            return;
        }
        for(String metric : allMetrics){
            if(!dictionary.contains(metric)){
                filteredSet.add(metric);
            }
        }
    }
}
