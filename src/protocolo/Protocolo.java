/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package protocolo;

import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;

public class Protocolo{
    SerialPort puerto = null;
    
    public Protocolo(String puerto){
        CommPortIdentifier portId;  
        Enumeration portList;  
        portList = CommPortIdentifier.getPortIdentifiers();  
         while (portList.hasMoreElements()){  
             portId = (CommPortIdentifier) portList.nextElement();  
             if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL && portId.getName().equalsIgnoreCase(puerto)){  
                 try {
                     this.puerto = (SerialPort) portId.open("PruebaProtocolo", 1971);
                 } catch (PortInUseException ex) {
                     Logger.getLogger(Protocolo.class.getName()).log(Level.SEVERE, null, ex);
                 }
             }    
       }  
    }
    
    public void iniciar(){
        while(true){
            System.out.println("Funcionando ");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(Protocolo.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Consulta");
        }
    }
    public void enviar(){
        
    }
    
    public void recibir(){
        
    }
}
