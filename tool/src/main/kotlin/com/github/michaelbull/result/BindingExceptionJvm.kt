package com.github.michaelbull.result

public class BindingException : Exception() {
    override fun fillInStackTrace(): Throwable {
        return this
    }
}
