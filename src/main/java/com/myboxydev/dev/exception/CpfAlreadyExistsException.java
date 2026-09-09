package com.myboxydev.dev.exception;

public class CpfAlreadyExistsException extends RuntimeException {
    public CpfAlreadyExistsException() {
        super("Este CPF já está associado a outra conta cadastrada.");
    }
}
