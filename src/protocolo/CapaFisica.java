/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package protocolo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;

public class CapaFisica extends Thread {

    CapaEnlace capaEnlace;
    SerialPort puertoSerie;
    InputStream inStream;
    OutputStream outStream;
    Trama frame = new Trama();

    public CapaFisica(CapaEnlace cap, String nombrePuerto) {
        this.capaEnlace = cap;
        CommPortIdentifier portId;
        Enumeration portList;
        portList = CommPortIdentifier.getPortIdentifiers();
        while (portList.hasMoreElements()) {
            portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL && portId.getName().equalsIgnoreCase(nombrePuerto)) {
                try {
                    this.puertoSerie = (SerialPort) portId.open("PruebaProtocolo", 1971); // params: appname, timeout
                    this.puertoSerie.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN); // Request to Send / Clear to Send
                    this.puertoSerie.setSerialPortParams(19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_ODD); //params: baudrate, dataBits, stopBits, parity
                } catch (Exception ex) {
                    Logger.getLogger(Protocolo.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void setTramaEnviar(Trama frame) {
        String secuenciaBits = frame.encabezado + String.valueOf(frame.seq) + String.valueOf(frame.ack) + frame.info + frame.sumaVerificacion + frame.cola;

        enviar(secuenciaBits);
    }

    public Trama getTramaEnviar() {
        return frame;
    }

    private void enviar(String secuencia) {
        String secFisica = "~" + secuencia + "~"; // Para verificar inicio y fin 
        System.out.println("Secuencia a enviar - - -> " + secFisica);
        char[] secuenciaBits = secFisica.toCharArray();
        try {
            outStream = this.puertoSerie.getOutputStream();
            for (int i = 0; i < secuenciaBits.length; i++) {
                char bit = secuenciaBits[i];
                System.out.println("Enviado: " + bit);
                outStream.write(bit);
            }
        } catch (IOException ex) {
            Logger.getLogger(CapaFisica.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override

    public void run() { //REPRESENTA AL RECIBIR
        // Esta escuchando si llega algo

        while (true) {
            try {
                String confirmacion = "";
                System.out.println("Listo Para Recibir");
                inStream = this.puertoSerie.getInputStream();
                String response = "";
                int c = inStream.read(); // lee el primer byte en ascii
                boolean control = true;
                boolean seg = false;
                while (c != -1 && control) { // Si hay algo en el buffer
                    char ch = (char) c;
                    response = response + ch;
      
                    if (ch == '~') {
                        if (seg) { // Ultimo caracter
                            control = false;
                            confirmacion = guardarTrama(response);
                            System.out.println("CONFIRMACION");

                            capaEnlace.eventoRecibir = "FRAME_ARRIVAL";
                        }else{ // Primer caracter

                            c = inStream.read();
                        }
                        seg = true;                        
                    }else{
                        c = inStream.read();

                    }
                
                    
                }                
            System.out.println("Respuesta: " + response);   
            } catch (IOException ex) {
                Logger.getLogger(CapaFisica.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private String guardarTrama(String tramaFisica) {
        // Lee los componentes del frame
        String trama = tramaFisica.substring(1, tramaFisica.length() - 1);
        System.out.println("Se recibio la secuencia");
        this.frame.encabezado = trama.substring(0, 8);
        this.frame.seq = Integer.parseInt(trama.substring(8, 9));
        this.frame.ack = Integer.parseInt(trama.substring(9, 10));
        this.frame.info = trama.substring(10, (trama.length() - 18));
        this.frame.sumaVerificacion = trama.substring(trama.length() - 18, trama.length() - 8);
        this.frame.cola = trama.substring((trama.length() - 8), trama.length());
        
        
        return "~"+this.frame.encabezado + this.frame.seq + this.frame.seq +"null"+ this.frame.cola+"~";
    }
}
