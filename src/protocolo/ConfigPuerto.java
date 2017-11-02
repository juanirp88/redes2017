package protocolo;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import javax.comm.CommPortIdentifier;

public class ConfigPuerto {
    
    public Collection buscarDispositivos(){
        Collection retorno = new ArrayList();
        
        CommPortIdentifier portId;  
        Enumeration portList;  
        portList = CommPortIdentifier.getPortIdentifiers();  
         while (portList.hasMoreElements()){  
             portId = (CommPortIdentifier) portList.nextElement();  
             if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL){  
                 System.out.println("Se encotro el puerto: " + portId.getName());
                 retorno.add(portId.getName());
             }    
       }  
        return retorno;
    }
}
