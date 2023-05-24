package com.thefirstlineofcode.chalk.android.core.stream.negotiants.sasl;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import java.util.Hashtable;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.oxm.annotation.AnnotatedParserFactory;
import com.thefirstlineofcode.basalt.oxm.binary.Base64;
import com.thefirstlineofcode.basalt.oxm.parsers.SimpleObjectParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stream.BindParser;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stream.sasl.FailureParserFactory;
import com.thefirstlineofcode.basalt.oxm.translators.SimpleObjectTranslatorFactory;
import com.thefirstlineofcode.basalt.oxm.translators.core.stream.sasl.AuthTranslatorFactory;
import com.thefirstlineofcode.basalt.xmpp.Constants;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Bind;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Feature;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Features;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Session;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Stream;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Abort;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Auth;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Challenge;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Failure;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Mechanisms;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Response;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Success;
import com.thefirstlineofcode.chalk.android.core.stream.StandardStreamer;
import com.thefirstlineofcode.chalk.core.stream.IAuthenticationCallback;
import com.thefirstlineofcode.chalk.core.stream.IAuthenticationToken;
import com.thefirstlineofcode.chalk.core.stream.INegotiationContext;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.UsernamePasswordToken;
import com.thefirstlineofcode.chalk.core.stream.negotiants.InitialStreamNegotiant;
import com.thefirstlineofcode.chalk.core.stream.negotiants.sasl.ISaslNegotiant;
import com.thefirstlineofcode.chalk.core.stream.negotiants.sasl.SaslAuthenticationFailure;
import com.thefirstlineofcode.chalk.core.stream.negotiants.sasl.SaslError;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.com.sun.security.sasl.ClientFactoryImpl;
import com.thefirstlineofcode.com.sun.security.sasl.digest.FactoryImpl;
import com.thefirstlineofcode.javax.sercurity.auth.callback.NameCallback;
import com.thefirstlineofcode.javax.sercurity.auth.callback.PasswordCallback;
import com.thefirstlineofcode.javax.sercurity.sasl.RealmCallback;
import com.thefirstlineofcode.javax.sercurity.sasl.Sasl;
import com.thefirstlineofcode.javax.sercurity.sasl.SaslClient;
import com.thefirstlineofcode.javax.sercurity.sasl.SaslException;


public class SaslNegotiant extends InitialStreamNegotiant implements ISaslNegotiant {

    private static final int MAX_FAILURE_COUNT_EXCCEED_READ_RESPONSE_TIMEOUT = 200;

    private static final int DEFAULT_SASL_PROCESS_TIMEOUT = 1000 * 2;

    public static final String DIGEST_MD5_MECHANISM = "DIGEST-MD5";
    public static final String CRAM_MD5_MECHANISM = "CRAM-MD5";
    public static final String PLAIN_MECHANISM = "PLAIN";

    
    private static IOxmFactory oxmFactory = OxmService.createStreamOxmFactory();

    private UsernamePasswordToken authToken;
    private int failureCount;
    private IAuthenticationCallback authCallback;
    private volatile boolean waitFailureAction;
    private boolean abortSasl;

    static {
        Security.addProvider(new SaslProvider());

        InitialStreamNegotiant.oxmFactory.register(ProtocolChain.first(Features.PROTOCOL).next(Bind.PROTOCOL),
                new AnnotatedParserFactory<>(BindParser.class)
        );
        InitialStreamNegotiant.oxmFactory.register(ProtocolChain.first(Features.PROTOCOL).next(Session.PROTOCOL),
                new SimpleObjectParserFactory<>(
                        Session.PROTOCOL,
                        Session.class)
        );

        oxmFactory.register(ProtocolChain.first(Challenge.PROTOCOL),
                new SimpleObjectParserFactory<>(
                        Challenge.PROTOCOL,
                        Challenge.class)
        );
        oxmFactory.register(ProtocolChain.first(Failure.PROTOCOL), new FailureParserFactory());
        oxmFactory.register(ProtocolChain.first(Success.PROTOCOL),
                new SimpleObjectParserFactory<>(
                        Success.PROTOCOL,
                        Success.class)
        );

        oxmFactory.register(Auth.class, new AuthTranslatorFactory());
        oxmFactory.register(Response.class, new SimpleObjectTranslatorFactory<>(
                        Response.class,
                        Response.PROTOCOL)
        );
        oxmFactory.register(Abort.class, new SimpleObjectTranslatorFactory<>(
                Abort.class,
                Abort.PROTOCOL));
    }

