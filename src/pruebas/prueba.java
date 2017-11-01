/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pruebas;



import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.comm.*;


public class prueba {

    public static void main(String[] args) {
        Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();
        // Identificamos cada puerto serie

        CommPortIdentifier portId = null; // Para asegurarnos de que al menos // contamos con un puerto serie
        while (portIdentifiers.hasMoreElements()) {
            CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();
            if (pid.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                portId = pid;
                break;
            }
        }
        if (portId == null) {
            System.err.println("No se ha encontrado ningún puerto serie ");
            System.exit(1);
        }
//
// Damos un nombre al puerto encontrado
//
        SerialPort port = null;
        try {
            port = (SerialPort) portId.open(
                    "name", // Nombre de la aplicación pidiendo el puerto
                    10000 // Espera máx. 10 seg. para adquirir puerto
                    );
        } catch (PortInUseException e) {
            System.err.println("Puerto ya en uso: " + e);
            System.exit(1);
        }
        try {
            //
            //Ahora se nos concede acceso exclusivo al puerto serie en particular. Podemos //configurar y obtener flujos de entrada y de salida.
            //Establecemos todos los parámetros.
            //Esto puede tener que ir en un bloque try / catch que arroja //UnsupportedCommOperationException
                    port.setSerialPortParams(
                            115200,
                            SerialPort.DATABITS_8,
                            SerialPort.STOPBITS_1,
                            SerialPort.PARITY_NONE);
        } catch (UnsupportedCommOperationException ex) {
            Logger.getLogger(prueba.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
