package org.raflab.lms.exceptions;

// korisnik je prijavljen, ali pravilo polaganja mu ne dozvoljava akciju (nije na spisku, van termina...)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
