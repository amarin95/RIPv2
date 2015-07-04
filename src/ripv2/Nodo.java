package ripv2;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Alberto
 */
//Clase definitoria de un nodo.
public class Nodo {

    private boolean actualizado = false;
    private String ip;
    private int saltoscambio = 0;

    Nodo(String ip) {
        this.ip = ip;
        saltoscambio = 0;
    }

    Nodo(String ip, boolean act) {
        this.ip = ip;
        actualizado = act;
        saltoscambio = 0;
    }

    public void setcero() {
        saltoscambio = 0;
    }

    public void aumentasalto() {
        saltoscambio++;
    }

    public int getsaltos() {
        return saltoscambio;
    }

    public String getip() {
        return ip;
    }

    public boolean getactualizado() {
        return actualizado;
    }
}
