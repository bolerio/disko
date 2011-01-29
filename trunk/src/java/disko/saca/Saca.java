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
package disko.saca;

import java.io.InputStreamReader;

import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.peer.serializer.JSONReader;
import org.hypergraphdb.peer.serializer.JSONWriter;
import org.hypergraphdb.storage.BAUtils;

/**
 * 
 * <p>
 * Static utility methods related to searching.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class Saca
{
	/**
	 * <p>
	 * 
	 * </p>
	 * @param graph
	 * @param resourceHandle
	 * @return
	 */
	public static BoostLink getUnconstraintedBoost(HyperGraph graph, HGHandle resourceHandle)
	{
		BoostLink boostLink = hg.getOne(graph, 
				hg.and(hg.type(BoostLink.class), hg.orderedLink(resourceHandle, 
						graph.getHandleFactory().nullHandle())));
		if (boostLink == null)
			boostLink = new BoostLink(resourceHandle, graph.getHandleFactory().nullHandle());
		return boostLink;
	}

	public static List<FeaturedLink> getAllFeaturedLinks(HyperGraph graph, HGHandle resourceHandle)
	{
		return hg.getAll(graph,
				hg.and(hg.type(FeaturedLink.class), hg.orderedLink(resourceHandle, hg.anyHandle())));				
	}
	
	public static List<BoostLink> getAllBoosts(HyperGraph graph, HGHandle resourceHandle)
	{
		return hg.getAll(graph,
				hg.and(hg.type(BoostLink.class), hg.orderedLink(resourceHandle, hg.anyHandle())));				
	}

    @SuppressWarnings("unchecked")
    public static <T> T readMsg(Socket socket) throws java.io.IOException
    {
        InputStreamReader in = new InputStreamReader(socket.getInputStream());        
        StringBuffer data = new StringBuffer();
        char[] buf = new char[1024];
        byte [] lengthBuf = new byte[4];
        socket.getInputStream().read(lengthBuf);
        int expectedSize = BAUtils.readInt(lengthBuf, 0);
        int count = 0;
        for (count = in.read(buf, 0, buf.length); count > -1; 
             count = in.read(buf, 0, buf.length))
        {
             data.append(buf, 0, count);
             if (data.length() >= expectedSize)
                 break;
        }
        JSONReader json = new JSONReader();
        T result = (T)json.read(data.toString());
        // The number of the beast signals we've successfully read a message.
        OutputStream out = socket.getOutputStream();
        out.write(666);
        out.flush();
        return result;
    }

    public static void writeMsg(Socket socket, Object msg) throws java.io.IOException
    {
        JSONWriter json = new JSONWriter();
        String asString = json.write(msg);
        OutputStream out = socket.getOutputStream();
        byte [] length = new byte[4];
        BAUtils.writeInt(asString.length(), length, 0);
        out.write(length);
        out.write(asString.getBytes());
        out.flush();
//        out.close();
        socket.getInputStream().read(); // read 666 ack
    }	
}
