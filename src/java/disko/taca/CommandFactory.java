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
package disko.taca;

import java.util.HashMap;

public class CommandFactory
{
	private static CommandFactory instance = new CommandFactory();
	private static HashMap<String, Class<? extends Command>> commands = new HashMap<String, Class<? extends Command>>();
	
	static
	{
		commands.put("submitDocument", SubmitDocument.class);
	}

	private CommandFactory() { }
	
	public static CommandFactory getInstance() { return instance; }
	
	public Command makeCommand(String name)
	{
		Class<? extends Command> cclass = commands.get(name);
		if (cclass == null)
			return null;
		else
			try { return cclass.newInstance(); }
			catch (Exception ex) { throw new RuntimeException(ex); }
	}
}
