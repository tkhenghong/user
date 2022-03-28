package com.awpghost.user.exceptions;

public class OTPVerificationException extends RuntimeException {
    public OTPVerificationException(String message) {
        super(message);
    }
}
