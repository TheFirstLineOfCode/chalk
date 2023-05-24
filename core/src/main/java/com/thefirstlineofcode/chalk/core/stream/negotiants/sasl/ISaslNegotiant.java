package com.thefirstlineofcode.chalk.core.stream.negotiants.sasl;

import com.thefirstlineofcode.chalk.core.stream.IAuthenticationToken;

/**
 * @author xb.zou
 * @date 2017/10/26
 */
public interface ISaslNegotiant {
    void retry(IAuthenticationToken authToken);
    void abort();
}
