package com.thefirstlineofcode.com.sun.security.sasl;

import java.util.logging.Level;

import com.thefirstlineofcode.javax.sercurity.sasl.SaslClient;
import com.thefirstlineofcode.javax.sercurity.sasl.SaslException;

/**
 * @Author: xb.zou
 * @Date: 2020/3/5
 * @Desc: to->
 */
final class CramMD5Client extends CramMD5Base implements SaslClient {
    private String username;

    /**
     * Creates a SASL mechanism with client credentials that it needs
     * to participate in CRAM-MD5 authentication exchange with the server.
     *
     * @param authID A  non-null string representing the principal
     * being authenticated.
     *
     * @param pw A non-null String or byte[]
     * containing the password. If it is an array, it is first cloned.
     */
    CramMD5Client(String authID, byte[] pw) throws SaslException {
        if (authID == null || pw == null) {
            throw new SaslException(
                    "CRAM-MD5: authentication ID and password must be specified");
        }

        username = authID;
        this.pw = pw;  // caller should have already cloned
    }

    /**
     * CRAM-MD5 has no initial response.
     */
    public boolean hasInitialResponse() {
        return false;
    }

    /**
     * Processes the challenge data.
     *
     * The server sends a challenge data using which the client must
     * compute an MD5-digest with its password as the key.
     *
     * @param challengeData A non-null byte array containing the challenge
     *        data from the server.
     * @return A non-null byte array containing the response to be sent to
     *        the server.
     * @throws SaslException If platform does not have MD5 support
     * @throw IllegalStateException if this method is invoked more than once.
     */
    public byte[] evaluateChallenge(byte[] challengeData)
            throws SaslException {

        // See if we've been here before
        if (completed) {
            throw new IllegalStateException(
                    "CRAM-MD5 authentication already completed");
        }

        if (aborted) {
            throw new IllegalStateException(
                    "CRAM-MD5 authentication previously aborted due to error");
        }

        // generate a keyed-MD5 digest from the user's password and challenge.
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "CRAMCLNT01:Received challenge: {0}",
                        new String(challengeData, "UTF8"));
            }

            String digest = HMAC_MD5(pw, challengeData);

            // clear it when we no longer need it
            clearPassword();

            // response is username + " " + digest
            String resp = username + " " + digest;

            logger.log(Level.FINE, "CRAMCLNT02:Sending response: {0}", resp);

            completed = true;

            return resp.getBytes("UTF8");
        } catch (java.security.NoSuchAlgorithmException e) {
            aborted = true;
            throw new SaslException("MD5 algorithm not available on platform", e);
        } catch (java.io.UnsupportedEncodingException e) {
            aborted = true;
            throw new SaslException("UTF8 not available on platform", e);
        }
    }
}
