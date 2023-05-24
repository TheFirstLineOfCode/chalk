package com.thefirstlineofcode.com.sun.security.sasl.digest;

import com.thefirstlineofcode.javax.sercurity.sasl.SaslException;

/**
 * @Author: xb.zou
 * @Date: 2020/3/5
 * @Desc: to->
 */
interface SecurityCtx {

    /**
     * Wrap out-going message and return wrapped message
     *
     * @throws SaslException
     */
    byte[] wrap(byte[] dest, int start, int len)
            throws SaslException;

    /**
     * Unwrap incoming message and return original message
     *
     * @throws SaslException
     */
    byte[] unwrap(byte[] outgoing, int start, int len)
            throws SaslException;
}
