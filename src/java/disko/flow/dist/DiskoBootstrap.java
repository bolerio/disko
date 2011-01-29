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
package disko.flow.dist;

import static org.hypergraphdb.peer.Structs.getPart;

import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.BeanDeserializerFactory;
import org.codehaus.jackson.map.deser.StdDeserializerProvider;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;
import org.hypergraphdb.HGException;
import org.hypergraphdb.app.dataflow.JacksonDeserializerFactory;
import org.hypergraphdb.app.dataflow.JacksonSerializerFactory;
import org.hypergraphdb.app.dataflow.JoinNetworkActivity;
import org.hypergraphdb.app.dataflow.NetworkPeerActivity;
import org.hypergraphdb.app.dataflow.TransferProcessorActivity;
import org.hypergraphdb.peer.BootstrapPeer;
import org.hypergraphdb.peer.HyperGraphPeer;
import org.hypergraphdb.util.HGUtils;
import org.hypergraphdb.util.Mapping;

import relex.concurrent.RelexTaskResult;

public class DiskoBootstrap implements BootstrapPeer
{
    public void bootstrap(final HyperGraphPeer peer, Map<String, Object> config)
    {
        peer.getActivityManager().registerActivityType(JoinNetworkActivity.TYPENAME, 
                                                       JoinNetworkActivity.class);
        new TransferProcessorActivity(null, null); // initialize workflow constants
        peer.getActivityManager().registerActivityType(TransferProcessorActivity.TYPENAME, 
                                                       TransferProcessorActivity.class);
        new NetworkPeerActivity(null, (UUID)null);
        peer.getActivityManager().registerActivityType(NetworkPeerActivity.TYPENAME, 
                                                       NetworkPeerActivity.class);
        
        new DiskoManageActivity(null);
        peer.getActivityManager().registerActivityType(DiskoManageActivity.TYPENAME,
                                                       DiskoManageActivity.class); 
        
        // Configure serialization:
        final ObjectMapper jsonObjectMapper = new ObjectMapper();
        JacksonSerializerFactory ser = new JacksonSerializerFactory(BeanSerializerFactory.instance);
        ser.getJavaSerializedClasses().add(RelexTaskResult.class);
        JacksonDeserializerFactory deser = new JacksonDeserializerFactory(BeanDeserializerFactory.instance);
        deser.getJavaSerializedClasses().add(RelexTaskResult.class);
        jsonObjectMapper.setSerializerFactory(ser);
        jsonObjectMapper.setDeserializerProvider(new StdDeserializerProvider(deser));
        
        peer.getObjectContext().put("dataflow-json-serializer",
        new Mapping<Object, String>() {
            public String eval(Object x)
            {
                StringWriter jsonWriter = new StringWriter();
                try
                {
                    jsonObjectMapper.writeValue(jsonWriter, x);
                }
                catch (Throwable e)
                {
                    throw new RuntimeException(e);
                }
                return jsonWriter.toString();                
            }
        });
        
        peer.getObjectContext().put("dataflow-json-deserializer",
        new Mapping<Map<String, Object>, Object>() {
            public Object eval(Map<String, Object> structure)
            {
                Class<?> clazz;
                try
                {
                    clazz = HGUtils.loadClass(peer.getGraph(),
                                                       (String)getPart(structure, "classname"));
                    return jsonObjectMapper.readValue((String)getPart(structure, "classname"), clazz);                
                }
                catch (Exception e)
                {
                    throw new HGException(e);
                }                                                           
            }
        });        
    }
}
