package protocolo;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.zip.CRC32;
import javax.swing.Timer;

public class CapaEnlace {

    private static int max_seq = 2;
    public static final String EVENT_ESPERANDO = "ESPERANDO";
    public static final String EVENT_FRAME_ARRIVAL = "FRAME_ARRIVAL";
    public static final String EVENT_CKSUM_ERR = "CKSUM_ERR";
    public static final String EVENT_TIMEOUT = "TIMEOUT";
    //Variables para enviar
    int next_frame_to_send = 0;
    public String eventoEnviar;
    private int seq_vencida;
    private int seq_enviada;
    private int timerDelay = 7000;
    private Timer temporizador;
    //Variables para recibir
    public String eventoRecibir;
    //Variables de entorno
    private CapaRed capaRed;
    private CapaFisica capaFisica;

    public void configurar(CapaRed capR, String nombrePuerto) {
        this.capaFisica = new CapaFisica(this, nombrePuerto);

        this.capaFisica.start(); //Inicia el run() de la capa fisica

        this.capaRed = capR;
    }

    public Thread iniciarEnviar() {
        Enviar hiloEnviar = new Enviar();

        hiloEnviar.start();

        return hiloEnviar;
    }

    public Thread iniciarRecibir() {
        Recibir hiloRecibir = new Recibir();

        hiloRecibir.start();

        return hiloRecibir;
    }

    class Enviar extends Thread {

        public void run() {
             //inicializa la variable de la siguiente trama de salida
            Trama frame = new Trama(); //define una variable de trabajo
            Paquete buffer; // define variable que recibe de la capa de red
            buffer = obtener_paquete_capa_red(); //obtiene el paquete de la capa de red
            boolean control = true;
            while (control) {
                eventoEnviar = EVENT_ESPERANDO;
                frame.encabezado = "01111110";
                frame.cola = "01111110";
                frame.info = buffer.data; //Inicializa la carga util de la trama
                frame.sumaVerificacion = calcular_suma_verificacion(buffer.data);
                frame.seq = next_frame_to_send; //Inicializa el numero de secuencia que envia
                enviar_capa_fisica(frame); //Envia la trama a la capa fisica
                Timer timer = start_timer(frame.seq);  //Inicializa el temporizador para el num de secuencia
                wait_for_event_enviar(EVENT_ESPERANDO); //Deja al metodo esperando por un evento distinto a ESPERANDO
                if (eventoEnviar.equals(EVENT_FRAME_ARRIVAL)) { //Si el evento recibido es FRAME_ARRIVAL
                    System.out.println("ENTRO AL FRAME ARRIVAL");
                    Trama frameRecibido = obtener_trama_capa_fisica(); //Solicita a la capa fisica la trama de verificacion
                    if (frameRecibido.ack == next_frame_to_send) { //Obtiene el ACK y si es el mismo a la trama enviada procede
                        timer.stop(); //Detiene el temporizador
                        buffer = obtener_paquete_capa_red(); // Obtiene el siguiente paquete de la capa de red.
                        control = false;
                        next_frame_to_send = (next_frame_to_send + 1) % max_seq; //Incrementa la trama a enviar en 1
                    }
                }
                System.out.println("SALE POR EL TEMP Y POR EL WAIT" + eventoEnviar);
            }
        }
    }

    private Paquete obtener_paquete_capa_red() {
        return this.capaRed.getSiguientePaquete();
    }

    private void enviar_capa_fisica(Trama frame) {
        this.capaFisica.setTramaEnviar(frame);
    }

    private Trama obtener_trama_capa_fisica() {
        return this.capaFisica.getTramaEnviar();
    }

