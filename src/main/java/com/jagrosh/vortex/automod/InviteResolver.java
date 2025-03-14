/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.automod;

import com.jagrosh.vortex.utils.FixedCache;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Invite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class InviteResolver {
    private final Logger log = LoggerFactory.getLogger(InviteResolver.class);
    private final FixedCache<String, Long> cached = new FixedCache<>(5000);

    public long resolve(String code, JDA jda) {
        log.debug("Attempting to resolve " + code);
        if (cached.contains(code)) {
            return cached.get(code);
        }

        try {
            Invite i = Invite.resolve(jda, code).complete(false);
            cached.put(code, i.getGuild().getIdLong());
            return i.getGuild().getIdLong();
        } catch (Exception ex) {
            cached.put(code, 0L);
            return 0L;
        }
    }
}
