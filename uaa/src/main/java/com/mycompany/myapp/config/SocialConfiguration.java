package com.mycompany.myapp.config;

import javax.inject.Inject;
import javax.inject.Scope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurer;
import org.springframework.social.config.annotation.SocialConfigurerAdapter;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionSignUp;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.support.ConnectionFactoryRegistry;
import org.springframework.social.connect.web.ProviderSignInController;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.social.security.AuthenticationNameUserIdSource;
import org.springframework.social.wso2is.connect.WSO2ISConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mycompany.myapp.repository.CustomSocialUsersConnectionRepository;
import com.mycompany.myapp.repository.SocialUserConnectionRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.social.CustomSignInAdapter;
import com.mycompany.myapp.security.social.SignupHandler;
import com.mycompany.myapp.service.UserService;

@Configuration
@EnableSocial
public class SocialConfiguration extends SocialConfigurerAdapter {

    private final Logger log = LoggerFactory.getLogger(SocialConfiguration.class);

    @Inject
    private SocialUserConnectionRepository socialUserConnectionRepository;
    
    @Inject
    private UserRepository userRepository;
    
    @Autowired
    private ConnectionSignUp autoSignUpHandler;
    
    @Inject
    private UserService userService;
    
    @Inject
    private JHipsterProperties jHipsterProperties;

	
	@Override
	public void addConnectionFactories(ConnectionFactoryConfigurer connectionFactoryConfigurer,
			Environment environment) {
		
        String witIdentityClientId = environment.getProperty("spring.social.witidentity.clientId");
        String witIdentityClientSecret = environment.getProperty("spring.social.witidentity.clientSecret");
        String witIdentityTokenURI = environment.getProperty("spring.social.witidentity.accessTokenUri");
        String witIdentityAuthURI = environment.getProperty("spring.social.witidentity.userAuthorizationUri");
        String witIdentityBaseURI = environment.getProperty("spring.social.witidentity.baseUri");
        
        if (witIdentityClientId != null && witIdentityClientSecret != null && witIdentityTokenURI != null && witIdentityAuthURI != null && witIdentityBaseURI != null) {
	        log.debug("Configuring WSO2IS ConnectionFactory");
	        connectionFactoryConfigurer.addConnectionFactory(
	        		new WSO2ISConnectionFactory(witIdentityClientId, witIdentityClientSecret, witIdentityAuthURI, witIdentityTokenURI, witIdentityBaseURI,false)
	        );
	    } else {
	        log.error("Cannot configure WSO2ConnectionFactory id or secret null");
	    }
	}

    @Override
    public UserIdSource getUserIdSource() {
        return new AuthenticationNameUserIdSource();
    }

    @Override
    public UsersConnectionRepository getUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator) {
    	CustomSocialUsersConnectionRepository repo = new CustomSocialUsersConnectionRepository(socialUserConnectionRepository, connectionFactoryLocator);
    	repo.setConnectionSignUp(autoSignUpHandler);
    	return repo;
    }
    
    @Bean
    public SignInAdapter signInAdapter() {
        return new CustomSignInAdapter();
    }
    
    @Bean
    public ProviderSignInController providerSignInController(ConnectionFactoryLocator connectionFactoryLocator, UsersConnectionRepository usersConnectionRepository, SignInAdapter signInAdapter) throws Exception {
        ProviderSignInController providerSignInController = new ProviderSignInController(connectionFactoryLocator, usersConnectionRepository, signInAdapter);
        providerSignInController.setApplicationUrl("http://linux-dev-01:8080/uaa");
        //providerSignInController.setSignUpUrl("/uaa/social/signup");
        return providerSignInController;
    }

    
	

}
