package com.thefirstlineofcode.com.sun.security.sasl.digest;

import com.thefirstlineofcode.com.sun.security.sasl.util.PolicyUtils;
import com.thefirstlineofcode.javax.sercurity.sasl.SaslClient;
import com.thefirstlineofcode.javax.sercurity.sasl.SaslClientFactory;
import com.thefirstlineofcode.javax.sercurity.sasl.SaslException;

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

/**
 * @Author: xb.zou
 * @Date: 2020/3/5
 * @Desc: to->
 */
public final class FactoryImpl implements SaslClientFactory {

    private static final String myMechs[] = { "DIGEST-MD5" };
    private static final int DIGEST_MD5 = 0;
    private static final int mechPolicies[] = {
            PolicyUtils.NOPLAINTEXT|PolicyUtils.NOANONYMOUS};

    /**
     * Empty constructor.
     */
    public FactoryImpl() {
    }

    /**
     * Returns a new instance of the DIGEST-MD5 SASL client mechanism.
     *
     * @throws SaslException If there is an error creating the DigestMD5
     * SASL client.
     * @returns a new SaslClient ; otherwise null if unsuccessful.
     */
    public SaslClient createSaslClient(String[] mechs,
                                       String authorizationId, String protocol, String serverName,
                                       Map<String,?> props, CallbackHandler cbh)
            throws SaslException {

        for (int i=0; i<mechs.length; i++) {
            if (mechs[i].equals(myMechs[DIGEST_MD5]) &&
                    PolicyUtils.checkPolicy(mechPolicies[DIGEST_MD5], props)) {

                if (cbh == null) {
                    throw new SaslException(
                            "Callback handler with support for RealmChoiceCallback, " +
                                    "RealmCallback, NameCallback, and PasswordCallback " +
                                    "required");
                }

                return new DigestMD5Client(authorizationId,
                        protocol, serverName, props, cbh);
            }
        }
        return null;
    }

    /**
     * Returns the authentication mechanisms that this factory can produce.
     *
     * @returns String[] {"DigestMD5"} if policies in env match those of this
     * factory.
     */
    public String[] getMechanismNames(Map<String,?> env) {
        return PolicyUtils.filterMechs(myMechs, mechPolicies, env);
    }
}
