package com.mycompany.myapp.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.social.connect.*;

import com.mycompany.myapp.domain.SocialUserConnection;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomSocialUsersConnectionRepository implements UsersConnectionRepository {

	private final Logger log = LoggerFactory.getLogger(CustomSocialUsersConnectionRepository.class);
	
    private SocialUserConnectionRepository socialUserConnectionRepository;

    private ConnectionFactoryLocator connectionFactoryLocator;
    private ConnectionSignUp connectionSignUp;

    public CustomSocialUsersConnectionRepository(SocialUserConnectionRepository socialUserConnectionRepository, ConnectionFactoryLocator connectionFactoryLocator) {
        this.socialUserConnectionRepository = socialUserConnectionRepository;
        this.connectionFactoryLocator = connectionFactoryLocator;
        
    }
    
	public void setConnectionSignUp(ConnectionSignUp connectionSignUp) {
		this.connectionSignUp = connectionSignUp;
    }

    @Override
    public List<String> findUserIdsWithConnection(Connection<?> connection) {
        ConnectionKey key = connection.getKey();
        List<SocialUserConnection> socialUserConnections =
            socialUserConnectionRepository.findAllByProviderIdAndProviderUserId(key.getProviderId(), key.getProviderUserId());
        if (socialUserConnections.size()==0 && connectionSignUp != null){
        	log.debug("No user connection found - create user");
        	String newUserId = connectionSignUp.execute(connection);
        	//create connection	
        	this.createConnectionRepository(newUserId).addConnection(connection);
        	return Arrays.asList(newUserId);
        }
        
        return socialUserConnections.stream()
            .map(SocialUserConnection::getUserId)
            .collect(Collectors.toList());
    };

    @Override
    public Set<String> findUserIdsConnectedTo(String providerId, Set<String> providerUserIds) {
        List<SocialUserConnection> socialUserConnections =
            socialUserConnectionRepository.findAllByProviderIdAndProviderUserIdIn(providerId, providerUserIds);
        return socialUserConnections.stream()
            .map(SocialUserConnection::getUserId)
            .collect(Collectors.toSet());
    };

    @Override
    public ConnectionRepository createConnectionRepository(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        return new CustomSocialConnectionRepository(userId, socialUserConnectionRepository, connectionFactoryLocator);
    };
	
	
}
