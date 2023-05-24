package com.thefirstlineofcode.chalk.android.core.stream;

import com.thefirstlineofcode.chalk.android.core.stream.negotiants.sasl.SaslNegotiant;
import com.thefirstlineofcode.chalk.core.stream.IStreamNegotiant;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.core.stream.negotiants.InitialStreamNegotiant;
import com.thefirstlineofcode.chalk.core.stream.negotiants.ResourceBindingNegotiant;
import com.thefirstlineofcode.chalk.core.stream.negotiants.SessionEstablishmentNegotiant;
import com.thefirstlineofcode.chalk.core.stream.negotiants.tls.TlsNegotiant;
import com.thefirstlineofcode.chalk.network.IConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xb.zou
 * @date 2017/10/26
 * @option 适用于Android Client的StandardStreamer
 */
public class StandardStreamer extends com.thefirstlineofcode.chalk.core.stream.StandardStreamer {
    public StandardStreamer(StandardStreamConfig streamConfig) {
        super(streamConfig);
    }

    public StandardStreamer(StandardStreamConfig streamConfig, IConnection connection) {
        super(streamConfig, connection);
    }

    @Override
    protected List<IStreamNegotiant> createNegotiants() {
        List<IStreamNegotiant> negotiants = new ArrayList<>();

        InitialStreamNegotiant initialStreamNegotiant = createInitialStreamNegotiant();
        negotiants.add(initialStreamNegotiant);

        TlsNegotiant tls = createTlsNegotiant();
        negotiants.add(tls);

        SaslNegotiant sasl = createSaslNegotiantForAndroid();
        negotiants.add(sasl);

        ResourceBindingNegotiant resourceBindingNegotiant = createResourceBindingNegotiant();
        negotiants.add(resourceBindingNegotiant);

        SessionEstablishmentNegotiant sessionEstablishmentNegotiant = createSessionEstablishmentNegotiant();
        negotiants.add(sessionEstablishmentNegotiant);

        setNegotiationReadResponseTimeout(negotiants);

        return negotiants;
    }



    protected SaslNegotiant createSaslNegotiantForAndroid() {
        SaslNegotiant sasl = new SaslNegotiant(streamConfig.getHost(), streamConfig.getLang(), authToken);
        sasl.setAuthenticationCallback(authenticationCallback);
        return sasl;
    }

}
