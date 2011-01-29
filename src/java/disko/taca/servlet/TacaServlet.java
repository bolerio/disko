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
package disko.taca.servlet;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import disko.taca.Command;
import disko.taca.CommandFactory;

public class TacaServlet extends HttpServlet
{
	private static final long serialVersionUID = -1;
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String commandName = request.getParameter("cmd");
		if (commandName == null)
			throw new ServletException();
		HashMap<String, String> commandAttributes = new HashMap<String, String>();
		for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); )
		{
			String name = e.nextElement().toString();
			if ("cmd".equals(name)) continue;
			else
				commandAttributes.put(name, request.getParameter(name));
		}
		Command command = CommandFactory.getInstance().makeCommand(commandName);
		if (command == null)
			throw new ServletException("Unknown command '" + commandName + "'");
		else
		{
			Object result = command.execute(commandAttributes);
			// perhaps use some streaming code that handles different result types?
			response.getWriter().println(result.toString());
		}
	}
}
