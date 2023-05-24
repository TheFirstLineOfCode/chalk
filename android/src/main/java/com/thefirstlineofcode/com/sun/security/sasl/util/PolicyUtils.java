package com.thefirstlineofcode.com.sun.security.sasl.util;

import java.util.Map;

import com.thefirstlineofcode.javax.sercurity.sasl.Sasl;

/**
 * @Author: xb.zou
 * @Date: 2020/3/5
 * @Desc: to->
 */
final public class PolicyUtils {
    // Can't create one of these
    private PolicyUtils() {
    }

    public final static int NOPLAINTEXT = 0x0001;
    public final static int NOACTIVE = 0x0002;
    public final static int NODICTIONARY = 0x0004;
    public final static int FORWARD_SECRECY = 0x0008;
    public final static int NOANONYMOUS = 0x0010;
    public final static int PASS_CREDENTIALS = 0x0200;

    /**
     * Determines whether a mechanism's characteristics, as defined in flags,
     * fits the security policy properties found in props.
     * @param flags The mechanism's security characteristics
     * @param props The security policy properties to check
     * @return true if passes; false if fails
     */
    public static boolean checkPolicy(int flags, Map<?, ?> props) {
        if (props == null) {
            return true;
        }

        if ("true".equalsIgnoreCase((String)props.get(Sasl.POLICY_NOPLAINTEXT))
                && (flags&NOPLAINTEXT) == 0) {
            return false;
        }
        if ("true".equalsIgnoreCase((String)props.get(Sasl.POLICY_NOACTIVE))
                && (flags&NOACTIVE) == 0) {
            return false;
        }
        if ("true".equalsIgnoreCase((String)props.get(Sasl.POLICY_NODICTIONARY))
                && (flags&NODICTIONARY) == 0) {
            return false;
        }
        if ("true".equalsIgnoreCase((String)props.get(Sasl.POLICY_NOANONYMOUS))
                && (flags&NOANONYMOUS) == 0) {
            return false;
        }
        if ("true".equalsIgnoreCase((String)props.get(Sasl.POLICY_FORWARD_SECRECY))
                && (flags&FORWARD_SECRECY) == 0) {
            return false;
        }
        if ("true".equalsIgnoreCase((String)props.get(Sasl.POLICY_PASS_CREDENTIALS))
                && (flags&PASS_CREDENTIALS) == 0) {
            return false;
        }

        return true;
    }

    /**
     * Given a list of mechanisms and their characteristics, select the
     * subset that conforms to the policies defined in props.
     * Useful for SaslXXXFactory.getMechanismNames(props) implementations.
     *
     */
    public static String[] filterMechs(String[] mechs, int[] policies,
                                       Map<?, ?> props) {
        if (props == null) {
            return mechs.clone();
        }

        boolean[] passed = new boolean[mechs.length];
        int count = 0;
        for (int i = 0; i< mechs.length; i++) {
            if (passed[i] = checkPolicy(policies[i], props)) {
                ++count;
            }
        }
        String[] answer = new String[count];
        for (int i = 0, j=0; i< mechs.length; i++) {
            if (passed[i]) {
                answer[j++] = mechs[i];
            }
        }

        return answer;
    }
}