    public SaslNegotiant(String hostName, String lang, IAuthenticationToken authToken) {
        super(hostName, lang);

        checkAuthToken(authToken);

        this.authToken = (UsernamePasswordToken)authToken;
        this.failureCount = 0;

        abortSasl = false;
    }

    private void checkAuthToken(IAuthenticationToken authToken) {
        if (authToken == null) {
            throw new IllegalArgumentException("Null authentication token.");
        }

        if (!(authToken instanceof UsernamePasswordToken)) {
            throw new IllegalArgumentException("Only UsernamePasswordToken supported.");
        }
    }

    @Override
    protected void doNegotiate(INegotiationContext context) throws ConnectionException, NegotiationException {
        @SuppressWarnings("unchecked")
        List<Feature> features = (List<Feature>)context.getAttribute(StandardStreamer.NEGOTIATION_KEY_FEATURES);
        Mechanisms mechanisms = findMechanisms(features);
        
        if (mechanisms == null) {
        	throw new NegotiationException(this, SaslError.MECHANISM_NOT_SUPPORTED);
        }
        
        String choseMechanism = chooseMechanism(mechanisms);
        
        if (choseMechanism == null) {
        	throw new NegotiationException(this, SaslError.MECHANISM_NOT_SUPPORTED);
        }
        
        negotiateSasl(context, choseMechanism);
        
        if (!abortSasl) {
            super.doNegotiate(context);
        }
    }

    protected void negotiateSasl(INegotiationContext context, String mechanism)
    		throws ConnectionException, NegotiationException {
        if (abortSasl || authToken == null)
            return;

        SaslClient saslClient = this.createSaslClient(mechanism);

        context.write(oxmFactory.translate(new Auth(mechanism)));

        while(!saslClient.isComplete()) {
            Object response = oxmFactory.parse(readResponse(DEFAULT_SASL_PROCESS_TIMEOUT));
            if (response instanceof Challenge) {
                String saslResponse = getSaslResponse(saslClient, (Challenge) response);
                context.write(oxmFactory.translate(new Response(saslResponse)));
            } else if (response instanceof Failure) {
                processFailure(context, (Failure)response);
                break;
            }else if(response instanceof IError){
                processError((IError)response, context, oxmFactory);
            }else{
                //success
                return;
            }
        }
        if (saslClient.isComplete()) {
            Object response = oxmFactory.parse(readResponse(DEFAULT_SASL_PROCESS_TIMEOUT));
            if (response instanceof Success) {
                return; // success
            } else if (response instanceof Failure) {
                processFailure(context, (Failure)response);
            } else {
                processError((IError)response, context, oxmFactory);
                return;
            }
        }

        try {
            synchronized (this) {
                if (waitFailureAction) {
                    wait(DEFAULT_SASL_PROCESS_TIMEOUT);
                }
                
                if (waitFailureAction) {
                	throw new ConnectionException(ConnectionException.Type.READ_RESPONSE_TIMEOUT);
                }
            }
        } catch (InterruptedException e) {
            throw new NegotiationException(this, e);
        }

        if (abortSasl) {
            context.write(oxmFactory.translate(new Abort()));
            Failure failure = (Failure)oxmFactory.parse(readResponse(DEFAULT_SASL_PROCESS_TIMEOUT));

            if (failure.getErrorCondition() == Failure.ErrorCondition.ABORTED) {
                context.write(oxmFactory.translate(new Stream(true)));
                Stream closeStream = (Stream)oxmFactory.parse(readResponse(DEFAULT_SASL_PROCESS_TIMEOUT));

                if (Boolean.TRUE.equals(closeStream.isClose())) {
                    context.close();
                }

                return;
            }
        } else {
            negotiateSasl(context, mechanism);
        }
    }

	protected SaslClient createSaslClient(String mechanism) throws NegotiationException {
		Hashtable<String, String> props = new Hashtable<>();
        props.put(Sasl.QOP, "auth");
		try {
           SaslClient saslClient = Sasl.createSaslClient(new String[] {mechanism}, getAuthorizationId(),
                    Constants.PROTOCOL_NAME, hostName, props, new DefaultCallbackHandler());
           return saslClient;
        } catch (SaslException e) {
            throw new NegotiationException(this, e);
        }
	}

