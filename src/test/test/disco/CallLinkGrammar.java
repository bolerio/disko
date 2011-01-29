/*******************************************************************************
 * Copyright (c) 2005, Kobrix Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Borislav Iordanov - initial API and implementation
 *     Murilo Saraiva de Queiroz - initial API and implementation
 ******************************************************************************/
package test.disco;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.linkgrammar.LGConfig;
import relex.Sentence;
import relex.parser.RemoteLGParser;

public class CallLinkGrammar
{
    public static void main(String [] argv)
    {
        final String [] phrases = new String [] {
          "Mr. Axelrod also batted down the suggestion by Jonathan Turley.",
          "Aside from commending the president�s pick of Judge Sonia Sotomayor, most liberal groups kept their comments to a minimum so that she could hold the spotlight.",
          "In an interview on Tuesday with CNN Radio, Mr. Steele said Republicans had serious concerns with Judge Sotomayor�s views on abortion, gun control and other issues.",
          "Conflicting views tend to create tension.",
          "I was suprised by the sense of guilt displayed by a child at such a young age.",
          "Probably doesn't necessarily mean certainly, if you get my meaning.",
          "This is absolutely ridiculous.",
          "The child abuse frequency is at an alarming rise.",
          "Why would anyone want to do that?",
          "Now, we will proceed to explain the complexity of language parsing.",
          "Perhaps I will fail just this one time, if you would only allow my to.",
          "Get out, now!",
          "Misconceptions and other acts of stupidity dominate the intellectual landscape of middle-aged morons.",
          "Love will last forever, but I'm not going to be there to see it."         
        };        
        LGConfig config = new LGConfig();
        config.setDictionaryLocation("d:/work/disco/trunk/data/linkparser");
        final RemoteLGParser parser = new RemoteLGParser();
        parser.getLinkGrammarClient().setHostname("localhost");
        parser.getLinkGrammarClient().setPort(9000);
        parser.setConfig(config);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        Random rand = new Random();
        while (true)
        {
            final int idx = rand.nextInt(phrases.length);
            pool.submit(new Runnable() { public void run() { 
                Sentence s = parser.parse(phrases[idx]); 
                System.out.println("Parsed " + s.getParses().size() + " parses.");                
            } });
            try { Thread.sleep(10); }
            catch (Throwable t) { }
        }
    }
}
