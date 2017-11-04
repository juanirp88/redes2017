/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package protocolo;

import main.Terminal;

public class CapaRed {
    Terminal terminal;
    Paquete paquete;
    Paquete paqueteRecibido;
    private CapaEnlace capaEnlace;
    private Thread enviar = null;

    public CapaRed(Terminal aThis, String dispositivoPuerto) {
        capaEnlace = new CapaEnlace();
        capaEnlace.configurar(this, dispositivoPuerto);
        this.terminal = aThis;
    }
    
    public void iniciar(){
        this.capaEnlace.iniciarRecibir(); // Inicia el hilo en la capa de enlace que espera para recibir
    }
    public Paquete getSiguientePaquete() {
        Paquete retorno = new Paquete();
        retorno.data = paquete.data;
        if (paquete.data.equals("")) {
            enviar.stop();
        }
        return retorno;
    }

    public void enviar(String info) {
        if(enviar!=null){
            enviar.stop();
        }        
        enviar = null;        
        Paquete p = new Paquete();
        p.data = info;
        paquete = p;
        enviar = capaEnlace.iniciarEnviar();
 
    }

    void setPaquete(Paquete paquete) {
        this.terminal.actualizarVentana(paquete.data);
    }
}
