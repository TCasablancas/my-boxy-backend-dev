package com.myboxydev.dev.exception;

public class AliasAlreadyExistsException extends RuntimeException{
    public AliasAlreadyExistsException(String alias) {
      super("O alias @" + alias + " já está em uso por outro usuário.");
    }
}
