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
                    this.puertoSerie = (SerialPort) portId.open("PruebaProtocolo", 1971);
                    /*
                    Al método idPuerto.open, hay que pasarle dos parámetros. 
                    Un cadena que describe al propietario del puerto (puede ser el nombre de nuestra aplicación), y el tiempo en milisegundos
                    que se esperará por un puerto bloqueado antes de lanzar la excepción de puerto en uso (en este caso, dos segundos).
                    */
                    this.puertoSerie.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
                    this.puertoSerie.setSerialPortParams(19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_ODD); // Se configuran los parametros
                } catch (Exception ex) {
                    Logger.getLogger(Protocolo.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void setTramaEnviar(Trama frame) {
        String secuenciaBits = frame.encabezado + String.valueOf(frame.seq) + String.valueOf(frame.ack) + frame.info + frame.sumaVerificacion + frame.cola;
        System.out.println("trama a enviar " + secuenciaBits );
        enviar(secuenciaBits);
    }

    public Trama getTramaEnviar() {
        return frame;
    }

    private void enviar(String secuencia) {
        String secFisica = "~" + secuencia + "~";
        System.out.println("SECUENCIA A ENVIAR - - -> " + secFisica);
        char[] secuenciaBits = secFisica.toCharArray();
        try {
            outStream = this.puertoSerie.getOutputStream();
            for (int i = 0; i < secuenciaBits.length; i++) {
                char bit = secuenciaBits[i];
                System.out.println("DETALLE ENVIO");
                System.out.println(bit);
                outStream.write(bit);
            }
        } catch (IOException ex) {
            Logger.getLogger(CapaFisica.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void run() { //REPRESENTA AL RECIBIR
        while (true) {
            try {
                String confirmacion = "";
                System.out.println("Listo Para Recibir");
                inStream = this.puertoSerie.getInputStream();
                String response = "";
                int c = inStream.read(); // lee el primer byte en ascii
                boolean control = true;
                boolean seg = false;
                while (c != -1 && control) {
                    char ch = (char) c;
                    response = response + ch;
      
                    if (ch == '~') {
                        if (seg) {
                            control = false;
                            confirmacion = guardarTrama(response);
                            System.out.println("CONFIRMACION");
                            capaEnlace.eventoRecibir = "FRAME_ARRIVAL"; // al eventoRecibir le asigna FRAME_ARRIVAL
                        }else{
                            c = inStream.read();
                        }
                        seg = true;                        
                    }else{
                        c = inStream.read();

                    }
                    System.out.println("Respuesta: " + response);     
                    
                }                

            } catch (IOException ex) {
                Logger.getLogger(CapaFisica.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private String guardarTrama(String tramaFisica) {
        // Lee los componentes del frame
        String trama = tramaFisica.substring(1, tramaFisica.length() - 1);
        System.out.println("SECUENCIA RECIBIDA!");
        this.frame.encabezado = trama.substring(0, 8);
        this.frame.seq = Integer.parseInt(trama.substring(8, 9));
        this.frame.ack = Integer.parseInt(trama.substring(9, 10));
        this.frame.info = trama.substring(10, (trama.length() - 18));
        this.frame.sumaVerificacion = trama.substring(trama.length() - 18, trama.length() - 8);
        this.frame.cola = trama.substring((trama.length() - 8), trama.length());
        
        
        return "~"+this.frame.encabezado + this.frame.seq + this.frame.seq +"null"+ this.frame.cola+"~";
    }
}
