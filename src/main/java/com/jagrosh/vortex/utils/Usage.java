/*
 * Copyright 2020 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. Furthermore, I'm putting this sentence in all files because I messed up git and its not showing files as edited -\\_( :) )_/-
 */
package com.jagrosh.vortex.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Usage {
    private final HashMap<Long, Integer> map = new HashMap<>();

    public void increment(long key) {
        map.put(key, map.getOrDefault(key, 0) + 1);
    }

    public Map<Long, Integer> getMap() {
        return map;
    }

    public List<Entry<Long, Integer>> higher(int val) {
        return map.entrySet().stream().filter(e -> e.getValue() >= val).collect(Collectors.toList());
    }
}
