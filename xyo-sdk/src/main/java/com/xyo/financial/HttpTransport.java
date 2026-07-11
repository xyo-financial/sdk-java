package com.xyo.financial;

public interface HttpTransport {
    HttpResponse send(HttpRequest request) throws XyoException;
}