    private Timer start_timer(int seq) {
        seq_enviada = seq;
        temporizador = new Timer(timerDelay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                eventoEnviar = EVENT_TIMEOUT;
                seq_vencida = seq_enviada;
                temporizador.stop();
            }
        });
        temporizador.start();
        return temporizador;
    }

    private void wait_for_event_enviar(String eventR) {
        boolean control = true;
        while (control) {
            if (!this.eventoEnviar.equals(EVENT_ESPERANDO)) {
                control = false;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                System.out.println("FALLO EL THREAD DEL WAITFOREVENT");
            }
        }
    }

    class Recibir extends Thread {
// Hilo encargado de recibir las tramas
        public void run() {
            Trama frameRecibido = new Trama(); //Define la variable a recibir
            Trama frameConfirmacion = new Trama();//define una variable de trabajo

            int frame_expected = 0;
            while (true) {
                wait_for_event_recibir(EVENT_ESPERANDO); //Sleep
                if (eventoRecibir.equals(EVENT_FRAME_ARRIVAL)) { //Estan llegando datos
                    frameRecibido = obtener_trama_capa_fisica();
                    if (frameRecibido.seq == frame_expected && !frameRecibido.info.equals("###")) {
                        boolean validacion = verificar_suma(frameRecibido);// ToDo
                        System.out.println("ENCABEZADO: " + frameRecibido.encabezado);
                        System.out.println("SEQ: " + frameRecibido.seq);
                        System.out.println("ASK: " + frameRecibido.ack);
                        System.out.println("INFO: " + frameRecibido.info);
                        System.out.println("SUMA: " + frameRecibido.sumaVerificacion);
                        System.out.println("COLA: " + frameRecibido.cola);
                        if (validacion) {
                            enviar_paquete_capa_red(frameRecibido.info);
                            frame_expected = (frame_expected + 1) % max_seq;
                        }
                        frameConfirmacion.ack = (frame_expected + 1) % max_seq;
                        frameConfirmacion.encabezado = "01111110";
                        frameConfirmacion.cola = "01111110";
                        frameConfirmacion.info = "###"; //Inicializa la carga util de la trama
                        frameConfirmacion.sumaVerificacion = calcular_suma_verificacion(frameConfirmacion.info);
                        System.out.println(frameConfirmacion.sumaVerificacion +" es la suma antes de enviar" );
                        frameConfirmacion.seq = 0;
                        enviar_capa_fisica(frameConfirmacion); //Enviar la confirmacion a la capa fisica
                    }
                    if(frameRecibido.info.equals("###")){ //Recibio la confirmacion
                        eventoEnviar = EVENT_FRAME_ARRIVAL;
                    }

                }
            }
        }
    }

    private void wait_for_event_recibir(String eventR) {
        this.eventoRecibir = eventR;
        boolean control = true;
        while (control) {
            if (!this.eventoRecibir.equals(EVENT_ESPERANDO)) {
                control = false;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                System.out.println("FALLO EL THREAD DEL WAITFOREVENT");
            }
        }
    }

private String calcular_suma_verificacion(String m) {

        String file = m;

    
        byte data[] = file.getBytes();

           // Compute CRC32 checksum
        CRC32 crc = new CRC32();
        crc.update(data);

        long crc32Checksum = crc.getValue();
        
        String suma = String.valueOf(crc32Checksum);
        if(suma.length()<10){ //controlo que siempre sea 10 el checksum completo con 0
            for (int i = 0; i < 10- suma.length(); i++) {
                suma = suma + "0";
            }
        }
        System.out.println("CRC32: " + suma + " para: " + file);
        
        
return suma;

}

    private void enviar_paquete_capa_red(String info) { //Muestra el paquete recibido
        Paquete paquete = new Paquete();
        paquete.data = info;
        capaRed.setPaquete(paquete);
    }

    private boolean verificar_suma(Trama frameRecibido) {
        
        
        
                String crcRecibido = frameRecibido.sumaVerificacion;
                
                String crcCalculado = calcular_suma_verificacion(frameRecibido.info);
                
                if(crcCalculado.equalsIgnoreCase(crcRecibido)){
                    
        return true;
                }
                
                else{
                    return false;
                }
    }
}
