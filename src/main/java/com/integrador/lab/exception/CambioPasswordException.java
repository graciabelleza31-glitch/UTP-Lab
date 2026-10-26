package com.integrador.lab.exception;

public class CambioPasswordException extends RuntimeException{
    public CambioPasswordException(String mensaje) {
        super(mensaje);
    }
}
