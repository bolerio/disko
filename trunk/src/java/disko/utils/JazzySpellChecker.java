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
package disko.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.engine.Word;
import com.swabunga.spell.event.SpellChecker;

public class JazzySpellChecker
{
    private static JazzySpellChecker instance;

    public static final String JAZZY_DICTIONARY = "jazzy.dictionary";
    public static final String DEFAULT_DICTIONARY_PATH = "data/dict";
    public static final String DICT_FILE = "english.0";

    protected static SpellDictionaryHashMap dictionary = null;
    protected static SpellChecker spellChecker = null;

    private JazzySpellChecker()
    {
        try
        {
            dictionary = new SpellDictionaryHashMap(
                    getDictionaryReader(JAZZY_DICTIONARY,
                                        DICT_FILE,
                                        DEFAULT_DICTIONARY_PATH));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        spellChecker = new SpellChecker(dictionary);
    }

    public static synchronized JazzySpellChecker getInstance()
    {
        if (instance == null)
        {
            instance = new JazzySpellChecker();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public List<Word> getSuggestions(String word, int threshold)
    {
        return spellChecker.getSuggestions(word, threshold);
    }

    /**
     * Determine the file that will be used.
     * 
     * First try to load the the file in the directory defined by the system
     * property. Then try to load the file as a resource in the jar file.
     * Finally, tries the default location (equivalent to -Dproperty=default)
     * 
     * @param propertyName
     *            TODO
     * 
     * @return
     * @throws FileNotFoundException
     */
    private static Reader getDictionaryReader(String propertyName, String file,
                                              String defaultDir)
            throws FileNotFoundException
    {
        InputStream in = null;
        String property = System.getProperty(propertyName);

        if (property != null)
        {
            in = new FileInputStream(property);
            if (in != null)
            {
                System.err.println("Info: Using file defined in "
                        + propertyName + ":" + property);
                return new InputStreamReader(in);
            }
        }

        in = JazzySpellChecker.class.getResourceAsStream("/" + file);
        if (in != null)
        {
            System.err.println("Info: Using " + file
                    + " from resource (jar file).");
            return new InputStreamReader(in);
        }

        String defaultFile = defaultDir + "/" + file;
        in = new FileInputStream(defaultFile);
        if (in != null)
        {
            System.err.println("Info: Using default " + defaultFile);
            return new InputStreamReader(in);
        }
        throw new RuntimeException("Error loading " + file + " file.");
    }

    public static void main(String[] args)
    {
        List<Word> suggestions = JazzySpellChecker.getInstance()
                .getSuggestions("frinds", 10);
        System.out.println(suggestions);
    }
}
