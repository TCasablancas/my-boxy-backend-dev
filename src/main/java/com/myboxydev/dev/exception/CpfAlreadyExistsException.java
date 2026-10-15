package com.myboxydev.dev.exception;

public class CpfAlreadyExistsException extends FieldConflictException {
    public CpfAlreadyExistsException() {
        super("cpf", "Este CPF já está associado a outra conta cadastrada.");
    }
}