    private void processFailure(INegotiationContext context, Failure response) throws NegotiationException, ConnectionException {
        waitFailureAction = true;
        authToken = null;
        failureCount++;

        Failure failure = (Failure)response;
        try {
            String message = readResponse(MAX_FAILURE_COUNT_EXCCEED_READ_RESPONSE_TIMEOUT);
            Stream closeStream = (Stream)oxmFactory.parse(message);
            if (closeStream.isClose()) {
                context.close();
            }

            authCallback.failed(new SaslAuthenticationFailure(this, failure.getErrorCondition(), false, failureCount));

            throw new NegotiationException(this, SaslError.MAX_FAILURE_COUNT_EXCCEED);
        } catch (ConnectionException e) {
            if (e.getType() == ConnectionException.Type.READ_RESPONSE_TIMEOUT) {
                authCallback.failed(new SaslAuthenticationFailure(this, failure.getErrorCondition(), true, failureCount));
                return;
            }

            throw e;
        }
    }

    private String getAuthorizationId() throws NegotiationException {
        if (authToken instanceof UsernamePasswordToken) {
            return ((UsernamePasswordToken)authToken).getUsername();
        }

        throw new NegotiationException(this, SaslError.UNKNOWN_AUTH_TOKEN_TYPE);
    }

    private String getSaslResponse(SaslClient saslClient, Challenge challenge) throws NegotiationException {
        byte[] data = Base64.decode(challenge.getText());

        try {
            data = saslClient.evaluateChallenge(data);
        } catch (SaslException e) {
            throw new NegotiationException(this, e);
        }

        if (data == null)
            return null;

        return Base64.encodeToString(data, false);
    }

    private class DefaultCallbackHandler implements CallbackHandler {

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    NameCallback nameCallback = (NameCallback)callback;
                    nameCallback.setName(authToken.getUsername());
                } else if (callback instanceof PasswordCallback) {
                    PasswordCallback passwordCallback = (PasswordCallback)callback;
                    passwordCallback.setPassword(authToken.getPassword());
                } else if (callback instanceof RealmCallback) {
                    RealmCallback realmCallback = (RealmCallback)callback;
                    realmCallback.setText(hostName);
                }
            }
        }

    }

    protected String chooseMechanism(Mechanisms mechanisms) {
        if (mechanisms.getMechanisms().contains(DIGEST_MD5_MECHANISM))
            return DIGEST_MD5_MECHANISM;

        if (mechanisms.getMechanisms().contains(CRAM_MD5_MECHANISM))
            return CRAM_MD5_MECHANISM;
        
        if (mechanisms.getMechanisms().contains(PLAIN_MECHANISM))
        	return PLAIN_MECHANISM;
        
        return null;
    }

    private Mechanisms findMechanisms(List<Feature> features) {
        for (Feature feature : features) {
            if (feature instanceof Mechanisms)
                return (Mechanisms)feature;
        }

        return null;
    }

    public void setAuthenticationCallback(IAuthenticationCallback authCallback) {
        this.authCallback = authCallback;
    }

    public IAuthenticationCallback getAuthenticationCallback() {
        return authCallback;
    }

    public void retry(IAuthenticationToken authToken) {
        checkAuthToken(authToken);

        this.authToken = (UsernamePasswordToken)authToken;
        synchronized (this) {
            waitFailureAction = false;
            notify();
        }
    }

    public void abort() {
        this.authToken = null;

        synchronized (this) {
            waitFailureAction = false;
            abortSasl = true;
            notify();
        }
    }
    
    @Override
	public void exceptionOccurred(ConnectionException exception) {
    	synchronized (this) {
    		if (exception.getType() == ConnectionException.Type.END_OF_STREAM && abortSasl)
        		return;
		}
    	
		this.exception = exception;
		synchronized (lock) {
			lock.notify();
		}
	}

	private static class SaslProvider extends Provider {
        private static final long serialVersionUID = 8622598936488630849L;
        private static final String INFO = "Sun SASL provider(implements client mechanisms for: DIGEST-MD5, GSSAPI, EXTERNAL, PLAIN, CRAM-MD5, NTLM; server mechanisms for: DIGEST-MD5, GSSAPI, CRAM-MD5, NTLM)";

        public SaslProvider() {
            super("SunSASL", 1.8D, INFO);
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    SaslProvider.this.put("SaslClientFactory.DIGEST-MD5", FactoryImpl.class.getName());
                    SaslProvider.this.put("SaslClientFactory.EXTERNAL", ClientFactoryImpl.class.getName());
                    SaslProvider.this.put("SaslClientFactory.PLAIN", ClientFactoryImpl.class.getName());
                    SaslProvider.this.put("SaslClientFactory.CRAM-MD5", ClientFactoryImpl.class.getName());
                    return null;
                }
            });
        }
    }
}
