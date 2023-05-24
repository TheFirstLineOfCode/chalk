package com.thefirstlineofcode.chalk.android;

import java.security.cert.Certificate;

import com.thefirstlineofcode.chalk.android.core.stream.StandardStreamer;
import com.thefirstlineofcode.chalk.core.stream.IStandardStreamer;
import com.thefirstlineofcode.chalk.core.stream.IStreamer;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.core.stream.StreamConfig;
import com.thefirstlineofcode.chalk.core.stream.negotiants.tls.IPeerCertificateTruster;
import com.thefirstlineofcode.chalk.network.IConnection;

/**
 * @author xb.zou
 * @date 2017/10/26
 * @option 适用于Android Client的StandardChatClient
 */
public class StandardChatClient extends com.thefirstlineofcode.chalk.core.StandardChatClient {
    public StandardChatClient(StandardStreamConfig streamConfig) {
        super(streamConfig);
    }

    @Override
    protected IStreamer createStreamer(StreamConfig streamConfig, IConnection connection) {
        IStandardStreamer standardStreamer = new StandardStreamer((StandardStreamConfig)streamConfig, connection);
        standardStreamer.setNegotiationListener(this);
        standardStreamer.setAuthenticationCallback(this);

        if (peerCertificateTruster != null) {
            standardStreamer.setPeerCertificateTruster(peerCertificateTruster);
        } else {
            // always trust peer certificate
            standardStreamer.setPeerCertificateTruster(new IPeerCertificateTruster() {
                @Override
                public boolean accept(Certificate[] certificates) {
                    return true;
                }
            });
        }

        return standardStreamer;
    }
}
