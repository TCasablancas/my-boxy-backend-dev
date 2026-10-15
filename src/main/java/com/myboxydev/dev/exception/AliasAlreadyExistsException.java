package com.myboxydev.dev.exception;

public class AliasAlreadyExistsException extends FieldConflictException {
    public AliasAlreadyExistsException(String alias) {
      super("alias", "O alias @" + alias + " já está em uso por outro usuário.");
    }
}
