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

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import org.hypergraphdb.peer.Structs;

public class SacaClient
{
    private String host;
    private int port;
    private int requestRetryCount = 1;
    private int connectRetryCount = 5;
    private long connectRetryWait = 1000l;
    
    private Object callSaca(Object request) throws InterruptedException, IOException
    {
        if (host == null || host.length() == 0 || port <= 1024)
            throw new RuntimeException("No hostname for remote saca client or invalid port number < 1024");
        
        //
        // Connect:
        //
        Socket socket = null;
        
        for (int i = 0; i < connectRetryCount && socket == null; i++)
        {
            try
            {
                socket = new Socket(host, port);
            }
            catch (UnknownHostException ex)
            {
                throw new RuntimeException("Host '" + host + "' not found.");
            }
            catch (IOException ex)
            {
                // ignore, retry...
            	ex.printStackTrace();
            }
            if (socket == null)
                Thread.sleep(connectRetryWait);
        }
        
        if (socket == null)
            throw new RuntimeException("Failed to connect to " + host + ":" + port);
        
        //
        // Call parser:
        //
        try
        {
            Saca.writeMsg(socket, request);
            return Saca.readMsg(socket);
        }
        finally
        {
            try { socket.close(); } catch (Throwable t) { }
        }
    }
    
    public SacaClient()
    {        
    }
    
    public SacaClient(String host, int port)
    {
        this.host = host;
        this.port = port;
    }
    
    public Object ping() throws InterruptedException
    {
        for (int i = 0; i < requestRetryCount; i++)
            try 
            {
                return callSaca(Structs.struct(SacaService.ACT, SacaService.PING)); 
            }
            catch (IOException ex) 
            { 
                if (i == 0) // Trace exception only on the first failure.
                {
                    System.err.println("Saca ping on "  + host + ":" + port + 
                                       " failed, will retry " + 
                                       Integer.toString(requestRetryCount - 1) +
                                       " more times.");
                    ex.printStackTrace();
                }                
            }
        return null;    	
    }
    
    public Object search(String question) throws InterruptedException
    {
        for (int i = 0; i < requestRetryCount; i++)
            try 
            {
                return callSaca(Structs.struct(SacaService.ACT, SacaService.QUERY_REQUEST, 
                                                       "question", question)); 
            }
            catch (IOException ex) 
            { 
                if (i == 0) // Trace exception only on the first failure.
                {
                    System.err.println("Saca remote call to " + host + ":" + port +  
                    			" failed on '" + 
                                       question + "'" + ", will retry " + 
                                       Integer.toString(requestRetryCount - 1) +
                                       " more times.");
                    ex.printStackTrace();
                }                
            }
        return null;
    }
    
    public String getHost()
    {
        return host;
    }
    public void setHost(String host)
    {
        this.host = host;
    }
    public int getPort()
    {
        return port;
    }
    public void setPort(int port)
    {
        this.port = port;
    }

    public int getRequestRetryCount()
    {
        return requestRetryCount;
    }

    public void setRequestRetryCount(int requestRetryCount)
    {
        this.requestRetryCount = requestRetryCount;
    }

    public int getConnectRetryCount()
    {
        return connectRetryCount;
    }

    public void setConnectRetryCount(int connectRetryCount)
    {
        this.connectRetryCount = connectRetryCount;
    }

    public long getConnectRetryWait()
    {
        return connectRetryWait;
    }

    public void setConnectRetryWait(long connectRetryWait)
    {
        this.connectRetryWait = connectRetryWait;
    }    
}
